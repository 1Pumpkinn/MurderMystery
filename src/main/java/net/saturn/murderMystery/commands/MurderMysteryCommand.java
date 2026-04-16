package net.saturn.murderMystery.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.game.GameManager;
import net.saturn.murderMystery.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class MurderMysteryCommand implements BasicCommand {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public MurderMysteryCommand(MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.prefix("§cOnly players can join."));
                    return;
                }
                if (gameManager.isGameRunning()) {
                    player.sendMessage(MessageUtil.prefix("§cA game is already running! Wait for the next round."));
                    return;
                }
                if (gameManager.isInLobby(player.getUniqueId())) {
                    player.sendMessage(MessageUtil.prefix("§cYou are already in the lobby."));
                    return;
                }
                gameManager.addToLobby(player);
                player.sendMessage(MessageUtil.prefix(
                        "§aYou joined the lobby! §8(" + gameManager.getLobbySize() + "/" + gameManager.getMinPlayers() + " min)"
                ));
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.prefix("§cOnly players can leave."));
                    return;
                }
                if (!gameManager.isInLobby(player.getUniqueId())) {
                    player.sendMessage(MessageUtil.prefix("§cYou are not in the lobby."));
                    return;
                }
                if (gameManager.isGameRunning()) {
                    player.sendMessage(MessageUtil.prefix("§cYou cannot leave mid-game. You will be treated as dead if you disconnect."));
                    return;
                }
                gameManager.removeFromLobby(player);
                player.sendMessage(MessageUtil.prefix("§7You left the lobby."));
            }
            case "start" -> {
                if (!sender.hasPermission("murdermystery.admin")) {
                    sender.sendMessage(MessageUtil.prefix("§cYou don't have permission."));
                    return;
                }
                if (gameManager.isGameRunning()) {
                    sender.sendMessage(MessageUtil.prefix("§cA game is already running."));
                    return;
                }
                if (gameManager.getLobbySize() < gameManager.getMinPlayers()) {
                    sender.sendMessage(MessageUtil.prefix("§cNeed at least " + gameManager.getMinPlayers() + " players."));
                    return;
                }
                sender.sendMessage(MessageUtil.prefix("§aForce-starting the game..."));
                gameManager.startGame();
            }
            case "stop", "end" -> {
                if (!sender.hasPermission("murdermystery.admin")) {
                    sender.sendMessage(MessageUtil.prefix("§cYou don't have permission."));
                    return;
                }
                if (!gameManager.isGameRunning()) {
                    sender.sendMessage(MessageUtil.prefix("§cNo game is running."));
                    return;
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
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("join", "leave", "status"));
            if (stack.getSender().hasPermission("murdermystery.admin")) {
                options.addAll(List.of("start", "stop"));
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
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
}