package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TeleportMenu extends Gui {

    private final NpcData d;

    public TeleportMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(36, "<dark_gray>Teleporter-Einstellungen");
        render();
    }

    private void changed() {
        plugin.manager().refresh(d);
        render();
    }

    private void render() {
        clear();
        fill(Util.filler());

        // Ziel
        set(11, Util.item(Material.GRASS_BLOCK, "<green>Meine Position als Ziel",
                "<gray>Stell dich an das Ziel und klicke.", "<gray>(Welt, Koordinaten und Blickrichtung)"), e -> {
            Player p = (Player) e.getWhoClicked();
            d.tpTarget = p.getLocation().clone();
            Util.msg(p, "<green>Ziel gesetzt: <white>" + EditMenu.describe(d.tpTarget));
            changed();
        });
        set(13, Util.item(Material.FILLED_MAP, "<aqua>Weltspawn waehlen",
                        "<gray>Waehle eine geladene Welt,", "<gray>der Spawnpunkt wird das Ziel."),
                e -> new WorldPicker(plugin, d).open((Player) e.getWhoClicked()));
        set(15, Util.item(Material.BOOK, "<yellow>Aktuelles Ziel", "<white>" + EditMenu.describe(d.tpTarget)), null);

        // Kosten: Item
        ItemStack ci = d.tpCostItem;
        ItemStack costShown = ci == null
                ? Util.item(Material.CHEST, "<aqua>Kosten: Item",
                "<gray>Aktuell: <white>keine",
                "",
                "<yellow>Mit Item am Cursor klicken = setzen",
                "<yellow>(Anzahl des Stapels wird uebernommen)")
                : Util.withLore(ci, "<aqua>Kosten pro Teleport",
                "<yellow>Ohne Item klicken = entfernen");
        set(20, costShown, e -> {
            ItemStack cursor = e.getCursor();
            d.tpCostItem = cursor.isEmpty() ? null : cursor.clone();
            changed();
        });

        // Kosten: XP-Level
        set(22, Util.item(Material.EXPERIENCE_BOTTLE,
                "<aqua>Kosten: <white>" + (d.tpCostXp > 0 ? d.tpCostXp + " XP-Level" : "keine"),
                "<gray>0 = kostenlos", "",
                "<yellow>Linksklick: +1  |  Rechtsklick: -1",
                "<yellow>Shift: +5 / -5"), e -> {
            int step = e.isShiftClick() ? 5 : 1;
            int v = d.tpCostXp + (e.isLeftClick() ? step : -step);
            d.tpCostXp = Math.max(0, Math.min(1000, v));
            changed();
        });

        // Wartezeit
        set(24, Util.item(Material.CLOCK,
                "<aqua>Wartezeit: <white>" + (d.tpWait > 0 ? d.tpWait + " Sekunden" : "sofort"),
                "<gray>Spieler muessen so lange still", "<gray>stehen bleiben.", "",
                "<yellow>Linksklick: +1  |  Rechtsklick: -1",
                "<yellow>Shift: +5 / -5"), e -> {
            int step = e.isShiftClick() ? 5 : 1;
            int v = d.tpWait + (e.isLeftClick() ? step : -step);
            d.tpWait = Math.max(0, Math.min(120, v));
            changed();
        });

        set(31, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
