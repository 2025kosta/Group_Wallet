package main.domain;

import java.time.LocalDateTime;

import main.enums.CardStatus;

/**
 * 계좌에 귀속된 카드(개인/모임 공통)
 * - 원카드 번호 저장하지 않고 마스킹 문자열만 보관
 * - BLOCKED면 거래 생성 불가(서비스 레이어에서 검증)
 */
public class Card {
    private final long id;
    private final long accountId;
    private final String maskedNo;
    private final String brand; // nullable
    private CardStatus status;
    private final LocalDateTime createdAt;

    private Card(long id, long accountId, String maskedNo, String brand, CardStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.maskedNo = maskedNo;
        this.brand = brand;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 카드 등록/발급 (기본 ACTIVE) */
    public static Card issue(long id, long accountId, String maskedNo, String brand) {
        return new Card(id, accountId, maskedNo, brand, CardStatus.ACTIVE, LocalDateTime.now());
    }

    public static Card fromDB(long id, long accountId, String maskedNo, String brand, CardStatus status, LocalDateTime createdAt) {
        return new Card(id, accountId, maskedNo, brand, status, createdAt);
    }

    public void block()   { this.status = CardStatus.BLOCKED; }
    public void unblock() { this.status = CardStatus.ACTIVE;  }

    public long getId() { return id; }
    public long getAccountId() { return accountId; }
    public String getMaskedNo() { return maskedNo; }
    public String getBrand() { return brand; }
    public CardStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
