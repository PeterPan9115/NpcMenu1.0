package dev.npcmenu;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class NpcData {

    public static final int TRADES = 9;

    public final String id;
    public String skin = "Steve";
    public String name = "<yellow>NPC";
    public NpcType type = NpcType.MESSAGE;

    // Entity / Position (null = noch nicht gespawnt)
    public UUID entityId;
    public Location location;

    // Shop: inputs[i] = Preis, outputs[i] = Ware
    public final ItemStack[] inputs = new ItemStack[TRADES];
    public final ItemStack[] outputs = new ItemStack[TRADES];

    // Teleporter
    public Location tpTarget;

    // Befehl
    public String command = "";
    public boolean asConsole = false;

    // Nachricht
    public String message = "<gray>Hallo!";

    // Server (Proxy)
    public String server = "";

    public NpcData(String id) {
        this.id = id;
    }

    public boolean spawned() {
        return entityId != null;
    }

    public void save(ConfigurationSection s) {
        s.set("skin", skin);
        s.set("name", name);
        s.set("type", type.name());
        s.set("entity", entityId == null ? null : entityId.toString());
        s.set("location", location);
        for (int i = 0; i < TRADES; i++) {
            s.set("trades." + i + ".in", inputs[i]);
            s.set("trades." + i + ".out", outputs[i]);
        }
        s.set("teleport", tpTarget);
        s.set("command", command);
        s.set("console", asConsole);
        s.set("message", message);
        s.set("server", server);
    }

    public static NpcData load(String id, ConfigurationSection s) {
        NpcData d = new NpcData(id);
        d.skin = s.getString("skin", "Steve");
        d.name = s.getString("name", "<yellow>NPC");
        try {
            d.type = NpcType.valueOf(s.getString("type", "MESSAGE"));
        } catch (IllegalArgumentException ignored) {
        }
        String ent = s.getString("entity");
        if (ent != null) d.entityId = UUID.fromString(ent);
        d.location = s.getLocation("location");
        for (int i = 0; i < TRADES; i++) {
            d.inputs[i] = s.getItemStack("trades." + i + ".in");
            d.outputs[i] = s.getItemStack("trades." + i + ".out");
        }
        d.tpTarget = s.getLocation("teleport");
        d.command = s.getString("command", "");
        d.asConsole = s.getBoolean("console", false);
        d.message = s.getString("message", "<gray>Hallo!");
        d.server = s.getString("server", "");
        return d;
    }
}
