package main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.db.DbUtil;
import main.domain.GroupMember;
import main.enums.MemberRole;

public class GroupRepository {

	public void save(GroupMember member) {
		String sql = "INSERT INTO group_member (account_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, member.getAccountId());
			pstmt.setLong(2, member.getUserId());
			pstmt.setString(3, member.getRole().name());
			pstmt.setTimestamp(4, Timestamp.valueOf(member.getJoinedAt()));

			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.err.println("멤버 저장 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void save(GroupMember member, Connection conn) {
		String sql = "INSERT INTO group_member (account_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)";

		// try-with-resources에서 Connection 생성을 제거합니다. PreparedStatement만 관리합니다.
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, member.getAccountId());
			pstmt.setLong(2, member.getUserId());
			pstmt.setString(3, member.getRole().name());
			pstmt.setTimestamp(4, Timestamp.valueOf(member.getJoinedAt()));

			pstmt.executeUpdate();

		} catch (SQLException e) {
			throw new RuntimeException("멤버 저장(트랜잭션) 중 오류 발생", e);
		}
	}

	public Optional<GroupMember> findByAccountIdAndUserId(long accountId, long userId) {
		String sql = "SELECT id, account_id, user_id, role, joined_at FROM group_member WHERE account_id = ? AND user_id = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, accountId);
			pstmt.setLong(2, userId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(mapRowToGroupMember(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("멤버 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public List<GroupMember> findByAccountId(long accountId) {
		String sql = "SELECT id, account_id, user_id, role, joined_at FROM group_member "
				+ "WHERE account_id = ? ORDER BY FIELD(role, 'OWNER', 'MEMBER'), joined_at ASC";
		List<GroupMember> members = new ArrayList<>();

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, accountId);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					members.add(mapRowToGroupMember(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("계좌의 모든 멤버 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return members;
	}

	public void updateRole(long memberId, MemberRole newRole) {
		String sql = "UPDATE group_member SET role = ? WHERE id = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, newRole.name());
			pstmt.setLong(2, memberId);

			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.err.println("멤버 역할 변경 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean isOwner(long accountId, long userId) {
		String sql = "SELECT 1 FROM group_member WHERE account_id = ? AND user_id = ? AND role = 'OWNER' LIMIT 1";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, accountId);
			pstmt.setLong(2, userId);

			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			System.err.println("OWNER 여부 확인 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("OWNER 여부 확인 중 DB 오류", e);
		}
	}

	public void delete(long memberId) {
		String sql = "DELETE FROM group_member WHERE id = ?";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, memberId);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.err.println("멤버 삭제 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public long countOwnersByAccountId(long accountId) {
		String sql = "SELECT COUNT(*) FROM group_member WHERE account_id = ? AND role = 'OWNER'";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, accountId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getLong(1);
				}
			}
		} catch (SQLException e) {
			System.err.println("OWNER 수 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	private GroupMember mapRowToGroupMember(ResultSet rs) throws SQLException {
		long id = rs.getLong("id");
		long accountId = rs.getLong("account_id");
		long userId = rs.getLong("user_id");
		MemberRole role = MemberRole.valueOf(rs.getString("role"));
		java.time.LocalDateTime joinedAt = rs.getTimestamp("joined_at").toLocalDateTime();
		return GroupMember.fromDB(id, accountId, userId, role, joinedAt);
	}

	public Optional<GroupMember> findByAccountIdAndUserId(long accountId, long userId, Connection conn) {
		String sql = "SELECT id, account_id, user_id, role, joined_at FROM group_member WHERE account_id = ? AND user_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			ps.setLong(2, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return Optional.of(mapRowToGroupMember(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException("멤버 조회(트랜잭션) 중 오류", e);
		}
		return Optional.empty();
	}

	public long countOwnersByAccountId(long accountId, Connection conn) {
		String sql = "SELECT COUNT(*) FROM group_member WHERE account_id = ? AND role = 'OWNER'";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getLong(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("OWNER 수 조회(트랜잭션) 중 오류", e);
		}
		return 0;
	}

	public void updateRole(long memberId, MemberRole newRole, Connection conn) {
		String sql = "UPDATE group_member SET role = ? WHERE id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, newRole.name());
			ps.setLong(2, memberId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("멤버 역할 변경(트랜잭션) 중 오류", e);
		}
	}

	public void delete(long memberId, Connection conn) {
		String sql = "DELETE FROM group_member WHERE id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, memberId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("멤버 삭제(트랜잭션) 중 오류", e);
		}
	}

	public boolean isOwner(long accountId, long userId, Connection conn) {
		String sql = "SELECT 1 FROM group_member WHERE account_id = ? AND user_id = ? AND role = 'OWNER' LIMIT 1";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			ps.setLong(2, userId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			throw new RuntimeException("OWNER 여부 확인(트랜잭션) 중 오류", e);
		}
	}
}