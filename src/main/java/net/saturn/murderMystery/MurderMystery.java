package net.saturn.murderMystery;

import net.saturn.murderMystery.commands.MurderMysteryCommand;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.listeners.GameListener;
import net.saturn.murderMystery.listeners.LobbyListener;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Register commands
        MurderMysteryCommand cmd = new MurderMysteryCommand(this);
        getCommand("murdermystery").setExecutor(cmd);
        getCommand("murdermystery").setTabCompleter(cmd);

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