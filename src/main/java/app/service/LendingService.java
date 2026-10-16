package app.service;

import app.dao.BookDao;
import app.dao.LoanDao;
import app.dao.MemberDao;
import app.model.BookCopy;
import app.model.Loan;
import app.model.Member;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LendingService {
    private final MemberDao memberDao;
    private final BookDao bookDao;
    private final LoanDao loanDao;
    private final BigDecimal finePerDay;

    public LendingService(MemberDao memberDao, BookDao bookDao, LoanDao loanDao) {
        this.memberDao = memberDao;
        this.bookDao = bookDao;
        this.loanDao = loanDao;
        this.finePerDay = new BigDecimal("50.00"); // 50 Naira per overdue day
    }

    /**
     * Issues a book to a member after validating all business policies.
     */
    public Loan issueBook(String studentStaffId, String copyBarcode) throws SQLException, IllegalStateException {
        // 1. Validate Member
        Member member = memberDao.findByStudentStaffId(studentStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + studentStaffId));

        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new IllegalStateException("Member account is suspended or inactive.");
        }

        // 2. Validate Borrowing Quota
        int activeLoans = memberDao.countActiveLoans(member.getMemberId());
        if (activeLoans >= member.getMaxBooksAllowed()) {
            throw new IllegalStateException("Borrowing limit reached (" + member.getMaxBooksAllowed() + " book max).");
        }

        // 3. Check for Outstanding Fines
        BigDecimal unpaidFines = memberDao.getTotalUnpaidFines(member.getMemberId());
        if (unpaidFines.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Member has unpaid fines of ₦" + unpaidFines + ". Clear fines first.");
        }

        // 4. Validate Physical Copy
        BookCopy copy = bookDao.findCopyByBarcode(copyBarcode)
                .orElseThrow(() -> new IllegalArgumentException("No book copy found with barcode: " + copyBarcode));

        if (!"AVAILABLE".equalsIgnoreCase(copy.getStatus())) {
            throw new IllegalStateException("Book copy is not available (Status: " + copy.getStatus() + ").");
        }

        // 5. Calculate Due Date based on Membership Type
        LocalDate dueDate = calculateDueDate(member.getMembershipType());

        // 6. Execute atomic transaction in DAO
        return loanDao.issueLoanTransaction(member.getMemberId(), copy.getCopyId(), dueDate);
    }

    /**
     * Returns a book copy, updates inventory, and assesses fines if overdue.
     */
    public void returnBook(int loanId) throws SQLException {
        loanDao.returnLoanTransaction(loanId, finePerDay);
    }

    public List<Loan> getActiveLoans() throws SQLException {
        return loanDao.findActiveLoans();
    }

    private LocalDate calculateDueDate(String membershipType) {
        LocalDate today = LocalDate.now();
        if ("FACULTY".equalsIgnoreCase(membershipType)) {
            return today.plusDays(30);
        } else if ("STAFF".equalsIgnoreCase(membershipType)) {
            return today.plusDays(21);
        } else {
            return today.plusDays(14); // Students get 14 days
        }
    }
}
