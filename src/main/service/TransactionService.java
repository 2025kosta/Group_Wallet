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

    /** 카드 지출 */
    // TransactionService 내 교체
    public void addExpenseCard(long cardId, long amount, String memo, LocalDateTime occurredAt, long createdByUserId) {
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
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // 계좌를 같은 커넥션에서 LOCK 걸고 읽기
            Account acc = accountRepository.findByIdForUpdate(accountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

            // 거래 저장
            txRepository.insertExpenseCard(
                    accountId,
                    amount,
                    memo,
                    java.sql.Timestamp.valueOf(occurredAt),
                    cardId,
                    createdByUserId,
                    conn
            );

            // 잔액 감소(같은 커넥션)
            long newBal = acc.getBalance() - amount;
            accountRepository.updateBalance(accountId, newBal, conn);

            conn.commit();
        } catch (RuntimeException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) {}
        }
    }


    /** 이체(동일 transfer_key로 OUT/IN 두 건) */
    public void transfer(long fromAccountId, long toAccountId, long amount, String memo, long createdByUserId) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        if (fromAccountId == toAccountId) throw new IllegalArgumentException("동일 계좌 간 이체는 불가합니다.");

        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // 같은 커넥션에서 두 계좌를 LOCK
            Account from = accountRepository.findByIdForUpdate(fromAccountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("출금 계좌를 찾을 수 없습니다."));
            Account to = accountRepository.findByIdForUpdate(toAccountId, conn)
                    .orElseThrow(() -> new IllegalArgumentException("입금 계좌를 찾을 수 없습니다."));

            if (from.getBalance() < amount) {
                throw new IllegalStateException("출금계좌 잔액이 부족합니다.");
            }

            String key = java.util.UUID.randomUUID().toString();
            java.sql.Timestamp now = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());

            txRepository.insertTransferOut(fromAccountId, amount, memo, now, key, createdByUserId, conn);
            txRepository.insertTransferIn(toAccountId, amount, memo, now, key, createdByUserId, conn);

            // 같은 커넥션으로 잔액 반영
            accountRepository.updateBalance(fromAccountId, from.getBalance() - amount, conn);
            accountRepository.updateBalance(toAccountId,   to.getBalance() + amount,   conn);

            conn.commit();
        } catch (RuntimeException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("거래 저장 오류", e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) {}
        }
    }

    /** 검색 */
    public List<TransactionListDto> search(long userId, Long accountIdFilter,
                                           LocalDate from, LocalDate to,
                                           Long minAmount, Long maxAmount) {
        try {
            return txRepository.search(userId, accountIdFilter, from, to, minAmount, maxAmount);
        } catch (Exception e) { // 레포가 체크예외 던지면 여기서 포장
            throw new RuntimeException("거래 검색 오류", e);
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignore) {}
        }
    }

    private void restoreAndClose(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignore) {}
        }
    }
}
