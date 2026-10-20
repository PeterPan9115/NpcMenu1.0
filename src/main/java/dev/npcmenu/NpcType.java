package dev.npcmenu;

import org.bukkit.Material;

public enum NpcType {
    SHOP(Material.EMERALD, "Shop / Trader"),
    TELEPORT(Material.ENDER_PEARL, "Teleporter"),
    COMMAND(Material.COMMAND_BLOCK, "Befehl ausfuehren"),
    MESSAGE(Material.WRITABLE_BOOK, "Nachricht"),
    SERVER(Material.COMPASS, "Server wechseln (Proxy)");

    public final Material icon;
    public final String label;

    NpcType(Material icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public NpcType next() {
        NpcType[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
