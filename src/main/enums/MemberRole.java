package main.enums;

/**
 * 모임 멤버 역할(2단계) OWNER : 모든 관리 권한(멤버 추가/제거, 이름 변경, 카드/거래 삭제 등) – 항상 최소 1명 유지
 * MEMBER: 기본 사용 권한(조회, 거래 등록 등)
 */
public enum MemberRole {
	OWNER, MEMBER
}
