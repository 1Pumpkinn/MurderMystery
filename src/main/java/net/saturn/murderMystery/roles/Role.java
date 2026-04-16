package net.saturn.murderMystery.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Role {
    MURDERER("Murderer", NamedTextColor.RED),
    SHERIFF("Sheriff", NamedTextColor.GOLD),
    INVESTIGATOR("Investigator", NamedTextColor.GREEN);

    private final String displayName;
    private final NamedTextColor color;

    Role(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public NamedTextColor getColor() { return color; }

    /** Legacy § color code for chat messages that still use legacy formatting. */
    public String getLegacyColor() {
        return switch (this) {
            case MURDERER     -> "§c";
            case SHERIFF      -> "§e";
            case INVESTIGATOR -> "§a";
        };
    }

    public Component toComponent() {
        return Component.text(displayName, color, TextDecoration.BOLD);
    }
}