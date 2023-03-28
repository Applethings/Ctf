package ga.pmc.ctf;

import com.google.gson.Gson;

public class CtfSettings {
    // Map Settings
    public int minBuildHeight = 0;
    public int minAllowedY = 0;
    public int maxBuildHeight = 255;
    public int maxAllowedY = 1024;
    public boolean allowPlacingNearSpawn = false;
    public boolean allowPlacingNearFlags = false;
    public boolean allowPlacingOutsideMap = false;

    // Points settings
    public int killPoints = 1;
    public int stealPoints = 1;
    public int capturePoints = 1;
    public boolean enablePoints = true;
    public int winRequiredTeamPoints = 500;
    public float captureSpeed = 0.05f;

    // Flag settings
    public int requiredFlags = 5;
    public boolean infiniteFlags = false;
    public boolean enableFlags = true;
    public boolean useBeaconFlags = false;

    // Vote Start settings
    public int minPlayers = 2;
    public float voteStartPercentage = 75;
    public boolean broadcastVotes = true;

    // Block Reset settings
    public int resetBlocksPrice = 25;
    public int blockResetTime = 10;
    public boolean enableBlockResetting = true;

    // PvP settings
    public boolean disableDamage = true;
    public boolean disableFallDamage = true;
    public boolean allowFriendlyFire = false;

    // Misc settings
    public boolean allowJoiningMidGame = true;
    public boolean allowDroppingItems = false;
    public int defaultGamemode = 0;
    public String scoreboardName = "CAPTURE THE FLAG";
    public int respawnTime = 0;

    public CtfSettings cloneSettings() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), CtfSettings.class);
    }

}
