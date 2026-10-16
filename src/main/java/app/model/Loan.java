package app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Loan {
    private int loanId;
    private int copyId;
    private int memberId;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;
    private String status; // 'ACTIVE', 'RETURNED', 'OVERDUE'
    private String remarks;

    // Display helpers for UI tables
    private String bookTitle;
    private String copyBarcode;
    private String memberName;

    public Loan() {}

    public Loan(int loanId, int copyId, int memberId, LocalDateTime issuedAt,
                LocalDate dueDate, LocalDateTime returnedAt, String status, String remarks) {
        this.loanId = loanId;
        this.copyId = copyId;
        this.memberId = memberId;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.returnedAt = returnedAt;
        this.status = status;
        this.remarks = remarks;
    }

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }

    public int getCopyId() { return copyId; }
    public void setCopyId(int copyId) { this.copyId = copyId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getCopyBarcode() { return copyBarcode; }
    public void setCopyBarcode(String copyBarcode) { this.copyBarcode = copyBarcode; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
