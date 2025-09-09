package main.domain;

import java.time.LocalDateTime;

import main.enums.TransactionKind;
import main.enums.TransactionMethod;

/**
 * 계좌 원장 거래(입금/출금/이체)
 * - TRANSFER(내부 이체), CARD(카드 OUT), OTHER(내부 조정)만 사용
 */
public class Transaction {
    private final long id;
    private final long accountId;
    private final TransactionKind kind;
    private final TransactionMethod method;
    private final long amount;
    private final String memo; // nullable
    private final LocalDateTime occurredAt;
    private final String transferKey; // nullable
    private final Long cardId;        // nullable
    private final Long createdByUserId; // nullable
    private final LocalDateTime createdAt;

    private Transaction(long id, long accountId, TransactionKind kind, TransactionMethod method, long amount,
                        String memo, LocalDateTime occurredAt, String transferKey, Long cardId,
                        Long createdByUserId, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.kind = kind;
        this.method = method;
        this.amount = amount;
        this.memo = memo;
        this.occurredAt = occurredAt;
        this.transferKey = transferKey;
        this.cardId = cardId;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public static Transaction income(long id, long accountId, long amount, String memo,
                                     LocalDateTime occurredAt, Long createdByUserId) {
        return new Transaction(id, accountId, TransactionKind.IN, TransactionMethod.OTHER,
                amount, memo, occurredAt, null, null, createdByUserId, LocalDateTime.now());
    }

    public static Transaction expenseOther(long id, long accountId, long amount, String memo,
                                           LocalDateTime occurredAt, Long createdByUserId) {
        return new Transaction(id, accountId, TransactionKind.OUT, TransactionMethod.OTHER,
                amount, memo, occurredAt, null, null, createdByUserId, LocalDateTime.now());
    }

    public static Transaction expenseCard(long id, long accountId, long amount, String memo,
                                          LocalDateTime occurredAt, long cardId, Long createdByUserId) {
        return new Transaction(id, accountId, TransactionKind.OUT, TransactionMethod.CARD,
                amount, memo, occurredAt, null, cardId, createdByUserId, LocalDateTime.now());
    }

    public static Transaction transferOut(long id, long fromAccountId, long amount, String memo,
                                          LocalDateTime occurredAt, String transferKey, Long createdByUserId) {
        return new Transaction(id, fromAccountId, TransactionKind.OUT, TransactionMethod.TRANSFER,
                amount, memo, occurredAt, transferKey, null, createdByUserId, LocalDateTime.now());
    }

    public static Transaction transferIn(long id, long toAccountId, long amount, String memo,
                                         LocalDateTime occurredAt, String transferKey, Long createdByUserId) {
        return new Transaction(id, toAccountId, TransactionKind.IN, TransactionMethod.TRANSFER,
                amount, memo, occurredAt, transferKey, null, createdByUserId, LocalDateTime.now());
    }

    public static Transaction fromDB(long id, long accountId, TransactionKind kind, TransactionMethod method, long amount,
                                     String memo, LocalDateTime occurredAt, String transferKey, Long cardId,
                                     Long createdByUserId, LocalDateTime createdAt) {
        return new Transaction(id, accountId, kind, method, amount, memo, occurredAt, transferKey, cardId, createdByUserId, createdAt);
    }

    public long getId() { return id; }
    public long getAccountId() { return accountId; }
    public TransactionKind getKind() { return kind; }
    public TransactionMethod getMethod() { return method; }
    public long getAmount() { return amount; }
    public String getMemo() { return memo; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getTransferKey() { return transferKey; }
    public Long getCardId() { return cardId; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
