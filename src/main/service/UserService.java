package main.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

import main.db.DbUtil;
import main.domain.Account;
import main.domain.GroupMember;
import main.domain.User;
import main.enums.MemberRole;
import main.repository.AccountRepository;
import main.repository.GroupRepository;
import main.repository.UserRepository;

public class UserService {

	private final UserRepository userRepository = new UserRepository();
	private final AccountRepository accountRepository = new AccountRepository();
	private final GroupRepository groupRepository = new GroupRepository();

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
	private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z]{2,20}$");

	public User createUser(String name, String email, String phone) {
		validateUserInfo(name, email);

		userRepository.findByEmail(email).ifPresent(user -> {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
		});

		User newUser = User.register(0, name.trim(), email.trim(), phone);
		return userRepository.save(newUser);
	}

	public Optional<User> login(String email) {
		return userRepository.findByEmail(email);
	}

	public void deleteUser(long userId) {
		// TODO: AccountService, GroupService 등을 호출하여 관련 데이터 확인 로직 추가
		System.out.println("SERVICE LOG: " + userId + " 사용자와 연결된 데이터 삭제 로직 수행");

		userRepository.deleteById(userId);
	}

	public void createGroupAccount(String name, long creatorUserId) {
		Connection conn = null;
		try {
			conn = DbUtil.getConnection();
			// 1. 트랜잭션 시작 (Auto-Commit 비활성화)
			conn.setAutoCommit(false);

			String accountNumber = generateUniqueAccountNumber();

			Account groupAccount = Account.createGroup(0, accountNumber, name);
			Account savedAccount = accountRepository.save(groupAccount, conn); // Connection 객체 전달

			GroupMember owner = GroupMember.join(0, savedAccount.getId(), creatorUserId, MemberRole.OWNER);
			groupRepository.save(owner, conn);

			// 3. 모든 작업이 성공하면 트랜잭션 커밋
			conn.commit();
			System.out.println("✅ 모임통장 생성 및 OWNER 등록이 완료되었습니다.");

		} catch (SQLException e) {
			// 4. 작업 중 하나라도 실패하면 롤백
			if (conn != null) {
				try {
					conn.rollback();
					System.err.println("❌ 트랜잭션 롤백: 모임통장 생성이 취소되었습니다.");
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
			throw new RuntimeException("모임통장 생성 중 오류가 발생했습니다.");
		} finally {
			// 5. 작업 완료 후 정리
			if (conn != null) {
				try {
					conn.setAutoCommit(true); // 커넥션 풀에 반환하기 전 기본 상태로 복원
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void validateUserInfo(String name, String email) {
		if (name == null || email == null) {
			throw new IllegalArgumentException("이름과 이메일은 필수 입력 항목입니다.");
		}

		name = name.trim();
		email = email.trim();

		if (!NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalArgumentException("이름은 한글 또는 영문 2~20자여야 합니다.");
		}

		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
		}
	}

	private String generateUniqueAccountNumber() {
		Random random = new Random();

		while (true) {
			String part1 = String.format("%03d", random.nextInt(1000));
			String part2 = String.format("%06d", random.nextInt(1000000));
			String accountNumber = "110-" + part1 + "-" + part2;

			boolean isDuplicate = accountRepository.findByAccountNumber(accountNumber).isPresent();

			if (!isDuplicate) {
				return accountNumber;
			}

		}
	}
}
