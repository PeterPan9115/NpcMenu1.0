package dev.npcmenu.gui;

import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        e.setCancelled(true);
        if (e.getClickedInventory() != inv) return;
        Consumer<InventoryClickEvent> a = actions.get(e.getSlot());
        if (a != null) a.accept(e);
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
