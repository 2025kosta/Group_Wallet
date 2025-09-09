package main.util;

import java.util.ArrayList;
import java.util.List;

public class ConsoleTable {

    public static void printTable(String title, String[] headers, List<String[]> rows) {
        if (title != null && !title.isEmpty()) {
            System.out.println(title);
        }
        if (headers == null) headers = new String[0];
        if (rows == null) rows = new ArrayList<>();

        int cols = headers.length;
        int[] w = new int[cols]; // 내용(문자열 자체)의 '표시 폭' 기준 너비

        for (int c = 0; c < cols; c++) {
            w[c] = Math.max(w[c], displayWidth(headers[c]));
        }
        for (String[] r : rows) {
            for (int c = 0; c < cols && c < r.length; c++) {
                w[c] = Math.max(w[c], displayWidth(r[c] == null ? "" : r[c]));
            }
        }

        // 상단 선
        printLine('┌', '┬', '┐', w);

        // 헤더
        printRow(headers, w);

        // 헤더-바디 구분선
        printLine('├', '┼', '┤', w);

        // 바디
        for (String[] r : rows) {
            printRow(r, w);
        }

        // 하단 선
        printLine('└', '┴', '┘', w);
    }

    private static void printLine(char left, char mid, char right, int[] w) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < w.length; i++) {
            int len = w[i] + 2; // 좌/우 여백 1칸씩
            for (int k = 0; k < len; k++) sb.append('─');
            sb.append(i == w.length - 1 ? right : mid);
        }
        System.out.println(sb);
    }

    private static void printRow(String[] cells, int[] w) {
        StringBuilder sb = new StringBuilder();
        sb.append('│');
        for (int i = 0; i < w.length; i++) {
            String text = (cells != null && i < cells.length && cells[i] != null) ? cells[i] : "";
            sb.append(' ');
            sb.append(text);
            // 표시 폭 기준으로 패딩
            int pad = w[i] - displayWidth(text);
            for (int k = 0; k < pad; k++) sb.append(' ');
            sb.append(' ');
            sb.append('│');
        }
        System.out.println(sb);
    }

    /** 한글/일본어/중국어/전각 기호는 2칸 폭으로 계산 */
    public static int displayWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (isWide(cp)) width += 2;
            else width += 1;
        }
        return width;
    }

    private static boolean isWide(int cp) {
        // 전각 폼
        if ((cp >= 0xFF01 && cp <= 0xFF60) || (cp >= 0xFFE0 && cp <= 0xFFE6)) return true;

        Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
        if (sc == Character.UnicodeScript.HAN   ||
                sc == Character.UnicodeScript.HIRAGANA ||
                sc == Character.UnicodeScript.KATAKANA ||
                sc == Character.UnicodeScript.HANGUL) return true;

        // CJK 기호/확장 블록 몇 개 추가
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

    public static List<String[]> withIndex(List<String[]> rows) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String[] nr = new String[r.length + 1];
            nr[0] = String.valueOf(i + 1);          // "번호" 컬럼
            System.arraycopy(r, 0, nr, 1, r.length);
            out.add(nr);
        }
        return out;
    }
}
