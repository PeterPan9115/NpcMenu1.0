package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class WorldPicker extends Gui {

    public WorldPicker(NpcMenu plugin, NpcData d) {
        super(plugin);
        init(54, "<dark_gray>Welt waehlen");
        for (int i = 45; i < 54; i++) inv.setItem(i, Util.filler());

        int slot = 0;
        for (World w : Bukkit.getWorlds()) {
            if (slot >= 45) break;
            Material icon = switch (w.getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };
            set(slot++, Util.item(icon, "<green>" + w.getName(), "<gray>Klicken: Spawn dieser Welt als Ziel"), e -> {
                Player p = (Player) e.getWhoClicked();
                Location spawn = w.getSpawnLocation().add(0.5, 0, 0.5);
                d.tpTarget = spawn;
                plugin.manager().refresh(d);
                Util.msg(p, "<green>Ziel gesetzt: <white>" + EditMenu.describe(spawn));
                new TeleportMenu(plugin, d).open(p);
            });
        }
        set(49, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new TeleportMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
