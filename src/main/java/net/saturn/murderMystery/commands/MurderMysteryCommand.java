package net.saturn.murderMystery.commands;

import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.game.GameState;
import net.saturn.murderMystery.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class MurderMysteryCommand implements CommandExecutor, TabCompleter {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public MurderMysteryCommand(MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.prefix("§cOnly players can join."));
                    return true;
                }
                if (gameManager.isGameRunning()) {
                    player.sendMessage(MessageUtil.prefix("§cA game is already running! Wait for the next round."));
                    return true;
                }
                if (gameManager.isInLobby(player.getUniqueId())) {
                    player.sendMessage(MessageUtil.prefix("§cYou are already in the lobby."));
                    return true;
                }
                gameManager.addToLobby(player);
                player.sendMessage(MessageUtil.prefix(
                        "§aYou joined the lobby! §8(" + gameManager.getLobbySize() + "/" + gameManager.getMinPlayers() + " min)"
                ));
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.prefix("§cOnly players can leave."));
                    return true;
                }
                if (!gameManager.isInLobby(player.getUniqueId())) {
                    player.sendMessage(MessageUtil.prefix("§cYou are not in the lobby."));
                    return true;
                }
                if (gameManager.isGameRunning()) {
                    player.sendMessage(MessageUtil.prefix("§cYou cannot leave mid-game. You will be treated as dead if you disconnect."));
                    return true;
                }
                gameManager.removeFromLobby(player);
                player.sendMessage(MessageUtil.prefix("§7You left the lobby."));
            }
            case "start" -> {
                if (!sender.hasPermission("murdermystery.admin")) {
                    sender.sendMessage(MessageUtil.prefix("§cYou don't have permission."));
                    return true;
                }
                if (gameManager.isGameRunning()) {
                    sender.sendMessage(MessageUtil.prefix("§cA game is already running."));
                    return true;
                }
                if (gameManager.getLobbySize() < gameManager.getMinPlayers()) {
                    sender.sendMessage(MessageUtil.prefix("§cNeed at least " + gameManager.getMinPlayers() + " players."));
                    return true;
                }
                sender.sendMessage(MessageUtil.prefix("§aForce-starting the game..."));
                gameManager.startGame();
            }
            case "stop", "end" -> {
                if (!sender.hasPermission("murdermystery.admin")) {
                    sender.sendMessage(MessageUtil.prefix("§cYou don't have permission."));
                    return true;
                }
                if (!gameManager.isGameRunning()) {
                    sender.sendMessage(MessageUtil.prefix("§cNo game is running."));
                    return true;
                }
                gameManager.forceEndGame();
                sender.sendMessage(MessageUtil.prefix("§cGame forcefully ended."));
            }
            case "status" -> {
                sender.sendMessage(MessageUtil.prefix("§7State: §f" + gameManager.getState()));
                sender.sendMessage(MessageUtil.prefix("§7Players in lobby: §f" + gameManager.getLobbySize()));
                if (gameManager.isGameRunning()) {
                    sender.sendMessage(MessageUtil.prefix("§7Alive: §f" + gameManager.getAlivePlayers().size()));
                    sender.sendMessage(MessageUtil.prefix("§7Dead: §f" + gameManager.getDeadPlayers().size()));
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.prefix("§7Commands:"));
        sender.sendMessage(MessageUtil.prefix("§f/mm join §7- Join the lobby"));
        sender.sendMessage(MessageUtil.prefix("§f/mm leave §7- Leave the lobby"));
        sender.sendMessage(MessageUtil.prefix("§f/mm status §7- View game status"));
        if (sender.hasPermission("murdermystery.admin")) {
            sender.sendMessage(MessageUtil.prefix("§f/mm start §7- Force start the game"));
            sender.sendMessage(MessageUtil.prefix("§f/mm stop §7- Force stop the game"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>(List.of("join", "leave", "status"));
            if (sender.hasPermission("murdermystery.admin")) {
                options.addAll(List.of("start", "stop"));
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}