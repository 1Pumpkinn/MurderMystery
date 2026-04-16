package net.saturn.murderMystery.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class MessageUtil {

    private static final String PREFIX = "§8[§cMurder§8] §r";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static Component prefix(String message) {
        return LEGACY.deserialize(PREFIX + message);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                LEGACY.deserialize(title),
                LEGACY.deserialize(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }
}