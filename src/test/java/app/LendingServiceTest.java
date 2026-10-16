package app;

import app.dao.*;
import app.model.Loan;
import app.service.LendingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class LendingServiceTest {

    private static LendingService lendingService;

    @BeforeAll
    static void setup() throws SQLException {
        // Initialize SQLite schema and seed records
        DatabaseManager.initializeDatabase();

        MemberDao memberDao = new SqliteMemberDao();
        BookDao bookDao = new SqliteBookDao();
        LoanDao loanDao = new SqliteLoanDao();

        lendingService = new LendingService(memberDao, bookDao, loanDao);
    }

    @Test
    void testIssueBookSuccessAndPreventDoubleBorrow() throws SQLException {
        String studentId = "CSC/2026/001";
        String barcode = "BC-CS-0001";

        // 1. Issue the book to the student
        Loan loan = lendingService.issueBook(studentId, barcode);
        assertNotNull(loan);
        assertTrue(loan.getLoanId() > 0);
        assertEquals("ACTIVE", loan.getStatus());

        // 2. Attempt to borrow the same copy again -> Must fail with IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            lendingService.issueBook(studentId, barcode);
        });

        assertTrue(exception.getMessage().contains("not available") || exception.getMessage().contains("BORROWED"));

        // 3. Return the book so the database remains clean for future runs
        lendingService.returnBook(loan.getLoanId());
    }
}
