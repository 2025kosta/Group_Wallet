package main.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import main.db.DbUtil;
import main.dto.TransactionListDto;
import main.enums.TransactionKind;
import main.enums.TransactionMethod;

public class TransactionRepository {

	// 클래스 내부에 아래 2개 메서드 추가

	/** OTHER-IN */
	public void insertIncomeOther(long accountId, long amount, String memo, Timestamp occurredAt,
								  long createdByUserId, Connection conn) {
		final String sql =
				"INSERT INTO `transaction` " +
						"(account_id, kind, method, amount, memo, occurred_at, transfer_key, card_id, created_by_user_id, created_at) " +
						"VALUES (?, 'IN', 'OTHER', ?, ?, ?, NULL, NULL, ?, CURRENT_TIMESTAMP)";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			ps.setLong(2, amount);
			if (memo == null) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, memo);
			ps.setTimestamp(4, occurredAt);
			ps.setLong(5, createdByUserId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("수입(OTHER) 저장 오류", e);
		}
	}

	/** OTHER-OUT */
	public void insertExpenseOther(long accountId, long amount, String memo, Timestamp occurredAt,
								   long createdByUserId, Connection conn) {
		final String sql =
				"INSERT INTO `transaction` " +
						"(account_id, kind, method, amount, memo, occurred_at, transfer_key, card_id, created_by_user_id, created_at) " +
						"VALUES (?, 'OUT', 'OTHER', ?, ?, ?, NULL, NULL, ?, CURRENT_TIMESTAMP)";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			ps.setLong(2, amount);
			if (memo == null) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, memo);
			ps.setTimestamp(4, occurredAt);
			ps.setLong(5, createdByUserId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("지출(OTHER) 저장 오류", e);
		}
	}


	// CARD 지출(단일행 OUT)
	public void insertExpenseCard(long accountId, long amount, String memo, Timestamp occurredAt, long cardId,
			Long createdByUserId, Connection conn) {
		final String sql = "INSERT INTO `transaction` "
				+ "(account_id, kind, method, amount, memo, occurred_at, transfer_key, card_id, created_by_user_id, created_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, CURRENT_TIMESTAMP)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, accountId);
			ps.setString(2, TransactionKind.OUT.name());
			ps.setString(3, TransactionMethod.CARD.name());
			ps.setLong(4, amount);
			if (memo != null) {
				ps.setString(5, memo);
			} else {
				ps.setNull(5, Types.VARCHAR);
			}
			ps.setTimestamp(6, occurredAt);
			ps.setLong(7, cardId);
			if (createdByUserId != null) {
				ps.setLong(8, createdByUserId);
			} else {
				ps.setNull(8, Types.BIGINT);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("거래 저장 오류", e);
		}
	}

	// TRANSFER - OUT
	public void insertTransferOut(long fromAccountId, long amount, String memo, Timestamp occurredAt,
			String transferKey, Long createdByUserId, Connection conn) {
		final String sql = "INSERT INTO `transaction` "
				+ "(account_id, kind, method, amount, memo, occurred_at, transfer_key, card_id, created_by_user_id, created_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, CURRENT_TIMESTAMP)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, fromAccountId);
			ps.setString(2, TransactionKind.OUT.name());
			ps.setString(3, TransactionMethod.TRANSFER.name());
			ps.setLong(4, amount);
			if (memo != null) {
				ps.setString(5, memo);
			} else {
				ps.setNull(5, Types.VARCHAR);
			}
			ps.setTimestamp(6, occurredAt);
			ps.setString(7, transferKey);
			if (createdByUserId != null) {
				ps.setLong(8, createdByUserId);
			} else {
				ps.setNull(8, Types.BIGINT);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("거래 저장 오류", e);
		}
	}

	// TRANSFER - IN
	public void insertTransferIn(long toAccountId, long amount, String memo, Timestamp occurredAt, String transferKey,
			Long createdByUserId, Connection conn) {
		final String sql = "INSERT INTO `transaction` "
				+ "(account_id, kind, method, amount, memo, occurred_at, transfer_key, card_id, created_by_user_id, created_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, CURRENT_TIMESTAMP)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, toAccountId);
			ps.setString(2, TransactionKind.IN.name());
			ps.setString(3, TransactionMethod.TRANSFER.name());
			ps.setLong(4, amount);
			if (memo != null) {
				ps.setString(5, memo);
			} else {
				ps.setNull(5, Types.VARCHAR);
			}
			ps.setTimestamp(6, occurredAt);
			ps.setString(7, transferKey);
			if (createdByUserId != null) {
				ps.setLong(8, createdByUserId);
			} else {
				ps.setNull(8, Types.BIGINT);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("거래 저장 오류", e);
		}
	}

	// 검색 (사용자 소유/멤버십 계좌 범위 안에서)
	public List<TransactionListDto> search(Long userId, Long accountIdFilter, LocalDate from, LocalDate to,
			Long minAmount, Long maxAmount) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.name AS account_name, a.account_number AS account_number, "
				+ "       c.masked_no AS card_masked_no, " + "       t.kind, t.method, t.amount, t.memo, t.occurred_at "
				+ "FROM `transaction` t " + "JOIN account a ON a.id = t.account_id "
				+ "LEFT JOIN card c ON c.id = t.card_id " + "WHERE 1=1 ");

		List<Object> params = new ArrayList<>();

		// 사용자 범위 제한
		sb.append(
				"AND (a.owner_user_id = ? OR a.id IN (SELECT gm.account_id FROM group_member gm WHERE gm.user_id = ?)) ");
		params.add(userId);
		params.add(userId);

		if (accountIdFilter != null) {
			sb.append("AND t.account_id = ? ");
			params.add(accountIdFilter);
		}
		if (from != null) {
			sb.append("AND DATE(t.occurred_at) >= ? ");
			params.add(Date.valueOf(from));
		}
		if (to != null) {
			sb.append("AND DATE(t.occurred_at) <= ? ");
			params.add(Date.valueOf(to));
		}
		if (minAmount != null) {
			sb.append("AND t.amount >= ? ");
			params.add(minAmount);
		}
		if (maxAmount != null) {
			sb.append("AND t.amount <= ? ");
			params.add(maxAmount);
		}

		sb.append("ORDER BY t.occurred_at DESC");

		List<TransactionListDto> rows = new ArrayList<>();
		try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sb.toString())) {

			for (int i = 0; i < params.size(); i++) {
				Object p = params.get(i);
				if (p instanceof java.sql.Date d) {
					ps.setDate(i + 1, d);
				} else if (p instanceof Long l) {
					ps.setLong(i + 1, l);
				} else {
					ps.setObject(i + 1, p);
				}
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					rows.add(new TransactionListDto(rs.getString("account_name"), rs.getString("account_number"),
							rs.getString("card_masked_no"), TransactionKind.valueOf(rs.getString("kind")),
							TransactionMethod.valueOf(rs.getString("method")), rs.getLong("amount"),
							rs.getString("memo"), rs.getTimestamp("occurred_at").toLocalDateTime()));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("거래 검색 오류", e);
		}
		return rows;
	}

	// 기록 존재 여부 확인
	public boolean existsByAccountId(long accountId) {
		String sql = "SELECT 1 FROM `transaction` WHERE account_id = ? LIMIT 1";

		try (Connection conn = DbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, accountId);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			System.err.println("계좌의 거래 내역 확인 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("거래 내역 확인 중 오류", e);
		}
	}
}
