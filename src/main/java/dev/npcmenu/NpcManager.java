package dev.npcmenu;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcManager {

    private final NpcMenu plugin;
    private final NamespacedKey key;
    private final Map<String, NpcData> npcs = new LinkedHashMap<>();
    private final File file;

    public NpcManager(NpcMenu plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "npc_id");
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public Collection<NpcData> all() {
        return npcs.values();
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("npcs");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s != null) npcs.put(id, NpcData.load(id, s));
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (NpcData d : npcs.values()) {
            d.save(cfg.createSection("npcs." + d.id));
        }
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Konnte npcs.yml nicht speichern: " + ex.getMessage());
        }
    }

    /** Spawnt den NPC an der Position und registriert ihn. */
    public void spawn(NpcData d, Location loc) {
        Mannequin m = loc.getWorld().spawn(loc, Mannequin.class, e -> apply(e, d));
        d.entityId = m.getUniqueId();
        d.location = loc.clone();
        npcs.put(d.id, d);
        save();
    }

    /** Wendet Name/Skin etc. auf die Entity an. */
    private void apply(Mannequin m, NpcData d) {
        m.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id);
        m.customName(Util.parse(d.name));
        m.setCustomNameVisible(true);
        m.setProfile(ResolvableProfile.resolvableProfile().name(d.skin).build());
        m.setImmovable(true);
        m.setInvulnerable(true);
        m.setSilent(true);
        m.setPersistent(true);
        m.setRemoveWhenFarAway(false);
        try {
            m.setDescription((net.kyori.adventure.text.Component) null); // versteckt den Standard-Text "NPC"
        } catch (Exception ignored) {
        }
    }

    public Mannequin find(NpcData d) {
        if (d.entityId == null || d.location == null || d.location.getWorld() == null) return null;
        d.location.getWorld().getChunkAt(d.location); // Chunk laden
        Entity e = Bukkit.getEntity(d.entityId);
        return e instanceof Mannequin m ? m : null;
    }

    /** Aktualisiert Name/Skin eines bereits gespawnten NPCs und speichert. */
    public void refresh(NpcData d) {
        if (!d.spawned()) return;
        Mannequin m = find(d);
        if (m != null) apply(m, d);
        save();
    }

    public void move(NpcData d, Location loc) {
        Mannequin m = find(d);
        if (m != null) m.teleportAsync(loc);
        d.location = loc.clone();
        save();
    }

    public void remove(NpcData d) {
        Mannequin m = find(d);
        if (m != null) m.remove();
        npcs.remove(d.id);
        save();
    }

    public NpcData fromEntity(Entity e) {
        String id = e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id == null ? null : npcs.get(id);
    }
}
