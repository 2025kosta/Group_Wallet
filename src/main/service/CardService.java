package main.service;

import main.db.DbUtil;
import main.domain.Card;
import main.enums.CardStatus;
import main.repository.CardRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class CardService {

    private final CardRepository cardRepository = new CardRepository();

    /** 카드 등록 (maskedNo 중복 체크) */
    public Card register(long accountId, String maskedNo, String brand) {
        cardRepository.findByMaskedNo(maskedNo).ifPresent(c -> {
            throw new IllegalArgumentException("동일 마스킹번호의 카드가 이미 등록되었습니다.");
        });
        // 도메인 팩토리 사용(저장 시 ACTIVE, createdAt now)
        Card toSave = Card.issue(0L, accountId, maskedNo, brand);
        return cardRepository.save(toSave);
    }

    /** 계좌별 카드 목록 */
    public List<Card> listByAccount(long accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    /** 여러 계좌의 카드 목록(브랜드 → 카드번호 정렬) */
    public List<Card> findCardsByAccountIds(List<Long> accountIds) {
        List<Card> cards = cardRepository.findByAccountIds(accountIds);
        cards.sort(Comparator
                .comparing((Card c) -> safeBrand(c.getBrand()))
                .thenComparing(Card::getMaskedNo, Comparator.nullsLast(String::compareTo)));
        return cards;
    }

    /** 상태 변경 */
    public void changeStatus(long cardId, CardStatus newStatus) {
        cardRepository.updateStatus(cardId, newStatus);
    }

    /** 삭제(연계 거래 존재 시 차단) */
    // CardService.java (교체: delete)

    public void delete(long cardId) {
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            if (cardRepository.existsTransactionByCardId(cardId, conn)) {
                throw new IllegalStateException("연결된 거래가 있어 카드를 삭제할 수 없습니다.");
            }
            cardRepository.deleteById(cardId, conn);

            conn.commit();
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (
                    SQLException ignore) {}
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
                try { conn.close(); } catch (SQLException ignore) {}
            }
        }
    }


    // 널/대소문자 안정화용
    private String safeBrand(String b) {
        return b == null ? "" : b.toUpperCase();
    }
}
