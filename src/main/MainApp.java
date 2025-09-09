package main;

import java.util.Scanner;

import main.controller.AccountController;
import main.controller.CardController;
import main.controller.GroupController;
import main.controller.TransactionController;
import main.controller.UserController;
import main.domain.User;

public class MainApp {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		UserController userController = new UserController(scanner);

		while (true) {
			System.out.println("\n================= 📊 모임통장 시스템 =================");
			System.out.println("1. 👤 사용자 생성");
			System.out.println("2. 🔐 로그인");
			System.out.println("0. ❌ 종료");
			System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			System.out.print("👉 선택(번호 입력): ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1" -> userController.createUser();
			case "2" -> {
				if (userController.login()) {
					showLoggedInMenu(scanner, userController);
				}
			}
			case "0" -> {
				System.out.println("\n✅ 시스템을 종료합니다.");
				return;
			}
			default -> System.out.println("\n❗ 잘못된 번호입니다. 다시 입력해주세요.");
			}
		}
	}

	private static void showLoggedInMenu(Scanner scanner, UserController userController) {
		User currentUser = userController.getCurrentUser();
		AccountController accountController = new AccountController(scanner, currentUser);
		GroupController groupController = new GroupController(scanner, currentUser);

		// ✅ 카드 컨트롤러: 기존 시그니처 유지
		CardController cardController = new CardController(scanner);

		// ✅ 트랜잭션 컨트롤러: (scanner, currentUser) 생성자 + showMenu() 호출로 정리
		TransactionController transactionController = new TransactionController(scanner, currentUser);

		while (true) {
			System.out.println("\n================ 📈 메인 메뉴 (" + currentUser.getName() + "님) =================");
			System.out.println("1. 🏦 계좌 관리");
			System.out.println("2. 👥 모임통장 관리");
			System.out.println("3. 💳 카드 관리");
			System.out.println("4. 💰 거래 기록");
			System.out.println("5. 🙍 회원 탈퇴");
			System.out.println("0. 🚪 로그아웃");
			System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			System.out.print("👉 선택(번호 입력): ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1":
				accountController.showMenu();
				break;
			case "2":
				groupController.showMenu();
				break;
			case "3":
				cardController.showMenu(currentUser);
				break;
			case "4":
				transactionController.showMenu();
				break;
			case "5":
				boolean isDeleted = userController.deleteCurrentUser();
				if (isDeleted) {
					return;
				}
				break;

			case "0":
				System.out.println("\n✅ 로그아웃되었습니다.");
				return;

			default:
				System.out.println("\n❗ 잘못된 번호입니다. 다시 입력해주세요.");
			}
		}
	}
}
