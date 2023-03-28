package ga.pmc.ctf;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CtfPlayer {
    public transient Player player;
    public transient CtfTeam team;
    public transient CtfTeam holdingFlag;
    public transient CtfScoreboard scoreboard;
    public transient int points;

    public Player getPlayer() {
        return player;
    }

    public void setCancelled(boolean b) {
    }
}
