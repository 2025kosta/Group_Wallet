package main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.db.DbUtil;
import main.domain.Account;
import main.enums.AccountType;

public class AccountRepository {

	public Account save(Account account) {
		try (Connection conn = DbUtil.getConnection()) {
			return save(account, conn);
		} catch (SQLException e) {
			System.err.println("계좌 저장(단일) 중 DB 커넥션 오류 발생: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Account save(Account account, Connection conn) {
		String sql = "INSERT INTO account (type, name, owner_user_id, balance, created_at) VALUES (?, ?, ?, ?, ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			pstmt.setString(1, account.getType().name());
			pstmt.setString(2, account.getName());

			if (account.getOwnerUserId() != null) {
				pstmt.setLong(3, account.getOwnerUserId());
			} else {
				pstmt.setNull(3, Types.BIGINT);
			}

			pstmt.setLong(4, account.getBalance());
			pstmt.setTimestamp(5, Timestamp.valueOf(account.getCreatedAt()));

			int affectedRows = pstmt.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("계좌 생성 실패: 영향 받은 행이 없습니다.");
			}

			try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					long newId = generatedKeys.getLong(1);
					return Account.fromDB(newId, account.getType(), account.getName(), account.getOwnerUserId(),
							account.getBalance(), account.getCreatedAt());
				} else {
					throw new SQLException("계좌 생성 실패: ID를 가져올 수 없습니다.");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("계좌 저장(트랜잭션) 중 오류 발생", e);
		}
	}

	public List<Account> findAllByUserId(long userId) {
		// 사용자가 직접 소유한 개인 계좌(a.owner_user_id) 또는
		// 사용자가 멤버로 속한 모임 계좌(gm.user_id)를 모두 조회
		String sql = "SELECT DISTINCT a.* " + "FROM account a " + "LEFT JOIN group_member gm ON a.id = gm.account_id "
				+ "WHERE a.owner_user_id = ? OR gm.user_id = ? "
				+ "ORDER BY FIELD(a.type, 'GROUP', 'PERSONAL'), a.name ASC";

		List<Account> accounts = new ArrayList<>();

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, userId);
			pstmt.setLong(2, userId);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					accounts.add(mapRowToAccount(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("사용자의 모든 계좌 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
		return accounts;
	}

	public Optional<Account> findById(long accountId) {
		String sql = "SELECT * FROM account WHERE id = ?";
		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, accountId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(mapRowToAccount(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public void updateBalance(long accountId, long newBalance) {
		String sql = "UPDATE account SET balance = ? WHERE id = ?";
		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, newBalance);
			pstmt.setLong(2, accountId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void deleteById(long accountId) {
		String sql = "DELETE FROM account WHERE id = ?";
		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, accountId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Optional<Account> findByNameAndOwnerUserId(String name, long ownerUserId) {
		String sql = "SELECT * FROM account WHERE owner_user_id = ? AND name = ? AND type = 'PERSONAL'";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, ownerUserId);
			pstmt.setString(2, name);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(mapRowToAccount(rs));
				}
			}
		} catch (SQLException e) {
			System.err.println("이름과 소유자로 계좌 조회 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return Optional.empty();
	}

	private Account mapRowToAccount(ResultSet rs) throws SQLException {
		long id = rs.getLong("id");
		AccountType type = AccountType.valueOf(rs.getString("type"));
		String name = rs.getString("name");
		long ownerUserIdLong = rs.getLong("owner_user_id");
		Long ownerUserId = rs.wasNull() ? null : ownerUserIdLong;
		long balance = rs.getLong("balance");
		java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

		return Account.fromDB(id, type, name, ownerUserId, balance, createdAt);
	}
}
