package app.dao;

import app.model.Member;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteMemberDao implements MemberDao {

    @Override
    public Optional<Member> findById(int memberId) throws SQLException {
        String sql = "SELECT * FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> findByStudentStaffId(String id) throws SQLException {
        String sql = "SELECT * FROM members WHERE UPPER(student_staff_id) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Member> findAll() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY full_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public void save(Member m) throws SQLException {
        String sql = "INSERT INTO members (student_staff_id, full_name, email, phone, membership_type, max_books_allowed, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getStudentStaffId().trim());
            ps.setString(2, m.getFullName().trim());
            ps.setString(3, m.getEmail().trim());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getMembershipType());
            ps.setInt(6, m.getMaxBooksAllowed());
            ps.setString(7, m.getStatus() != null ? m.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setMemberId(keys.getInt(1));
            }
        }
    }

    @Override
    public int countActiveLoans(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public BigDecimal getTotalUnpaidFines(int memberId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount - paid_amount), 0.0) FROM fines WHERE member_id = ? AND status IN ('UNPAID', 'PARTIALLY_PAID')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

        private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setStudentStaffId(rs.getString("student_staff_id"));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setMembershipType(rs.getString("membership_type"));
        m.setMaxBooksAllowed(rs.getInt("max_books_allowed"));
        m.setStatus(rs.getString("status"));

        String created = rs.getString("created_at");
        if (created != null && !created.isBlank()) {
            try {
                String clean = created.replace(" ", "T");
                if (clean.length() >= 19) {
                    m.setCreatedAt(java.time.LocalDateTime.parse(clean.substring(0, 19)));
                }
            } catch (Exception ignored) {}
        }
        return m;
    }

}
