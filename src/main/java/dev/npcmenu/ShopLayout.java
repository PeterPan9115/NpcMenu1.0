package dev.npcmenu;

/** Anordnung von Preis (zu tauschendes Item) und Ware im Shop-Fenster. */
public enum ShopLayout {
    ROWS_PRICE_TOP("Reihen: Preis oben, Ware unten"),
    ROWS_GOODS_TOP("Reihen: Ware oben, Preis unten"),
    PAIRS_PRICE_LEFT("Paare: Preis links  ->  Ware rechts"),
    PAIRS_PRICE_RIGHT("Paare: Ware links  <-  Preis rechts");

    public final String label;

    ShopLayout(String label) {
        this.label = label;
    }

    public boolean pairs() {
        return this == PAIRS_PRICE_LEFT || this == PAIRS_PRICE_RIGHT;
    }

    public int size() {
        return pairs() ? 36 : 27;
    }

    private int base(int i) {
        return (i / 3) * 9 + (i % 3) * 3;
    }

    /** Slot des Preis-Items fuer Tausch i. */
    public int in(int i) {
        return switch (this) {
            case ROWS_PRICE_TOP -> i;
            case ROWS_GOODS_TOP -> 9 + i;
            case PAIRS_PRICE_LEFT -> base(i);
            case PAIRS_PRICE_RIGHT -> base(i) + 2;
        };
    }

    /** Slot der Ware fuer Tausch i. */
    public int out(int i) {
        return switch (this) {
            case ROWS_PRICE_TOP -> 9 + i;
            case ROWS_GOODS_TOP -> i;
            case PAIRS_PRICE_LEFT -> base(i) + 2;
            case PAIRS_PRICE_RIGHT -> base(i);
        };
    }

    /** Pfeil-Slot (nur Paare-Layouts), sonst -1. */
    public int arrow(int i) {
        return pairs() ? base(i) + 1 : -1;
    }

    public int infoSlot() { return size() - 5; }
    public int layoutSlot() { return size() - 3; }
    public int doneSlot() { return size() - 1; }

    public ShopLayout next() {
        ShopLayout[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
