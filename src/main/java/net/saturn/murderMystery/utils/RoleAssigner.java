package net.saturn.murderMystery.utils;

import net.saturn.murderMystery.roles.GamePlayer;
import net.saturn.murderMystery.roles.Role;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleAssigner {

    /**
     * Dynamically assigns roles based on player count:
     *
     *  Players  | Murderers | Sheriffs | Investigators
     *  ---------|-----------|----------|---------------
     *   4-6     |     1     |    1     |    rest
     *   7-10    |     1     |    1     |    rest
     *  11-15    |     2     |    2     |    rest
     *  16+      |     2     |    2     |    rest  (can scale further)
     */
    public static Map<Player, GamePlayer> assignRoles(List<Player> players) {
        int count = players.size();

        int murderers = count >= 11 ? 2 : 1;
        int sheriffs  = count >= 11 ? 2 : 1;

        // Make sure we don't exceed player count
        murderers = Math.min(murderers, count / 4);
        sheriffs  = Math.min(sheriffs,  count / 4);

        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < murderers; i++) roles.add(Role.MURDERER);
        for (int i = 0; i < sheriffs;  i++) roles.add(Role.SHERIFF);
        while (roles.size() < count)         roles.add(Role.INVESTIGATOR);

        Collections.shuffle(roles);

        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        Map<Player, GamePlayer> result = new HashMap<>();
        for (int i = 0; i < shuffledPlayers.size(); i++) {
            Player p = shuffledPlayers.get(i);
            result.put(p, new GamePlayer(p, roles.get(i)));
        }
        return result;
    }
}