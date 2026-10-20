package dev.npcmenu.gui;

import dev.npcmenu.LimitPeriod;
import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.ShopLayout;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ShopMenu extends Gui {

    private final NpcData d;
    private final UUID viewer;
    private int page;

    public ShopMenu(NpcMenu plugin, NpcData d, Player viewer, int page) {
        super(plugin);
        this.d = d;
        this.viewer = viewer.getUniqueId();
        this.page = page;
        init(d.layout.size(), d.name);
        render();
    }

    private boolean valid(int i) {
        return d.inputs[i] != null && d.outputs[i] != null;
    }

    private int pageCount() {
        int max = 0;
        for (int i = 0; i < NpcData.TRADES; i++) {
            if (valid(i)) max = Math.max(max, i / NpcData.PER_PAGE);
        }
        return max + 1;
    }

    private boolean hasAny() {
        for (int i = 0; i < NpcData.TRADES; i++) if (valid(i)) return true;
        return false;
    }

    private int limitOf(int i) {
        return d.limitsEnabled ? d.limitMax[i] : 0;
    }

    private void render() {
        clear();
        fill(Util.filler());
        ShopLayout layout = d.layout;
        int pages = pageCount();
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;

        for (int j = 0; j < NpcData.PER_PAGE; j++) {
            final int i = page * NpcData.PER_PAGE + j;
            if (!valid(i)) continue;
            ItemStack in = d.inputs[i];
            ItemStack out = d.outputs[i];

            List<String> outLore = new ArrayList<>();
            outLore.add("<green>Du bekommst:");
            int max = limitOf(i);
            LimitPeriod per = d.limitPeriod[i];
            boolean soldOut = false;
            if (max > 0) {
                int used = plugin.manager().used(d.id, i, viewer, per);
                soldOut = used >= max;
                outLore.add("<gray>Limit: <white>" + used + "/" + max + " <gray>(" + per.label + ")");
                if (soldOut) {
                    long rem = plugin.manager().remainingMillis(d.id, i, viewer, per);
                    outLore.add(rem < 0 ? "<red>Nicht mehr verfuegbar" : "<red>Wieder verfuegbar in " + Util.duration(rem));
                }
            }
            outLore.add(soldOut ? "<red>Limit erreicht" : "<yellow>Klicken zum Tauschen");

            set(layout.in(j), Util.withLore(in, "<red>Du gibst:", "<gray>" + in.getAmount() + "x"),
                    e -> trade((Player) e.getWhoClicked(), i));
            set(layout.out(j), Util.withLore(out, outLore.toArray(new String[0])),
                    e -> trade((Player) e.getWhoClicked(), i));
            if (layout.arrow(j) >= 0) {
                set(layout.arrow(j), Util.item(Material.ARROW, "<yellow>Tauschen"),
                        e -> trade((Player) e.getWhoClicked(), i));
            }
        }

        if (!hasAny()) {
            set(13, Util.item(Material.BARRIER, "<red>Dieser Shop ist leer."), null);
        }
        if (pages > 1) {
            if (page > 0) {
                set(layout.prevSlot(), Util.item(Material.ARROW, "<yellow>Vorherige Seite",
                        "<gray>Seite " + page + "/" + pages), e -> {
                    page--;
                    render();
                });
            }
            if (page < pages - 1) {
                set(layout.nextSlot(), Util.item(Material.ARROW, "<yellow>Naechste Seite",
                        "<gray>Seite " + (page + 2) + "/" + pages), e -> {
                    page++;
                    render();
                });
            }
        }
    }

    private void trade(Player p, int i) {
        if (!valid(i)) return;
        ItemStack in = d.inputs[i];
        ItemStack out = d.outputs[i];

        int max = limitOf(i);
        LimitPeriod per = d.limitPeriod[i];
        if (max > 0) {
            int used = plugin.manager().used(d.id, i, viewer, per);
            if (used >= max) {
                long rem = plugin.manager().remainingMillis(d.id, i, viewer, per);
                Util.msg(p, rem < 0
                        ? "<red>Du hast das Limit fuer diesen Tausch erreicht."
                        : "<red>Limit erreicht. Wieder moeglich in <white>" + Util.duration(rem));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                render();
                return;
            }
        }

        PlayerInventory pi = p.getInventory();
        if (!pi.containsAtLeast(in, in.getAmount())) {
            Util.msg(p, "<red>Dir fehlen: <white>" + Util.describeItem(in));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        pi.removeItem(in.clone());
        pi.addItem(out.clone()).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        if (max > 0) plugin.manager().recordUse(d.id, i, viewer, per);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        render();
    }
}
