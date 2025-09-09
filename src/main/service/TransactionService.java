package main.service;

import main.db.DbUtil;
import main.domain.Account;
import main.domain.Card;
import main.dto.TransactionListDto;
import main.enums.CardStatus;
import main.repository.AccountRepository;
import main.repository.CardRepository;
import main.repository.TransactionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransactionService {
    private final AccountRepository accountRepository = new AccountRepository();
    private final CardRepository cardRepository = new CardRepository();
    private final TransactionRepository txRepository = new TransactionRepository();

    /* ===================== OTHER: 수입 ===================== */
    public void addIncomeOther(long accountId, long amount, String memo,
                               LocalDateTime occurredAt, long createdByUserId) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");

        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            // 같은 커넥션에서 계좌 잠금(일관성 보장)
            Account acc = accountRepository.findByIdForUpdate(accountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

            Timestamp ts = Timestamp.valueOf(occurredAt == null ? LocalDateTime.now() : occurredAt);

            // 거래 저장(IN / OTHER)
            txRepository.insertIncomeOther(accountId, amount, memo, ts, createdByUserId, conn);

            // 잔액 증가 (같은 커넥션)
            accountRepository.increaseBalance(accountId, amount, conn);

            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            restoreAndClose(conn);
        }
    }

    /* ===================== OTHER: 지출 ===================== */
    public void addExpenseOther(long accountId, long amount, String memo,
                                LocalDateTime occurredAt, long createdByUserId) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");

        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            // 잠금 + 현재 잔액 확인
            Account acc = accountRepository.findByIdForUpdate(accountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
            if (acc.getBalance() < amount) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }

            Timestamp ts = Timestamp.valueOf(occurredAt == null ? LocalDateTime.now() : occurredAt);

            // 거래 저장(OUT / OTHER)
            txRepository.insertExpenseOther(accountId, amount, memo, ts, createdByUserId, conn);

            // 잔액 감소
            accountRepository.decreaseBalance(accountId, amount, conn);

            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            restoreAndClose(conn);
        }
    }

    /* ===================== CARD: 지출 ===================== */
    public void addExpenseCard(long cardId, long amount, String memo,
                               LocalDateTime occurredAt, long createdByUserId) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다."));
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("BLOCKED 카드로는 기록할 수 없습니다.");
        }
        long accountId = card.getAccountId();

        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            // 같은 커넥션에서 계좌 잠금 + 잔액확인
            Account acc = accountRepository.findByIdForUpdate(accountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
            if (acc.getBalance() < amount) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }

            Timestamp ts = Timestamp.valueOf(occurredAt == null ? LocalDateTime.now() : occurredAt);

            // 거래 저장(OUT / CARD)
            txRepository.insertExpenseCard(accountId, amount, memo, ts, cardId, createdByUserId, conn);

            // 잔액 감소 (같은 커넥션)
            accountRepository.decreaseBalance(accountId, amount, conn);

            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            restoreAndClose(conn);
        }
    }

    /* ===================== TRANSFER: 이체 ===================== */
    public void transfer(long fromAccountId, long toAccountId, long amount,
                         String memo, long createdByUserId) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        if (fromAccountId == toAccountId) throw new IllegalArgumentException("동일 계좌 간 이체는 불가합니다.");

        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            // Deadlock 회피: id 순으로 잠금
            long firstId = Math.min(fromAccountId, toAccountId);
            long secondId = Math.max(fromAccountId, toAccountId);

            Account first = accountRepository.findByIdForUpdate(firstId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
            Account second = accountRepository.findByIdForUpdate(secondId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

            Account from = (first.getId() == fromAccountId) ? first : second;
            Account to   = (first.getId() == toAccountId)   ? first : second;

            if (from.getBalance() < amount) {
                throw new IllegalStateException("출금계좌 잔액이 부족합니다.");
            }

            String transferKey = UUID.randomUUID().toString();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            // 거래 두 건(OUT/IN)
            txRepository.insertTransferOut(from.getId(), amount, memo, now, transferKey, createdByUserId, conn);
            txRepository.insertTransferIn(to.getId(),   amount, memo, now, transferKey, createdByUserId, conn);

            // 잔액 반영(같은 커넥션)
            accountRepository.decreaseBalance(from.getId(), amount, conn);
            accountRepository.increaseBalance(to.getId(),   amount, conn);

            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            restoreAndClose(conn);
        }
    }

    /* ===================== 검색 ===================== */
    public List<TransactionListDto> search(long userId, Long accountIdFilter,
                                           LocalDate from, LocalDate to,
                                           Long minAmount, Long maxAmount) {
        return txRepository.search(userId, accountIdFilter, from, to, minAmount, maxAmount);
    }

    /* ===================== 유틸 ===================== */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
    }
    private void restoreAndClose(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }
}
