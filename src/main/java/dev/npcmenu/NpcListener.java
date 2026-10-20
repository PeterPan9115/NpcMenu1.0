package dev.npcmenu;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.npcmenu.gui.EditMenu;
import dev.npcmenu.gui.ShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public final class NpcListener implements Listener {

    private final NpcMenu plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Set<UUID> warmup = new HashSet<>();

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
            case SHOP -> new ShopMenu(plugin, d, p, 0).open(p);
            case TELEPORT -> teleport(p, d);
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

    // ------------------------------------------------------------ Teleporter

    /** Gibt eine Fehlermeldung zurueck, wenn der Spieler die Kosten nicht tragen kann, sonst null. */
    private String missing(Player p, NpcData d) {
        if (d.tpCostXp > 0 && p.getLevel() < d.tpCostXp) {
            return "<red>Dir fehlen XP-Level. Benoetigt: <white>" + d.tpCostXp;
        }
        ItemStack c = d.tpCostItem;
        if (c != null && !p.getInventory().containsAtLeast(c, c.getAmount())) {
            return "<red>Dir fehlt: <white>" + Util.describeItem(c);
        }
        return null;
    }

    private void teleport(Player p, NpcData d) {
        if (d.tpTarget == null || d.tpTarget.getWorld() == null) {
            Util.msg(p, "<red>Dieser Teleporter hat noch kein Ziel.");
            return;
        }
        if (warmup.contains(p.getUniqueId())) {
            Util.msg(p, "<red>Du wirst bereits teleportiert.");
            return;
        }
        String err = missing(p, d);
        if (err != null) {
            Util.msg(p, err);
            return;
        }
        if (d.tpWait <= 0) {
            finishTeleport(p, d);
            return;
        }

        warmup.add(p.getUniqueId());
        Location start = p.getLocation();
        int[] left = {d.tpWait};
        Util.msg(p, "<yellow>Teleport in <white>" + d.tpWait + " Sekunden<yellow> - bewege dich nicht!");
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!p.isOnline()) {
                warmup.remove(p.getUniqueId());
                task.cancel();
                return;
            }
            Location now = p.getLocation();
            if (now.getWorld() != start.getWorld() || now.distanceSquared(start) > 0.25) {
                warmup.remove(p.getUniqueId());
                task.cancel();
                Util.msg(p, "<red>Teleport abgebrochen, du hast dich bewegt.");
                return;
            }
            if (left[0] <= 0) {
                warmup.remove(p.getUniqueId());
                task.cancel();
                finishTeleport(p, d);
                return;
            }
            p.sendActionBar(Util.parse("<yellow>Teleport in <white>" + left[0] + "s"));
            left[0]--;
        }, 0L, 20L);
    }

    private void finishTeleport(Player p, NpcData d) {
        String err = missing(p, d);
        if (err != null) {
            Util.msg(p, err);
            return;
        }
        if (d.tpCostXp > 0) p.setLevel(p.getLevel() - d.tpCostXp);
        if (d.tpCostItem != null) p.getInventory().removeItem(d.tpCostItem.clone());
        p.teleportAsync(d.tpTarget);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldown.remove(e.getPlayer().getUniqueId());
        warmup.remove(e.getPlayer().getUniqueId());
    }
}
