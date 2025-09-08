package main.dto;

import main.enums.MemberRole;

public class GroupMemberDto {
	private final long userId;
	private final String userName;
	private final String userEmail;
	private final MemberRole role;

	public GroupMemberDto(long userId, String userName, String userEmail, MemberRole role) {
		this.userId = userId;
		this.userName = userName;
		this.userEmail = userEmail;
		this.role = role;
	}

	public long getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public MemberRole getRole() {
		return role;
	}
}
