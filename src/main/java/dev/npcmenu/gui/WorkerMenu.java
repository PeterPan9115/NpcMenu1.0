package dev.npcmenu.gui;

import dev.npcmenu.NpcData;
import dev.npcmenu.NpcMenu;
import dev.npcmenu.Util;
import dev.npcmenu.WorkerRates;
import dev.npcmenu.WorkerStore;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Fenster des Arbeiters: Wunsch-Item + Dauer waehlen, Auftrag starten, spaeter abholen. */
public final class WorkerMenu extends Gui {

    private static final int[] PRESETS = {1, 5, 10, 15, 30, 60, 120, 180, 240, 360, 480, 720, 1440, 2880, 10080};

    private final NpcData d;
    private final UUID viewer;
    private Material selected;
    private int durIdx = 2;

    public WorkerMenu(NpcMenu plugin, NpcData d, Player viewer) {
        super(plugin);
        this.d = d;
        this.viewer = viewer.getUniqueId();
        init(27, "<dark_gray>Arbeiter");
        render();
    }

    private List<Integer> options() {
        List<Integer> l = new ArrayList<>();
        for (int m : PRESETS) if (m <= d.workerMaxMinutes) l.add(m);
        if (l.isEmpty()) l.add(1);
        return l;
    }

    private double rateOf(Material m) {
        return WorkerRates.rate(m, plugin.getConfig());
    }

    private void render() {
        clear();
        fill(Util.filler());
        WorkerStore.Job job = plugin.workers().get(d.id, viewer);
        if (job != null) renderJob(job);
        else renderSetup();
    }

    // ------------------------------------------------------------ Einrichten

    private void renderSetup() {
        List<Integer> opts = options();
        durIdx = Math.max(0, Math.min(durIdx, opts.size() - 1));
        int minutes = opts.get(durIdx);

        set(4, Util.item(Material.PAPER, "<yellow>Arbeiter",
                "<gray>Wunsch-Item festlegen, Dauer waehlen",
                "<gray>und den Arbeiter losschicken.",
                "<gray>Je seltener das Item, desto weniger",
                "<gray>sammelt er.", "",
                "<gray>Geschwindigkeit: <white>" + d.workerSpeed + "%",
                "<gray>Max. Dauer: <white>" + Util.duration(d.workerMaxMinutes * 60000L)), null);

        double rate = selected == null ? 0 : rateOf(selected);

        if (selected == null) {
            set(11, Util.item(Material.CHEST, "<aqua>Gewuenschtes Item",
                    "<gray>Nimm ein Item auf den Cursor",
                    "<gray>und klicke hier.",
                    "<gray>Es wird nicht verbraucht."), e -> chooseItem((Player) e.getWhoClicked(), e.getCursor()));
        } else {
            set(11, Util.withLore(new ItemStack(selected), "<aqua>Gewaehltes Item",
                    "<gray>Seltenheit: " + WorkerRates.tier(rate),
                    "<gray>Grundrate: <white>" + WorkerRates.fmt(rate) + " pro Stunde",
                    "",
                    "<yellow>Anderes Item: mit Cursor klicken"), e -> chooseItem((Player) e.getWhoClicked(), e.getCursor()));
        }

        set(13, Util.item(Material.CLOCK, "<aqua>Dauer: <white>" + Util.duration(minutes * 60000L),
                "<gray>Wie lange der Arbeiter farmt.", "",
                "<yellow>Linksklick: laenger  |  Rechtsklick: kuerzer"), e -> {
            if (e.isLeftClick()) durIdx = Math.min(opts.size() - 1, durIdx + 1);
            else durIdx = Math.max(0, durIdx - 1);
            render();
        });

        if (selected == null) {
            set(15, Util.item(Material.BARRIER, "<gray>Auftrag starten", "<gray>Waehle zuerst ein Item."), null);
        } else {
            double exp = WorkerRates.expected(rate, d.workerSpeed, minutes);
            if (exp < 0.5) {
                double perMin = rate * (d.workerSpeed / 100.0) / 60.0;
                int need = perMin <= 0 ? 0 : (int) Math.ceil(0.5 / perMin);
                set(15, Util.item(Material.BARRIER, "<red>Zeit zu kurz",
                        "<gray>Dieses Item ist selten.",
                        "<gray>Waehle mindestens ca. <white>" + Util.duration(need * 60000L) + "<gray>."), null);
            } else {
                int lo = Math.max(1, (int) Math.floor(exp * 0.85));
                int hi = Math.max(lo, (int) Math.ceil(exp * 1.15));
                set(15, Util.item(Material.LIME_DYE, "<green><bold>Auftrag starten",
                        "<gray>Item: <white>" + WorkerRates.nice(selected),
                        "<gray>Dauer: <white>" + Util.duration(minutes * 60000L),
                        "<gray>Erwartet: <white>ca. " + lo + " - " + hi + "x",
                        "", "<yellow>Klicken zum Starten"), e -> start((Player) e.getWhoClicked()));
            }
        }
    }

    private void chooseItem(Player p, ItemStack cursor) {
        if (cursor.isEmpty()) {
            selected = null;
        } else {
            Material m = cursor.getType();
            if (rateOf(m) <= 0) {
                Util.msg(p, "<red>Das kann der Arbeiter nicht farmen <gray>(nur Rohstoffe, keine gecrafteten oder extrem seltenen Items).");
                return;
            }
            selected = m;
        }
        render();
    }

