package net.saturn.murderMystery.listeners;

import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.game.GameState;
import net.saturn.murderMystery.roles.GamePlayer;
import net.saturn.murderMystery.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LobbyListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public LobbyListener(MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove from lobby
        gameManager.removeFromLobby(player);

        // If they were in a running game, treat as death
        if (gameManager.isGameRunning()) {
            GamePlayer gp = gameManager.getGamePlayer(player.getUniqueId());
            if (gp != null && gp.isAlive()) {
                player.sendMessage(MessageUtil.prefix("§7You left the game."));
                gameManager.handleDeath(player, null);
            }
        }
    }
}