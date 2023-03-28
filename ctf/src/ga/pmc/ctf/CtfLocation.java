package ga.pmc.ctf;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Locale;

public class CtfLocation {

    public int x,y,z;
    public float yaw,pitch;

    public String toString() {
        return "CtfLocation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }

    public Location location() {
        return new Location(Bukkit.getWorlds().get(0), x, y, z, yaw, pitch);
    }
}
