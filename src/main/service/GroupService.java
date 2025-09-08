package main.service;

import java.util.List;

import main.domain.GroupMember;
import main.domain.User;
import main.enums.MemberRole;
import main.repository.GroupRepository;
import main.repository.UserRepository;

public class GroupService {

	private final GroupRepository groupRepository = new GroupRepository();
	private final UserRepository userRepository = new UserRepository(); // 이메일로 사용자 찾기 위해 필요

	public void addInitialOwner(long accountId, long userId) {
		GroupMember owner = GroupMember.join(0, accountId, userId, MemberRole.OWNER);
		groupRepository.save(owner);
	}

	public void addMember(long accountId, String memberEmail, MemberRole role, long actionUserId) {
		checkOwnerPermission(accountId, actionUserId);

		User userToAdd = userRepository.findByEmail(memberEmail)
				.orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));

		groupRepository.findByAccountIdAndUserId(accountId, userToAdd.getId()).ifPresent(m -> {
			throw new IllegalArgumentException("이미 가입된 사용자입니다.");
		});

		GroupMember newMember = GroupMember.join(0, accountId, userToAdd.getId(), role);
		groupRepository.save(newMember);
	}

	public void changeMemberRole(long accountId, long targetUserId, MemberRole newRole, long actionUserId) {
		checkOwnerPermission(accountId, actionUserId);

		GroupMember memberToChange = groupRepository.findByAccountIdAndUserId(accountId, targetUserId)
				.orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

		if (memberToChange.isOwner() && newRole == MemberRole.MEMBER) {
			if (groupRepository.countOwnersByAccountId(accountId) <= 1) {
				throw new IllegalStateException("OWNER가 0명이 될 수 없어 역할을 변경할 수 없습니다.");
			}
		}

		groupRepository.updateRole(memberToChange.getId(), newRole);
	}

	public void removeMember(long accountId, long targetUserId, long actionUserId) {
		checkOwnerPermission(accountId, actionUserId);

		GroupMember memberToRemove = groupRepository.findByAccountIdAndUserId(accountId, targetUserId)
				.orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

		if (memberToRemove.isOwner()) {
			if (groupRepository.countOwnersByAccountId(accountId) <= 1) {
				throw new IllegalStateException("마지막 OWNER는 제거할 수 없습니다.");
			}
		}

		groupRepository.delete(memberToRemove.getId());
	}

	public List<GroupMember> getGroupMembers(long accountId) {
		return groupRepository.findByAccountId(accountId);
	}

	private void checkOwnerPermission(long accountId, long userId) {
		GroupMember requester = groupRepository.findByAccountIdAndUserId(accountId, userId)
				.orElseThrow(() -> new SecurityException("작업을 요청한 사용자가 멤버가 아닙니다."));

		if (!requester.isOwner()) {
			throw new SecurityException("OWNER만 수행할 수 있는 작업입니다.");
		}
	}
}