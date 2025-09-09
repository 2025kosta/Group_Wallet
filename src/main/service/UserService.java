package main.service;

import java.util.Optional;
import java.util.regex.Pattern;

import main.domain.User;
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
		if (accountRepository.hasAnyAccount(userId)) {
			throw new IllegalStateException("❌ 소유하거나 참여 중인 계좌가 있어 탈퇴할 수 없습니다.");
		}

		userRepository.deleteById(userId);
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
}
