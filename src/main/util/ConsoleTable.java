package main.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConsoleTable {

    public static void printTable(String title, String[] headers, List<String[]> rows) {
        if (title != null && !title.isEmpty()) System.out.println(title);
        if (headers == null) headers = new String[0];
        if (rows == null) rows = new ArrayList<>();

        final int cols = headers.length;
        final int[] w = new int[cols];
        final Align[] bodyAlign = inferAlignments(headers, rows);
        final Align[] headAlign = new Align[cols];
        for (int i = 0; i < cols; i++) headAlign[i] = Align.CENTER;

        // 1) 너비 계산(ANSI/0폭 제거본 기준)
        for (int c = 0; c < cols; c++) {
            w[c] = Math.max(w[c], visualWidth(headers[c]));
        }
        for (String[] r : rows) {
            for (int c = 0; c < cols && c < r.length; c++) {
                w[c] = Math.max(w[c], visualWidth(r[c]));
            }
        }

        // 2) 프레임 + 출력
        printLine('┌', '┬', '┐', w);
        printRow(headers, w, headAlign);
        printLine('├', '┼', '┤', w);
        for (String[] r : rows) printRow(r, w, bodyAlign);
        printLine('└', '┴', '┘', w);
    }

    public static List<String[]> withIndex(List<String[]> rows) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String[] nr = new String[r.length + 1];
            nr[0] = String.valueOf(i + 1);
            System.arraycopy(r, 0, nr, 1, r.length);
            out.add(nr);
        }
        return out;
    }

    /* ===== 내부 구현 ===== */

    private enum Align { LEFT, CENTER, RIGHT }

    // ANSI CSI 시퀀스 제거 (예: \u001B[31m)
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    // 자동 링크화 방지: 하이픈 주변에 WORD JOINER(0폭, 줄바꿈 금지) 삽입
    private static String antiLinkify(String s) {
        if (s == null || s.isEmpty()) return "";
        // 계좌번호/전화번호류에서 하이픈마다 2060 삽입 (보이는 건 동일, 폭은 0)
        return s.replace("-", "\u2060-\u2060");
    }

    // 폭 계산 전 정리: ANSI 제거 + 0폭 문자 제거
    private static String normalizeForWidth(String s) {
        if (s == null) return "";
        String t = ANSI.matcher(s).replaceAll("");
        // ZWSP(200B), ZWNJ(200C), ZWJ(200D), WJ(2060), VS-16(FE0F) 제거
        return t.replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "")
                .replace("\uFE0F", "");
    }

    private static Align[] inferAlignments(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        Align[] out = new Align[cols];
        for (int c = 0; c < cols; c++) {
            String h = headers[c] == null ? "" : headers[c];
            String hn = h.replaceAll("\\s+", "");
            if (hn.equals("번호") || hn.equalsIgnoreCase("no") || hn.equalsIgnoreCase("idx")) {
                out[c] = Align.CENTER; continue;
            }
            if (hn.contains("금액") || hn.contains("잔액") || hn.contains("가격")
                    || hn.equalsIgnoreCase("amount") || hn.equalsIgnoreCase("price")) {
                out[c] = Align.RIGHT; continue;
            }
            // 데이터 힌트로 숫자/통화 추정
            int samples=0, numeric=0;
            for (String[] row : rows) {
                if (row == null || c >= row.length) continue;
                String v = row[c] == null ? "" : row[c].trim();
                if (v.isEmpty()) continue;
                samples++;
                if (looksNumeric(v)) numeric++;
            }
            out[c] = (samples>0 && numeric*1.0/samples>=0.7) ? Align.RIGHT : Align.LEFT;
        }
        return out;
    }

    private static boolean looksNumeric(String v) {
        String norm = normalizeForWidth(v)
                .replace(",", "")
                .replace("원", "")
                .replace("₩", "")
                .replaceAll("\\s+", "");
        return norm.matches("^[+-]?\\d+(\\.\\d+)?$");
    }

    private static void printLine(char left, char mid, char right, int[] w) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < w.length; i++) {
            int len = w[i] + 2; // 양옆 1칸 여백
            for (int k = 0; k < len; k++) sb.append('─');
            sb.append(i == w.length - 1 ? right : mid);
        }
        System.out.println(sb);
    }

    private static void printRow(String[] cells, int[] w, Align[] align) {
        StringBuilder sb = new StringBuilder();
        sb.append('│');
        for (int i = 0; i < w.length; i++) {
            String raw = (cells != null && i < cells.length && cells[i] != null) ? cells[i] : "";
            String text = antiLinkify(raw);            // 출력용(하이퍼링크 방지)
            int cellW = visualWidth(raw);              // 폭 계산은 정리본 기준
            int padTotal = Math.max(0, w[i] - cellW);

            int leftPad = 1, rightPad = 1;            // 기본 여백 1칸
            switch (align[i]) {
                case LEFT -> rightPad += padTotal;
                case RIGHT -> leftPad += padTotal;
                case CENTER -> {
                    leftPad += padTotal / 2;
                    rightPad += padTotal - (padTotal / 2);
                }
            }
            for (int k = 0; k < leftPad; k++) sb.append(' ');
            sb.append(text);
            for (int k = 0; k < rightPad; k++) sb.append(' ');
            sb.append('│');
        }
        System.out.println(sb);
    }

    /** 이모지/전각/조합 고려 실제 표시 폭 계산 */
    public static int visualWidth(String s) {
        String t = normalizeForWidth(s);
        if (t.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            i += Character.charCount(cp);

            // 결합 문자류는 위에서 제거했지만, 혹시 남은 마크는 폭 0
            int type = Character.getType(cp);
            if (type == Character.NON_SPACING_MARK ||
                    type == Character.ENCLOSING_MARK ||
                    type == Character.COMBINING_SPACING_MARK) continue;

            // 이모지/픽토그래프는 2폭 취급
            if ((cp >= 0x1F300 && cp <= 0x1FAFF) || (cp >= 0x2600 && cp <= 0x27FF)) {
                width += 2; continue;
            }

            if (isWide(cp)) width += 2;
            else width += 1;
        }
        return width;
    }

    private static boolean isWide(int cp) {
        if ((cp >= 0xFF01 && cp <= 0xFF60) || (cp >= 0xFFE0 && cp <= 0xFFE6)) return true;
        Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
        if (sc == Character.UnicodeScript.HAN ||
                sc == Character.UnicodeScript.HIRAGANA ||
                sc == Character.UnicodeScript.KATAKANA ||
                sc == Character.UnicodeScript.HANGUL) return true;

        Character.UnicodeBlock blk = Character.UnicodeBlock.of(cp);
        return blk == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                blk == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS    ||
                blk == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS     ||
                blk == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                blk == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                blk == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO  ||
                blk == Character.UnicodeBlock.HANGUL_JAMO                ||
                blk == Character.UnicodeBlock.HANGUL_SYLLABLES           ||
                blk == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS;
    }
}
