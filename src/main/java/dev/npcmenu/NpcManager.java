package dev.npcmenu;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NpcManager {

    private final NpcMenu plugin;
    private final NamespacedKey key;
    private final Map<String, NpcData> npcs = new LinkedHashMap<>();
    private final File file;
    private final File usageFile;
    /** Schluessel "npcId;trade;uuid" -> {anzahl, fensterStart} */
    private final Map<String, long[]> usage = new HashMap<>();

    public NpcManager(NpcMenu plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "npc_id");
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
        this.usageFile = new File(plugin.getDataFolder(), "usage.yml");
    }

    public Collection<NpcData> all() {
        return npcs.values();
    }

    // ---------------------------------------------------------------- Laden/Speichern

    public void load() {
        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = cfg.getConfigurationSection("npcs");
            if (root != null) {
                for (String id : root.getKeys(false)) {
                    ConfigurationSection s = root.getConfigurationSection(id);
                    if (s != null) npcs.put(id, NpcData.load(id, s));
                }
            }
        }
        loadUsage();
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

    private void loadUsage() {
        if (!usageFile.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(usageFile);
        for (String line : c.getStringList("entries")) {
            String[] p = line.split(";");
            if (p.length != 5) continue;
            try {
                usage.put(p[0] + ";" + p[1] + ";" + p[2],
                        new long[]{Long.parseLong(p[3]), Long.parseLong(p[4])});
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void saveUsage() {
        YamlConfiguration c = new YamlConfiguration();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, long[]> en : usage.entrySet()) {
            out.add(en.getKey() + ";" + en.getValue()[0] + ";" + en.getValue()[1]);
        }
        c.set("entries", out);
        try {
            plugin.getDataFolder().mkdirs();
            c.save(usageFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Konnte usage.yml nicht speichern: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- Tausch-Limits

    private String ukey(String npc, int trade, UUID u) {
        return npc + ";" + trade + ";" + u;
    }

    /** Wie oft der Spieler diesen Tausch im aktuellen Zeitraum schon gemacht hat. */
    public int used(String npc, int trade, UUID u, LimitPeriod per) {
        String k = ukey(npc, trade, u);
        long[] a = usage.get(k);
        if (a == null) return 0;
        if (per != LimitPeriod.FOREVER && System.currentTimeMillis() - a[1] >= per.millis) {
            usage.remove(k);
            return 0;
        }
        return (int) a[0];
    }

    /** Millisekunden bis zum Zuruecksetzen (-1 = nie). */
    public long remainingMillis(String npc, int trade, UUID u, LimitPeriod per) {
        if (per == LimitPeriod.FOREVER) return -1;
        long[] a = usage.get(ukey(npc, trade, u));
        if (a == null) return 0;
        return Math.max(0, per.millis - (System.currentTimeMillis() - a[1]));
    }

    public void recordUse(String npc, int trade, UUID u, LimitPeriod per) {
        String k = ukey(npc, trade, u);
        long now = System.currentTimeMillis();
        long[] a = usage.get(k);
        if (a == null || (per != LimitPeriod.FOREVER && now - a[1] >= per.millis)) {
            a = new long[]{0, now};
            usage.put(k, a);
        }
        a[0]++;
    }

    // ---------------------------------------------------------------- Entity

    /** Spawnt den NPC an der Position und registriert ihn. */
    public void spawn(NpcData d, Location loc) {
        Mannequin m = loc.getWorld().spawn(loc, Mannequin.class, e -> apply(e, d));
        d.entityId = m.getUniqueId();
        d.location = loc.clone();
        npcs.put(d.id, d);
        save();
    }

    /** Wendet Name/Skin/Ausruestung auf die Entity an. */
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
        EntityEquipment eq = m.getEquipment();
        if (eq != null) {
            for (EquipmentSlot slot : NpcData.EQUIP_SLOTS) {
                ItemStack it = d.equipment.get(slot);
                eq.setItem(slot, it == null ? new ItemStack(Material.AIR) : it.clone());
            }
        }
    }

    public Mannequin find(NpcData d) {
        if (d.entityId == null || d.location == null || d.location.getWorld() == null) return null;
        d.location.getWorld().getChunkAt(d.location); // Chunk laden
        Entity e = Bukkit.getEntity(d.entityId);
        return e instanceof Mannequin m ? m : null;
    }

    /** Aktualisiert Name/Skin/Ausruestung eines bereits gespawnten NPCs und speichert. */
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

    /** Teleportiert einen Spieler zum NPC. */
    public void teleportTo(Player p, NpcData d) {
        Mannequin m = find(d);
        Location loc = m != null ? m.getLocation() : d.location;
        if (loc == null || loc.getWorld() == null) {
            Util.msg(p, "<red>Dieser NPC ist noch nicht gespawnt.");
            return;
        }
        p.teleportAsync(loc.clone());
        Util.msg(p, "<green>Zum NPC teleportiert.");
    }

    public void remove(NpcData d) {
        Mannequin m = find(d);
        if (m != null) m.remove();
        npcs.remove(d.id);
        usage.keySet().removeIf(k -> k.startsWith(d.id + ";"));
        save();
    }

    public NpcData fromEntity(Entity e) {
        String id = e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id == null ? null : npcs.get(id);
    }
}
