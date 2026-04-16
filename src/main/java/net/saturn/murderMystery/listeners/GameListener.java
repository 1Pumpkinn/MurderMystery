package net.saturn.murderMystery.listeners;

import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.roles.GamePlayer;
import net.saturn.murderMystery.roles.Role;
import net.saturn.murderMystery.utils.ItemFactory;
import net.saturn.murderMystery.utils.MessageUtil;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GameListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public GameListener(MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /** Handle melee hits — only murderer knife should kill */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameRunning()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        GamePlayer attackerGp = gameManager.getGamePlayer(attacker.getUniqueId());
        GamePlayer victimGp   = gameManager.getGamePlayer(victim.getUniqueId());

        // Cancel all damage in game by default
        event.setCancelled(true);

        if (attackerGp == null || victimGp == null) return;
        if (!attackerGp.isAlive() || !victimGp.isAlive()) return;

        // Only murderer can kill with knife
        if (attackerGp.getRole() == Role.MURDERER
                && ItemFactory.isMurdererKnife(attacker.getInventory().getItemInMainHand())) {
            gameManager.handleDeath(victim, attacker);
        } else {
            attacker.sendMessage(MessageUtil.prefix("§cYou cannot attack other players with that."));
        }
    }

    /** Tag sheriff arrows when fired so we can identify them on hit */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!gameManager.isGameRunning()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        GamePlayer gp = gameManager.getGamePlayer(shooter.getUniqueId());
        if (gp == null || gp.getRole() != Role.SHERIFF) return;

        // Tag the arrow
        arrow.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, ItemFactory.SHERIFF_ARROW_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN, true
        );
        arrow.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "shooter_uuid"),
                org.bukkit.persistence.PersistentDataType.STRING, shooter.getUniqueId().toString()
        );

        // Consume the arrow from inventory
        shooter.getInventory().remove(
                shooter.getInventory().getItem(1)
        );
    }

    /** Handle sheriff arrow hits */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!gameManager.isGameRunning()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(event.getHitEntity() instanceof Player victim)) return;

        // Check if this is a sheriff arrow
        org.bukkit.NamespacedKey arrowKey = new org.bukkit.NamespacedKey(plugin, ItemFactory.SHERIFF_ARROW_TAG);
        if (!arrow.getPersistentDataContainer().has(arrowKey, org.bukkit.persistence.PersistentDataType.BOOLEAN)) return;

        arrow.remove();
        event.setCancelled(true);

        // Get shooter
        String shooterUuidStr = arrow.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "shooter_uuid"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
        if (shooterUuidStr == null) return;

        Player shooter = plugin.getServer().getPlayer(java.util.UUID.fromString(shooterUuidStr));
        if (shooter == null) return;

        GamePlayer victimGp = gameManager.getGamePlayer(victim.getUniqueId());
        if (victimGp == null || !victimGp.isAlive()) return;

        if (victimGp.getRole() == Role.MURDERER) {
            // Great shot!
            shooter.sendMessage(MessageUtil.prefix("§a§lDirect hit! The murderer is dead!"));
            gameManager.handleDeath(victim, shooter);
        } else {
            // Shot an innocent — sheriff dies, drops bow
            shooter.sendMessage(MessageUtil.prefix("§c§lYou shot " + victim.getName() + " — an innocent!"));
            victim.sendMessage(MessageUtil.prefix("§aThe sheriff shot at you but they paid the price!"));
            gameManager.handleDeath(shooter, null);
        }
    }

    /** Prevent dropping game items */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!gameManager.isGameRunning()) return;
        if (!gameManager.isInLobby(event.getPlayer().getUniqueId())) return;

        org.bukkit.inventory.ItemStack dropped = event.getItemDrop().getItemStack();
        if (ItemFactory.isMurdererKnife(dropped) || ItemFactory.isSheriffBow(dropped)) {
            event.setCancelled(true);
        }
    }

    /** Prevent sheriff from using bow slot wrong way */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!gameManager.isGameRunning()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        GamePlayer gp = gameManager.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        // If sheriff has no arrows left, cancel bow interaction
        if (gp.getRole() == Role.SHERIFF
                && ItemFactory.isSheriffBow(player.getInventory().getItemInMainHand())
                && player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW), 1) == false
                && !player.getInventory().contains(org.bukkit.Material.ARROW)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.prefix("§c§lOut of ammo! Your shot is gone."));
        }
    }
}