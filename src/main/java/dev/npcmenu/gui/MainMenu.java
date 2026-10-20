package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class MainMenu extends Gui {

    public MainMenu(NpcMenu plugin, int page) {
        super(plugin);
        init(54, "<dark_gray>NPC-Verwaltung");

        List<NpcData> list = new ArrayList<>(plugin.manager().all());
        int start = page * 45;
        for (int i = 0; i < 45; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            NpcData d = list.get(idx);
            set(i, head(d), e -> new EditMenu(plugin, d).open((Player) e.getWhoClicked()));
        }

        for (int i = 45; i < 54; i++) set(i, Util.filler(), null);

        if (page > 0) {
            set(45, Util.item(Material.ARROW, "<yellow>Vorherige Seite"),
                    e -> new MainMenu(plugin, page - 1).open((Player) e.getWhoClicked()));
        }
        if (start + 45 < list.size()) {
            set(53, Util.item(Material.ARROW, "<yellow>Naechste Seite"),
                    e -> new MainMenu(plugin, page + 1).open((Player) e.getWhoClicked()));
        }
        set(49, Util.item(Material.EMERALD_BLOCK, "<green><bold>+ Neuer NPC",
                        "<gray>Erstellt einen neuen NPC", "<gray>an deiner Position."),
                e -> new EditMenu(plugin, new NpcData(Util.newId())).open((Player) e.getWhoClicked()));
    }

    private ItemStack head(NpcData d) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        try {
            meta.setPlayerProfile(Bukkit.createProfile(d.skin));
        } catch (Exception ignored) {
        }
        it.setItemMeta(meta);
        ItemStack named = Util.item(Material.PLAYER_HEAD, d.name,
                "<gray>Typ: <white>" + d.type.label,
                "<gray>Skin: <white>" + d.skin,
                "",
                "<yellow>Klicken zum Bearbeiten");
        // Kopf-Profil auf das benannte Item uebertragen
        named.setItemMeta(mergeSkull(named, meta));
        return named;
    }

    private SkullMeta mergeSkull(ItemStack named, SkullMeta skull) {
        SkullMeta m = (SkullMeta) named.getItemMeta();
        m.setPlayerProfile(skull.getPlayerProfile());
        return m;
    }
}
