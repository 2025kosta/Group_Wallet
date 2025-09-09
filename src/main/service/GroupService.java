package main.service;

import java.util.ArrayList;
import java.util.List;

import main.domain.GroupMember;
import main.domain.User;
import main.dto.GroupMemberDto;
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

	public List<GroupMember> findMembersByAccountId(long accountId) {
		return groupRepository.findByAccountId(accountId);
	}

	public void addMember(long accountId, long actionUserId, String memberEmail) {
		checkOwnerPermission(accountId, actionUserId);

		User userToAdd = userRepository.findByEmail(memberEmail)
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 이메일의 사용자를 찾을 수 없습니다."));

		groupRepository.findByAccountIdAndUserId(accountId, userToAdd.getId()).ifPresent(m -> {
			throw new IllegalArgumentException("❌ 이미 가입된 사용자입니다.");
		});

		GroupMember newMember = GroupMember.join(0, accountId, userToAdd.getId(), MemberRole.MEMBER);
		groupRepository.save(newMember);
	}

	public void changeMemberRole(long accountId, long actionUserId, String targetUserEmail, String newRoleStr) {
		checkOwnerPermission(accountId, actionUserId);

		User targetUser = userRepository.findByEmail(targetUserEmail)
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 이메일의 사용자를 찾을 수 없습니다."));

		GroupMember memberToChange = groupRepository.findByAccountIdAndUserId(accountId, targetUser.getId())
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 멤버를 찾을 수 없습니다."));

		MemberRole newRole;
		try {
			newRole = MemberRole.valueOf(newRoleStr);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("❌ 'OWNER' 또는 'MEMBER' 역할만 지정할 수 있습니다.");
		}

		if (memberToChange.getRole() == newRole) {
			throw new IllegalStateException("이미 '" + newRole + "' 역할을 가지고 있습니다.");
		}

		if (memberToChange.isOwner() && newRole == MemberRole.MEMBER) {
			if (groupRepository.countOwnersByAccountId(accountId) <= 1) {
				throw new IllegalStateException("❌ 마지막 OWNER의 역할은 변경할 수 없습니다.");
			}
		}

		groupRepository.updateRole(memberToChange.getId(), newRole);
	}

	public void removeMember(long accountId, long actionUserId, String targetUserEmail) {
		checkOwnerPermission(accountId, actionUserId);

		User targetUser = userRepository.findByEmail(targetUserEmail)
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 이메일의 사용자를 찾을 수 없습니다."));

		GroupMember memberToRemove = groupRepository.findByAccountIdAndUserId(accountId, targetUser.getId())
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 멤버를 찾을 수 없습니다."));

		if (memberToRemove.isOwner()) {
			if (groupRepository.countOwnersByAccountId(accountId) <= 1) {
				throw new IllegalStateException("❌ 마지막 OWNER는 제거할 수 없습니다.");
			}
		}
		groupRepository.delete(memberToRemove.getId());
	}

	public List<GroupMember> getGroupMembers(long accountId) {
		return groupRepository.findByAccountId(accountId);
	}

	public List<GroupMemberDto> findMemberInfoByAccountId(long accountId) {
		List<GroupMember> members = groupRepository.findByAccountId(accountId);
		List<GroupMemberDto> memberInfos = new ArrayList<>();

		for (GroupMember member : members) {
			User user = userRepository.findById(member.getUserId())
					.orElse(User.register(member.getUserId(), "(알 수 없는 사용자)", "N/A", null)); // 사용자를 못찾을 경우를 대비한 기본값

			GroupMemberDto dto = new GroupMemberDto(user.getId(), user.getName(), user.getEmail(), member.getRole());
			memberInfos.add(dto);
		}
		return memberInfos;
	}

	private void checkOwnerPermission(long accountId, long userId) {
		GroupMember requester = groupRepository.findByAccountIdAndUserId(accountId, userId)
				.orElseThrow(() -> new SecurityException("작업을 요청한 사용자가 멤버가 아닙니다."));

		if (!requester.isOwner()) {
			throw new SecurityException("OWNER만 수행할 수 있는 작업입니다.");
		}
	}
}