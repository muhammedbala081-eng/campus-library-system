package app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatabaseSeeder {

    private static final String[][] FACULTIES = {
        {"Computer Science & Software", "CSC"},
        {"Electrical & Electronics Engineering", "EEE"},
        {"Mechanical Engineering", "MEE"},
        {"Civil Engineering", "CVE"},
        {"Biomedical Sciences", "BMS"},
        {"Medicine & Surgery", "MED"},
        {"Pharmacy & Pharmacology", "PHA"},
        {"Nursing Science", "NUR"},
        {"Business Administration", "BUS"},
        {"Accounting & Finance", "ACC"},
        {"Economics", "ECO"},
        {"Law & Jurisprudence", "LAW"},
        {"Mass Communication", "MAC"},
        {"Architecture & Building", "ARC"},
        {"Physics & Electronics", "PHY"},
        {"Mathematics & Statistics", "MTH"},
        {"Pure & Applied Chemistry", "CHM"},
        {"Psychology & Behavioral Science", "PSY"}
    };

    private static final String[] FIRST_NAMES = {
        "Ahmed", "Chinedu", "Fatima", "Ibrahim", "Zainab", "Olumide", "Ngozi",
        "Blessing", "Emeka", "Amina", "Yusuf", "Adebayo", "Hauwa", "Tunde",
        "Folake", "Kelechi", "Maryam", "Suleiman", "Chioma", "Aliyu",
        "Grace", "Daniel", "Samuel", "David", "Esther", "Sarah", "Victor"
    };

    private static final String[] LAST_NAMES = {
        "Bello", "Okafor", "Adeyemi", "Mohammed", "Nwosu", "Sule", "Balogun",
        "Eze", "Abubakar", "Danjuma", "Okonkwo", "Mustapha", "Adeleke", "Garba",
        "Sanusi", "Ogunleye", "Obi", "Kalu", "Gambo", "Shehu", "Johnson", "Peters"
    };

    private static final String[] BOOK_PREFIXES = {
        "Introduction to", "Fundamentals of", "Advanced Principles of",
        "Modern Handbook of", "Applied", "Essential Guide to", "The Theory of",
        "Practical Case Studies in", "Contemporary", "Mastering",
        "Foundation Course in", "Numerical Methods in", "Ethics and Practice of"
    };

    private static final String[] PUBLISHERS = {
        "Oxford University Press", "Cambridge University Press", "Pearson Education",
        "McGraw-Hill Higher Education", "Wiley Academic", "Springer Nature",
        "Elsevier Science", "MIT Press", "Routledge", "Academic Press"
    };

    public static synchronized void seedIfEmpty(Connection conn) {
        try {
            if (hasEnoughData(conn)) {
                return;
            }

            System.out.println("Populating 1,000 books across faculties, 150 students, 30 faculty members, and active loans...");
            conn.setAutoCommit(false);

            List<Integer> categoryIds = seedCategories(conn);
            List<Integer> memberIds = seedMembers(conn, 150, 30);
            List<Integer> copyIds = seedBooksAndCopies(conn, categoryIds, 1000);
            seedActiveLoans(conn, memberIds, copyIds, 25);

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Seeding completed successfully! Database ready.");
        } catch (Exception e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            System.err.println("Seeding notice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean hasEnoughData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books")) {
            if (rs.next()) {
                return rs.getInt(1) >= 50;
            }
        }
        return false;
    }

    private static List<Integer> seedCategories(Connection conn) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String insertSql = "INSERT OR IGNORE INTO categories (name, code, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (String[] cat : FACULTIES) {
                ps.setString(1, cat[0]);
                ps.setString(2, cat[1]);
                ps.setString(3, "Academic collection for " + cat[0]);
                ps.executeUpdate();
            }
        }

        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT category_id FROM categories ORDER BY category_id ASC")) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private static List<Integer> seedMembers(Connection conn, int studentCount, int facultyCount) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        Random rng = new Random(1001);

        String sql = "INSERT INTO members (student_staff_id, full_name, email, phone, membership_type, max_books_allowed, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= studentCount; i++) {
                String first = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[rng.nextInt(LAST_NAMES.length)];
                String studentId = String.format("STU/2026/%04d", i);
                String email = String.format("stu%03d@campus.edu.ng", i);
                String phone = String.format("0803%07d", rng.nextInt(10_000_000));

                ps.setString(1, studentId);
                ps.setString(2, first + " " + last);
                ps.setString(3, email);
                ps.setString(4, phone);
                ps.setString(5, "STUDENT");
                ps.setInt(6, 3);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) ids.add(rs.getInt(1));
                }
            }

            for (int i = 1; i <= facultyCount; i++) {
                String first = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[rng.nextInt(LAST_NAMES.length)];
                String staffId = String.format("FAC/2026/%03d", i);
                String email = String.format("prof.%s%d@campus.edu.ng", last.toLowerCase(), i);
                String phone = String.format("0802%07d", rng.nextInt(10_000_000));

                ps.setString(1, staffId);
                ps.setString(2, "Dr. " + first + " " + last);
                ps.setString(3, email);
                ps.setString(4, phone);
                ps.setString(5, "FACULTY");
                ps.setInt(6, 5);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private static List<Integer> seedBooksAndCopies(Connection conn, List<Integer> catIds, int totalBooks) throws SQLException {
        List<Integer> copyIds = new ArrayList<>();
        Random rng = new Random(2026);

        String bookSql = "INSERT INTO books (isbn, title, author, publisher, publication_year, category_id, total_copies, available_copies) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String copySql = "INSERT INTO book_copies (book_id, barcode, status, condition_notes, acquired_date) " +
                         "VALUES (?, ?, 'AVAILABLE', 'Good condition', ?)";

        try (PreparedStatement psBook = conn.prepareStatement(bookSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psCopy = conn.prepareStatement(copySql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 1; i <= totalBooks; i++) {
                int catIndex = rng.nextInt(catIds.size());
                int catId = catIds.get(catIndex);
                String facultyName = FACULTIES[catIndex][0];
                String prefix = BOOK_PREFIXES[rng.nextInt(BOOK_PREFIXES.length)];
                String title = prefix + " " + facultyName + " Vol. " + ((i % 5) + 1);
                String author = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rng.nextInt(LAST_NAMES.length)];
                String publisher = PUBLISHERS[rng.nextInt(PUBLISHERS.length)];
                int year = 2012 + rng.nextInt(13);
                String isbn = String.format("978-0-%03d-%05d-%d", (i % 899) + 100, i, (i % 9));

                int copiesCount = 2 + rng.nextInt(3);

                psBook.setString(1, isbn);
                psBook.setString(2, title);
                psBook.setString(3, author);
                psBook.setString(4, publisher);
                psBook.setInt(5, year);
                psBook.setInt(6, catId);
                psBook.setInt(7, copiesCount);
                psBook.setInt(8, copiesCount);
                psBook.executeUpdate();

                int generatedBookId;
                try (ResultSet rs = psBook.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedBookId = rs.getInt(1);
                    } else {
                        continue;
                    }
                }

                for (int c = 1; c <= copiesCount; c++) {
                    String barcode = String.format("BC-%04d-%02d", i, c);
                    LocalDate acquired = LocalDate.now().minusDays(rng.nextInt(300));
                    psCopy.setInt(1, generatedBookId);
                    psCopy.setString(2, barcode);
                    psCopy.setString(3, acquired.toString());
                    psCopy.executeUpdate();

                    try (ResultSet rs = psCopy.getGeneratedKeys()) {
                        if (rs.next()) {
                            copyIds.add(rs.getInt(1));
                        }
                    }
                }
            }
        }
        return copyIds;
    }

    private static void seedActiveLoans(Connection conn, List<Integer> memberIds, List<Integer> copyIds, int count) throws SQLException {
        if (memberIds.isEmpty() || copyIds.size() < count) return;

        Random rng = new Random(777);
        LocalDate today = LocalDate.now();

        String insertLoan = "INSERT INTO loans (copy_id, member_id, issued_at, due_date, status, remarks) " +
                            "VALUES (?, ?, ?, ?, ?, 'Sample active borrow')";
        String updateCopy = "UPDATE book_copies SET status = 'BORROWED' WHERE copy_id = ?";
        String updateBook = "UPDATE books SET available_copies = available_copies - 1 " +
                            "WHERE book_id = (SELECT book_id FROM book_copies WHERE copy_id = ?)";

        try (PreparedStatement psLoan = conn.prepareStatement(insertLoan);
             PreparedStatement psCopy = conn.prepareStatement(updateCopy);
             PreparedStatement psBook = conn.prepareStatement(updateBook)) {

            for (int i = 0; i < count; i++) {
                int copyId = copyIds.get(i);
                int memberId = memberIds.get(rng.nextInt(memberIds.size()));

                boolean overdue = (i % 2 == 0);
                String issuedAt = overdue ? today.minusDays(22).toString() + " 10:00:00" : today.minusDays(4).toString() + " 11:30:00";
                String dueDate = overdue ? today.minusDays(8).toString() : today.plusDays(10).toString();
                String status = overdue ? "OVERDUE" : "ACTIVE";

                psLoan.setInt(1, copyId);
                psLoan.setInt(2, memberId);
                psLoan.setString(3, issuedAt);
                psLoan.setString(4, dueDate);
                psLoan.setString(5, status);
                psLoan.executeUpdate();

                psCopy.setInt(1, copyId);
                psCopy.executeUpdate();

                psBook.setInt(1, copyId);
                psBook.executeUpdate();
            }
        }
    }
}
