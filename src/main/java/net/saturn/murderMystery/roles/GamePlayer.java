package net.saturn.murderMystery.roles;

import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private final String name;
    private Role role;
    private boolean alive;

    public GamePlayer(Player player, Role role) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.role = role;
        this.alive = true;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}