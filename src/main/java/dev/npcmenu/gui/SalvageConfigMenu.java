package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class SalvageConfigMenu extends Gui {

    private final NpcData d;

    public SalvageConfigMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, "<dark_gray>Verschrotter-Einstellungen");
        render();
    }

    private void changed() {
        plugin.manager().refresh(d);
        render();
    }

    private void render() {
        clear();
        fill(Util.filler());

        set(11, Util.item(Material.GRINDSTONE, "<aqua>Rueckgabe: <white>" + d.salvagePercent + "%",
                "<gray>Wie viel der Zutaten zurueck-",
                "<gray>kommt (abgerundet).",
                "<gray>100% = alles", "",
                "<yellow>Linksklick: +5  |  Rechtsklick: -5",
                "<yellow>Shift: +25 / -25"), e -> {
            int step = e.isShiftClick() ? 25 : 5;
            int v = d.salvagePercent + (e.isLeftClick() ? step : -step);
            d.salvagePercent = Math.max(0, Math.min(100, v));
            changed();
        });

        set(13, Util.item(d.salvageDurability ? Material.LIME_DYE : Material.RED_DYE,
                "<aqua>Haltbarkeit zaehlt: " + (d.salvageDurability ? "<green>JA" : "<red>NEIN"),
                "<gray>JA: Beschaedigte Werkzeuge und",
                "<gray>Ruestungen geben entsprechend",
                "<gray>weniger zurueck.", "",
                "<yellow>Klicken: umschalten"), e -> {
            d.salvageDurability = !d.salvageDurability;
            changed();
        });

        set(15, Util.item(Material.BOOK, "<yellow>So funktioniert's",
                "<gray>Das Item wird anhand des normalen",
                "<gray>Crafting-Rezepts zerlegt.",
                "<gray>Gibt es mehrere Rezepte, wird das",
                "<gray>erste verwendet. Items ohne Rezept",
                "<gray>koennen nicht verschrottet werden.",
                "",
                "<gray>Sperrliste: <white>config.yml"), null);

        set(22, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
