package dev.npcmenu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Berechnet anhand der Minecraft-Crafting-Rezepte, was beim Verschrotten zurueckkommt. */
public final class Salvage {

    private Salvage() {}

    public record Result(Map<Material, Integer> items, int consumed, String error) {
        public boolean ok() {
            return error == null;
        }
    }

    private static Result fail(String msg) {
        return new Result(Map.of(), 0, msg);
    }

    public static Result compute(ItemStack in, NpcData d, Set<String> blacklist) {
        if (in == null || in.getType().isAir()) return fail("Lege ein Item zum Verschrotten ab.");
        Material type = in.getType();
        if (blacklist.contains(type.name())) return fail("Dieses Item darf nicht verschrottet werden.");

        Recipe recipe = null;
        Map<Material, Integer> ingredients = null;
        for (Recipe r : Bukkit.getRecipesFor(new ItemStack(type))) {
            Map<Material, Integer> ing = ingredients(r);
            if (ing == null || ing.isEmpty() || ing.containsKey(type)) continue;
            recipe = r;
            ingredients = ing;
            break;
        }
        if (recipe == null) return fail("Fuer dieses Item gibt es kein Crafting-Rezept.");

        int resultAmount = Math.max(1, recipe.getResult().getAmount());
        int batches = in.getAmount() / resultAmount;
        if (batches <= 0) return fail("Du brauchst mindestens " + resultAmount + " Stueck.");

        double factor = d.salvagePercent / 100.0;
        if (d.salvageDurability) {
            int maxDur = type.getMaxDurability();
            ItemMeta meta = in.getItemMeta();
            if (maxDur > 0 && meta instanceof Damageable dm) {
                factor *= Math.max(0.0, 1.0 - (double) dm.getDamage() / maxDur);
            }
        }

        Map<Material, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Material, Integer> en : ingredients.entrySet()) {
            int n = (int) Math.floor(en.getValue() * batches * factor + 1e-9);
            if (n > 0) out.put(en.getKey(), n);
        }
        if (out.isEmpty()) return fail("Das ergibt nichts (Item zu beschaedigt oder Rueckgabe zu niedrig).");
        return new Result(out, batches * resultAmount, null);
    }

    /** Zutaten eines Crafting-Rezepts (null bei Ofen-, Schmiede- usw. Rezepten). */
    private static Map<Material, Integer> ingredients(Recipe r) {
        Map<Material, Integer> map = new LinkedHashMap<>();
        if (r instanceof ShapedRecipe sr) {
            Map<Character, RecipeChoice> choices = sr.getChoiceMap();
            for (String row : sr.getShape()) {
                for (char c : row.toCharArray()) {
                    RecipeChoice ch = choices.get(c);
                    if (ch == null) continue;
                    Material m = first(ch);
                    if (m == null) return null;
                    map.merge(m, 1, Integer::sum);
                }
            }
            return map;
        }
        if (r instanceof ShapelessRecipe sl) {
            for (RecipeChoice ch : sl.getChoiceList()) {
                Material m = first(ch);
                if (m == null) return null;
                map.merge(m, 1, Integer::sum);
            }
            return map;
        }
        return null;
    }

    private static Material first(RecipeChoice ch) {
        if (ch instanceof RecipeChoice.MaterialChoice mc) {
            List<Material> l = mc.getChoices();
            return l.isEmpty() ? null : l.get(0);
        }
        if (ch instanceof RecipeChoice.ExactChoice ec) {
            List<ItemStack> l = ec.getChoices();
            return l.isEmpty() ? null : l.get(0).getType();
        }
        return null;
    }
}
