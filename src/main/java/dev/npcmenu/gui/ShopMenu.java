package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ShopMenu extends Gui {

    private final NpcData d;

    public ShopMenu(NpcMenu plugin, NpcData d) {
        super(plugin);
        this.d = d;
        init(27, d.name);
        fill(Util.filler());

        boolean any = false;
        for (int i = 0; i < NpcData.TRADES; i++) {
            ItemStack in = d.inputs[i];
            ItemStack out = d.outputs[i];
            if (in == null || out == null) continue;
            any = true;
            final int idx = i;
            set(i, Util.withLore(in, "<red>Du gibst:", "<gray>" + in.getAmount() + "x"), e -> trade((Player) e.getWhoClicked(), idx));
            set(i + 9, Util.withLore(out, "<green>Du bekommst:", "<yellow>Klicken zum Tauschen"), e -> trade((Player) e.getWhoClicked(), idx));
        }
        if (!any) {
            set(13, Util.item(Material.BARRIER, "<red>Dieser Shop ist leer."), null);
        }
    }

    private void trade(Player p, int i) {
        ItemStack in = d.inputs[i];
        ItemStack out = d.outputs[i];
        if (in == null || out == null) return;

        PlayerInventory pi = p.getInventory();
        if (!pi.containsAtLeast(in, in.getAmount())) {
            Util.msg(p, "<red>Dir fehlen: <white>" + in.getAmount() + "x " + in.getType().name().toLowerCase().replace('_', ' '));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        pi.removeItem(in.clone());
        pi.addItem(out.clone()).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
    }
}
