package main.enums;

public enum CardBrand {
    BC("BC"),
    SAMSUNG("삼성"),
    HYUNDAI("현대");

    private final String display;

    CardBrand(String display) { this.display = display; }

    public String displayName() { return display; }
}
