package main.domain;

import java.time.LocalDateTime;

import main.enums.AccountType;

/**
 * 모든 계좌(개인/모임) 도메인 - 잔액은 Account.balance에 보관되며 거래 생성/삭제 시 갱신됨
 */
public class Account {

	/** PK: 계좌 식별자 (nullable: false) */
	private final long id;

	private final String accountNumber;

	/** 계좌 종류: PERSONAL / GROUP (nullable: false) */
	private final AccountType type;

	/** 계좌 이름(표시용) (nullable: false) */
	private String name;

	/**
	 * 개인 계좌 소유자(User.id) - PERSONAL: 필수, GROUP: null (nullable: true)
	 */
	private final Long ownerUserId;

	/** 현재 잔액(원) (nullable: false) */
	private long balance;

	/** 생성 시각(감사/이력용) (nullable: false) */
	private final LocalDateTime createdAt;

	private Account(long id, String accountNumber, AccountType type, String name, Long ownerUserId, long balance,
			LocalDateTime createdAt) {
		this.id = id;
		this.accountNumber = accountNumber;
		this.type = type;
		this.name = name;
		this.ownerUserId = ownerUserId;
		this.balance = balance;
		this.createdAt = createdAt;
	}

	/** 개인 계좌 생성 (초기 잔액 0) */
	public static Account createPersonal(long id, String accountNumber, String name, long ownerUserId,
			long initialBalance) {
		return new Account(id, accountNumber, AccountType.PERSONAL, name, ownerUserId, initialBalance,
				LocalDateTime.now());
	}

	/** 모임 계좌 생성(생성자는 GroupMember로 OWNER 등록 필요, 초기 잔액 0) */
	public static Account createGroup(long id, String accountNumber, String name, long initialBalance) {
		return new Account(id, accountNumber, AccountType.GROUP, name, null, initialBalance, LocalDateTime.now());
	}

	/** 계좌명 변경 */
	public void changeName(String newName) {
		this.name = newName;
	}

	/** 잔액 증액(입금/이체-입금 반영) */
	public void increase(long amount) {
		this.balance = Math.addExact(this.balance, amount);
	}

	/** 잔액 감액(지출/이체-출금 반영) */
	public void decrease(long amount) {
		this.balance = Math.subtractExact(this.balance, amount);
	}

	// Getter
	public long getId() {
		return id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public AccountType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Long getOwnerUserId() {
		return ownerUserId;
	} // nullable when GROUP

	public long getBalance() {
		return balance;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public static Account fromDB(long id, String accountNumber, AccountType type, String name, Long ownerUserId,
			long balance, java.time.LocalDateTime createdAt) {
		return new Account(id, accountNumber, type, name, ownerUserId, balance, createdAt);
	}

}
