package dev.npcmenu.gui;

import dev.npcmenu.LimitPeriod;
import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Limits pro Tausch (Seite des Shop-Editors). Oben die Ware, Mitte das Maximum, unten der Zeitraum. */
public final class LimitMenu extends Gui {

    private final NpcData d;
    private final int page;

    public LimitMenu(NpcMenu plugin, NpcData d, int page) {
        super(plugin);
        this.d = d;
        this.page = page;
        init(36, "<dark_gray>Limits - Seite " + (page + 1));
        render();
    }

    private void changed() {
        plugin.manager().refresh(d);
        render();
    }

    private void render() {
        clear();
        fill(Util.filler());

        for (int j = 0; j < NpcData.PER_PAGE; j++) {
            final int i = page * NpcData.PER_PAGE + j;
            ItemStack out = d.outputs[i];

            ItemStack shown = out == null
                    ? Util.item(Material.BARRIER, "<gray>Tausch " + (i + 1) + " (leer)")
                    : Util.withLore(out, "<gray>Tausch " + (i + 1));
            set(j, shown, null);

            int max = d.limitMax[i];
            set(9 + j, Util.item(Material.PAPER,
                    max > 0 ? "<yellow>Max: <white>" + max : "<yellow>Max: <white>unbegrenzt",
                    "<gray>Wie oft ein Spieler diesen",
                    "<gray>Tausch machen darf.",
                    "<gray>0 = kein Limit",
                    "",
                    "<yellow>Linksklick: +1  |  Rechtsklick: -1",
                    "<yellow>Shift: +10 / -10"), e -> {
                int step = e.isShiftClick() ? 10 : 1;
                int v = d.limitMax[i] + (e.isLeftClick() ? step : -step);
                d.limitMax[i] = Math.max(0, Math.min(9999, v));
                changed();
            });

            set(18 + j, Util.item(Material.CLOCK,
                    "<aqua>Zeitraum: <white>" + d.limitPeriod[i].label,
                    "<gray>Nach diesem Zeitraum (ab dem",
                    "<gray>ersten Kauf) wird das Limit",
                    "<gray>zurueckgesetzt.",
                    "",
                    "<yellow>Klicken: wechseln"), e -> {
                d.limitPeriod[i] = d.limitPeriod[i].next();
                changed();
            });
        }

        set(28, Util.item(d.limitsEnabled ? Material.LIME_DYE : Material.RED_DYE,
                "<aqua>Limits: " + (d.limitsEnabled ? "<green>AN" : "<red>AUS"),
                "<gray>AUS = alle Limits dieses NPCs",
                "<gray>sind ausgeschaltet (Einstellungen",
                "<gray>bleiben gespeichert).",
                "",
                "<yellow>Klicken: umschalten"), e -> {
            d.limitsEnabled = !d.limitsEnabled;
            changed();
        });

        set(31, Util.item(Material.BOOK, "<yellow>Hinweis",
                "<gray>Obere Reihe: Ware des Tauschs",
                "<gray>Mittlere Reihe: Maximum",
                "<gray>Untere Reihe: Zeitraum"), null);

        set(35, Util.item(Material.ARROW, "<yellow>Zurueck zum Shop-Editor"),
                e -> new TraderEditor(plugin, d, page).open((Player) e.getWhoClicked()));
    }
}