    private void start(Player p) {
        if (selected == null || plugin.workers().get(d.id, viewer) != null) return;
        double rate = rateOf(selected);
        int minutes = options().get(durIdx);
        int amount = WorkerRates.roll(rate, d.workerSpeed, minutes, ThreadLocalRandom.current());
        if (amount <= 0) {
            Util.msg(p, "<red>Die Zeit ist fuer dieses Item zu kurz.");
            return;
        }
        long now = System.currentTimeMillis();
        WorkerStore.Job j = new WorkerStore.Job();
        j.material = selected.name();
        j.amount = amount;
        j.start = now;
        j.end = now + minutes * 60_000L;
        j.notified = false;
        plugin.workers().put(d.id, viewer, j);
        Util.msg(p, "<green>Der Arbeiter ist unterwegs! <gray>Du wirst ueber der XP-Leiste benachrichtigt, wenn er fertig ist.");
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 1f, 1f);
        render();
    }

    // ------------------------------------------------------------ Laufender / fertiger Auftrag

    private void renderJob(WorkerStore.Job job) {
        Material m = Material.matchMaterial(job.material);
        if (m == null) m = Material.STONE;
        long now = System.currentTimeMillis();
        boolean done = now >= job.end;
        long total = Math.max(1, job.end - job.start);
        double frac = Math.max(0, Math.min(1, (double) (now - job.start) / total));

        set(11, Util.withLore(new ItemStack(m, Math.max(1, Math.min(64, job.amount))), "<aqua>Dein Auftrag",
                "<gray>Item: <white>" + WorkerRates.nice(m),
                "<gray>Menge: <white>" + job.amount + "x"), null);

        if (done) {
            set(13, Util.item(Material.LIME_STAINED_GLASS_PANE, "<green><bold>Fertig!",
                    "<gray>Dein Arbeiter ist zurueck."), null);
            set(15, Util.item(Material.CHEST, "<green><bold>Abholen",
                    "<gray>Menge: <white>" + job.amount + "x " + WorkerRates.nice(m), "",
                    "<yellow>Klicken zum Abholen"), e -> collect((Player) e.getWhoClicked()));
        } else {
            int filled = (int) Math.round(frac * 20);
            String bar = "<green>" + "|".repeat(filled) + "<dark_gray>" + "|".repeat(20 - filled)
                    + " <white>" + Math.round(frac * 100) + "%";
            set(13, Util.item(Material.CLOCK, "<yellow>Arbeitet...",
                    "<gray>Noch: <white>" + Util.duration(job.end - now), bar), null);
            int partial = (int) Math.floor(job.amount * frac);
            set(15, Util.item(Material.RED_DYE, "<red>Abbrechen",
                    "<gray>Du bekommst nur den bisherigen",
                    "<gray>Teil: <white>ca. " + partial + "x", "",
                    "<yellow>Shift + Klick zum Bestaetigen"), e -> {
                Player p = (Player) e.getWhoClicked();
                if (!e.isShiftClick()) {
                    Util.msg(p, "<red>Zum Abbrechen Shift + Klick benutzen.");
                    return;
                }
                cancel(p);
            });
        }
        set(22, Util.item(Material.REPEATER, "<yellow>Aktualisieren"), e -> render());
    }

    private int give(Player p, Material m, int n, boolean drop) {
        while (n > 0) {
            int stack = Math.min(n, m.getMaxStackSize());
            Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(m, stack));
            n -= stack;
            if (!left.isEmpty()) {
                int lo = 0;
                for (ItemStack it : left.values()) lo += it.getAmount();
                if (drop) {
                    left.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                } else {
                    return lo + n;
                }
            }
        }
        return 0;
    }

    private void collect(Player p) {
        WorkerStore.Job job = plugin.workers().get(d.id, viewer);
        if (job == null || System.currentTimeMillis() < job.end) return;
        Material m = Material.matchMaterial(job.material);
        if (m == null) {
            plugin.workers().remove(d.id, viewer);
            render();
            return;
        }
        int remaining = give(p, m, job.amount, false);
        if (remaining > 0) {
            job.amount = remaining;
            plugin.workers().put(d.id, viewer, job);
            Util.msg(p, "<red>Dein Inventar ist voll. <gray>Der Rest (" + remaining + "x) bleibt beim Arbeiter.");
        } else {
            plugin.workers().remove(d.id, viewer);
            Util.msg(p, "<green>Abgeholt!");
        }
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        render();
    }

    private void cancel(Player p) {
        WorkerStore.Job job = plugin.workers().get(d.id, viewer);
        if (job == null) return;
        long now = System.currentTimeMillis();
        double frac = Math.max(0, Math.min(1, (double) (now - job.start) / Math.max(1, job.end - job.start)));
        int partial = (int) Math.floor(job.amount * frac);
        Material m = Material.matchMaterial(job.material);
        if (m != null && partial > 0) give(p, m, partial, true);
        plugin.workers().remove(d.id, viewer);
        Util.msg(p, "<yellow>Auftrag abgebrochen. <gray>Erhalten: " + partial + "x");
        render();
    }
}
