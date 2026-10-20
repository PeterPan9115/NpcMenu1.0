package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.ShopLayout;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Editor fuer die Tausche (mehrere Seiten, waehlbares Layout).
 * Items werden nur KOPIERT, du verlierst nichts aus deinem Inventar.
 */
public final class TraderEditor extends Gui {

    private final NpcData d;
    private final ShopLayout layout;
    private final int page;
    private final Set<Integer> editable = new HashSet<>();

    public TraderEditor(NpcMenu plugin, NpcData d) {
        this(plugin, d, 0);
    }

    public TraderEditor(NpcMenu plugin, NpcData d, int page) {
        super(plugin);
        this.d = d;
        this.layout = d.layout;
        this.page = page;
        init(layout.size(), "<dark_gray>Shop-Editor - Seite " + (page + 1) + "/" + NpcData.MAX_PAGES);
        fill(Util.filler());

        for (int j = 0; j < NpcData.PER_PAGE; j++) {
            int i = page * NpcData.PER_PAGE + j;
            int in = layout.in(j);
            int out = layout.out(j);
            editable.add(in);
            editable.add(out);
            inv.setItem(in, d.inputs[i] == null ? null : d.inputs[i].clone());
            inv.setItem(out, d.outputs[i] == null ? null : d.outputs[i].clone());
            if (layout.arrow(j) >= 0) {
                inv.setItem(layout.arrow(j), Util.item(Material.ARROW, "<gray>Tausch " + (i + 1)));
            }
        }

        if (page > 0) {
            inv.setItem(layout.prevSlot(), Util.item(Material.ARROW, "<yellow>Vorherige Seite",
                    "<gray>Seite " + page));
        }
        if (page < NpcData.MAX_PAGES - 1) {
            inv.setItem(layout.nextSlot(), Util.item(Material.ARROW, "<yellow>Naechste Seite",
                    "<gray>Seite " + (page + 2)));
        }
        inv.setItem(layout.limitSlot(), Util.item(Material.CLOCK, "<aqua>Limits / Cooldowns",
                "<gray>Pro Tausch einstellbar,", "<gray>z.B. max. 5x pro Tag.", "",
                "<yellow>Klicken zum Oeffnen"));
        inv.setItem(layout.infoSlot(), Util.item(Material.PAPER, "<yellow>Anleitung",
                "<gray>Item aufnehmen und auf einen leeren",
                "<gray>Slot klicken = Item hinterlegen (Kopie).",
                "<gray>Linksklick auf Item: <white>Anzahl +1",
                "<gray>Rechtsklick auf Item: <white>Anzahl -1",
                "<gray>Shift-/Mittelklick: <white>leeren",
                "",
                "<gray>Preis = was der Spieler gibt",
                "<gray>Ware = was der Spieler bekommt",
                "<gray>Beide Slots eines Tauschs muessen belegt sein."));
        inv.setItem(layout.layoutSlot(), Util.item(Material.COMPARATOR, "<aqua>Layout",
                "<white>" + layout.label, "", "<yellow>Klicken: Anordnung wechseln"));
        inv.setItem(layout.doneSlot(), Util.item(Material.LIME_DYE, "<green><bold>Fertig"));
    }

    private void saveToData() {
        for (int j = 0; j < NpcData.PER_PAGE; j++) {
            int i = page * NpcData.PER_PAGE + j;
            ItemStack in = inv.getItem(layout.in(j));
            ItemStack out = inv.getItem(layout.out(j));
            d.inputs[i] = in == null ? null : in.clone();
            d.outputs[i] = out == null ? null : out.clone();
        }
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        boolean top = e.getClickedInventory() == inv;

        if (!top) {
            // Eigenes Inventar: normal benutzbar, aber nichts nach oben schieben
            if (e.isShiftClick() || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        int slot = e.getSlot();
        Player p = (Player) e.getWhoClicked();

        if (slot == layout.doneSlot()) {
            new EditMenu(plugin, d).open(p);
            return;
        }
        if (slot == layout.layoutSlot()) {
            saveToData();
            d.layout = layout.next();
            plugin.manager().refresh(d);
            new TraderEditor(plugin, d, page).open(p);
            return;
        }
        if (slot == layout.prevSlot() && page > 0) {
            saveToData();
            new TraderEditor(plugin, d, page - 1).open(p);
            return;
        }
        if (slot == layout.nextSlot() && page < NpcData.MAX_PAGES - 1) {
            saveToData();
            new TraderEditor(plugin, d, page + 1).open(p);
            return;
        }
        if (slot == layout.limitSlot()) {
            saveToData();
            new LimitMenu(plugin, d, page).open(p);
            return;
        }
        if (!editable.contains(slot)) return;

        ItemStack cursor = e.getCursor();
        ItemStack current = inv.getItem(slot);

        if (!cursor.isEmpty()) {
            inv.setItem(slot, cursor.clone());
        } else if (current != null) {
            ClickType c = e.getClick();
            if (c == ClickType.SHIFT_LEFT || c == ClickType.SHIFT_RIGHT || c == ClickType.MIDDLE) {
                inv.setItem(slot, null);
            } else if (c == ClickType.LEFT) {
                ItemStack n = current.clone();
                n.setAmount(Math.min(n.getMaxStackSize(), n.getAmount() + 1));
                inv.setItem(slot, n);
            } else if (c == ClickType.RIGHT) {
                if (current.getAmount() <= 1) {
                    inv.setItem(slot, null);
                } else {
                    ItemStack n = current.clone();
                    n.setAmount(n.getAmount() - 1);
                    inv.setItem(slot, n);
                }
            }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        for (int raw : e.getRawSlots()) {
            if (raw < inv.getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        saveToData();
        plugin.manager().refresh(d);
    }
}
