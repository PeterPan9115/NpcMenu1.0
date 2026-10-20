package dev.npcmenu;

/** Zeitraum, nach dem ein Tausch-Limit zurueckgesetzt wird (ab dem ersten Kauf). */
public enum LimitPeriod {
    MINUTE("Minute", 60_000L),
    HOUR("Stunde", 3_600_000L),
    DAY("Tag", 86_400_000L),
    WEEK("Woche", 604_800_000L),
    MONTH("Monat (30 Tage)", 2_592_000_000L),
    FOREVER("Dauerhaft (nie zurueck)", -1L);

    public final String label;
    public final long millis;

    LimitPeriod(String label, long millis) {
        this.label = label;
        this.millis = millis;
    }

    public LimitPeriod next() {
        LimitPeriod[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
