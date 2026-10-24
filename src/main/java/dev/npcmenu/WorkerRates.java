package dev.npcmenu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Wie schnell ein Arbeiter ein Item farmen kann (Items pro Stunde bei 100% Geschwindigkeit). */
public final class WorkerRates {

    private static final Map<Material, Double> BASE = new HashMap<>();

    private WorkerRates() {}

    private static void put(double rate, String... names) {
        for (String n : names) {
            Material m = Material.matchMaterial(n);
            if (m != null) BASE.put(m, rate);
        }
    }

    static {
        // sehr haeufig
        put(1000, "COBBLESTONE", "DIRT", "SAND", "GRAVEL", "NETHERRACK", "COBBLED_DEEPSLATE", "STONE",
                "ANDESITE", "DIORITE", "GRANITE", "DEEPSLATE", "RED_SAND");
        // haeufig (Pflanzen, einfache Rohstoffe)
        put(600, "WHEAT", "CARROT", "POTATO", "BEETROOT", "SUGAR_CANE", "PUMPKIN", "MELON_SLICE",
                "BAMBOO", "CACTUS", "KELP", "SWEET_BERRIES", "SEAGRASS");
        put(400, "ROTTEN_FLESH", "BONE", "STRING", "FEATHER", "CLAY_BALL", "SNOWBALL", "FLINT", "COAL",
                "EGG", "INK_SAC", "COCOA_BEANS", "NETHER_WART", "RED_MUSHROOM", "BROWN_MUSHROOM", "APPLE");
        // ungewoehnlich
        put(250, "LEATHER", "GUNPOWDER", "SPIDER_EYE", "RAW_COPPER", "REDSTONE", "LAPIS_LAZULI",
                "PRISMARINE_SHARD", "BEEF", "PORKCHOP", "CHICKEN", "MUTTON", "RABBIT", "COD", "SALMON");
        put(120, "RAW_IRON", "QUARTZ", "GLOWSTONE_DUST");
        // selten
        put(60, "RAW_GOLD", "OBSIDIAN", "SLIME_BALL", "BLAZE_ROD");
        put(40, "MAGMA_CREAM");
        put(25, "ENDER_PEARL", "PRISMARINE_CRYSTALS");
        put(8, "GHAST_TEAR");
        // episch
        put(12, "DIAMOND", "EMERALD");
        put(3, "ANCIENT_DEBRIS", "NETHERITE_SCRAP");
        put(2, "NAUTILUS_SHELL");
        // nicht farmbar
        put(0, "NETHER_STAR", "DRAGON_EGG", "ELYTRA", "TOTEM_OF_UNDYING", "ENCHANTED_GOLDEN_APPLE",
                "BEDROCK", "BARRIER", "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
                "SPAWNER", "TRIAL_SPAWNER", "VAULT", "DRAGON_HEAD", "WITHER_SKELETON_SKULL", "TRIDENT",
                "SHULKER_SHELL", "HEART_OF_THE_SEA", "DEBUG_STICK", "STRUCTURE_BLOCK", "JIGSAW",
                "END_PORTAL_FRAME", "REINFORCED_DEEPSLATE", "NETHERITE_INGOT", "BEACON", "LIGHT");
    }

    private static boolean hasCraftingRecipe(Material m) {
        for (Recipe r : Bukkit.getRecipesFor(new ItemStack(m))) {
            if (r instanceof ShapedRecipe || r instanceof ShapelessRecipe) return true;
        }
        return false;
    }

    /** Items pro Stunde bei 100%. 0 = kann nicht gefarmt werden. */
    public static double rate(Material m, FileConfiguration cfg) {
        if (m == null || m.isAir() || !m.isItem()) return 0;
        String name = m.name();

        if (cfg.contains("worker-rates." + name)) {
            return Math.max(0, cfg.getDouble("worker-rates." + name));
        }
        Double base = BASE.get(m);
        if (base != null) return base;

        if (name.startsWith("MUSIC_DISC_") || name.endsWith("_SPAWN_EGG")) return 0;
        if (name.endsWith("_LOG")) return 600;
        if (name.endsWith("_LEAVES")) return 400;
        if (name.endsWith("_SAPLING")) return 150;

        // Gecraftete Items kann man nicht "farmen"
        if (hasCraftingRecipe(m)) return 0;

        return Math.max(0, cfg.getDouble("worker-default-rate", 100));
    }

    public static String tier(double rate) {
        if (rate >= 400) return "<gray>Haeufig";
        if (rate >= 100) return "<green>Ungewoehnlich";
        if (rate >= 20) return "<aqua>Selten";
        return "<light_purple>Episch";
    }

    public static double expected(double rate, int speedPercent, int minutes) {
        return rate * (speedPercent / 100.0) * (minutes / 60.0);
    }

    /** Gewuerfelte Menge fuer den Auftrag (schwankt leicht, 0 = Zeit zu kurz). */
    public static int roll(double rate, int speedPercent, int minutes, Random rnd) {
        double exp = expected(rate, speedPercent, minutes);
        if (exp < 0.5) return 0;
        exp *= 0.85 + 0.30 * rnd.nextDouble();
        int base = (int) Math.floor(exp);
        double frac = exp - base;
        int amount = base + (rnd.nextDouble() < frac ? 1 : 0);
        return Math.max(1, amount);
    }

    public static String fmt(double v) {
        return v >= 10 ? String.valueOf(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    public static String nice(Material m) {
        return m.name().toLowerCase().replace('_', ' ');
    }
}
