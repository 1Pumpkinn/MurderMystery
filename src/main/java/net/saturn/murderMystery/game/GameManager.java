package net.saturn.murderMystery.game;

import net.saturn.murderMystery.MurderMystery;
import net.saturn.murderMystery.events.EventManager;
import net.saturn.murderMystery.roles.GamePlayer;
import net.saturn.murderMystery.roles.Role;
import net.saturn.murderMystery.utils.ItemFactory;
import net.saturn.murderMystery.utils.MessageUtil;
import net.saturn.murderMystery.utils.RoleAssigner;
import net.saturn.murderMystery.utils.VisionManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final MurderMystery plugin;
    private GameState state = GameState.WAITING;

    // Player tracking
    private final Map<UUID, GamePlayer> gamePlayers = new HashMap<>();
    private final List<GamePlayer> deadPlayers = new ArrayList<>();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // Lobbied players (before game starts)
    private final Set<UUID> lobbyPlayers = new HashSet<>();

    // Sub-systems
    private EventManager eventManager;
    private VisionManager visionManager;

    // Countdown
    private BukkitTask countdownTask;
    private int countdownSeconds = 30;

    // Minimum players to start
    private static final int MIN_PLAYERS = 4;

    public GameManager(MurderMystery plugin) {
        this.plugin = plugin;
    }

    // ─── Lobby ───────────────────────────────────────────────────────────────

    public boolean addToLobby(Player player) {
        if (state != GameState.WAITING && state != GameState.STARTING) return false;
        lobbyPlayers.add(player.getUniqueId());
        Bukkit.broadcast(MessageUtil.prefix("§a" + player.getName() + " §7joined the lobby. §8[§f" + lobbyPlayers.size() + "§8]"));
        checkCountdown();
        return true;
    }

    public boolean removeFromLobby(Player player) {
        boolean removed = lobbyPlayers.remove(player.getUniqueId());
        if (removed) {
            Bukkit.broadcast(MessageUtil.prefix("§c" + player.getName() + " §7left the lobby."));
            checkCountdown();
        }
        return removed;
    }

    private void checkCountdown() {
        if (lobbyPlayers.size() >= MIN_PLAYERS && state == GameState.WAITING) {
            startCountdown();
        } else if (lobbyPlayers.size() < MIN_PLAYERS && state == GameState.STARTING) {
            cancelCountdown();
        }
    }

    private void startCountdown() {
        state = GameState.STARTING;
        countdownSeconds = 30;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSeconds <= 0) {
                startGame();
                return;
            }
            if (countdownSeconds <= 5 || countdownSeconds % 10 == 0) {
                Bukkit.broadcast(MessageUtil.prefix("§eGame starts in §f" + countdownSeconds + "§e seconds!"));
                for (UUID uid : lobbyPlayers) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
            }
            countdownSeconds--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        state = GameState.WAITING;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        Bukkit.broadcast(MessageUtil.prefix("§cNot enough players. Countdown cancelled."));
    }

    // ─── Game Start ──────────────────────────────────────────────────────────

    public void startGame() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (lobbyPlayers.size() < MIN_PLAYERS) {
            Bukkit.broadcast(MessageUtil.prefix("§cNot enough players to start!"));
            state = GameState.WAITING;
            return;
        }

        state = GameState.RUNNING;
        deadPlayers.clear();
        deathLocations.clear();
        gamePlayers.clear();

        // Gather online lobby players
        List<Player> online = lobbyPlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Assign roles
        Map<Player, GamePlayer> assigned = RoleAssigner.assignRoles(online);
        for (Map.Entry<Player, GamePlayer> entry : assigned.entrySet()) {
            gamePlayers.put(entry.getKey().getUniqueId(), entry.getValue());
        }

        // Setup each player
        for (Player player : online) {
            GamePlayer gp = gamePlayers.get(player.getUniqueId());
            setupPlayer(player, gp);
        }

        // Start sub-systems
        visionManager = new VisionManager(plugin, this::getAlivePlayers);
        visionManager.start();

        eventManager = new EventManager(plugin, this);
        eventManager.start();

        Bukkit.broadcast(MessageUtil.prefix("§a§lThe game has begun! Good luck..."));
    }

    private void setupPlayer(Player player, GamePlayer gp) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        // Remove all potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Give role items and notify
        switch (gp.getRole()) {
            case MURDERER -> {
                player.getInventory().setItem(0, ItemFactory.createMurdererKnife());
                player.sendMessage(MessageUtil.prefix("§c§lYou are the §4§lMURDERER§c§l! Eliminate everyone."));
                MessageUtil.sendTitle(player, "§c§lMURDERER", "§7Eliminate all players...", 10, 60, 10);
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.5f);
            }
            case SHERIFF -> {
                PlayerInventory inv = player.getInventory();
                inv.setItem(0, ItemFactory.createSheriffBow());
                inv.setItem(1, ItemFactory.createSheriffArrow());
                player.sendMessage(MessageUtil.prefix("§6§lYou are the §e§lSHERIFF§6§l! Protect the innocents."));
                player.sendMessage(MessageUtil.prefix("§7Shooting an innocent will kill YOU and drop your bow."));
                MessageUtil.sendTitle(player, "§e§lSHERIFF", "§7Protect the innocent...", 10, 60, 10);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            }
            case INVESTIGATOR -> {
                player.sendMessage(MessageUtil.prefix("§a§lYou are an §2§lINVESTIGATOR§a§l! Observe and report."));
                player.sendMessage(MessageUtil.prefix("§7Use chat to share clues. Survive!"));
                MessageUtil.sendTitle(player, "§a§lINVESTIGATOR", "§7Observe and survive...", 10, 60, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }

    // ─── Death Handling ──────────────────────────────────────────────────────

    /**
     * Called when a player is killed. Handles role logic and win checking.
     *
     * @param victim   the player who died
     * @param killer   the player who killed them (null if event kill)
     */
    public void handleDeath(Player victim, Player killer) {
        GamePlayer gp = gamePlayers.get(victim.getUniqueId());
        if (gp == null || !gp.isAlive()) return;

        gp.setAlive(false);
        deadPlayers.add(gp);
        deathLocations.put(victim.getUniqueId(), victim.getLocation().clone());

        victim.getInventory().clear();
        victim.setGameMode(GameMode.SPECTATOR);
        visionManager.stop(); // will re-apply without this player

        // Re-apply vision to remaining players only
        visionManager = new VisionManager(plugin, this::getAlivePlayers);
        visionManager.start();

        // Broadcast death
        String killerName = killer != null ? killer.getName() : "the darkness";
        Bukkit.broadcast(MessageUtil.prefix("§c☠ " + victim.getName() + " was killed by " + killerName + "!"));
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);

        // Sheriff self-kill logic (shot an innocent)
        if (killer != null) {
            GamePlayer killerGp = gamePlayers.get(killer.getUniqueId());
            if (killerGp != null && killerGp.getRole() == Role.SHERIFF && gp.getRole() != Role.MURDERER) {
                killer.sendMessage(MessageUtil.prefix("§c§lYou shot an innocent! You pay with your life."));
                // Drop the bow at the shooter's location
                killer.getWorld().dropItemNaturally(killer.getLocation(), ItemFactory.createSheriffBow());
                handleDeath(killer, null);
                return;
            }
        }

        checkWinConditions();
    }

    // ─── Win Conditions ──────────────────────────────────────────────────────

    private void checkWinConditions() {
        List<GamePlayer> alive = gamePlayers.values().stream()
                .filter(GamePlayer::isAlive).toList();

        long murderers    = alive.stream().filter(gp -> gp.getRole() == Role.MURDERER).count();
        long nonMurderers = alive.stream().filter(gp -> gp.getRole() != Role.MURDERER).count();

        if (murderers == 0) {
            endGame(false); // Murderer(s) dead → innocents win
        } else if (nonMurderers == 0) {
            endGame(true);  // Only murderers remain → murderer wins
        }
    }

    private void endGame(boolean murdererWon) {
        if (state != GameState.RUNNING) return;
        state = GameState.ENDED;

        stopSubSystems();

        if (murdererWon) {
            Bukkit.broadcast(MessageUtil.prefix("§c§lTHE MURDERER WINS! §r§cEvery innocent has been eliminated."));
            for (UUID uid : lobbyPlayers) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    MessageUtil.sendTitle(p, "§c§lMURDERER WINS", "§7All innocents eliminated", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
                }
            }
        } else {
            Bukkit.broadcast(MessageUtil.prefix("§a§lINNOCENTS WIN! §r§aThe murderer has been stopped."));
            for (UUID uid : lobbyPlayers) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    MessageUtil.sendTitle(p, "§a§lINNOCENTS WIN", "§7The murderer has been stopped", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }
        }

        // Reveal all roles after game ends
        StringBuilder roleReveal = new StringBuilder("§7--- §fRole Reveal §7---\n");
        for (GamePlayer gp : gamePlayers.values()) {
            String status = gp.isAlive() ? "§a(alive)" : "§c(dead)";
            roleReveal.append("§f").append(gp.getName())
                    .append(" §8» ").append(gp.getRole().getColor())
                    .append(gp.getRole().getDisplayName())
                    .append(" ").append(status).append("\n");
        }
        Bukkit.broadcast(MessageUtil.prefix(roleReveal.toString()));

        // Reset after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, 200L);
    }

    // ─── Reset ───────────────────────────────────────────────────────────────

    private void resetGame() {
        for (UUID uid : lobbyPlayers) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
        gamePlayers.clear();
        deadPlayers.clear();
        deathLocations.clear();
        lobbyPlayers.clear();
        state = GameState.WAITING;
        Bukkit.broadcast(MessageUtil.prefix("§7The game has been reset. Type §f/mm join §7to play again!"));
    }

    public void forceEndGame() {
        stopSubSystems();
        resetGame();
    }

    private void stopSubSystems() {
        if (eventManager != null) { eventManager.stop(); eventManager = null; }
        if (visionManager != null) { visionManager.stop(); visionManager = null; }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public List<Player> getAlivePlayers() {
        return gamePlayers.values().stream()
                .filter(GamePlayer::isAlive)
                .map(gp -> Bukkit.getPlayer(gp.getUuid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<GamePlayer> getDeadPlayers() { return Collections.unmodifiableList(deadPlayers); }
    public Location getDeathLocation(UUID uuid) { return deathLocations.get(uuid); }
    public GamePlayer getGamePlayer(UUID uuid) { return gamePlayers.get(uuid); }
    public boolean isGameRunning() { return state == GameState.RUNNING; }
    public boolean isInLobby(UUID uuid) { return lobbyPlayers.contains(uuid); }
    public GameState getState() { return state; }
    public int getLobbySize() { return lobbyPlayers.size(); }
    public int getMinPlayers() { return MIN_PLAYERS; }
}