package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class WorkerConfigMenu extends Gui {

    private final NpcData d;

    public WorkerConfigMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, "<dark_gray>Arbeiter-Einstellungen");
        render();
    }

    private void changed() {
        plugin.manager().refresh(d);
        render();
    }

    private void render() {
        clear();
        fill(Util.filler());

        set(11, Util.item(Material.CLOCK, "<aqua>Maximale Dauer: <white>" + Util.duration(d.workerMaxMinutes * 60000L),
                "<gray>Laengste Zeit, fuer die ein Spieler", "<gray>den Arbeiter losschicken darf.", "",
                "<yellow>Linksklick: +1 Std  |  Rechtsklick: -1 Std",
                "<yellow>Shift: +10 Min / -10 Min"), e -> {
            int step = e.isShiftClick() ? 10 : 60;
            int v = d.workerMaxMinutes + (e.isLeftClick() ? step : -step);
            d.workerMaxMinutes = Math.max(1, Math.min(10080, v));
            changed();
        });

        set(13, Util.item(Material.SUGAR, "<aqua>Geschwindigkeit: <white>" + d.workerSpeed + "%",
                "<gray>100% = normale Farm-Rate.",
                "<gray>Hoeher = mehr Items in der gleichen Zeit.", "",
                "<yellow>Linksklick: +10  |  Rechtsklick: -10",
                "<yellow>Shift: +50 / -50"), e -> {
            int step = e.isShiftClick() ? 50 : 10;
            int v = d.workerSpeed + (e.isLeftClick() ? step : -step);
            d.workerSpeed = Math.max(10, Math.min(1000, v));
            changed();
        });

        set(15, Util.item(Material.BOOK, "<yellow>So funktioniert's",
                "<gray>Spieler legen ein Wunsch-Item fest",
                "<gray>und stellen die Dauer ein. Je",
                "<gray>seltener das Item, desto weniger",
                "<gray>kommt dabei heraus.",
                "<gray>Gecraftete Items sind nicht farmbar.",
                "",
                "<gray>Eigene Raten: <white>config.yml",
                "<gray>(worker-rates / worker-default-rate)"), null);

        set(22, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
