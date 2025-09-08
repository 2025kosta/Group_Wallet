package main.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import main.db.DbUtil;
import main.domain.Account;
import main.domain.GroupMember;
import main.enums.AccountType;
import main.enums.MemberRole;
import main.repository.AccountRepository;
import main.repository.GroupRepository;

public class AccountService {
	private final AccountRepository accountRepository = new AccountRepository();
	private final GroupRepository groupRepository = new GroupRepository();

	public Account createPersonalAccount(String name, long ownerUserId) {
		accountRepository.findByNameAndOwnerUserId(name, ownerUserId).ifPresent(account -> {
			// 중복된 계좌가 존재하므로, 에러를 발생시켜 프로세스를 중단시킵니다.
			throw new IllegalArgumentException("❌ 동일한 이름의 개인계좌가 이미 존재합니다.");
		});
		Account newAccount = Account.createPersonal(0, name, ownerUserId);
		return accountRepository.save(newAccount);
	}

	public List<Account> findMyAccounts(long userId) {
		return accountRepository.findAllByUserId(userId);
	}

	public List<Account> findMyGroupAccounts(long userId) {
		List<Account> allMyAccounts = accountRepository.findAllByUserId(userId);

		return allMyAccounts.stream().filter(account -> account.getType() == AccountType.GROUP)
				.collect(Collectors.toList());
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

	public void changeAccountName(long accountId, String newName, long currentUserId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("해당 계좌를 찾을 수 없습니다."));

		boolean hasPermission = false;
		if (account.getType() == AccountType.PERSONAL) {
			if (account.getOwnerUserId().equals(currentUserId)) {
				hasPermission = true;
			}
		} else {
			if (groupRepository.isOwner(accountId, currentUserId)) {
				hasPermission = true;
			}
		}

		if (!hasPermission) {
			throw new IllegalStateException("계좌 이름을 변경할 권한이 없습니다.");
		}
		accountRepository.updateName(accountId, newName);
	}

	public void deleteAccount(long accountId, long currentUserId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("❌ 해당 계좌를 찾을 수 없습니다. (ID: " + accountId + ")"));

		boolean hasPermission = false;
		if (account.getType() == AccountType.PERSONAL) {
			if (account.getOwnerUserId().equals(currentUserId)) {
				hasPermission = true;
			}
		} else {
			if (groupRepository.isOwner(accountId, currentUserId)) {
				hasPermission = true;
			}
		}

		if (!hasPermission) {
			throw new IllegalStateException("❌ 계좌를 삭제할 권한이 없습니다.");
		}

//        if (transactionRepository.existsByAccountId(accountId)) {
//            throw new IllegalStateException("❌ 연결된 거래 내역이 있어 계좌를 삭제할 수 없습니다.");
//        }

		accountRepository.deleteById(accountId);
	}
}