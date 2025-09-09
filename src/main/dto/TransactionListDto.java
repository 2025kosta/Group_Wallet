package main.dto;

import main.enums.TransactionKind;
import main.enums.TransactionMethod;

import java.time.LocalDateTime;

public class TransactionListDto {
    public final String accountName;
    public final String accountNumber;
    public final String cardMaskedNo; // null 허용
    public final TransactionKind kind;
    public final TransactionMethod method;
    public final long amount;
    public final String memo; // null 허용
    public final LocalDateTime occurredAt;

    public TransactionListDto(String accountName,
                              String accountNumber,
                              String cardMaskedNo,
                              TransactionKind kind,
                              TransactionMethod method,
                              long amount,
                              String memo,
                              LocalDateTime occurredAt) {
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.cardMaskedNo = cardMaskedNo;
        this.kind = kind;
        this.method = method;
        this.amount = amount;
        this.memo = memo;
        this.occurredAt = occurredAt;
    }
}
