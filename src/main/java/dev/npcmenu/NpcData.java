package dev.npcmenu;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class NpcData {

    public static final int PER_PAGE = 9;
    public static final int MAX_PAGES = 10;
    public static final int TRADES = PER_PAGE * MAX_PAGES;

    public static final EquipmentSlot[] EQUIP_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
    };

    public final String id;
    public String skin = "Steve";
    public String name = "<yellow>NPC";
    public NpcType type = NpcType.MESSAGE;

    // Entity / Position (null = noch nicht gespawnt)
    public UUID entityId;
    public Location location;

    // Ausruestung
    public final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    // Shop: inputs[i] = Preis, outputs[i] = Ware
    public final ItemStack[] inputs = new ItemStack[TRADES];
    public final ItemStack[] outputs = new ItemStack[TRADES];
    public ShopLayout layout = ShopLayout.ROWS_PRICE_TOP;

    // Shop-Limits (limitMax 0 = unbegrenzt)
    public boolean limitsEnabled = true;
    public final int[] limitMax = new int[TRADES];
    public final LimitPeriod[] limitPeriod = new LimitPeriod[TRADES];

    // Teleporter
    public Location tpTarget;
    public ItemStack tpCostItem;
    public int tpCostXp = 0;
    public int tpWait = 0;

    // Befehl
    public String command = "";
    public boolean asConsole = false;

    // Nachricht
    public String message = "<gray>Hallo!";

    // Server (Proxy)
    public String server = "";

    public NpcData(String id) {
        this.id = id;
        Arrays.fill(limitPeriod, LimitPeriod.DAY);
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
        for (Map.Entry<EquipmentSlot, ItemStack> en : equipment.entrySet()) {
            s.set("equip." + en.getKey().name(), en.getValue());
        }
        for (int i = 0; i < TRADES; i++) {
            if (inputs[i] != null) s.set("trades." + i + ".in", inputs[i]);
            if (outputs[i] != null) s.set("trades." + i + ".out", outputs[i]);
            if (limitMax[i] > 0) {
                s.set("trades." + i + ".limit", limitMax[i]);
                s.set("trades." + i + ".period", limitPeriod[i].name());
            }
        }
        s.set("layout", layout.name());
        s.set("limitsEnabled", limitsEnabled);
        s.set("teleport", tpTarget);
        s.set("tpCostItem", tpCostItem);
        s.set("tpCostXp", tpCostXp);
        s.set("tpWait", tpWait);
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
        for (EquipmentSlot slot : EQUIP_SLOTS) {
            ItemStack it = s.getItemStack("equip." + slot.name());
            if (it != null) d.equipment.put(slot, it);
        }
        for (int i = 0; i < TRADES; i++) {
            d.inputs[i] = s.getItemStack("trades." + i + ".in");
            d.outputs[i] = s.getItemStack("trades." + i + ".out");
            d.limitMax[i] = s.getInt("trades." + i + ".limit", 0);
            try {
                d.limitPeriod[i] = LimitPeriod.valueOf(s.getString("trades." + i + ".period", "DAY"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        try {
            d.layout = ShopLayout.valueOf(s.getString("layout", "ROWS_PRICE_TOP"));
        } catch (IllegalArgumentException ignored) {
        }
        d.limitsEnabled = s.getBoolean("limitsEnabled", true);
        d.tpTarget = s.getLocation("teleport");
        d.tpCostItem = s.getItemStack("tpCostItem");
        d.tpCostXp = s.getInt("tpCostXp", 0);
        d.tpWait = s.getInt("tpWait", 0);
        d.command = s.getString("command", "");
        d.asConsole = s.getBoolean("console", false);
        d.message = s.getString("message", "<gray>Hallo!");
        d.server = s.getString("server", "");
        return d;
    }
}
