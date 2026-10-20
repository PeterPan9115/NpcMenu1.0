package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class CommandMenu extends Gui {

    public CommandMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        init(27, "<dark_gray>Befehl-Einstellungen");
        fill(Util.filler());

        set(11, Util.item(Material.COMMAND_BLOCK, "<aqua>Befehl setzen",
                "<gray>Aktuell: <white>/" + d.command,
                "<gray>%player% = Name des Spielers", "", "<yellow>Klicken: Befehl eingeben"), e -> {
            Player p = (Player) e.getWhoClicked();
            plugin.chat().ask(p, "<yellow>Gib den <white>Befehl<yellow> ein (ohne /, z.B. <white>spawn<yellow> oder <white>give %player% apple<yellow>):",
                    s -> {
                        d.command = s.startsWith("/") ? s.substring(1) : s;
                        plugin.manager().refresh(d);
                    }, () -> new CommandMenu(plugin, d).open(p));
        });

        set(13, Util.item(d.asConsole ? Material.REDSTONE_BLOCK : Material.PLAYER_HEAD,
                "<aqua>Ausfuehren als: <white>" + (d.asConsole ? "Konsole" : "Spieler"),
                "<gray>Konsole = volle Rechte (Vorsicht!)", "", "<yellow>Klicken: umschalten"), e -> {
            d.asConsole = !d.asConsole;
            plugin.manager().refresh(d);
            new CommandMenu(plugin, d).open((Player) e.getWhoClicked());
        });

        set(22, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }
}
