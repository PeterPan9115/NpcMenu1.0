package dev.npcmenu;

import dev.npcmenu.gui.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcMenu extends JavaPlugin {

    private NpcManager manager;
    private ChatInput chat;
    private WorkerStore workers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new NpcManager(this);
        manager.load();
        workers = new WorkerStore(this);
        workers.load();
        chat = new ChatInput(this);

        getServer().getPluginManager().registerEvents(chat, this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getScheduler().runTaskTimer(this, () -> manager.saveUsage(), 6000L, 6000L);
        getServer().getScheduler().runTaskTimer(this, () -> workers.tick(), 100L, 100L);

        var cmd = getCommand("npc");
        if (cmd != null) cmd.setExecutor(new NpcCommand(this));
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
            manager.saveUsage();
        }
        if (workers != null) workers.save();
    }

    public NpcManager manager() { return manager; }
    public ChatInput chat() { return chat; }
    public WorkerStore workers() { return workers; }
}
