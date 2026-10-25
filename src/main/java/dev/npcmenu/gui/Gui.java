package dev.npcmenu.gui;

import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Gui implements InventoryHolder {

    protected final NpcMenu plugin;
    protected Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Gui(NpcMenu plugin) {
        this.plugin = plugin;
    }

    protected void init(int size, String title) {
        inv = Bukkit.createInventory(this, size, Util.parse(title));
    }

    protected void clear() {
        inv.clear();
        actions.clear();
    }

    protected void fill(ItemStack item) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    public void onClick(InventoryClickEvent e) {
        Inventory clicked = e.getClickedInventory();
        if (clicked == null) {
            e.setCancelled(true);
            return;
        }
        if (clicked != inv) {
            // Eigenes Inventar: Items duerfen aufgenommen werden (fuer die Auswahl per Cursor),
            // aber es darf nichts ins Menue geschoben oder eingesammelt werden.
            InventoryAction a = e.getAction();
            if (e.isShiftClick()
                    || a == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || a == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
            return;
        }
        e.setCancelled(true);
        Consumer<InventoryClickEvent> act = actions.get(e.getSlot());
        if (act != null) act.accept(e);
    }

    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    public void onClose(InventoryCloseEvent e) {
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
