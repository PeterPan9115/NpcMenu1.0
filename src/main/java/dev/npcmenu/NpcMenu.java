package dev.npcmenu;

import dev.npcmenu.gui.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcMenu extends JavaPlugin {

    private NpcManager manager;
    private ChatInput chat;

    @Override
    public void onEnable() {
        manager = new NpcManager(this);
        manager.load();
        chat = new ChatInput(this);

        getServer().getPluginManager().registerEvents(chat, this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        var cmd = getCommand("npc");
        if (cmd != null) cmd.setExecutor(new NpcCommand(this));
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    public NpcManager manager() { return manager; }
    public ChatInput chat() { return chat; }
}
