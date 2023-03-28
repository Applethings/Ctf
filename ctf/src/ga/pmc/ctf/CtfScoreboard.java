package ga.pmc.ctf;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class CtfScoreboard {
    public Scoreboard score;

    public Objective objective;

    public static CtfScoreboard createScoreboard() {
        CtfScoreboard sc = new CtfScoreboard();
        sc.score = Bukkit.getScoreboardManager().getNewScoreboard();
        sc.objective = sc.score.registerNewObjective("scoreboard", "dummy");
        sc.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return sc;
    }

    public void setScoreboardDisplayName(String n) {
        objective.setDisplayName(n);
    }

    public void clear(int index) {
        ArrayList<String> clear = new ArrayList<>();
        for (String s : score.getEntries()) {
            Score score = objective.getScore(s);
            if (score.getScore() == index)
                clear.add(s);
        }
        clear.forEach(score::resetScores);
    }

    public void set(String x, int index) {
        ArrayList<String> clear = new ArrayList<>();
        for (String s : score.getEntries()) {
            Score score = objective.getScore(s);
            if (score.getScore() == index && !score.getEntry().equalsIgnoreCase(x))
                clear.add(s);
        }
        clear.forEach(score::resetScores);
        objective.getScore(x).setScore(index);
    }

    private int lastSize = 0;

    public void set(String... x) {
        int s = x.length;
        for(int i = x.length; i<lastSize; i++) {
            clear(i);
        }
        for (String X : x) set(X, s--);
        lastSize = x.length;
    }
}
