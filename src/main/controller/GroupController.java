package main.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.domain.Account;
import main.domain.User;
import main.dto.GroupMemberDto;
import main.service.AccountService;
import main.service.GroupService;
import main.util.ConsoleTable;

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

		// 표: 번호 | 유형 | 계좌 이름 | 계좌번호
		List<String[]> rows = new ArrayList<>();
		for (Account a : myGroupAccounts) {
			rows.add(new String[]{
					a.getType().name(), a.getName(), a.getAccountNumber()
			});
		}
		var withIdx = ConsoleTable.withIndex(rows);
		ConsoleTable.printTable(null,
				new String[]{"번호","유형","계좌 이름","계좌번호"},
				withIdx
		);
		System.out.println("0) 취소");

		try {
			System.out.print("👉 선택 (번호): ");
			int choice = Integer.parseInt(scanner.nextLine().trim());
			if (choice == 0) return;

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
			String choice = scanner.nextLine().trim();

			long accountId = groupAccount.getId();
			switch (choice) {
				case "1" -> viewAllMembers(accountId);
				case "2" -> addMember(accountId);
				case "3" -> removeMember(accountId);
				case "4" -> changeMemberRole(accountId);
				case "0" -> { return; }
				default -> System.out.println("❗ 잘못된 입력입니다.");
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

		List<String[]> rows = new ArrayList<>();
		for (GroupMemberDto m : members) {
			rows.add(new String[]{ m.getUserName(), m.getRole().name() });
		}
		var withIdx = ConsoleTable.withIndex(rows);
		ConsoleTable.printTable(null,
				new String[]{"번호","이름","역할"},
				withIdx
		);
	}

	private void addMember(long accountId) {
		displayMembers(accountId);
		try {
			System.out.print("추가할 멤버의 이메일: ");
			String email = scanner.nextLine().trim();
			if (email.isEmpty()) { System.out.println("📢 취소되었습니다."); return; }
			groupService.addMember(accountId, currentUser.getId(), email);
			System.out.println("✅ 멤버 추가가 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 멤버 추가 실패: " + e.getMessage());
		}
	}

	private void removeMember(long accountId) {
		try {
			System.out.println("\n----- [🚫 멤버 제거] -----");
			List<GroupMemberDto> members = displayMembers(accountId);
			if (members == null || members.isEmpty()) return;

			System.out.print("제거할 멤버의 번호: ");
			int seq = Integer.parseInt(scanner.nextLine().trim());

			if (seq > 0 && seq <= members.size()) {
				GroupMemberDto target = members.get(seq - 1);
				System.out.printf("'%s'님을 정말로 제거하시겠습니까? (y/n): ", target.getUserName());
				if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
					System.out.println("📢 제거가 취소되었습니다.");
					return;
				}
				groupService.removeMember(accountId, currentUser.getId(), target.getUserEmail());
				System.out.println("✅ 멤버 제거가 완료되었습니다.");
			} else {
				System.out.println("❗ 잘못된 번호입니다.");
			}
		} catch (NumberFormatException e) {
			System.err.println("❗ 번호는 숫자만 입력해주세요.");
		} catch (Exception e) {
			System.err.println("❌ 멤버 제거 실패: " + e.getMessage());
		}
	}

	private void changeMemberRole(long accountId) {
		try {
			System.out.println("\n----- [👑 멤버 역할 변경] -----");
			List<GroupMemberDto> members = displayMembers(accountId);
			if (members == null || members.isEmpty()) return;

			System.out.print("역할을 변경할 멤버의 번호: ");
			int seq = Integer.parseInt(scanner.nextLine().trim());

			if (seq > 0 && seq <= members.size()) {
				GroupMemberDto target = members.get(seq - 1);
				System.out.print("새로운 역할 (OWNER 또는 MEMBER): ");
				String roleStr = scanner.nextLine().trim().toUpperCase();

				groupService.changeMemberRole(accountId, currentUser.getId(), target.getUserEmail(), roleStr);
				System.out.println("✅ 역할 변경이 완료되었습니다.");
			} else {
				System.out.println("❗ 잘못된 번호입니다.");
			}
		} catch (NumberFormatException e) {
			System.err.println("❗ 번호는 숫자만 입력해주세요.");
		} catch (Exception e) {
			System.err.println("❌ 역할 변경 실패: " + e.getMessage());
		}
	}

	/** 멤버 목록 표로 보여주고 리스트 반환(번호 선택에 사용) */
	private List<GroupMemberDto> displayMembers(long accountId) {
		List<GroupMemberDto> members = groupService.findMemberInfoByAccountId(accountId);

		if (members.isEmpty()) {
			System.out.println("📢 등록된 멤버가 없습니다.");
			return members;
		}

		List<String[]> rows = new ArrayList<>();
		for (GroupMemberDto m : members) {
			rows.add(new String[]{ m.getUserName(), m.getRole().name() });
		}
		var withIdx = ConsoleTable.withIndex(rows);
		ConsoleTable.printTable("\n----- [👥 멤버 목록] -----",
				new String[]{"번호","이름","역할"},
				withIdx
		);
		return members;
	}
}
