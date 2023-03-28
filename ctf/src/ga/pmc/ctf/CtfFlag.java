package ga.pmc.ctf;

public class CtfFlag {

    public transient double captureStatus = 0;
    public transient CtfTeam captureTeam;
    public transient CtfTeam prevCaptureTeam;

    public CtfLocation pos;
    public int team;
    public double captureRadius = 3;

    public CtfTeam getTeam() {
        for(CtfTeam t : Start.game.teams) {
            if(t.block == team) {
                return t;
            }
        }
        return null;
    }

}
