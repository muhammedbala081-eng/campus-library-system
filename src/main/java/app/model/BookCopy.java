package app.model;

import java.time.LocalDate;

public class BookCopy {
    private int copyId;
    private int bookId;
    private String barcode;
    private String status; // 'AVAILABLE', 'BORROWED', 'MAINTENANCE', 'LOST'
    private String conditionNotes;
    private LocalDate acquiredDate;
    private String bookTitle; // Helper for display

    public BookCopy() {}

    public BookCopy(int copyId, int bookId, String barcode, String status, String conditionNotes, LocalDate acquiredDate) {
        this.copyId = copyId;
        this.bookId = bookId;
        this.barcode = barcode;
        this.status = status;
        this.conditionNotes = conditionNotes;
        this.acquiredDate = acquiredDate;
    }

    public int getCopyId() { return copyId; }
    public void setCopyId(int copyId) { this.copyId = copyId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }

    public LocalDate getAcquiredDate() { return acquiredDate; }
    public void setAcquiredDate(LocalDate acquiredDate) { this.acquiredDate = acquiredDate; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
}
