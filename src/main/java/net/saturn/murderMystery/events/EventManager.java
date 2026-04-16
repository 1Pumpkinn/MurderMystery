package net.saturn.murderMystery.events;

import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.roles.GamePlayer;
import net.saturn.murderMystery.roles.Role;
import net.saturn.murderMystery.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private BukkitTask eventTask;
    private BukkitTask flickerTask;

    // Min/max seconds between events
    private static final int EVENT_MIN_SECONDS = 60;
    private static final int EVENT_MAX_SECONDS = 120;

    public EventManager(MurderMystery plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void start() {
        scheduleNextEvent();
    }

    public void stop() {
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        if (flickerTask != null) {
            flickerTask.cancel();
            flickerTask = null;
        }
    }

    private void scheduleNextEvent() {
        Random random = new Random();
        int delay = (EVENT_MIN_SECONDS + random.nextInt(EVENT_MAX_SECONDS - EVENT_MIN_SECONDS)) * 20;

        eventTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!gameManager.isGameRunning()) return;
            triggerRandomEvent();
            scheduleNextEvent();
        }, delay);
    }

    private void triggerRandomEvent() {
        EventType[] types = EventType.values();

        // Filter out BODY_FOUND if no dead players
        List<EventType> available = new ArrayList<>(Arrays.asList(types));
        if (gameManager.getDeadPlayers().isEmpty()) {
            available.remove(EventType.BODY_FOUND);
        }
        // Need at least 2 alive for swap
        if (gameManager.getAlivePlayers().size() < 2) {
            available.remove(EventType.SWAP);
        }

        EventType chosen = available.get(new Random().nextInt(available.size()));
        broadcastEvent(chosen);

        switch (chosen) {
            case BLACKOUT -> doBlackout();
            case FLICKER -> doFlicker();
            case FOG -> doFog();
            case ALARM -> doAlarm();
            case SWAP -> doSwap();
            case BODY_FOUND -> doBodyFound();
        }
    }

    private void broadcastEvent(EventType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtil.sendTitle(player, type.getAnnouncement(), "", 10, 40, 10);
        }
        Bukkit.broadcast(MessageUtil.prefix(type.getAnnouncement()));
    }

    // ─── Event Implementations ───────────────────────────────────────────────

    /** Full darkness for 8 seconds */
    private void doBlackout() {
        List<Player> alive = gameManager.getAlivePlayers();
        for (Player p : alive) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 8 * 20, 5, false, false));
        }
        // Restore after
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : alive) {
                if (p.isOnline()) p.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }, 8 * 20L);
    }

    /** Rapidly flicker blindness on and off for 5 seconds */
    private void doFlicker() {
        final int[] ticks = {0};
        final int totalTicks = 5 * 20;
        List<Player> alive = gameManager.getAlivePlayers();

        flickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (ticks[0] >= totalTicks) {
                for (Player p : alive) {
                    if (p.isOnline()) p.removePotionEffect(PotionEffectType.DARKNESS);
                }
                flickerTask.cancel();
                return;
            }
            boolean blind = (ticks[0] / 4) % 2 == 0;
            for (Player p : alive) {
                if (!p.isOnline()) continue;
                if (blind) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 6, 2, false, false));
                } else {
                    p.removePotionEffect(PotionEffectType.DARKNESS);
                }
            }
            ticks[0]++;
        }, 0L, 1L);
    }

    /** Heavy blindness/fog for 5 seconds */
    private void doFog() {
        for (Player p : gameManager.getAlivePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 0, false, false));
        }
    }

    /** Reveal the location of a random alive non-murderer player */
    private void doAlarm() {
        List<Player> candidates = gameManager.getAlivePlayers().stream()
                .filter(p -> {
                    GamePlayer gp = gameManager.getGamePlayer(p.getUniqueId());
                    return gp != null && gp.getRole() != Role.MURDERER;
                })
                .toList();
        if (candidates.isEmpty()) return;

        Player target = candidates.get(new Random().nextInt(candidates.size()));
        Location loc = target.getLocation();
        String coordStr = String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());

        Bukkit.broadcast(MessageUtil.prefix(
                "§e§lALARM: §r§e" + target.getName() + " was spotted at " + coordStr + "!"
        ));
    }

    /** Swap two random alive players' positions */
    private void doSwap() {
        List<Player> alive = new ArrayList<>(gameManager.getAlivePlayers());
        Collections.shuffle(alive);
        Player a = alive.get(0);
        Player b = alive.get(1);

        Location locA = a.getLocation().clone();
        Location locB = b.getLocation().clone();

        a.teleport(locB);
        b.teleport(locA);

        a.sendMessage(MessageUtil.prefix("§d§lYou were swapped with §r§d" + b.getName() + "§d!"));
        b.sendMessage(MessageUtil.prefix("§d§lYou were swapped with §r§d" + a.getName() + "§d!"));
    }

    /** Reveal a random dead player's last known location */
    private void doBodyFound() {
        List<GamePlayer> dead = gameManager.getDeadPlayers();
        if (dead.isEmpty()) return;

        GamePlayer victim = dead.get(new Random().nextInt(dead.size()));
        Location loc = gameManager.getDeathLocation(victim.getUuid());
        if (loc == null) return;

        String coordStr = String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
        Bukkit.broadcast(MessageUtil.prefix(
                "§c§lBODY FOUND: §r§c" + victim.getName() + "'s body was found near " + coordStr + "!"
        ));
    }
}