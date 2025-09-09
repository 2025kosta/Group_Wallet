package main.controller;

import java.util.Scanner;

import main.domain.User;
import main.exception.EmailAlreadyExistsException;
import main.service.UserService;

public class UserController {
	private final Scanner scanner;
	private final UserService userService;
	private User currentUser; // 로그인 성공 시 여기에 사용자 정보 저장

	public UserController(Scanner scanner) {
		this.scanner = scanner;
		this.userService = new UserService();
	}

	public void createUser() {
		try {
			System.out.println("\n----- [👤 사용자 생성] -----");
			System.out.print("이름: ");
			String name = scanner.nextLine();
			System.out.print("이메일: ");
			String email = scanner.nextLine();
			System.out.print("연락처 (선택사항): ");
			String phone = scanner.nextLine();

			User createdUser = userService.createUser(name, email, phone);
			System.out.println("✅ '" + createdUser.getName() + "'님, 사용자 생성이 완료되었습니다.");
		} catch (EmailAlreadyExistsException e) {
			System.err.println("❌ 생성 실패: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("❌ 알 수 없는 오류가 발생했습니다: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean login() {
		try {
			System.out.println("\n----- [🔐 로그인] -----");
			System.out.print("이메일: ");
			String email = scanner.nextLine();

			this.currentUser = userService.login(email)
					.orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자가 없습니다."));
			System.out.println("\n✅ " + currentUser.getName() + "님, 환영합니다!");
			return true;
		} catch (IllegalArgumentException e) {
			System.err.println("❌ 로그인 실패: " + e.getMessage());
			return false;
		} catch (Exception e) {
			System.err.println("❌ 알 수 없는 오류가 발생했습니다: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public boolean deleteCurrentUser() {
		System.out.println("\n----- [🗑️ 회원 탈퇴] -----");
		System.out.println("⚠️ 경고: 회원 탈퇴 시 모든 계좌와 거래 내역이 영구적으로 삭제됩니다.");
		System.out.print("정말로 탈퇴하시겠습니까? (y/n): ");
		String confirmation = scanner.nextLine();

		if (!"y".equalsIgnoreCase(confirmation)) {
			System.out.println("📢 회원 탈퇴가 취소되었습니다.");
			return false;
		}

		try {
			userService.deleteUser(currentUser.getId());
			System.out.println("✅ 회원 탈퇴가 완료되었습니다. 이용해주셔서 감사합니다.");
			this.currentUser = null; // 현재 사용자 정보 초기화
			return true;
		} catch (Exception e) {
			System.err.println("❌ 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
			return false;
		}
	}

	public User getCurrentUser() {
		return this.currentUser;
	}
}
