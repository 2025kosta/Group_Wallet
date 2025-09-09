package main.controller;

import main.domain.Account;
import main.domain.Card;
import main.domain.User;
import main.dto.TransactionListDto;
import main.service.AccountService;
import main.service.TransactionService;
import main.service.CardService;
import main.util.ConsoleTable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * 거래 기록 컨트롤러
 * - 콘솔 출력 형식 팀원 것과 통일
 * - 표는 ConsoleTable 사용 (전각 폭 대응)
 */
public class TransactionController {
    private final Scanner scanner;
    private final User currentUser;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CardService cardService;

    public TransactionController(Scanner scanner, User currentUser) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.transactionService = new TransactionService();
        this.accountService = new AccountService();
        this.cardService = new CardService();
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n----- [💰 거래 기록] -----");
            System.out.println("1. 지출(CARD) 추가");
            System.out.println("2. 이체(TRANSFER)");
            System.out.println("3. 거래 조회/검색");
            System.out.println("0. 이전 메뉴");
            System.out.print("👉 선택(번호 입력): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addExpenseCard(); break;
                case "2": transfer(); break;
                case "3": searchTransactions(); break;
                case "0": return;
                default: System.out.println("❗ 잘못된 번호입니다. 다시 입력해주세요.");
            }
        }
    }

    /** 1) 카드 지출 */
    private void addExpenseCard() {
        System.out.println("\n----- [💳 지출(CARD) 추가] -----");

        // 카드 선택(내 모든 계좌 → 모든 카드) : 잔액 포함 표시
        CardPicker.PickedCard selected = new CardPicker(scanner, currentUser).pickCardFromAllMyCards();
        if (selected == null) return;

        // 금액
        System.out.print("\n금액: ");
        String amountStr = scanner.nextLine().trim();
        if (amountStr.isEmpty()) {
            System.err.println("❌ 처리 실패: 금액은 필수입니다.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountStr);
            if (amount <= 0) {
                System.err.println("❌ 처리 실패: 금액은 0보다 커야 합니다.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 금액은 숫자만 입력하세요.");
            return;
        }

        // 메모
        System.out.print("메모(선택): ");
        String memo = scanner.nextLine().trim();
        if (memo.isEmpty()) memo = null;

        // 발생 시각
        System.out.print("발생 시각 (yyyy-MM-dd HH:mm, 엔터=지금): ");
        String when = scanner.nextLine().trim();
        LocalDateTime occurredAt;
        if (when.isEmpty()) {
            occurredAt = LocalDateTime.now(); // 즉시 진행 (멈춤 없음)
        } else {
            try {
                occurredAt = LocalDateTime.parse(when, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception e) {
                System.err.println("❌ 처리 실패: 날짜/시간 형식이 올바르지 않습니다. 예) 2025-09-09 13:20");
                return;
            }
        }

        try {
            transactionService.addExpenseCard(selected.cardId, amount, memo, occurredAt, currentUser.getId());
            System.out.println("✅ 기록이 등록되었습니다.");
        } catch (Exception e) {
            System.err.println("❌ 처리 실패: " + e.getMessage());
        }
    }

    /** 2) 이체 */
    private void transfer() {
        System.out.println("\n----- [🔁 이체(TRANSFER)] -----");
        List<Account> myAccounts = accountService.findMyAccounts(currentUser.getId());
        if (myAccounts.isEmpty()) {
            System.out.println("✅ 사용자의 계좌가 없습니다.");
            return;
        }

        Long fromId = pickAccount("출금", myAccounts);
        if (fromId == null) return;
        Long toId = pickAccount("입금", myAccounts);
        if (toId == null) return;
        if (fromId.equals(toId)) {
            System.err.println("❌ 처리 실패: 동일한 계좌로 이체할 수 없습니다.");
            return;
        }

        System.out.print("금액: ");
        String amountStr = scanner.nextLine().trim();
        if (amountStr.isEmpty()) {
            System.err.println("❌ 처리 실패: 금액은 필수입니다.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountStr);
            if (amount <= 0) {
                System.err.println("❌ 처리 실패: 금액은 0보다 커야 합니다.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 금액은 숫자만 입력하세요.");
            return;
        }

        System.out.print("메모(선택): ");
        String memo = scanner.nextLine().trim();
        if (memo.isEmpty()) memo = null;

        try {
            transactionService.transfer(fromId, toId, amount, memo, currentUser.getId());
            System.out.println("✅ 기록이 등록되었습니다.");
        } catch (Exception e) {
            System.err.println("❌ 처리 실패: " + e.getMessage());
        }
    }

    /** 3) 검색 */
    private void searchTransactions() {
        System.out.println("\n----- [🔎 거래 조회/검색] -----");
        List<Account> myAccounts = accountService.findMyAccounts(currentUser.getId());
        Long accountFilter = null;

        if (!myAccounts.isEmpty()) {
            // 표용 데이터 만들기
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            for (Account a : myAccounts) {
                rows.add(new String[]{
                        a.getType().name(),
                        a.getName(),
                        a.getAccountNumber(),
                        String.format("%,d원", a.getBalance())
                });
            }
            // 번호 컬럼 포함 + 타이틀 폭 보정(이모지 폭 이슈로 대시 1개씩 추가)
            ConsoleTable.printTable(
                    "------ [🏦 계좌 선택] ------",
                    new String[]{"번호", "유형", "계좌이름", "계좌번호", "잔액"},
                    ConsoleTable.withIndex(rows)
            );
            System.out.println("0) 전체");
            System.out.print("👉 선택(번호): ");
            String sel = scanner.nextLine().trim();
            if (!sel.isEmpty() && !"0".equals(sel)) {
                try {
                    int idx = Integer.parseInt(sel) - 1;
                    if (idx >= 0 && idx < myAccounts.size()) {
                        accountFilter = myAccounts.get(idx).getId();
                    }
                } catch (NumberFormatException ignore) {}
            }
        }

        LocalDate from = null, to = null;
        System.out.print("시작일 (yyyy-MM-dd, 엔터=제외): ");
        String s = scanner.nextLine().trim();
        if (!s.isEmpty()) {
            try { from = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
            catch (Exception e) { System.err.println("형식 오류, 시작일 제외"); }
        }
        System.out.print("종료일 (yyyy-MM-dd, 엔터=제외): ");
        s = scanner.nextLine().trim();
        if (!s.isEmpty()) {
            try { to = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
            catch (Exception e) { System.err.println("형식 오류, 종료일 제외"); }
        }

        Long min = null, max = null;
        System.out.print("최소 금액(엔터=제외): ");
        s = scanner.nextLine().trim();
        if (!s.isEmpty()) try { min = Long.parseLong(s); } catch (NumberFormatException ignore) {}
        System.out.print("최대 금액(엔터=제외): ");
        s = scanner.nextLine().trim();
        if (!s.isEmpty()) try { max = Long.parseLong(s); } catch (NumberFormatException ignore) {}

        List<TransactionListDto> rows = transactionService.search(currentUser.getId(), accountFilter, from, to, min, max);
        if (rows.isEmpty()) {
            System.out.println("✅ 조건에 맞는 기록이 없습니다.");
            return;
        }

        // 거래 목록 표 (ID 미노출, 계좌/카드/방향/수단/금액/메모)
        List<String[]> out = new ArrayList<>();
        for (TransactionListDto r : rows) {
            out.add(new String[]{
                    r.accountName,
                    r.accountNumber,
                    (r.cardMaskedNo == null ? "-" : r.cardMaskedNo),
                    r.kind.name(),
                    r.method.name(),
                    String.format("%,d원", r.amount),
                    (r.memo == null ? "-" : r.memo)
            });
        }
        ConsoleTable.printTable("\n----- [📜 거래 목록] -----",
                new String[]{"계좌 이름", "계좌번호", "카드번호", "방향", "수단", "금액", "메모"},
                out
        );
    }

    private Long pickAccount(String label, List<Account> list) {
        System.out.println("\n----- [🔁 " + label + " 계좌 선택] -----");

        if (list.isEmpty()) {
            System.out.println("✅ 사용자의 계좌가 없습니다.");
            return null;
        }

        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Account a : list) {
            rows.add(new String[]{
                    a.getType().name(),
                    a.getName(),
                    a.getAccountNumber(),
                    String.format("%,d원", a.getBalance())
            });
        }

        ConsoleTable.printTable(
                "------ [🏦 계좌 선택] ------",
                new String[]{"번호", "유형", "계좌이름", "계좌번호", "잔액"},
                ConsoleTable.withIndex(rows)
        );
        System.out.println("0) 취소");
        System.out.print("👉 계좌 선택(번호): ");
        String sel = scanner.nextLine().trim();
        if (sel.equals("0") || sel.isEmpty()) {
            System.out.println("❎ 작업이 취소되었습니다.");
            return null;
        }
        try {
            int idx = Integer.parseInt(sel) - 1;
            if (idx < 0 || idx >= list.size()) throw new NumberFormatException();
            return list.get(idx).getId();
        } catch (NumberFormatException e) {
            System.err.println("❌ 처리 실패: 올바른 번호를 선택하세요.");
            return null;
        }
    }

    /** 내부: 카드 선택 공통 (잔액 포함) */
    private class CardPicker {
        private final Scanner scanner;
        private final User user;

        CardPicker(Scanner scanner, User user) {
            this.scanner = scanner;
            this.user = user;
        }

        class PickedCard {
            long cardId;
            String maskedNo;
            String accountLine;
            PickedCard(long cardId, String maskedNo, String accountLine) {
                this.cardId = cardId;
                this.maskedNo = maskedNo;
                this.accountLine = accountLine;
            }
        }

        PickedCard pickCardFromAllMyCards() {
            List<Account> accounts = accountService.findMyAccounts(user.getId());
            if (accounts.isEmpty()) {
                System.out.println("✅ 등록된 카드가 없습니다.");
                return null;
            }

            // 내 모든 계좌 id 모아 카드 조회
            List<Long> ids = new ArrayList<>();
            for (Account a : accounts) ids.add(a.getId());
            List<Card> cards = cardService.findCardsByAccountIds(ids);

            if (cards.isEmpty()) {
                System.out.println("✅ 등록된 카드가 없습니다.");
                return null;
            }

            // 정렬: 브랜드(문자열) → 카드번호
            cards.sort(Comparator.comparing(Card::getBrand, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Card::getMaskedNo));

            // 표 데이터 준비 (번호 | 카드번호 | 브랜드 | 상태 | 계좌이름 | 계좌번호 | 잔액 | 유형)
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < cards.size(); i++) {
                var c = cards.get(i);
                Account a = null;
                for (Account acc : accounts) if (acc.getId() == c.getAccountId()) { a = acc; break; }

                String accName = (a == null ? "-" : a.getName());
                String accNo   = (a == null ? "-" : a.getAccountNumber());
                String bal     = (a == null ? "-" : String.format("%,d원", a.getBalance()));
                String typ     = (a == null ? "-" : a.getType().name());

                rows.add(new String[]{
                        String.valueOf(i + 1),
                        c.getMaskedNo(),
                        c.getBrand(),
                        c.getStatus().name(),
                        accName,
                        accNo,
                        bal,
                        typ
                });
            }

            ConsoleTable.printTable(
                    "----- [💳 카드 선택] -----",
                    new String[]{"번호", "카드번호", "브랜드", "상태", "계좌이름", "계좌번호", "잔액", "유형"},
                    rows
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
                Card c = cards.get(idx);

                Account a = null;
                for (Account acc : accounts) if (acc.getId() == c.getAccountId()) { a = acc; break; }
                String accountLine = (a == null)
                        ? "-"
                        : String.format("[%s] %s (%s) | 잔액 %s",
                        a.getType().name(),
                        a.getName(),
                        a.getAccountNumber(),
                        String.format("%,d원", a.getBalance()));

                return new PickedCard(c.getId(), c.getMaskedNo(), accountLine);
            } catch (NumberFormatException e) {
                System.err.println("❌ 처리 실패: 올바른 번호를 선택하세요.");
                return null;
            }
        }
    }
}
