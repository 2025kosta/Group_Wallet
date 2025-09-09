package main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

import main.db.DbUtil;
import main.domain.User;

public class UserRepository {

	public User save(User user) {
		String sql = "INSERT INTO users (name, email, phone, created_at) VALUES (?, ?, ?, ?)";

		try (Connection conn = DbUtil.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			pstmt.setString(1, user.getName());
			pstmt.setString(2, user.getEmail());
			pstmt.setString(3, user.getPhone());
			pstmt.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));

			int affectedRows = pstmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("사용자 생성 실패: 영향 받은 행이 없습니다.");
			}

			try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					long newId = generatedKeys.getLong(1);
					return User.fromDB(newId, user.getName(), user.getEmail(), user.getPhone(), user.getCreatedAt());
				} else {
					throw new SQLException("사용자 생성 실패: ID를 가져올 수 없습니다.");
				}
			}
		} catch (SQLException e) {
			System.err.println("사용자 저장 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Optional<User> findByEmail(String email) {
		String sql = "SELECT id, name, email, phone, created_at FROM users WHERE email = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, email);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(mapRowToUser(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("이메일로 사용자 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public Optional<User> findById(long id) {
		String sql = "SELECT id, name, email, phone, created_at FROM users WHERE id = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, id);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(mapRowToUser(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("ID로 사용자 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public void deleteById(long userId) {
		String sql = "DELETE FROM users WHERE id = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, userId);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.err.println("사용자 삭제 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private User mapRowToUser(ResultSet rs) throws SQLException {
		long id = rs.getLong("id");
		String name = rs.getString("name");
		String email = rs.getString("email");
		String phone = rs.getString("phone");
		java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

		return User.fromDB(id, name, email, phone, createdAt);
	}
}
