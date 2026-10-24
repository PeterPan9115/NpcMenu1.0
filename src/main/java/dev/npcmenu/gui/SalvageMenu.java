package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Salvage;
import dev.npcmenu.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Fenster des Verschrotters: Item in den Slot legen, auf den Amboss klicken. */
public final class SalvageMenu extends Gui {

    private static final int INPUT = 10;
    private static final int BUTTON = 12;

    private final NpcData d;

    public SalvageMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(36, "<dark_gray>Verschrotter");
        render();
    }

    private Set<String> blacklist() {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("salvage-blacklist")) set.add(s.toUpperCase());
        return set;
    }

    private void scheduleRender() {
        Bukkit.getScheduler().runTask(plugin, () -> render());
    }

    private void render() {
        ItemStack keep = inv.getItem(INPUT);
        clear();
        fill(Util.filler());
        inv.setItem(INPUT, keep);

        set(1, Util.item(Material.PAPER, "<yellow>Item hier ablegen",
                "<gray>Lege unten das Item in den", "<gray>freien Slot (oder Shift-Klick",
                "<gray>aus deinem Inventar)."), null);

        Salvage.Result r = Salvage.compute(keep, d, blacklist());
        if (keep == null || keep.getType().isAir()) {
            set(BUTTON, Util.item(Material.GRINDSTONE, "<gray>Verschrotten", "<gray>Lege zuerst ein Item ab."), null);
        } else if (!r.ok()) {
            set(BUTTON, Util.item(Material.BARRIER, "<red>Nicht moeglich", "<gray>" + r.error()), null);
        } else {
            set(BUTTON, Util.item(Material.ANVIL, "<green><bold>Verschrotten",
                    "<gray>Verbraucht: <white>" + r.consumed() + "x",
                    "<gray>Rueckgabe: <white>" + d.salvagePercent + "%", "",
                    "<yellow>Klicken zum Verschrotten"), e -> salvage((Player) e.getWhoClicked()));

            int slot = 18;
            for (Map.Entry<Material, Integer> en : r.items().entrySet()) {
                if (slot > 26) break;
                ItemStack show = new ItemStack(en.getKey(), Math.min(64, en.getValue()));
                set(slot++, Util.withLore(show, "<green>Du bekommst: <white>" + en.getValue() + "x"), null);
            }
        }

        set(14, Util.item(Material.BOOK, "<yellow>Info",
                "<gray>Rueckgabe: <white>" + d.salvagePercent + "%",
                "<gray>Haltbarkeit zaehlt: <white>" + (d.salvageDurability ? "ja" : "nein"),
                "<gray>Rest-Items (z.B. zu wenige",
                "<gray>fuer ein volles Rezept) bleiben",
                "<gray>im Slot und kommen beim",
                "<gray>Schliessen zurueck."), null);
    }

    private void salvage(Player p) {
        ItemStack in = inv.getItem(INPUT);
        Salvage.Result r = Salvage.compute(in, d, blacklist());
        if (!r.ok()) {
            Util.msg(p, "<red>" + r.error());
            return;
        }
        ItemStack left = in.clone();
        left.setAmount(in.getAmount() - r.consumed());
        inv.setItem(INPUT, left.getAmount() <= 0 ? null : left);

        for (Map.Entry<Material, Integer> en : r.items().entrySet()) {
            int n = en.getValue();
            while (n > 0) {
                int stack = Math.min(n, en.getKey().getMaxStackSize());
                p.getInventory().addItem(new ItemStack(en.getKey(), stack))
                        .values().forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
                n -= stack;
            }
        }
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        render();
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        boolean top = e.getClickedInventory() == inv;

        if (!top) {
            if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
                return;
            }
            if (e.isShiftClick()) {
                e.setCancelled(true);
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;
                ItemStack cur = inv.getItem(INPUT);
                if (cur == null) {
                    inv.setItem(INPUT, clicked.clone());
                    e.setCurrentItem(null);
                } else if (cur.isSimilar(clicked)) {
                    int move = Math.min(cur.getMaxStackSize() - cur.getAmount(), clicked.getAmount());
                    if (move > 0) {
                        ItemStack n = cur.clone();
                        n.setAmount(cur.getAmount() + move);
                        inv.setItem(INPUT, n);
                        clicked.setAmount(clicked.getAmount() - move);
                        e.setCurrentItem(clicked.getAmount() <= 0 ? null : clicked);
                    }
                }
                render();
            }
            return;
        }

        int slot = e.getSlot();
        if (slot == INPUT) {
            scheduleRender(); // Slot darf normal benutzt werden
            return;
        }
        e.setCancelled(true);
        if (slot == BUTTON && inv.getItem(BUTTON) != null && inv.getItem(BUTTON).getType() == Material.ANVIL) {
            salvage((Player) e.getWhoClicked());
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        for (int raw : e.getRawSlots()) {
            if (raw < inv.getSize() && raw != INPUT) {
                e.setCancelled(true);
                return;
            }
        }
        scheduleRender();
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        ItemStack it = inv.getItem(INPUT);
        if (it == null || it.getType().isAir()) return;
        inv.setItem(INPUT, null);
        Player p = (Player) e.getPlayer();
        p.getInventory().addItem(it).values().forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
    }
}
