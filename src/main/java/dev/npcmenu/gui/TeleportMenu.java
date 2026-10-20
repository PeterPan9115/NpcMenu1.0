package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class TeleportMenu extends Gui {

    public TeleportMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        init(27, "<dark_gray>Teleporter-Ziel");
        fill(Util.filler());

        set(11, Util.item(Material.GRASS_BLOCK, "<green>Meine Position als Ziel",
                "<gray>Stell dich an das Ziel und klicke.", "<gray>(Welt, Koordinaten und Blickrichtung)"), e -> {
            Player p = (Player) e.getWhoClicked();
            d.tpTarget = p.getLocation().clone();
            plugin.manager().refresh(d);
            Util.msg(p, "<green>Ziel gesetzt: <white>" + EditMenu.describe(d.tpTarget));
            new TeleportMenu(plugin, d).open(p);
        });

        set(13, Util.item(Material.FILLED_MAP, "<aqua>Weltspawn waehlen",
                "<gray>Waehle eine geladene Welt,", "<gray>der Spawnpunkt wird das Ziel."),
                e -> new WorldPicker(plugin, d).open((Player) e.getWhoClicked()));

        set(15, Util.item(Material.BOOK, "<yellow>Aktuelles Ziel", "<white>" + EditMenu.describe(d.tpTarget)), null);

        set(22, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
