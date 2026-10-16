package app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Fine {
    private int fineId;
    private int loanId;
    private int memberId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status; // 'UNPAID', 'PARTIALLY_PAID', 'PAID', 'WAIVED'
    private LocalDateTime assessedAt;
    private LocalDateTime paidAt;

    public Fine() {}

    public Fine(int fineId, int loanId, int memberId, BigDecimal amount,
                BigDecimal paidAmount, String status, LocalDateTime assessedAt, LocalDateTime paidAt) {
        this.fineId = fineId;
        this.loanId = loanId;
        this.memberId = memberId;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.assessedAt = assessedAt;
        this.paidAt = paidAt;
    }

    public int getFineId() { return fineId; }
    public void setFineId(int fineId) { this.fineId = fineId; }

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
