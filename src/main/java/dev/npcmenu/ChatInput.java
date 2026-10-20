package dev.npcmenu;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Laesst Spieler Text im Chat eingeben und oeffnet danach wieder das Menue. */
public final class ChatInput implements Listener {

    private record Pending(Consumer<String> setter, Runnable reopen) {}

    private final NpcMenu plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public ChatInput(NpcMenu plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String prompt, Consumer<String> setter, Runnable reopen) {
        pending.put(p.getUniqueId(), new Pending(setter, reopen));
        Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
        Util.msg(p, prompt);
        Util.msg(p, "<gray>Schreibe <white>abbrechen<gray>, um abzubrechen.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending pd = pending.remove(e.getPlayer().getUniqueId());
        if (pd == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!text.equalsIgnoreCase("abbrechen")) pd.setter().accept(text);
            pd.reopen().run();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
