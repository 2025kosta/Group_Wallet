package main.enums;

/**
 * 거래 수단(내부 전용)
 * TRANSFER : 내부 계좌 간 이체(OUT/IN 두 행 + 동일 transferKey)
 * CARD     : 연결 카드로 결제된 지출(OUT) 단일행
 * OTHER    : 내부 조정/수수료/오류정정 등 단일행
 */
public enum TransactionMethod {
    TRANSFER, CARD, OTHER
}
