package app.model;

import java.time.LocalDateTime;

public class Member {
    private int memberId;
    private String studentStaffId;
    private String fullName;
    private String email;
    private String phone;
    private String membershipType; // 'STUDENT', 'FACULTY', 'STAFF'
    private int maxBooksAllowed;
    private String status;         // 'ACTIVE', 'SUSPENDED'
    private LocalDateTime createdAt;

    public Member() {}

    public Member(int memberId, String studentStaffId, String fullName, String email, String phone,
                  String membershipType, int maxBooksAllowed, String status, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.studentStaffId = studentStaffId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.membershipType = membershipType;
        this.maxBooksAllowed = maxBooksAllowed;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getStudentStaffId() { return studentStaffId; }
    public void setStudentStaffId(String studentStaffId) { this.studentStaffId = studentStaffId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public int getMaxBooksAllowed() { return maxBooksAllowed; }
    public void setMaxBooksAllowed(int maxBooksAllowed) { this.maxBooksAllowed = maxBooksAllowed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return fullName + " (" + studentStaffId + ") - " + membershipType;
    }
}
