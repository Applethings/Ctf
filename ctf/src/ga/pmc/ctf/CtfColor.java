package ga.pmc.ctf;

import org.bukkit.Color;

public class CtfColor {

    public int red;
    public int green;
    public int blue;

    public String toString() {
        return "CtfColor{" +
                "red=" + red +
                ", green=" + green +
                ", blue=" + blue +
                '}';
    }

    public Color toColor() {
        return Color.fromRGB(red, green, blue);
    }
}
