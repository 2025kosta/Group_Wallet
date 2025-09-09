package main.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import main.db.DbUtil;
import main.domain.Account;
import main.domain.GroupMember;
import main.enums.AccountType;
import main.enums.MemberRole;
import main.repository.AccountRepository;
import main.repository.GroupRepository;
import main.repository.TransactionRepository;

public class AccountService {
	private final AccountRepository accountRepository = new AccountRepository();
	private final GroupRepository groupRepository = new GroupRepository();
	private final TransactionRepository transactionRepository = new TransactionRepository();

	public Account createPersonalAccount(String name, long ownerUserId, long initialBalance) {
		if (initialBalance < 0) {
			throw new IllegalArgumentException("초기 입금액은 0보다 작을 수 없습니다.");
		}
		accountRepository.findByNameAndOwnerUserId(name, ownerUserId).ifPresent(account -> {
			throw new IllegalArgumentException("❌ 동일한 이름의 개인계좌가 이미 존재합니다.");
		});

		Connection conn = null;
		try {
			conn = DbUtil.getConnection();
			conn.setAutoCommit(false);

			String accountNumber = generateUniqueAccountNumber();
			Account newAccount = Account.createPersonal(0, accountNumber, name.trim(), ownerUserId, initialBalance);
			Account savedAccount = accountRepository.save(newAccount, conn);

			conn.commit();
			return savedAccount;
		} catch (Exception e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
				}
			}
			throw new RuntimeException("개인 계좌 생성 중 오류 발생", e);
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	public List<Account> findMyAccounts(long userId) {
		return accountRepository.findAllByUserId(userId);
	}

	public List<Account> findMyGroupAccounts(long userId) {
		List<Account> allMyAccounts = accountRepository.findAllByUserId(userId);

		return allMyAccounts.stream().filter(account -> account.getType() == AccountType.GROUP)
				.collect(Collectors.toList());
	}

	public void createGroupAccount(String name, long creatorUserId, long initialBalance) {
		if (initialBalance < 0) {
			throw new IllegalArgumentException("초기 입금액은 0보다 작을 수 없습니다.");
		}

		Connection conn = null;
		try {
			conn = DbUtil.getConnection();
			conn.setAutoCommit(false);

			// 1단계: 계좌 생성 (잔액 0)
			String accountNumber = generateUniqueAccountNumber();
			Account groupAccount = Account.createGroup(0, accountNumber, name, initialBalance);
			Account savedAccount = accountRepository.save(groupAccount, conn);

			// 2단계: 생성자를 OWNER로 등록
			GroupMember owner = GroupMember.join(0, savedAccount.getId(), creatorUserId, MemberRole.OWNER);
			groupRepository.save(owner, conn);

			conn.commit(); // 3가지 작업(계좌생성, OWNER등록, 초기입금)을 모두 성공하면 커밋
			System.out.println("✅ 모임통장 생성 및 OWNER 등록이 완료되었습니다.");

		} catch (Exception e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
				}
			}
			throw new RuntimeException("모임통장 생성 중 오류가 발생했습니다.", e);
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	public void changeAccountName(String accountNumber, String newName, long currentUserId) {
		Account account = accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new IllegalArgumentException("해당 계좌를 찾을 수 없습니다."));

		boolean hasPermission = false;
		if (account.getType() == AccountType.PERSONAL) {
			if (account.getOwnerUserId().equals(currentUserId)) {
				hasPermission = true;
			}
		} else {
			if (groupRepository.isOwner(account.getId(), currentUserId)) {
				hasPermission = true;
			}
		}

		if (!hasPermission) {
			throw new IllegalStateException("계좌 이름을 변경할 권한이 없습니다.");
		}
		accountRepository.updateName(account.getId(), newName);
	}

	public void deleteAccount(String accountNumber, long currentUserId) {
		Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(
				() -> new IllegalArgumentException("❌ 해당 계좌를 찾을 수 없습니다. (account number: " + accountNumber + ")"));

		boolean hasPermission = false;
		if (account.getType() == AccountType.PERSONAL) {
			if (account.getOwnerUserId().equals(currentUserId)) {
				hasPermission = true;
			}
		} else {
			if (groupRepository.isOwner(account.getId(), currentUserId)) {
				hasPermission = true;
			}
		}

		if (!hasPermission) {
			throw new IllegalStateException("❌ 계좌를 삭제할 권한이 없습니다.");
		}

		if (transactionRepository.existsByAccountId(account.getId())) {
			throw new IllegalStateException("❌ 연결된 거래 내역이 있어 계좌를 삭제할 수 없습니다.");
		}

		accountRepository.deleteById(account.getId());
	}

	private String generateUniqueAccountNumber() {
		Random random = new Random();
		while (true) {
			String part1 = String.format("%03d", random.nextInt(1000));
			String part2 = String.format("%06d", random.nextInt(1000000));
			String accountNumber = "110-" + part1 + "-" + part2;

			if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
				return accountNumber;
			}
		}
	}
}