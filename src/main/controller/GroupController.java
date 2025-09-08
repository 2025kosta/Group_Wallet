package main.controller;

import java.util.List;
import java.util.Scanner;

import main.domain.Account;
import main.domain.User;
import main.dto.GroupMemberDto;
import main.service.AccountService;
import main.service.GroupService;

public class GroupController {
	private final Scanner scanner;
	private final User currentUser;
	private final GroupService groupService;
	private final AccountService accountService;

	public GroupController(Scanner scanner, User currentUser) {
		this.scanner = scanner;
		this.currentUser = currentUser;
		this.groupService = new GroupService();
		this.accountService = new AccountService();
	}

	public void showMenu() {
		System.out.println("\n----- [📂 관리할 모임통장 선택] -----");
		List<Account> myGroupAccounts = accountService.findMyGroupAccounts(currentUser.getId());

		if (myGroupAccounts.isEmpty()) {
			System.out.println("📢 관리할 모임통장이 없습니다.");
			return;
		}

		for (int i = 0; i < myGroupAccounts.size(); i++) {
			Account acc = myGroupAccounts.get(i);
			System.out.printf("%d. %s (ID: %d)\n", i + 1, acc.getName(), acc.getId());
		}
		System.out.println("0. 취소");

		try {
			System.out.print("👉 선택 (번호): ");
			int choice = Integer.parseInt(scanner.nextLine());
			if (choice == 0) {
				return;
			}
			if (choice > 0 && choice <= myGroupAccounts.size()) {
				Account selectedAccount = myGroupAccounts.get(choice - 1);
				manageSpecificGroupMenu(selectedAccount);
			} else {
				System.out.println("❗ 잘못된 번호입니다.");
			}
		} catch (NumberFormatException e) {
			System.out.println("❗ 숫자만 입력해주세요.");
		}
	}

	private void manageSpecificGroupMenu(Account groupAccount) {
		while (true) {
			System.out.println("\n----- [\"" + groupAccount.getName() + "\" 멤버 관리] -----");
			System.out.println("1. 멤버 목록 조회");
			System.out.println("2. 멤버 추가");
			System.out.println("3. 멤버 제거");
			System.out.println("4. 멤버 역할 변경");
			System.out.println("0. 모임통장 선택으로 돌아가기");
			System.out.print("👉 선택: ");
			String choice = scanner.nextLine();

			long accountId = groupAccount.getId();
			switch (choice) {
			case "1":
				viewAllMembers(accountId);
				break;
			case "2":
				addMember(accountId);
				break;
			case "3":
				removeMember(accountId);
				break;
			case "4":
				changeMemberRole(accountId);
				break;
			case "0":
				return;
			default:
				System.out.println("❗ 잘못된 입력입니다.");
			}
		}
	}

	private void viewAllMembers(long accountId) {
		System.out.println("\n----- [👥 멤버 목록] -----");
		List<GroupMemberDto> members = groupService.findMemberInfoByAccountId(accountId);

		if (members.isEmpty()) {
			System.out.println("📢 등록된 멤버가 없습니다.");
			return;
		}
		System.out.println("----------------------------------------------------------");
		System.out.printf("%-5s | %-20s | %s\n", "ID", "이름", "역할");
		System.out.println("----------------------------------------------------------");

		for (GroupMemberDto memberInfo : members) {
			System.out.printf("%-5d | %-20s | %s\n", memberInfo.getUserId(), memberInfo.getUserName(),
					memberInfo.getRole());
		}
		System.out.println("----------------------------------------------------------");
	}

	private void addMember(long accountId) {
		try {
			System.out.println("\n----- [📧 멤버 추가] -----");
			System.out.print("추가할 멤버의 이메일: ");
			String email = scanner.nextLine();
			groupService.addMember(accountId, currentUser.getId(), email);
			System.out.println("✅ 멤버 추가가 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 멤버 추가 실패: " + e.getMessage());
		}
	}

	private void removeMember(long accountId) {
		try {
			System.out.println("\n----- [🚫 멤버 제거] -----");
			System.out.print("제거할 멤버의 이메일: ");
			String email = scanner.nextLine();
			groupService.removeMember(accountId, currentUser.getId(), email);
			System.out.println("✅ 멤버 제거가 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 멤버 제거 실패: " + e.getMessage());
		}
	}

	private void changeMemberRole(long accountId) {
		try {
			System.out.println("\n----- [👑 멤버 역할 변경] -----");
			System.out.print("역할을 변경할 멤버의 이메일: ");
			String email = scanner.nextLine();
			System.out.print("새로운 역할 (OWNER 또는 MEMBER): ");
			String roleStr = scanner.nextLine().toUpperCase();
			groupService.changeMemberRole(accountId, currentUser.getId(), email, roleStr);
			System.out.println("✅ 역할 변경이 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 역할 변경 실패: " + e.getMessage());
		}
	}
}