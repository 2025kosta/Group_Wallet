package main.domain;

import java.time.LocalDateTime;

import main.enums.MemberRole;

/**
 * 모임 계좌에 속한 사용자와 역할 - (accountId는 Account.type=GROUP 전제) - OWNER 최소 1명 규칙은
 * 서비스/Validator에서 보장
 */
public class GroupMember {

	/** PK: 멤버 식별자 (nullable: false) */
	private final long id;

	/** 모임 계좌 ID(Account.id) (nullable: false) */
	private final long accountId;

	/** 사용자 ID(User.id) (nullable: false) */
	private final long userId;

	/** 역할: OWNER / MEMBER (nullable: false) */
	private MemberRole role;

	/** 가입 일시 (nullable: false) */
	private final LocalDateTime joinedAt;

	private GroupMember(long id, long accountId, long userId, MemberRole role, LocalDateTime joinedAt) {
		this.id = id;
		this.accountId = accountId;
		this.userId = userId;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	/** 멤버 가입 */
	public static GroupMember join(long id, long accountId, long userId, MemberRole role) {
		return new GroupMember(id, accountId, userId, role, LocalDateTime.now());
	}

	/** 역할 변경(OWNER 최소 1명 검증은 바깥에서) */
	public void changeRole(MemberRole newRole) {
		this.role = newRole;
	}

	/** 헬퍼: OWNER 여부 */
	public boolean isOwner() {
		return this.role == MemberRole.OWNER;
	}

	// Getter
	public long getId() {
		return id;
	}

	public long getAccountId() {
		return accountId;
	}

	public long getUserId() {
		return userId;
	}

	public MemberRole getRole() {
		return role;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public static GroupMember fromDB(long id, long accountId, long userId, MemberRole role,
			java.time.LocalDateTime joinedAt) {
		return new GroupMember(id, accountId, userId, role, joinedAt);
	}
}
