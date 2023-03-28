package ga.pmc.ctf;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;

public class CtfTeam {

    public transient ArrayList<CtfPlayer> players = new ArrayList<>();
    public transient ArrayList<CtfTeam> capturedFlags = new ArrayList<>();
    public transient int teamPoints = 0;

    public CtfLocation spawn;
    public CtfColor color;
    public int block;
    public ChatColor chatColor;
    public String name;

    public String toString() {
        return "CtfTeam{" +
                "spawn=" + spawn +
                ", color=" + color +
                ", block=" + block +
                ", chatColor=" + chatColor +
                ", name='" + name + '\'' +
                '}';
    }
}
