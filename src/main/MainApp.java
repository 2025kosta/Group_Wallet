package main;

import java.util.Scanner;

import main.controller.AccountController;
import main.controller.GroupController;
import main.controller.UserController;
import main.domain.User;

public class MainApp {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		UserController userController = new UserController(scanner);

		while (true) {
			System.out.println("\n================= 📊 자산관리 시스템 =================");
			System.out.println("1. 👤 사용자 생성");
			System.out.println("2. 🔐 로그인");
			System.out.println("0. ❌ 종료");
			System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			System.out.print("👉 선택(번호 입력): ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1":
				userController.createUser();
				break;
			case "2":
				// 로그인 성공 시, 로그인된 사용자의 메뉴 루프로 진입
				if (userController.login()) {
					showLoggedInMenu(scanner, userController);
				}
				break;
			case "0":
				System.out.println("\n✅ 시스템을 종료합니다.");
				return; // 프로그램 종료
			default:
				System.out.println("\n❗ 잘못된 번호입니다. 다시 입력해주세요.");
			}
		}
	}

	private static void showLoggedInMenu(Scanner scanner, UserController userController) {
		User currentUser = userController.getCurrentUser();
		AccountController accountController = new AccountController(scanner, currentUser);
		GroupController groupController = new GroupController(scanner, currentUser);

		while (true) {
			System.out.println("\n================ 📈 메인 메뉴 (" + currentUser.getName() + "님) =================");
			System.out.println("1. 🏦 계좌 관리");
			System.out.println("2. 👥 모임통장 관리");
			System.out.println("0. 🚪 로그아웃");
			System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			System.out.print("👉 선택(번호 입력): ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1" -> accountController.showMenu();
			case "2" -> groupController.showMenu();

			case "0" -> System.out.println("\n✅ 로그아웃되었습니다.");
			default -> System.out.println("\n❗ 잘못된 번호입니다. 다시 입력해주세요.");
			}
		}
	}
}