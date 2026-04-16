package net.saturn.murderMystery;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.saturn.murderMystery.commands.MurderMysteryCommand;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.listeners.GameListener;
import net.saturn.murderMystery.listeners.LobbyListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        gameManager = new GameManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);

        // Register commands using Paper's lifecycle API
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            MurderMysteryCommand cmd = new MurderMysteryCommand(this);
            event.registrar().register(
                    "murdermystery",
                    "Main Murder Mystery command",
                    List.of("mm"),
                    cmd
            );
        });

        getLogger().info("MurderMystery plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameRunning()) {
            gameManager.forceEndGame();
        }
        getLogger().info("MurderMystery plugin disabled!");
    }

    public static MurderMystery getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}