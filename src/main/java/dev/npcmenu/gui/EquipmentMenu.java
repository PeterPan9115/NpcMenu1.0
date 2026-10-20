package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Ruestung und Hand-Items des NPCs. Items werden nur kopiert. */
public final class EquipmentMenu extends Gui {

    private final NpcData d;

    public EquipmentMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, "<dark_gray>NPC-Ausruestung");
        render();
    }

    private int slotOf(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> 10;
            case CHEST -> 11;
            case LEGS -> 12;
            case FEET -> 13;
            case HAND -> 15;
            default -> 16;
        };
    }

    private String labelOf(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> "Kopf";
            case CHEST -> "Brust";
            case LEGS -> "Hose";
            case FEET -> "Schuhe";
            case HAND -> "Haupthand";
            default -> "Zweithand";
        };
    }

    private void changed() {
        plugin.manager().refresh(d);
        render();
    }

    private void render() {
        clear();
        fill(Util.filler());

        for (EquipmentSlot es : NpcData.EQUIP_SLOTS) {
            ItemStack cur = d.equipment.get(es);
            ItemStack shown = cur == null
                    ? Util.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<gray>" + labelOf(es) + ": leer",
                    "<yellow>Mit Item am Cursor klicken = setzen")
                    : Util.withLore(cur, "<gray>" + labelOf(es), "<yellow>Ohne Item klicken = entfernen");
            set(slotOf(es), shown, e -> {
                ItemStack cursor = e.getCursor();
                if (!cursor.isEmpty()) {
                    ItemStack copy = cursor.clone();
                    copy.setAmount(1);
                    d.equipment.put(es, copy);
                } else {
                    d.equipment.remove(es);
                }
                changed();
            });
        }

        set(4, Util.item(Material.PLAYER_HEAD, "<aqua>Meine Ausruestung uebernehmen",
                "<gray>Kopiert Ruestung und Hand-Items,", "<gray>die du gerade traegst.", "",
                "<yellow>Klicken"), e -> {
            Player p = (Player) e.getWhoClicked();
            PlayerInventory pi = p.getInventory();
            copy(EquipmentSlot.HEAD, pi.getHelmet());
            copy(EquipmentSlot.CHEST, pi.getChestplate());
            copy(EquipmentSlot.LEGS, pi.getLeggings());
            copy(EquipmentSlot.FEET, pi.getBoots());
            copy(EquipmentSlot.HAND, pi.getItemInMainHand());
            copy(EquipmentSlot.OFF_HAND, pi.getItemInOffHand());
            changed();
        });

        set(22, Util.item(Material.ARROW, "<yellow>Zurueck"),
                e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
    }

    private void copy(EquipmentSlot es, ItemStack it) {
        if (it == null || it.getType().isAir()) {
            d.equipment.remove(es);
        } else {
            d.equipment.put(es, it.clone());
        }
    }
}
