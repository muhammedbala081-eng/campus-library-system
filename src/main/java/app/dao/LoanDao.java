package app.dao;

import app.model.Loan;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface LoanDao {
    /**
     * Executes the atomic borrow transaction required by CSC 508:
     * 1. Checks that the copy is AVAILABLE
     * 2. Inserts a new loan record
     * 3. Sets the physical copy to BORROWED
     * 4. Decrements book's available_copies count
     * All inside a single commit/rollback transaction.
     */
    Loan issueLoanTransaction(int memberId, int copyId, LocalDate dueDate) throws SQLException;

    /**
     * Executes the atomic return transaction:
     * 1. Marks loan as RETURNED
     * 2. Sets copy status back to AVAILABLE
     * 3. Increments book's available_copies count
     * 4. Assesses overdue fine if returned past dueDate
     */
    void returnLoanTransaction(int loanId, BigDecimal finePerDay) throws SQLException;

    List<Loan> findActiveLoans() throws SQLException;
    List<Loan> findLoansByMemberId(int memberId) throws SQLException;
}
