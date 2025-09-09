package main.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.domain.Account;
import main.domain.User;
import main.service.AccountService;
import main.util.ConsoleTable;

public class AccountController {
	private final Scanner scanner;
	private final User currentUser;
	private final AccountService accountService;

	public AccountController(Scanner scanner, User currentUser) {
		this.scanner = scanner;
		this.currentUser = currentUser;
		this.accountService = new AccountService();
	}

	public void showMenu() {
		while (true) {
			System.out.println("\n----- [🏦 계좌 관리] -----");
			System.out.println("1. 내 모든 계좌 조회");
			System.out.println("2. 개인 계좌 생성");
			System.out.println("3. 모임통장 계좌 생성");
			System.out.println("4. 계좌 이름 변경");
			System.out.println("5. 계좌 삭제");
			System.out.println("0. 메인 메뉴로 돌아가기");
			System.out.print("👉 선택: ");
			String choice = scanner.nextLine().trim();

			switch (choice) {
			case "1" -> viewAllMyAccounts();
			case "2" -> createPersonalAccount();
			case "3" -> createGroupAccount();
			case "4" -> changeAccountName();
			case "5" -> deleteAccount();
			case "0" -> {
				return;
			}
			default -> System.out.println("❗ 잘못된 입력입니다.");
			}
		}
	}

	private void viewAllMyAccounts() {
		displayAndCheckAccounts();
	}

	private void createPersonalAccount() {
		try {
			System.out.println("\n----- [👤 개인 계좌 생성] -----");
			System.out.print("계좌 이름: ");
			String name = scanner.nextLine();
			System.out.print("초기 입금액 (숫자만 입력): ");
			long initialBalance = Long.parseLong(scanner.nextLine().trim());
			accountService.createPersonalAccount(name, currentUser.getId(), initialBalance);
			System.out.println("✅ 개인 계좌 '" + name + "' 생성이 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 계좌 생성 실패: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void createGroupAccount() {
		try {
			System.out.println("\n----- [👥 모임통장 생성] -----");
			System.out.print("모임통장 이름: ");
			String name = scanner.nextLine();
			System.out.print("초기 입금액 (숫자만 입력): ");
			long initialBalance = Long.parseLong(scanner.nextLine());
			accountService.createGroupAccount(name, currentUser.getId(), initialBalance);
			System.out.println("✅ 모임통장 '" + name + "' 생성이 완료되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 모임통장 생성 실패: " + e.getMessage());
		}
	}

	private void changeAccountName() {
		try {
			System.out.println("\n----- [✏️ 계좌 이름 변경] -----");

			boolean hasAccounts = displayAndCheckAccounts();
			if (!hasAccounts) {
				return;
			}

			System.out.print("이름을 변경할 계좌의 계좌번호: ");
			String accountNumber = scanner.nextLine().trim();
			System.out.print("새로운 계좌 이름: ");
			String newName = scanner.nextLine().trim();

			accountService.changeAccountName(accountNumber, newName, currentUser.getId());
			System.out.println("✅ 계좌 이름이 성공적으로 변경되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 이름 변경 실패: " + e.getMessage());
		}
	}

	private void deleteAccount() {
		try {
			System.out.println("\n----- [🗑️ 계좌 삭제] -----");
			boolean hasAccounts = displayAndCheckAccounts();
			if (!hasAccounts) {
				return;
			}

			System.out.print("삭제할 계좌의 계좌번호: ");
			String accountNumber = scanner.nextLine().trim();
			System.out.print("⚠️ 정말로 삭제하시겠습니까? (y/n): ");
			if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
				System.out.println("📢 삭제가 취소되었습니다.");
				return;
			}
			accountService.deleteAccount(accountNumber, currentUser.getId());
			System.out.println("✅ 계좌가 성공적으로 삭제되었습니다.");
		} catch (Exception e) {
			System.err.println("❌ 삭제 실패: " + e.getMessage());
		}
	}

	/** 표로 계좌 목록 출력 (계좌번호/종류/이름/잔액) */
	private boolean displayAndCheckAccounts() {
		List<Account> myAccounts = accountService.findMyAccounts(currentUser.getId());
		if (myAccounts.isEmpty()) {
			System.out.println("📢 조회, 변경 또는 삭제할 계좌가 없습니다.");
			return false;
		}

		List<String[]> rows = new ArrayList<>();
		for (Account a : myAccounts) {
			rows.add(new String[] { a.getAccountNumber(), a.getType().name().equals("PERSONAL") ? "개인" : "모임",
					a.getName(), String.format("%,d원", a.getBalance()) });
		}
		ConsoleTable.printTable("\n----- [🗂️ 계좌 목록 (계좌번호를 확인하세요)] -----",
				new String[] { "계좌번호", "계좌 종류", "계좌 이름", "잔액" }, rows);
		return true;
	}
}
