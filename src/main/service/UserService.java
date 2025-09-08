package main.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

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

	public User createUser(String name, String email, String phone) {
		userRepository.findByEmail(email).ifPresent(user -> {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
		});

		User newUser = User.register(0, name, email, phone);
		userRepository.save(newUser);
		return newUser;
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

			// 2. 비즈니스 로직 수행
			// - 1단계: 계좌 생성
			Account groupAccount = Account.createGroup(0, name);
			Account savedAccount = accountRepository.save(groupAccount, conn); // Connection 객체 전달

			// - 2단계: 생성자를 OWNER로 등록
			GroupMember owner = GroupMember.join(0, savedAccount.getId(), creatorUserId, MemberRole.OWNER);
			groupRepository.save(owner, conn); // 동일한 Connection 객체 전달

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
			// 5. 작업 완료 후 커넥션 정리
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
}
