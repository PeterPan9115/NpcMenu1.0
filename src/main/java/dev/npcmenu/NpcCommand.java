package dev.npcmenu;

import dev.npcmenu.gui.MainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class NpcCommand implements CommandExecutor {

    private final NpcMenu plugin;

    public NpcCommand(NpcMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur fuer Spieler.");
            return true;
        }
        if (!p.hasPermission("npcmenu.admin")) {
            Util.msg(p, "<red>Keine Berechtigung.");
            return true;
        }
        new MainMenu(plugin, 0).open(p);
        return true;
    }
}
