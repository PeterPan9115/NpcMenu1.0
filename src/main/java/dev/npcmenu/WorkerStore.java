package dev.npcmenu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Speichert die laufenden Arbeiter-Auftraege pro Spieler und NPC. */
public final class WorkerStore {

    public static final class Job {
        public String material;
        public int amount;
        public long start;
        public long end;
        public boolean notified;
    }

    private final NpcMenu plugin;
    private final File file;
    private final Map<String, Job> jobs = new HashMap<>();

    public WorkerStore(NpcMenu plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "workers.yml");
    }

    private String key(String npc, UUID u) {
        return npc + ";" + u;
    }

    public Job get(String npc, UUID u) {
        return jobs.get(key(npc, u));
    }

    public void put(String npc, UUID u, Job j) {
        jobs.put(key(npc, u), j);
        save();
    }

    public void remove(String npc, UUID u) {
        jobs.remove(key(npc, u));
        save();
    }

    public void removeNpc(String npc) {
        jobs.keySet().removeIf(k -> k.startsWith(npc + ";"));
        save();
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        for (String line : c.getStringList("jobs")) {
            String[] p = line.split(";");
            if (p.length != 7) continue;
            try {
                Job j = new Job();
                j.material = p[2];
                j.amount = Integer.parseInt(p[3]);
                j.start = Long.parseLong(p[4]);
                j.end = Long.parseLong(p[5]);
                j.notified = Boolean.parseBoolean(p[6]);
                jobs.put(p[0] + ";" + p[1], j);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration c = new YamlConfiguration();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Job> en : jobs.entrySet()) {
            Job j = en.getValue();
            out.add(en.getKey() + ";" + j.material + ";" + j.amount + ";" + j.start + ";" + j.end + ";" + j.notified);
        }
        c.set("jobs", out);
        try {
            plugin.getDataFolder().mkdirs();
            c.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Konnte workers.yml nicht speichern: " + ex.getMessage());
        }
    }

    /** Wird regelmaessig aufgerufen: meldet fertige Auftraege ueber der XP-Leiste. */
    public void tick() {
        long now = System.currentTimeMillis();
        boolean dirty = false;
        for (Map.Entry<String, Job> en : jobs.entrySet()) {
            Job j = en.getValue();
            if (j.notified || j.end > now) continue;
            String[] k = en.getKey().split(";");
            if (k.length != 2) continue;
            Player p;
            try {
                p = Bukkit.getPlayer(UUID.fromString(k[1]));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (p == null) continue;
            j.notified = true;
            dirty = true;
            notifyPlayer(p, k[0], j);
        }
        if (dirty) save();
    }

    private void notifyPlayer(Player p, String npcId, Job j) {
        NpcData d = plugin.manager().get(npcId);
        Component name = d == null ? Component.text("Arbeiter") : Util.parse(d.name);
        Material m = Material.matchMaterial(j.material);
        String item = m == null ? j.material : WorkerRates.nice(m);
        Component msg = Util.parse("<green>Dein Arbeiter ")
                .append(name)
                .append(Util.parse("<green> ist fertig! <gray>(" + j.amount + "x " + item + ") <yellow>Jetzt abholen!"));
        p.sendActionBar(msg);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.sendActionBar(msg);
        }, 60L);
    }
}
