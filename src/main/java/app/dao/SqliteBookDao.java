package app.dao;

import app.model.Book;
import app.model.BookCopy;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteBookDao implements BookDao {

    @Override
    public Optional<Book> findById(int bookId) throws SQLException {
        String sql = "SELECT b.*, c.name AS category_name FROM books b " +
                     "JOIN categories c ON b.category_id = c.category_id WHERE b.book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapBook(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) throws SQLException {
        String sql = "SELECT b.*, c.name AS category_name FROM books b " +
                     "JOIN categories c ON b.category_id = c.category_id WHERE b.isbn = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapBook(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.*, c.name AS category_name FROM books b " +
                     "JOIN categories c ON b.category_id = c.category_id ORDER BY b.title ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapBook(rs));
        }
        return list;
    }

    @Override
    public List<Book> search(String keyword) throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.*, c.name AS category_name FROM books b " +
                     "JOIN categories c ON b.category_id = c.category_id " +
                     "WHERE b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ? ORDER BY b.title ASC";
        String pattern = "%" + keyword.trim() + "%";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBook(rs));
            }
        }
        return list;
    }

    @Override
    public void save(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, author, publisher, publication_year, category_id, total_copies, available_copies) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn().trim());
            ps.setString(2, book.getTitle().trim());
            ps.setString(3, book.getAuthor().trim());
            ps.setString(4, book.getPublisher());
            ps.setInt(5, book.getPublicationYear());
            ps.setInt(6, book.getCategoryId());
            ps.setInt(7, book.getTotalCopies());
            ps.setInt(8, book.getAvailableCopies());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setBookId(keys.getInt(1));
            }
        }
    }

    @Override
    public List<BookCopy> findCopiesByBookId(int bookId) throws SQLException {
        List<BookCopy> list = new ArrayList<>();
        String sql = "SELECT bc.*, b.title AS book_title FROM book_copies bc " +
                     "JOIN books b ON bc.book_id = b.book_id WHERE bc.book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCopy(rs));
            }
        }
        return list;
    }

    @Override
    public Optional<BookCopy> findCopyByBarcode(String barcode) throws SQLException {
        String sql = "SELECT bc.*, b.title AS book_title FROM book_copies bc " +
                     "JOIN books b ON bc.book_id = b.book_id WHERE UPPER(bc.barcode) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapCopy(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public void addCopy(int bookId, String barcode, String condition) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String insertCopy = "INSERT INTO book_copies (book_id, barcode, status, condition_notes) VALUES (?, ?, 'AVAILABLE', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertCopy)) {
                    ps.setInt(1, bookId);
                    ps.setString(2, barcode.trim());
                    ps.setString(3, condition);
                    ps.executeUpdate();
                }

                String updateBook = "UPDATE books SET total_copies = total_copies + 1, available_copies = available_copies + 1 WHERE book_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateBook)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
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

    private Book mapBook(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("book_id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setPublisher(rs.getString("publisher"));
        b.setPublicationYear(rs.getInt("publication_year"));
        b.setCategoryId(rs.getInt("category_id"));
        b.setCategoryName(rs.getString("category_name"));
        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailableCopies(rs.getInt("available_copies"));
        return b;
    }

        private BookCopy mapCopy(ResultSet rs) throws SQLException {
        BookCopy c = new BookCopy();
        c.setCopyId(rs.getInt("copy_id"));
        c.setBookId(rs.getInt("book_id"));
        c.setBarcode(rs.getString("barcode"));
        c.setStatus(rs.getString("status"));
        c.setConditionNotes(rs.getString("condition_notes"));
        c.setBookTitle(rs.getString("book_title"));
        
        String d = rs.getString("acquired_date");
        if (d != null && !d.isBlank()) {
            try {
                // Takes the 'YYYY-MM-DD' part cleanly
                c.setAcquiredDate(LocalDate.parse(d.substring(0, 10)));
            } catch (Exception ignored) {}
        }
        return c;
    }

}
