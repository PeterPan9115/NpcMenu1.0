package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.NpcType;
import dev.npcmenu.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class EditMenu extends Gui {

    private final NpcData d;

    public EditMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, "<dark_gray>NPC: " + (d.spawned() ? "bearbeiten" : "neu erstellen"));
        fill(Util.filler());

        // Skin
        set(10, Util.item(Material.PLAYER_HEAD, "<aqua>Skin",
                "<gray>Aktuell: <white>" + d.skin,
                "", "<yellow>Klicken: Minecraft-Name eingeben"), e -> {
            Player p = (Player) e.getWhoClicked();
            plugin.chat().ask(p, "<yellow>Gib den <white>Minecraft-Namen<yellow> fuer den Skin ein:", s -> {
                if (!s.matches("[A-Za-z0-9_]{1,16}")) {
                    Util.msg(p, "<red>Ungueltiger Spielername.");
                    return;
                }
                d.skin = s;
                plugin.manager().refresh(d);
            }, () -> reopen(p));
        });

        // Name
        set(11, Util.item(Material.NAME_TAG, "<aqua>Name",
                "<gray>Aktuell: <reset>" + d.name,
                "<gray>Farben: <white>&a</white> oder <white><green></white>",
                "", "<yellow>Klicken: Namen eingeben"), e -> {
            Player p = (Player) e.getWhoClicked();
            plugin.chat().ask(p, "<yellow>Gib den <white>Namen<yellow> des NPCs ein:", s -> {
                d.name = s;
                plugin.manager().refresh(d);
            }, () -> reopen(p));
        });

        // Typ
        set(12, Util.item(d.type.icon, "<aqua>Typ: <white>" + d.type.label,
                "<gray>Was soll der NPC machen?",
                "", "<yellow>Klicken: Typ wechseln"), e -> {
            d.type = d.type.next();
            plugin.manager().refresh(d);
            reopen((Player) e.getWhoClicked());
        });

        // Typ-Einstellungen
        set(13, Util.item(Material.COMPARATOR, "<aqua>Einstellungen",
                configLore()), e -> openConfig((Player) e.getWhoClicked()));

        if (!d.spawned()) {
            set(16, Util.item(Material.LIME_DYE, "<green><bold>NPC spawnen",
                    "<gray>Spawnt den NPC an deiner Position."), e -> {
                Player p = (Player) e.getWhoClicked();
                Location loc = p.getLocation();
                loc.setYaw(loc.getYaw() + 180f);
                loc.setPitch(0f);
                plugin.manager().spawn(d, loc);
                p.closeInventory();
                Util.msg(p, "<green>NPC erstellt! <gray>(Shift + Rechtsklick auf den NPC = bearbeiten)");
            });
        } else {
            set(16, Util.item(Material.ENDER_EYE, "<green>Hierher bewegen",
                    "<gray>Setzt den NPC an deine Position."), e -> {
                Player p = (Player) e.getWhoClicked();
                Location loc = p.getLocation();
                loc.setYaw(loc.getYaw() + 180f);
                loc.setPitch(0f);
                plugin.manager().move(d, loc);
                Util.msg(p, "<green>NPC verschoben.");
            });
            set(22, Util.item(Material.TNT, "<red><bold>NPC loeschen",
                    "<gray>Zum Bestaetigen: <white>Shift + Klick"), e -> {
                if (!e.isShiftClick()) {
                    Util.msg(e.getWhoClicked(), "<red>Zum Loeschen Shift + Klick benutzen.");
                    return;
                }
                plugin.manager().remove(d);
                e.getWhoClicked().closeInventory();
                Util.msg(e.getWhoClicked(), "<red>NPC geloescht.");
            });
        }

        set(18, Util.item(Material.ARROW, "<yellow>Zurueck zur Liste"),
                e -> new MainMenu(plugin, 0).open((Player) e.getWhoClicked()));
    }

    private void reopen(Player p) {
        new EditMenu(plugin, d).open(p);
    }

    private String[] configLore() {
        return switch (d.type) {
            case SHOP -> new String[]{"<gray>Preise und Waren festlegen", "", "<yellow>Klicken zum Oeffnen"};
            case TELEPORT -> new String[]{"<gray>Ziel: <white>" + describe(d.tpTarget), "", "<yellow>Klicken zum Oeffnen"};
            case COMMAND -> new String[]{"<gray>Befehl: <white>/" + d.command,
                    "<gray>Als: <white>" + (d.asConsole ? "Konsole" : "Spieler"), "", "<yellow>Klicken zum Oeffnen"};
            case MESSAGE -> new String[]{"<gray>Text: <reset>" + d.message, "", "<yellow>Klicken: Text eingeben"};
            case SERVER -> new String[]{"<gray>Server: <white>" + (d.server.isBlank() ? "-" : d.server),
                    "", "<yellow>Klicken: Servernamen eingeben"};
        };
    }

    public static String describe(Location l) {
        if (l == null || l.getWorld() == null) return "nicht gesetzt";
        return l.getWorld().getName() + " (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")";
    }

    private void openConfig(Player p) {
        switch (d.type) {
            case SHOP -> new TraderEditor(plugin, d).open(p);
            case TELEPORT -> new TeleportMenu(plugin, d).open(p);
            case COMMAND -> new CommandMenu(plugin, d).open(p);
            case MESSAGE -> plugin.chat().ask(p,
                    "<yellow>Gib die <white>Nachricht<yellow> ein (%player% = Spielername):",
                    s -> {
                        d.message = s;
                        plugin.manager().refresh(d);
                    }, () -> reopen(p));
            case SERVER -> plugin.chat().ask(p,
                    "<yellow>Gib den <white>Servernamen<yellow> aus deiner Proxy-Config ein:",
                    s -> {
                        d.server = s;
                        plugin.manager().refresh(d);
                    }, () -> reopen(p));
        }
    }
}
