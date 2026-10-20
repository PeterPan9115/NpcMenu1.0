package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Obere Reihe (Slot 0-8):  Das gibt der Spieler (Preis)
 * Mittlere Reihe (9-17):   Das bekommt der Spieler (Ware)
 *
 * Die Items werden nur KOPIERT, du verlierst nichts aus deinem Inventar.
 */
public final class TraderEditor extends Gui {

    private final NpcData d;

    public TraderEditor(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, "<dark_gray>Shop: Oben = Preis, Mitte = Ware");

        for (int i = 0; i < NpcData.TRADES; i++) {
            inv.setItem(i, d.inputs[i] == null ? null : d.inputs[i].clone());
            inv.setItem(i + 9, d.outputs[i] == null ? null : d.outputs[i].clone());
        }
        for (int i = 18; i < 27; i++) inv.setItem(i, Util.filler());

        inv.setItem(22, Util.item(Material.PAPER, "<yellow>Anleitung",
                "<gray>Oben: <white>Das gibt der Spieler",
                "<gray>Mitte: <white>Das bekommt der Spieler",
                "",
                "<gray>Item aufnehmen und auf einen Slot",
                "<gray>klicken = Item hinterlegen (Kopie).",
                "<gray>Linksklick auf Item: <white>Anzahl +1",
                "<gray>Rechtsklick auf Item: <white>Anzahl -1",
                "<gray>Shift-/Mittelklick: <white>leeren"));
        inv.setItem(26, Util.item(Material.LIME_DYE, "<green><bold>Fertig"));
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

        if (slot == 26) {
            new EditMenu(plugin, d).open((Player) e.getWhoClicked());
            return;
        }
        if (slot >= 18) return;

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
                if (current.getAmount() <= 1) inv.setItem(slot, null);
                else {
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
        for (int i = 0; i < NpcData.TRADES; i++) {
            ItemStack in = inv.getItem(i);
            ItemStack out = inv.getItem(i + 9);
            d.inputs[i] = in == null ? null : in.clone();
            d.outputs[i] = out == null ? null : out.clone();
        }
        plugin.manager().refresh(d);
    }
}
