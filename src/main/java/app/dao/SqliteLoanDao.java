package app.dao;

import app.model.Loan;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class SqliteLoanDao implements LoanDao {

    @Override
    public Loan issueLoanTransaction(int memberId, int copyId, LocalDate dueDate) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Begin ACID transaction
            try {
                // 1. Check physical copy status and find owning book_id
                int bookId;
                String status;
                String checkSql = "SELECT book_id, status FROM book_copies WHERE copy_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, copyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Book copy ID " + copyId + " does not exist.");
                        }
                        bookId = rs.getInt("book_id");
                        status = rs.getString("status");
                    }
                }

                if (!"AVAILABLE".equalsIgnoreCase(status)) {
                    throw new SQLException("Copy is currently not available (Status: " + status + ").");
                }

                // 2. Insert into loans table
                int loanId;
                String insertLoan = "INSERT INTO loans (copy_id, member_id, due_date, status) VALUES (?, ?, ?, 'ACTIVE')";
                try (PreparedStatement ps = conn.prepareStatement(insertLoan, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, copyId);
                    ps.setInt(2, memberId);
                    ps.setString(3, dueDate.toString());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Failed to retrieve generated loan ID.");
                        loanId = keys.getInt(1);
                    }
                }

                // 3. Mark physical copy as BORROWED
                String updateCopy = "UPDATE book_copies SET status = 'BORROWED' WHERE copy_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateCopy)) {
                    ps.setInt(1, copyId);
                    ps.executeUpdate();
                }

                // 4. Decrement available copies in books catalog
                String updateBook = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0";
                try (PreparedStatement ps = conn.prepareStatement(updateBook)) {
                    ps.setInt(1, bookId);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Catalog shows zero available copies for this book.");
                    }
                }

                // Commit entire transaction
                conn.commit();

                Loan loan = new Loan();
                loan.setLoanId(loanId);
                loan.setCopyId(copyId);
                loan.setMemberId(memberId);
                loan.setIssuedAt(LocalDateTime.now());
                loan.setDueDate(dueDate);
                loan.setStatus("ACTIVE");
                return loan;

            } catch (SQLException ex) {
                conn.rollback(); // Roll back all 4 operations on any error
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void returnLoanTransaction(int loanId, BigDecimal finePerDay) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Fetch loan details
                int copyId;
                int memberId;
                LocalDate dueDate;
                String status;
                String fetchSql = "SELECT copy_id, member_id, due_date, status FROM loans WHERE loan_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(fetchSql)) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Loan ID " + loanId + " not found.");
                        copyId = rs.getInt("copy_id");
                        memberId = rs.getInt("member_id");
                        dueDate = LocalDate.parse(rs.getString("due_date"));
                        status = rs.getString("status");
                    }
                }

                if (!"ACTIVE".equalsIgnoreCase(status) && !"OVERDUE".equalsIgnoreCase(status)) {
                    throw new SQLException("Loan is already closed (Status: " + status + ").");
                }

                // 2. Find book_id from copy
                int bookId;
                try (PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM book_copies WHERE copy_id = ?")) {
                    ps.setInt(1, copyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        bookId = rs.getInt("book_id");
                    }
                }

                // 3. Mark loan as RETURNED with timestamp
                String updateLoan = "UPDATE loans SET status = 'RETURNED', returned_at = CURRENT_TIMESTAMP WHERE loan_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateLoan)) {
                    ps.setInt(1, loanId);
                    ps.executeUpdate();
                }

                // 4. Mark physical copy back to AVAILABLE
                try (PreparedStatement ps = conn.prepareStatement("UPDATE book_copies SET status = 'AVAILABLE' WHERE copy_id = ?")) {
                    ps.setInt(1, copyId);
                    ps.executeUpdate();
                }

                // 5. Increment catalog available copies
                try (PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                // 6. Assess overdue fine if returned past due date
                LocalDate returnDate = LocalDate.now();
                if (returnDate.isAfter(dueDate)) {
                    long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
                    BigDecimal fineAmount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

                    String insertFine = "INSERT INTO fines (loan_id, member_id, amount, status) VALUES (?, ?, ?, 'UNPAID')";
                    try (PreparedStatement ps = conn.prepareStatement(insertFine)) {
                        ps.setInt(1, loanId);
                        ps.setInt(2, memberId);
                        ps.setBigDecimal(3, fineAmount);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public List<Loan> findActiveLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql = "SELECT l.*, b.title AS book_title, bc.barcode AS copy_barcode, m.full_name AS member_name " +
                     "FROM loans l " +
                     "JOIN book_copies bc ON l.copy_id = bc.copy_id " +
                     "JOIN books b ON bc.book_id = b.book_id " +
                     "JOIN members m ON l.member_id = m.member_id " +
                     "WHERE l.status IN ('ACTIVE', 'OVERDUE') ORDER BY l.due_date ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    @Override
    public List<Loan> findLoansByMemberId(int memberId) throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql = "SELECT l.*, b.title AS book_title, bc.barcode AS copy_barcode, m.full_name AS member_name " +
                     "FROM loans l " +
                     "JOIN book_copies bc ON l.copy_id = bc.copy_id " +
                     "JOIN books b ON bc.book_id = b.book_id " +
                     "JOIN members m ON l.member_id = m.member_id " +
                     "WHERE l.member_id = ? ORDER BY l.issued_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLoan(rs));
            }
        }
        return list;
    }

        private Loan mapLoan(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setLoanId(rs.getInt("loan_id"));
        l.setCopyId(rs.getInt("copy_id"));
        l.setMemberId(rs.getInt("member_id"));

        // Safely parse issued_at from SQLite text format
        String issued = rs.getString("issued_at");
        if (issued != null && !issued.isBlank()) {
            try {
                String clean = issued.replace(" ", "T");
                if (clean.length() >= 19) {
                    l.setIssuedAt(LocalDateTime.parse(clean.substring(0, 19)));
                }
            } catch (Exception ignored) {}
        }

        // Safely parse due_date
        String due = rs.getString("due_date");
        if (due != null && !due.isBlank()) {
            try {
                l.setDueDate(LocalDate.parse(due.substring(0, 10)));
            } catch (Exception ignored) {}
        }

        // Safely parse returned_at
        String ret = rs.getString("returned_at");
        if (ret != null && !ret.isBlank()) {
            try {
                String clean = ret.replace(" ", "T");
                if (clean.length() >= 19) {
                    l.setReturnedAt(LocalDateTime.parse(clean.substring(0, 19)));
                }
            } catch (Exception ignored) {}
        }

        l.setStatus(rs.getString("status"));
        l.setRemarks(rs.getString("remarks"));
        l.setBookTitle(rs.getString("book_title"));
        l.setCopyBarcode(rs.getString("copy_barcode"));
        l.setMemberName(rs.getString("member_name"));
        return l;
    }

}
