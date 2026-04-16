package net.saturn.murderMystery.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Role {
    MURDERER("Murderer", NamedTextColor.RED),
    SHERIFF("Sheriff", NamedTextColor.BLUE),
    INVESTIGATOR("Investigator", NamedTextColor.GREEN);

    private final String displayName;
    private final NamedTextColor color;

    Role(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Component toComponent() {
        return Component.text(displayName, color, TextDecoration.BOLD);
    }
}