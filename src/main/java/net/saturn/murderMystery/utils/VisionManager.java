package net.saturn.murderMystery.utils;

import net.saturn.murderMystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.function.Supplier;

/**
 * Applies a soft darkness effect to all alive players to simulate limited vision.
 * Amplifier 1 of DARKNESS shrinks the visible sphere to ~10 blocks — noticeable
 * but not oppressive. The task refreshes every 2 seconds so the effect never
 * expires mid-game.
 */
public class VisionManager {

    private static final int VISION_AMPLIFIER = 1;   // 0 = ~14 blocks, 1 = ~10 blocks, 2 = ~7 blocks
    private static final int REFRESH_TICKS = 40;     // refresh every 2 seconds

    private final MurderMystery plugin;
    private final Supplier<List<Player>> alivePlayersSupplier;
    private BukkitTask task;

    public VisionManager(MurderMystery plugin, Supplier<List<Player>> alivePlayersSupplier) {
        this.plugin = plugin;
        this.alivePlayersSupplier = alivePlayersSupplier;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::applyVision, 0L, REFRESH_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // Remove lingering effects
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    /** Temporarily boost a player's vision (e.g., sheriff's flashlight power-up) */
    public void grantTemporaryVision(Player player, int durationTicks) {
        player.removePotionEffect(PotionEffectType.DARKNESS);
        // Re-apply at lower amplifier after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, REFRESH_TICKS + 10, VISION_AMPLIFIER, false, false, false
                ));
            }
        }, durationTicks);
    }

    private void applyVision() {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.DARKNESS,
                REFRESH_TICKS + 10,   // slightly longer than refresh to avoid flicker
                VISION_AMPLIFIER,
                false,  // no ambient particles
                false,  // no particles at all
                false   // no icon (keeps HUD clean)
        );
        for (Player p : alivePlayersSupplier.get()) {
            if (p.isOnline()) {
                p.addPotionEffect(effect);
            }
        }
    }
}