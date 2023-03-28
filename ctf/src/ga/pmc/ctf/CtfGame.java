package ga.pmc.ctf;

import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class CtfGame {

    public transient boolean running = false;
    public transient boolean starting = false;
    public transient long gameStartTime;
    public transient ArrayList<Location> placedBlocks = new ArrayList<>();
    public transient ArrayList<Player> startVotes = new ArrayList<>();

    public CtfLocation spawnLocation;
    @SerializedName("settings") public CtfSettings savedSettings;
    public transient CtfSettings settings;

    public CtfTeam[] teams;
    public CtfFlag[] flags;
    public CtfItem[] items = new CtfItem[0];
    public CtfNpc[] npcs = new CtfNpc[0];


    public CtfTeam getPlayerTeam(Player player) {
        for(CtfTeam t : teams) {
            for(CtfPlayer p : t.players) {
                if(p.player.getUniqueId().equals(player.getUniqueId())) {
                    return t;
                }
            }
        }
        return null;
    }
    public CtfPlayer getPlayer(Player player) {
        for(CtfTeam t : teams) {
            for(CtfPlayer p : t.players) {
                if(p.player.getUniqueId().equals(player.getUniqueId())) {
                    return p;
                }
            }
        }
        return null;
    }

    public int getVoteCountToStart() {
        var online = Math.ceil(Bukkit.getOnlinePlayers().size() * (settings.voteStartPercentage / 100f));
        return Math.max(Math.max(settings.minPlayers, (int) online), 1);
    }

    public void destroyPlacedBlocks() {
        placedBlocks.forEach(e -> e.getBlock().setType(Material.AIR));
        placedBlocks.clear();
    }

}
