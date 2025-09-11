package main.repository;

import main.db.DbUtil;
import main.domain.Card;
import main.enums.CardStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardRepository {

    public Card save(Card card) {
        String sql = "INSERT INTO card (account_id, masked_no, brand, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, card.getAccountId());
            ps.setString(2, card.getMaskedNo());
            ps.setString(3, card.getBrand()); // 문자열 저장(BC/SAMSUNG/HYUNDAI)
            ps.setString(4, card.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(card.getCreatedAt()));

            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("카드 생성 실패");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long newId = rs.getLong(1);
                    return Card.issue(newId, card.getAccountId(), card.getMaskedNo(), card.getBrand());
                }
            }
            throw new SQLException("카드 생성 실패: ID 생성 안됨");
        } catch (SQLException e) {
            throw new RuntimeException("카드 저장 중 오류", e);
        }
    }

    public Optional<Card> findById(long id) {
        String sql = "SELECT id, account_id, masked_no, brand, status, created_at FROM card WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 조회 오류", e);
        }
        return Optional.empty();
    }

    public Optional<Card> findByMaskedNo(String maskedNo) {
        String sql = "SELECT id, account_id, masked_no, brand, status, created_at FROM card WHERE masked_no = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maskedNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 조회 오류", e);
        }
        return Optional.empty();
    }

    public List<Card> findByAccountId(long accountId) {
        String sql = "SELECT id, account_id, masked_no, brand, status, created_at FROM card WHERE account_id = ?";
        List<Card> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 목록 조회 오류", e);
        }
        return list;
    }

    public void updateStatus(long cardId, CardStatus status) {
        String sql = "UPDATE card SET status = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("카드 상태 변경 오류", e);
        }
    }

    public boolean existsTransactionByCardId(long cardId) {
        String sql = "SELECT 1 FROM `transaction` WHERE card_id = ? LIMIT 1";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 연계 거래 존재여부 확인 오류", e);
        }
    }

    public void deleteById(long cardId) {
        String sql = "DELETE FROM card WHERE id = ?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("카드 삭제 오류", e);
        }
    }

    private Card map(ResultSet rs) throws SQLException {
        return Card.fromDB(
                rs.getLong("id"),
                rs.getLong("account_id"),
                rs.getString("masked_no"),
                rs.getString("brand"), // 문자열 그대로 저장/조회
                CardStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }


    public List<Card> findByAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) return java.util.Collections.emptyList();

        StringBuilder q = new StringBuilder(
                "SELECT id, account_id, masked_no, brand, status, created_at FROM card WHERE account_id IN ("
        );
        for (int i = 0; i < accountIds.size(); i++) {
            if (i > 0) q.append(',');
            q.append('?');
        }
        q.append(')');

        List<Card> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(q.toString())) {
            for (int i = 0; i < accountIds.size(); i++) {
                ps.setLong(i + 1, accountIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 목록 조회 오류", e);
        }
        return list;
    }


    public boolean existsTransactionByCardId(long cardId, Connection conn) {
        String sql = "SELECT 1 FROM `transaction` WHERE card_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("카드 연계 거래 존재여부(트랜잭션) 확인 오류", e);
        }
    }

    public void deleteById(long cardId, Connection conn) {
        String sql = "DELETE FROM card WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("카드 삭제(트랜잭션) 오류", e);
        }
    }


}
