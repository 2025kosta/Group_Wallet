package main.controller;

import main.domain.Account;
import main.domain.Card;
import main.domain.User;
import main.enums.CardBrand;
import main.enums.CardStatus;
import main.service.AccountService;
import main.service.CardService;
import main.util.ConsoleTable;

import java.util.*;
import java.util.stream.Collectors;

public class CardController {
    private final Scanner scanner;
    private final CardService cardService;
    private final AccountService accountService;
    private final Random random = new Random();

    public CardController(Scanner scanner) {
        this.scanner = scanner;
        this.cardService = new CardService();
        this.accountService = new AccountService();
    }

    public void showMenu(User currentUser) {
        while (true) {
            System.out.println("\n----- [💳 카드 관리] -----");
            System.out.println("1. 카드 등록");
            System.out.println("2. 카드 목록 조회");
            System.out.println("3. 카드 상태 변경(ACTIVE/BLOCKED)");
            System.out.println("4. 카드 삭제");
            System.out.println("0. 이전 메뉴");
            System.out.print("👉 선택(번호 입력): ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> registerCard(currentUser);
                    case "2" -> listCards(currentUser);
                    case "3" -> changeStatus(currentUser);
                    case "4" -> deleteCard(currentUser);
                    case "0" -> { return; }
                    default -> System.out.println("\n❗ 잘못된 번호입니다. 다시 입력해주세요.");
                }
            } catch (Exception e) {
                System.err.println("❌ 처리 실패: " + e.getMessage());
            }
        }
    }

    // 1) 카드 등록
    private void registerCard(User currentUser) {
        System.out.println("\n----- [💳 카드 등록] -----");
        Long accountId = pickAccountId(currentUser, /*showAll*/ false, /*showBalance*/ true);
        if (accountId == null) return;

        CardBrand brand = pickBrand();
        if (brand == null) return;

        Card created = null;
        String last = null;
        for (int i = 0; i < 7 && created == null; i++) {
            String masked = generateDigitCardNo();
            last = masked;
            try {
                created = cardService.register(accountId, masked, brand.name());
            } catch (IllegalArgumentException ignoreDup) {
                // 중복 시 재시도
            }
        }
        if (created == null) {
            throw new IllegalStateException("카드 번호 충돌로 등록에 실패했습니다. 다시 시도해주세요. (마지막: " + last + ")");
        }

        Account acc = accountService.findMyAccounts(currentUser.getId())
                .stream().filter(a -> a.getId() == accountId).findFirst().orElse(null);

        String[][] rows = {
                {
                        created.getMaskedNo(),
                        brandDisplay(created.getBrand()),
                        created.getStatus().name(),
                        acc != null ? acc.getName() : "-",
                        acc != null ? acc.getAccountNumber() : "-",
                        acc != null ? acc.getType().name() : "-",
                        acc != null ? String.format("%,d원", acc.getBalance()) : "-"
                }
        };
        ConsoleTable.printTable("✅ 카드가 등록되었습니다.",
                new String[]{"카드번호","브랜드","상태","계좌이름","계좌번호","유형","잔액"},
                java.util.Arrays.asList(rows)
        );
    }

    // 2) 카드 목록(카드→계좌 순, 표)
    private void listCards(User currentUser) {
        System.out.println("\n----- [💳 카드 목록] -----");
        Map<Long, Account> accMap = getMyAccountMap(currentUser);
        List<Card> cards = getUserCards(currentUser);

        if (cards.isEmpty()) {
            System.out.println("✅ 등록된 카드가 없습니다.");
            return;
        }

        cards.sort(Comparator
                .comparing((Card c) -> brandDisplay(c.getBrand()))
                .thenComparing(Card::getMaskedNo, Comparator.nullsLast(String::compareTo)));

        List<String[]> rows = new ArrayList<>();
        for (Card c : cards) {
            Account a = accMap.get(c.getAccountId());
            rows.add(new String[]{
                    c.getMaskedNo(),
                    brandDisplay(c.getBrand()),
                    c.getStatus().name(),
                    a != null ? a.getName() : "-",
                    a != null ? a.getAccountNumber() : "-",
                    a != null ? a.getType().name() : "-",
                    a != null ? String.format("%,d원", a.getBalance()) : "-"
            });
        }
        ConsoleTable.printTable(null,
                new String[]{"카드번호","브랜드","상태","계좌이름","계좌번호","유형","잔액"},
                rows
        );
    }

    // 3) 상태 변경
    private void changeStatus(User currentUser) {
        System.out.println("\n----- [💳 카드 상태 변경] -----");
        Card selected = pickCardFromAllMyCards(currentUser);
        if (selected == null) return;

        System.out.print("상태(ACTIVE/BLOCKED): ");
        String in = scanner.nextLine().trim().toUpperCase();
        CardStatus newSt;
        try {
            newSt = CardStatus.valueOf(in);
        } catch (Exception e) {
            System.err.println("❌ 처리 실패: ACTIVE 또는 BLOCKED만 입력하세요.");
            return;
        }

        if (selected.getStatus() == newSt) {
            System.out.println("ℹ️ 변경 없음: 이미 " + newSt + " 상태입니다.");
            return;
        }
        cardService.changeStatus(selected.getId(), newSt);
        System.out.println("✅ 카드 상태가 변경되었습니다.");
    }

    // 4) 삭제
    private void deleteCard(User currentUser) {
        System.out.println("\n----- [💳 카드 삭제] -----");
        Card selected = pickCardFromAllMyCards(currentUser);
        if (selected == null) return;

        try {
            cardService.delete(selected.getId());
            System.out.println("✅ 카드가 삭제되었습니다.");
        } catch (IllegalStateException e) {
            System.err.println("❌ 삭제 실패: " + e.getMessage());
        }
    }

    // ────────────── 내부 유틸 ──────────────

    private Map<Long, Account> getMyAccountMap(User currentUser) {
        return accountService.findMyAccounts(currentUser.getId())
                .stream().collect(Collectors.toMap(Account::getId, a -> a));
    }

    private List<Card> getUserCards(User currentUser) {
        var am = getMyAccountMap(currentUser);
        if (am.isEmpty()) return java.util.Collections.emptyList();
        var ids = new ArrayList<Long>(am.keySet());
        return cardService.findCardsByAccountIds(ids);
    }

    /** 표기준 계좌 선택: 번호 | 유형 | 이름 | 계좌번호 | 잔액 */
    private Long pickAccountId(User currentUser, boolean showAllOption, boolean showBalance) {
        List<Account> accounts = accountService.findMyAccounts(currentUser.getId());
        if (accounts.isEmpty()) {
            System.out.println("✅ 등록된 계좌가 없습니다.");
            return null;
        }
        List<String[]> rows = new ArrayList<>();
        for (Account a : accounts) {
            rows.add(new String[]{
                    a.getType().name(),
                    a.getName(),
                    a.getAccountNumber(),
                    showBalance ? String.format("%,d원", a.getBalance()) : "-"
            });
        }
        var rowsWithIdx = ConsoleTable.withIndex(rows);
        ConsoleTable.printTable("------ [🏦 계좌 선택] ------",
                new String[]{"번호","유형","계좌이름","계좌번호","잔액"},
                rowsWithIdx);
        if (showAllOption) System.out.println("0) 전체");
        else System.out.println("0) 취소");
        System.out.print("👉 선택(번호): ");
        String sel = scanner.nextLine().trim();
        if (sel.equals("0") || sel.isEmpty()) {
            System.out.println("❎ 작업이 취소되었습니다.");
            return null;
        }
        try {
            int idx = Integer.parseInt(sel) - 1;
            if (idx < 0 || idx >= accounts.size()) throw new NumberFormatException();
            return accounts.get(idx).getId();
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 올바른 번호를 선택하세요.");
            return null;
        }
    }

    /** 카드 선택도 표로(번호 포함) */
    private Card pickCardFromAllMyCards(User currentUser) {
        Map<Long, Account> accMap = getMyAccountMap(currentUser);
        List<Card> cards = getUserCards(currentUser);
        if (cards.isEmpty()) {
            System.out.println("✅ 등록된 카드가 없습니다.");
            return null;
        }
        cards.sort(Comparator
                .comparing((Card c) -> brandDisplay(c.getBrand()))
                .thenComparing(Card::getMaskedNo, Comparator.nullsLast(String::compareTo)));

        List<String[]> rows = new ArrayList<>();
        for (Card c : cards) {
            Account a = accMap.get(c.getAccountId());
            rows.add(new String[]{
                    c.getMaskedNo(),
                    brandDisplay(c.getBrand()),
                    c.getStatus().name(),
                    a != null ? a.getName() : "-",
                    a != null ? a.getAccountNumber() : "-",
                    a != null ? a.getType().name() : "-"
            });
        }
        var withIdx = ConsoleTable.withIndex(rows);
        ConsoleTable.printTable("----- [💳 카드 선택] -----",
                new String[]{"번호","카드번호","브랜드","상태","계좌이름","계좌번호","유형"},
                withIdx
        );
        System.out.println("0) 취소");
        System.out.print("👉 카드 선택(번호): ");
        String sel = scanner.nextLine().trim();
        if (sel.equals("0") || sel.isEmpty()) {
            System.out.println("❎ 작업이 취소되었습니다.");
            return null;
        }
        try {
            int idx = Integer.parseInt(sel) - 1;
            if (idx < 0 || idx >= cards.size()) throw new NumberFormatException();
            return cards.get(idx);
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 올바른 번호를 선택하세요.");
            return null;
        }
    }

    private CardBrand pickBrand() {
        System.out.println("브랜드 선택:");
        CardBrand[] brands = CardBrand.values();
        List<String[]> rows = new ArrayList<>();
        for (CardBrand b : brands) {
            rows.add(new String[]{ b.displayName(), b.name() });
        }
        var withIdx = ConsoleTable.withIndex(rows);
        ConsoleTable.printTable(null, new String[]{"번호","브랜드(표시)","코드"}, withIdx);
        System.out.println("0) 취소");
        System.out.print("👉 선택: ");
        String sel = scanner.nextLine().trim();
        if (sel.equals("0") || sel.isEmpty()) {
            System.out.println("❎ 작업이 취소되었습니다.");
            return null;
        }
        try {
            int idx = Integer.parseInt(sel) - 1;
            if (idx < 0 || idx >= brands.length) throw new NumberFormatException();
            return brands[idx];
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 올바른 번호를 선택하세요.");
            return null;
        }
    }

    // "1234-5678-9012-3456"
    private String generateDigitCardNo() {
        StringBuilder sb = new StringBuilder(19);
        for (int i = 0; i < 16; i++) {
            int d = random.nextInt(10);
            sb.append(d);
            if (i == 3 || i == 7 || i == 11) sb.append('-');
        }
        return sb.toString();
    }

    private String brandDisplay(String stored) {
        if ("SAMSUNG".equalsIgnoreCase(stored)) return "SAMSUNG";
        if ("HYUNDAI".equalsIgnoreCase(stored)) return "HYUNDAI";
        return "BC";
    }
}
