package main.domain;

import java.time.LocalDateTime;

/** 서비스 사용자(로그인 주체) */
public class User {

	/** PK: 사용자 식별자 (nullable: false) */
	private final long id;

	/** 사용자 이름(표시용) (nullable: false) */
	private final String name;

	/** 로그인 이메일(고유) (nullable: false) */
	private final String email;

	/** 연락처 (nullable: true) */
	private final String phone;

	/** 생성 시각(감사/이력용) (nullable: false) */
	private final LocalDateTime createdAt;

	private User(long id, String name, String email, String phone, LocalDateTime createdAt) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.phone = phone; // nullable
		this.createdAt = createdAt;
	}

	/** 새 사용자 등록 */
	public static User register(long id, String name, String email, String phone) {
		return new User(id, name, email, phone, LocalDateTime.now());
	}

	// Getter
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	} // nullable

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public static User fromDB(long id, String name, String email, String phone, java.time.LocalDateTime createdAt) {
		// private 생성자를 호출하여 객체 생성
		return new User(id, name, email, phone, createdAt);
	}
}
