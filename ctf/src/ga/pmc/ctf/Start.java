package ga.pmc.ctf;

import com.google.gson.GsonBuilder;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Start extends JavaPlugin implements Listener {

    public static CtfGame game;
    public static Start instance;

    public static final char[] characters = "⬜▒⬛".toCharArray();

    public void onEnable() {
        instance = this;
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        File file = new File(new File("plugins"), "ctfconfig.json");
        try {
            String data = new String(Files.readAllBytes(file.toPath()));
            if(data.isEmpty()) data = "{}";
            game = new GsonBuilder().create().fromJson(data, CtfGame.class);
            game.settings = game.savedSettings.cloneSettings();
        } catch (IOException e) {
            e.printStackTrace();
            game = new CtfGame();
        }
        getCommand("ctf").setExecutor(new CtfCommand());
        getServer().getPluginManager().registerEvents(this, this);
        for(Player p : getServer().getOnlinePlayers()) {
            warpPlayer(p);
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if(game.running && game.settings.useBeaconFlags) {
                for(CtfFlag f : game.flags) {
                    for(CtfTeam team : game.teams) {
                        for (CtfPlayer player : team.players) {
                            if (f.pos.location().distanceSquared(player.player.getLocation()) >= f.captureRadius * f.captureRadius) {
                                continue;
                            }
                            if (f.captureTeam != player.team) {
                                if (f.captureTeam != null) f.captureStatus *= -1;
                                f.prevCaptureTeam = f.captureTeam;
                                f.captureTeam = player.team;
                            }
                            f.captureStatus += game.settings.captureSpeed;
                        }
                    }

                    if (f.captureStatus > 1) f.captureStatus = 1;
                    var bloc = f.pos.location();
                    Random r = new Random();
                    for (int x = -5; x <= 5; x++) {
                        for (int y = -5; y <= 5; y++) {
                            for (int z = -5; z <= 5; z++) {
                                var loc = new Location(bloc.getWorld(), bloc.getX() + x, bloc.getY() + y, bloc.getZ() + z);
                                if (loc.getBlock().getType() == Material.WOOL) {
                                    r.setSeed((long) (loc.getX() * loc.getY() * loc.getZ() * 912 - loc.getZ() * 91023 + loc.getX() * 7812) * 7);

                                    if (r.nextDouble() < f.captureStatus) {
                                        loc.getBlock().setData((byte) f.captureTeam.block);
                                    } else {
                                        if (r.nextDouble() > -f.captureStatus) {
                                            loc.getBlock().setData((byte) 0);
                                        } else {
                                            loc.getBlock().setData((byte) f.prevCaptureTeam.block);
                                        }
                                    }
                                }else if(loc.getBlock().getType() == Material.STAINED_GLASS) {
                                    if (0.99 < f.captureStatus) {
                                        loc.getBlock().setData((byte) f.captureTeam.block);
                                    } else if (-0.99 < f.captureStatus) {
                                        loc.getBlock().setData((byte) 0);
                                    } else {
                                        loc.getBlock().setData((byte) f.prevCaptureTeam.block);
                                    }
                                }
                            }
                        }
                    }
                    if(f.captureStatus >= 1) {
                        f.captureTeam.teamPoints++;
                    }
                }
                updateScoreboard();
                for(CtfTeam t : game.teams) {
                    if(t.teamPoints/10 >= game.settings.winRequiredTeamPoints) {
                        Bukkit.broadcastMessage("§aTeam " + t.chatColor + t.name + " §awon!");
                        game.startVotes.clear();
                        Start.preEnd();
                        Start.game.running = false;
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException g) {
                            g.printStackTrace();
                        }
                        for (Location l : Start.game.placedBlocks) {
                            l.getBlock().setType(Material.AIR);
                        }
                        Start.game.placedBlocks = new ArrayList<>();
                        for (CtfTeam x : Start.game.teams) {
                            x.players.clear();
                            x.capturedFlags.clear();
                        }
                        for (Player p : Bukkit.getOnlinePlayers()) Start.warpPlayer(p);
                        try {
                            Start.end();
                        } catch (IOException g) {
                            g.printStackTrace();
                        }
                    }
                }
            }
        }, 1, 1);
    }
    @EventHandler
    public void onWeatherChange(final WeatherChangeEvent e) {
        if (e.toWeatherState()) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onEntityLeftClick(EntityDamageByEntityEvent ee) {
        if(game.running) {
            if(ee.getEntityType()== EntityType.VILLAGER) {
                if(ee.getDamager() instanceof Player) {
                    UUID uuid = ee.getDamager().getUniqueId();
                    CtfPlayer player = null;
                    for(CtfTeam t : Start.game.teams) {
                        for(CtfPlayer r : t.players) {
                            if(r.player.getUniqueId().equals(uuid)) {
                                player = r;
                                break;
                            }
                        }
                    }
                    CtfNpc npc = null;
                    for(CtfNpc n : game.npcs) {
                        if(n.entity.getUniqueId().equals(ee.getEntity().getUniqueId())) {
                            npc = n;
                            break;
                        }
                    }
                    if(npc==null) {
                        return;
                    }
                    if(player!=null) {
                        if(player.team!=null) npc.click(player);
                        ee.setCancelled(true);
                    }else {
                        ee.setCancelled(true);
                    }
                }else {
                    ee.setCancelled(true);
                }
            }
        }
    }

    public static void updateVotes() {
        boolean oldState = game.starting;
        game.starting = game.getVoteCountToStart() <= game.startVotes.size();
        if(!oldState && game.starting) {
            updateCountdown(5 * 20);
        }
    }

    public static void startGame() {
        for(CtfTeam t : game.teams) {
            t.players.clear();
            t.capturedFlags.clear();
        }

        ArrayList<? extends Player> playersShuffle = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(playersShuffle);
        for(int i = 0; i<playersShuffle.size(); i++) {
            CtfTeam m = game.teams[i % game.teams.length];
            CtfPlayer p = new CtfPlayer();
            p.player = playersShuffle.get(i);
            p.team = m;
            m.players.add(p);
        }

        for(Location l : game.placedBlocks) l.getBlock().setType(Material.AIR);
        game.placedBlocks = new ArrayList<>();
        game.startVotes.clear();
        game.running = true;
        game.gameStartTime = System.currentTimeMillis();
        for(Player p : Bukkit.getOnlinePlayers()) Start.warpPlayer(p, true);
        Arrays.asList(game.npcs).forEach(CtfNpc::spawn);
    }

    public static void updateCountdown(int left) {
        if(left == 0) {
            Bukkit.broadcastMessage("§aGame started");
            startGame();
            return;
        }
        if(left % 20 == 0) {
            Bukkit.broadcastMessage("§aStarting in " + (left / 20));
        }
        Bukkit.getScheduler().runTaskLater(instance, () -> {
            if(game.starting && !game.running) {
                updateCountdown(left - 1);
            }else {
                Bukkit.broadcastMessage("§cCountdown stopped");
            }
        }, 1L);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if(!game.running) {
            var i = e.getItem();
            if(i == null) return;
            var m = i.getItemMeta();
            if(Objects.equals(m.getDisplayName(), "Return to Lobby")) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);
                try {
                    out.writeUTF("Connect");
                    out.writeUTF("lobby");
                } catch (IOException ignored) {}
                e.getPlayer().sendPluginMessage(instance, "BungeeCord", b.toByteArray());
            }else if(Objects.equals(m.getDisplayName(), "Start")) {
                int req = game.getVoteCountToStart();
                if(game.startVotes.contains(e.getPlayer())) {
                    game.startVotes.remove(e.getPlayer());
                    if(game.settings.broadcastVotes) {
                        Bukkit.broadcastMessage("§a" + e.getPlayer().getName() + " un-voted to start the game. (" + game.startVotes.size() + "/" + req + ")");
                    }else {
                        e.getPlayer().sendMessage("§a" + e.getPlayer().getName() + " un-voted to start the game. (" + game.startVotes.size() + "/" + req + ")");
                    }
                }else {
                    game.startVotes.add(e.getPlayer());
                    if(game.settings.broadcastVotes) {
                        Bukkit.broadcastMessage("§a" + e.getPlayer().getName() + " voted to start the game. (" + game.startVotes.size() + "/" + req + ")");
                    }else {
                        e.getPlayer().sendMessage("§a" + e.getPlayer().getName() + " voted to start the game. (" + game.startVotes.size() + "/" + req + ")");
                    }
                }
                updateVotes();
            }
        }
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent e) {
        if(game.running) {
            for(CtfNpc npc : game.npcs) {
                if(e.getRightClicked().getUniqueId().equals(npc.entity.getUniqueId())) {
                    CtfPlayer player = game.getPlayer(e.getPlayer());
                    if(player!=null) npc.click(player);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    public static void preEnd() {
        if(game.running) {
            long time = (System.currentTimeMillis() - game.gameStartTime) / 1000;
            Bukkit.broadcastMessage(ChatColor.AQUA + "Game took " + ChatColor.GOLD + time + "s");
            if(game.settings.enableFlags && !game.settings.useBeaconFlags) {
                for (var team : game.teams) {
                    Bukkit.broadcastMessage(team.chatColor + team.name + " " + ChatColor.AQUA + "captured " + ChatColor.GOLD + team.capturedFlags.size() + " flags");
                }
            }
        }
        game.destroyPlacedBlocks();
    }

    public static void end() throws IOException {
        if(game.running) Arrays.asList(game.npcs).forEach(CtfNpc::despawn);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF("lobby");
        } catch (IOException ignored) {}
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.sendPluginMessage(instance, "BungeeCord", b.toByteArray());
        }
        Bukkit.getScheduler().runTaskLater(instance, Bukkit::shutdown, 5);
    }

    public static ItemStack renameItem(ItemStack item, String name) {
        var meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static void warpPlayer(Player p) {
        warpPlayer(p, false);
    }

    public static void warpPlayer(Player p, boolean force) {
        p.setNoDamageTicks(30);
        p.setFallDistance(0);
        p.setLevel(0);
        p.setExp(0);
        p.setFoodLevel(20);
        p.setHealth(20d);
        p.setSaturation(20f);
        p.getActivePotionEffects().forEach((a) -> p.removePotionEffect(a.getType()));
        givePlayerInventory(p);
        p.spigot().setCollidesWithEntities(true);
        if(!Start.game.running) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            p.setPlayerListName("§r" + p.getName());
            p.setDisplayName("§r" + p.getName());
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.getInventory().setItem(0, renameItem(new ItemStack(Material.NETHER_STAR), "Start"));
            p.getInventory().setItem(8, renameItem(new ItemStack(Material.REDSTONE), "Return to Lobby"));
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(new Location(p.getLocation().getWorld(), game.spawnLocation.x+0.5d, game.spawnLocation.y, game.spawnLocation.z+0.5d, game.spawnLocation.yaw, game.spawnLocation.pitch));
        }else {
            CtfPlayer player = game.getPlayer(p);
            if(player == null) {
                p.setGameMode(GameMode.SPECTATOR);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                return;
            }
            if(player.scoreboard==null) {
                player.scoreboard = CtfScoreboard.createScoreboard();
                player.scoreboard.setScoreboardDisplayName("§e§l" + game.settings.scoreboardName);
                p.setScoreboard(player.scoreboard.score);
                updateScoreboard(player);
            }
            if(p.getScoreboard()!=player.scoreboard.score) {
                p.setScoreboard(player.scoreboard.score);
            }

            if(player.holdingFlag !=null) {
                Bukkit.broadcastMessage(addColoredNames(p.getName() + " §clost " + player.holdingFlag.chatColor + player.holdingFlag.name + "'s Flag"));
                player.holdingFlag = null;
                updateScoreboard();
            }

            CtfTeam team = player.team;
            p.setPlayerListName("§r" + (team==null?"":team.chatColor) + p.getName() + "§r");
            p.setDisplayName("§r" + (team==null?"":team.chatColor) + p.getName() + "§r");

            if(game.settings.respawnTime == 0 || force) {
                p.setGameMode(team==null?GameMode.SPECTATOR:GameMode.getByValue(game.settings.defaultGamemode));
            }else {
                p.setGameMode(GameMode.SPECTATOR);
                respawnTimer(player, game.settings.respawnTime);
            }
            if(team!=null) p.teleport(new Location(p.getLocation().getWorld(), team.spawn.x+0.5d, team.spawn.y, team.spawn.z+0.5d, team.spawn.yaw, team.spawn.pitch));
            else p.teleport(new Location(p.getLocation().getWorld(), game.spawnLocation.x+0.5d, game.spawnLocation.y, game.spawnLocation.z+0.5d, game.spawnLocation.yaw, game.spawnLocation.pitch));
        }
    }

    public static void respawnTimer(CtfPlayer player, int remaining) {
        Bukkit.getScheduler().runTaskLater(instance, () -> {
            if(remaining == 0) {
                var p = player.player;
                var team = player.team;
                if(team != null) p.teleport(new Location(p.getLocation().getWorld(), team.spawn.x+0.5d, team.spawn.y, team.spawn.z+0.5d, team.spawn.yaw, team.spawn.pitch));
                else p.teleport(new Location(p.getLocation().getWorld(), game.spawnLocation.x+0.5d, game.spawnLocation.y, game.spawnLocation.z+0.5d, game.spawnLocation.yaw, game.spawnLocation.pitch));
                p.setGameMode(team==null?GameMode.SPECTATOR:GameMode.getByValue(game.settings.defaultGamemode));
                return;
            }
            if(remaining == 20) {
                var p = player.player;
                p.sendMessage(ChatColor.GOLD + "Respawning in " + ChatColor.AQUA + "1 second");
            }else if(remaining % 20 == 0) {
                var p = player.player;
                p.sendMessage(ChatColor.GOLD + "Respawning in " + ChatColor.AQUA + (remaining / 20) + " seconds");
            }
            respawnTimer(player, remaining - 1);
        }, 1);
    }

    public static void updateScoreboard() {
        if(game.running) {
            Arrays.asList(game.teams).forEach((e) -> e.players.forEach(Start::updateScoreboard));
        }
    }

    public static void updateScoreboard(CtfPlayer player) {
        CtfScoreboard scoreboard = player.scoreboard;
        ArrayList<String> lines = new ArrayList<>();
        lines.add(" ");
        if(game.settings.enablePoints) {
            lines.add("Points: §6" + player.points);
            lines.add("  ");
        }
        for(CtfTeam t : game.teams) {
            StringBuilder flags = new StringBuilder();
            if(game.settings.enableFlags) {
                flags.append("§8: ");
                if(!game.settings.useBeaconFlags) {
                    int flagsamount = 0;
                    if (!game.settings.infiniteFlags) {
                        for (CtfTeam captured : t.capturedFlags) {
                            flagsamount++;
                            flags.append(captured.chatColor.toString()).append(characters[2]);
                        }
                    }
                    for (CtfPlayer p : t.players) {
                        if (p.holdingFlag != null) {
                            flagsamount++;
                            flags.append(p.holdingFlag.chatColor.toString()).append(characters[1]);
                        }
                    }
                    if (!game.settings.infiniteFlags) {
                        for (int i = 0; i < game.settings.requiredFlags - flagsamount; i++) {
                            flags.append("§8").append(characters[0]);
                        }
                    } else {
                        if (flagsamount == 0) flags.append("§8").append(characters[0]);
                    }
                }else {
                    flags.append(t.teamPoints/10).append("/").append(game.settings.winRequiredTeamPoints);
                }
            }
            lines.add(t.chatColor + t.name + flags + (t == player.team ? " §7(You)" : ""));
        }
        lines.add("   ");
        scoreboard.set(lines.toArray(new String[0]));
    }

    public static void givePlayerInventory(Player player) {
        if(game.running) {
            CtfTeam team = game.getPlayerTeam(player);
            PlayerInventory i = player.getInventory();
            ItemStack[] stacks = new ItemStack[36];
            for (int x = 0; x < 36; x++) {
                CtfItem item = Start.game.items[x];
                if (item != null) {
                    ItemStack a = item.toItem();
                    if (a.getDurability() == 32767) a.setDurability((short) (team==null?0:team.block));
                    stacks[x] = a;
                }
            }
            i.setContents(stacks);

            stacks = new ItemStack[4];
            for (int x = 0; x < 4; x++) {
                CtfItem item = Start.game.items[x + 36];
                if (item != null) {
                    ItemStack a = item.toItem();
                    ItemMeta m = a.getItemMeta();
                    if(team != null && m instanceof LeatherArmorMeta b) {
                        b.setColor(team.color.toColor());
                        a.setItemMeta(m);
                    }
                    stacks[x] = a;
                }
            }
            i.setArmorContents(stacks);
        }
    }

    public HashMap<Player, AttackData> data = new HashMap<>();

    @EventHandler
    public void onPlayerInventory(InventoryClickEvent e) {
        if(e.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        if(e.getSlotType()==InventoryType.SlotType.ARMOR) e.setCancelled(true);
        if(!game.running) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        e.setCancelled(!game.settings.allowDroppingItems && e.getPlayer().getGameMode() != GameMode.CREATIVE);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if(e.getEntity() instanceof Player) {
            if(e.getEntity().getKiller() != null) {
                CtfPlayer player = game.getPlayer(e.getEntity().getKiller());
                if(player != null) {
                    addPoints(player, game.settings.killPoints);
                    player.player.playSound(player.player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
                }
            }
        }
    }

    public static void addPoints(CtfPlayer player, int points) {
        if(game.settings.enablePoints && points != 0) {
            player.player.sendMessage("§6+" + points + " points");
            player.player.playSound(player.player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 2f);
            player.points += points;
            updateScoreboard(player);
        }
    }

    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent e) {
        if(e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if(e.getTo().getY()<=game.settings.minAllowedY) {
            warpPlayer(e.getPlayer());
            AttackData data = this.data.get(e.getPlayer());
            if(data !=null && data.time+5000>System.currentTimeMillis()) {
                Entity who = data.entity;
                if(data.entity instanceof Projectile) {
                    who = (Entity) ((Projectile) data.entity).getShooter();
                }
                Bukkit.broadcastMessage(addColoredNames(e.getPlayer().getName() + " was knocked out of the world by " + who.getName()));
                if(who instanceof Player pl) {
                    CtfPlayer player = game.getPlayer(pl);
                    if(player != null) {
                        addPoints(player, game.settings.killPoints);
                        player.player.playSound(player.player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
                    }
                }
            }else {
                Bukkit.broadcastMessage(addColoredNames(e.getPlayer().getName() + " fell out of the world"));
            }
        }else {
            if(game.running) {
                if(game.settings.maxAllowedY <= e.getPlayer().getLocation().getY()) {
                    Bukkit.broadcastMessage(addColoredNames(e.getPlayer().getName() + " was too high"));
                    warpPlayer(e.getPlayer());
                    return;
                }
                CtfPlayer pp = game.getPlayer(e.getPlayer());
                if(pp == null) return;
                if(game.settings.enableFlags) {
                    if(!game.settings.useBeaconFlags) {
                        if (pp.holdingFlag == null) {
                            for(CtfFlag f : game.flags) {
                                CtfTeam a = f.getTeam();
                                if (f.pos.location().distanceSquared(e.getTo()) < f.captureRadius * f.captureStatus) {
                                    if (a != pp.team) {
                                        pp.holdingFlag = a;
                                        pp.player.getInventory().setHelmet(new ItemStack(Material.BANNER, 1, (short) (15 - a.block)));
                                        pp.player.setPlayerListName(pp.player.getPlayerListName() + " " + a.chatColor + characters[1]);
                                        Bukkit.broadcastMessage(addColoredNames(e.getPlayer().getName() + " §atook " + a.chatColor + a.name + "'s Flag"));
                                        addPoints(pp, game.settings.stealPoints);
                                        updateScoreboard();
                                        return;
                                    }
                                }
                            }
                        } else {
                            for(CtfFlag f : game.flags) {
                                if(f.getTeam() != pp.team) continue;
                                if (f.pos.location().distanceSquared(e.getTo()) < f.captureRadius * f.captureStatus) {
                                    pp.player.getInventory().setHelmet(null);
                                    pp.player.setPlayerListName("§r" + pp.team.chatColor + pp.player.getName() + "§r");
                                    Bukkit.broadcastMessage(addColoredNames(e.getPlayer().getName() + " §acaptured " + pp.holdingFlag.chatColor + pp.holdingFlag.name + "'s Flag"));
                                    pp.team.capturedFlags.add(pp.holdingFlag);
                                    addPoints(pp, game.settings.capturePoints);
                                    pp.holdingFlag = null;
                                    updateScoreboard();
                                    for (CtfTeam a : game.teams) {
                                        int c = a.capturedFlags.size();
                                        if (c >= game.settings.requiredFlags && !game.settings.infiniteFlags) {
                                            Bukkit.broadcastMessage("§aTeam " + a.chatColor + a.name + " §awon!");
                                            game.startVotes.clear();
                                            Start.preEnd();
                                            Start.game.running = false;
                                            try {
                                                TimeUnit.SECONDS.sleep(3);
                                            } catch (InterruptedException g) {
                                                g.printStackTrace();
                                            }
                                            for (Location l : Start.game.placedBlocks) {
                                                l.getBlock().setType(Material.AIR);
                                            }
                                            Start.game.placedBlocks = new ArrayList<>();
                                            for (CtfTeam t : Start.game.teams) {
                                                t.players.clear();
                                                t.capturedFlags.clear();
                                            }
                                            for (Player p : Bukkit.getOnlinePlayers()) Start.warpPlayer(p);
                                            try {
                                                Start.end();
                                            } catch (IOException g) {
                                                g.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        game.startVotes.remove(e.getPlayer());
        updateVotes();

        if(game.running) {
            int online = 0;
            for(CtfTeam t : game.teams) {
                t.players.removeIf((f) -> {
                    if(f.player.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                        if(f.holdingFlag !=null) {
                            Bukkit.broadcastMessage(addColoredNames(f.player.getName() + " §clost " + f.holdingFlag.chatColor + f.holdingFlag.name + "'s Flag"));
                            f.holdingFlag = null;
                        }
                        return true;
                    }

                    return false;
                });
                online += t.players.size();
            }
            if(online == 0) {
                try {
                    Start.preEnd();
                    Arrays.asList(game.npcs).forEach(CtfNpc::despawn);
                    end();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if(game.running && game.settings.allowJoiningMidGame) {
            boolean hasTeam = game.getPlayerTeam(e.getPlayer()) != null;
            if(!hasTeam) {
                ArrayList<CtfTeam> teams = new ArrayList<>(Arrays.asList(game.teams));
                teams.sort((c1, c2) -> Integer.compare(c2.players.size(), c1.players.size()));
                Collections.reverse(teams);
                int smallest = teams.get(0).players.size();
                ArrayList<CtfTeam> teams2 = new ArrayList<>();
                for(CtfTeam t : teams) if(t.players.size()==smallest) teams2.add(t);
                Collections.shuffle(teams2);
                CtfPlayer p = new CtfPlayer();
                p.player = e.getPlayer();
                p.team = teams2.get(0);
                p.team.players.add(p);
            }
        }
        warpPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        warpPlayer(e.getEntity());
        e.setDeathMessage(addColoredNames(e.getDeathMessage()));
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if(!game.running) {
            if (e.getEntity() instanceof Player) {
                e.setCancelled(true);
            }
        }
        if(game.settings.disableFallDamage && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
        }
        if(game.settings.disableDamage) {
            e.setDamage(0);
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent e) {
        if(e.getEntity() instanceof Player dst) {
            if(!game.settings.allowFriendlyFire) {
                var src = e.getDamager();
                Player playerSrc = null; // some magic code to check who damaged the player
                if (src instanceof Player p) {
                    playerSrc = p;
                } else if (src instanceof Projectile p) {
                    if (p.getShooter() instanceof Player pl) {
                        playerSrc = pl;
                    }
                }
                if (playerSrc != null) {
                    var teamA = game.getPlayerTeam(playerSrc);
                    var teamB = game.getPlayerTeam(dst);
                    if (teamA != null && teamB != null) { // ensure both players are already in a team
                        if (teamA == teamB) { // check if the teams are the same
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            AttackData d = new AttackData();
            d.time = System.currentTimeMillis();
            d.entity = e.getDamager();
            data.entrySet().removeIf((f) -> d.entity.equals(f.getKey()));
            data.put((Player) e.getEntity(), d);
        }
    }

    public static String addColoredNames(String text) {
        for(CtfTeam team : Start.game.teams) {
            for(CtfPlayer player : team.players) {
                if(text.contains(player.player.getName())) {
                    text = text.replace(player.player.getName(), player.player.getDisplayName());
                }
            }
        }
        return text;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        e.setMessage(addColoredNames(e.getMessage()));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if(Start.game.running) {
            if(!game.settings.allowPlacingOutsideMap && (e.getBlock().getLocation().getBlockY()>=game.settings.maxBuildHeight || e.getBlock().getLocation().getBlockY()<=game.settings.minBuildHeight)) {
                e.getPlayer().sendMessage("§cYou cannot place blocks here!");
                e.setCancelled(true);
                return;
            }
            for(CtfFlag f : game.flags) {
                if(game.settings.enableFlags) {
                    if (!game.settings.allowPlacingNearFlags && f.pos.location().distanceSquared(e.getBlock().getLocation()) < f.captureRadius * f.captureRadius) {
                        e.getPlayer().sendMessage("§cYou cannot place blocks near flags!");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            for(CtfTeam a : game.teams) {
                if(!game.settings.allowPlacingNearSpawn && a.spawn.location().distanceSquared(e.getBlock().getLocation())<9) {
                    e.getPlayer().sendMessage("§cYou cannot place blocks near spawn!");
                    e.setCancelled(true);
                    return;
                }
            }
            game.placedBlocks.add(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(Start.game.running) {
            for (Location l : game.placedBlocks) {
                if (l.equals(e.getBlock().getLocation())) {
                    return;
                }
            }
            e.getPlayer().sendMessage("§cYou can only break blocks placed by players!");
            e.setCancelled(true);
        }
    }

    public void onDisable() {
        Arrays.asList(game.npcs).forEach(CtfNpc::despawn);
        File file = new File(new File("plugins"), "ctfconfig.json");
        try {
            Files.writeString(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(game), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Start.preEnd();
            end();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
