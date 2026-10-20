package dev.npcmenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class Util {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Util() {}

    /** Unterstuetzt MiniMessage (<red>) und &-Farbcodes (&c). */
    public static Component parse(String s) {
        if (s.indexOf('&') >= 0 && s.indexOf('<') < 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
        }
        return MM.deserialize(s);
    }

    private static Component noItalic(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(noItalic(parse(name)));
        if (lore.length > 0) {
            meta.lore(Arrays.stream(lore).map(l -> noItalic(parse(l))).toList());
        }
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack withLore(ItemStack base, String... lines) {
        ItemStack c = base.clone();
        List<Component> l = c.lore() == null ? new ArrayList<>() : new ArrayList<>(c.lore());
        l.add(Component.empty());
        for (String line : lines) l.add(noItalic(parse(line)));
        c.lore(l);
        return c;
    }

    public static ItemStack filler() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.empty());
        it.setItemMeta(meta);
        return it;
    }

    public static void msg(CommandSender to, String mini) {
        to.sendMessage(parse("<dark_gray>[<gold>NPC<dark_gray>] " + mini));
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
