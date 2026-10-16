package app.dao;

import app.model.Member;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MemberDao {
    Optional<Member> findById(int memberId) throws SQLException;
    Optional<Member> findByStudentStaffId(String studentStaffId) throws SQLException;
    List<Member> findAll() throws SQLException;
    void save(Member member) throws SQLException;
    int countActiveLoans(int memberId) throws SQLException;
    BigDecimal getTotalUnpaidFines(int memberId) throws SQLException;
}
