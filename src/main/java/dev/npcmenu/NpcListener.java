package dev.npcmenu;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.npcmenu.gui.EditMenu;
import dev.npcmenu.gui.ShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcListener implements Listener {

    private final NpcMenu plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public NpcListener(NpcMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        NpcData d = plugin.manager().fromEntity(e.getRightClicked());
        if (d == null) return;
        e.setCancelled(true);

        Player p = e.getPlayer();

        // Shift + Rechtsklick (Admin) = bearbeiten
        if (p.isSneaking() && p.hasPermission("npcmenu.admin")) {
            new EditMenu(plugin, d).open(p);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - cooldown.getOrDefault(p.getUniqueId(), 0L) < 500) return;
        cooldown.put(p.getUniqueId(), now);

        switch (d.type) {
            case SHOP -> new ShopMenu(plugin, d).open(p);
            case TELEPORT -> {
                if (d.tpTarget == null || d.tpTarget.getWorld() == null) {
                    Util.msg(p, "<red>Dieser Teleporter hat noch kein Ziel.");
                } else {
                    p.teleportAsync(d.tpTarget);
                }
            }
            case COMMAND -> {
                if (d.command.isBlank()) return;
                String c = d.command.replace("%player%", p.getName());
                if (c.startsWith("/")) c = c.substring(1);
                Bukkit.dispatchCommand(d.asConsole ? Bukkit.getConsoleSender() : p, c);
            }
            case MESSAGE -> p.sendMessage(Util.parse(d.message.replace("%player%", p.getName())));
            case SERVER -> {
                if (d.server.isBlank()) return;
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(d.server);
                p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (plugin.manager().fromEntity(e.getEntity()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldown.remove(e.getPlayer().getUniqueId());
    }
}
