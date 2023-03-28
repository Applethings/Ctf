package ga.pmc.ctf;

import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;

import static ga.pmc.ctf.Start.game;
import static ga.pmc.ctf.Start.updateScoreboard;

public class CtfCommand implements CommandExecutor {

    public static final ArrayList<CtfSubCommand> SUB_COMMANDS = new ArrayList<>();

    static {
        SUB_COMMANDS.add(new CtfSubCommand("start", true, (sender, ignored1, ignored2, arguments) -> {
            if(game.running) {
                sender.sendMessage("§cGame already running");
                return false;
            }

            Start.startGame();
            return false;
        }));
        SUB_COMMANDS.add(new CtfSubCommand("points", true, (sender, ignored1, ignored2, arguments) -> {
            if(!game.settings.enablePoints) {
                sender.sendMessage("§cPoints are disabled in this game");
                return false;
            }
            if(arguments.length < 3) {
                sender.sendMessage("§7Usage: §c/ctf points <player> <points>");
                return false;
            }
            Player player = Bukkit.getPlayerExact(arguments[1]);
            if(player == null) {
                sender.sendMessage("§7<player>: §cUnknown player");
                return false;
            }
            int points;
            try {
                points = Integer.parseInt(arguments[2]);
            }catch(NumberFormatException e) {
                sender.sendMessage("§7<points>: §cNot a Number");
                return false;
            }
            if(!game.running) {
                sender.sendMessage("§cGame is not running");
                return false;
            }
            CtfPlayer ctfPlayer = game.getPlayer(player);
            if(ctfPlayer == null) {
                sender.sendMessage("§7<player>: §cPlayer not found in game");
                return false;
            }
            ctfPlayer.points += points;
            sender.sendMessage("§aPoints for " + player.getDisplayName() + " §asuccessfully updated.");
            updateScoreboard(ctfPlayer);
            return false;
        }));
        SUB_COMMANDS.add(new CtfSubCommand("stop", true, (sender, ignored1, ignored2, arguments) -> {
            Start.preEnd();

            game.startVotes.clear();
            game.running = false;
            Arrays.asList(game.npcs).forEach(CtfNpc::despawn);
            for(CtfTeam t : game.teams) {
                t.players.clear();
                t.capturedFlags.clear();
            }
            for(Player p : Bukkit.getOnlinePlayers()) Start.warpPlayer(p);
            try {
                Start.end();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }));
        SUB_COMMANDS.add(new CtfSubCommand("settings", true, (sender, ignored1, ignored2, arguments) -> {
            String requestedSetting = arguments.length > 1 ? arguments[1] : null;
            Field[] fieldSettings = CtfSettings.class.getDeclaredFields();
            if("print".equals(requestedSetting)) {
                sender.sendMessage("§7----- §cSetting List§7 -----");
                for(Field setting : fieldSettings) {
                    try {
                        sender.sendMessage("§c" + setting.getName() + "§7: §a" + setting.get(game.settings));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                return false;
            }

            Field setting = null;
            for(Field s : fieldSettings) {
                if(s.getName().equalsIgnoreCase(requestedSetting)) {
                    setting = s;
                    break;
                }
            }
            if(setting == null) {
                StringBuilder available = new StringBuilder();
                for (Field t : fieldSettings) {
                    available.append("§c").append(t.getName()).append("§7, ");
                }
                if (available.length() != 0)
                    available = new StringBuilder(available.substring(0, available.length() - 2));
                sender.sendMessage("§7Available settings: " + available);
                return false;
            }

            String valueStr = arguments.length > 2 ? arguments[2] : null;
            if(valueStr == null) {
                try {
                    sender.sendMessage("§7Current §c" + setting.getName() + "§7 value: §a" + setting.get(game.settings));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }

            Class<?> valueType = setting.getType();
            if(valueType == Integer.TYPE) {
                try {
                    int val = Integer.parseInt(valueStr);
                    setting.setInt(game.settings, val);
                    sender.sendMessage("§7Changed setting §c" + setting.getName() + "§7to value: §a" + val);
                }catch(NumberFormatException e) {
                    sender.sendMessage("§7Invalid value for setting §c" + setting.getName());
                }catch (IllegalAccessException e) {
                    sender.sendMessage("§7Failed to set value for setting §c" + setting.getName());
                }
            }else if(valueType == Boolean.TYPE) {
                try {
                    boolean val = Boolean.parseBoolean(valueStr);
                    setting.setBoolean(game.settings, val);
                    sender.sendMessage("§7Changed setting §c" + setting.getName() + "§7 to value: §a" + val);
                }catch(NumberFormatException e) {
                    sender.sendMessage("§7Invalid value for setting §c" + setting.getName());
                }catch (IllegalAccessException e) {
                    sender.sendMessage("§7Failed to set value for setting §c" + setting.getName());
                }
            }else if(valueType == Float.TYPE) {
                try {
                    float val = Float.parseFloat(valueStr);
                    setting.setFloat(game.settings, val);
                    sender.sendMessage("§7Changed setting §c" + setting.getName() + "§7 to value: §a" + val);
                }catch(NumberFormatException e) {
                    sender.sendMessage("§7Invalid value for setting §c" + setting.getName());
                }catch (IllegalAccessException e) {
                    sender.sendMessage("§7Failed to set value for setting §c" + setting.getName());
                }
            }else if(valueType == String.class) {
                try {
                    String[] vals = new String[arguments.length - 2];
                    System.arraycopy(arguments, 2, vals, 0, vals.length);
                    String val = String.join(" ", vals);
                    setting.set(game.settings, val);
                    sender.sendMessage("§7Changed setting §c" + setting.getName() + "§7 to value: §a" + val);
                }catch (IllegalAccessException e) {
                    sender.sendMessage("§7Failed to set value for setting §c" + setting.getName());
                }
            }else {
                sender.sendMessage("§7Cannot set value for setting §c" + setting.getName());
            }

            Start.updateScoreboard();

            return false;
        }));
        SUB_COMMANDS.add(new CtfSubCommand("reload", true, (sender, ignored1, ignored2, arguments) -> {
            if(game.running) {
                long time = (System.currentTimeMillis()-game.gameStartTime)/1000;
                Bukkit.broadcastMessage(ChatColor.GREEN + "Game took " + time + "s");
            }
            game.destroyPlacedBlocks();
            game.startVotes.clear();
            game.running = false;
            Arrays.asList(game.npcs).forEach(CtfNpc::despawn);
            for(CtfTeam t : game.teams) {
                t.players.clear();
                t.capturedFlags.clear();
            }
            for(Player p : Bukkit.getOnlinePlayers()) Start.warpPlayer(p);
            File file = new File(new File("plugins"), "ctfconfig.json");
            try {
                String data = new String(Files.readAllBytes(file.toPath()));
                game = new GsonBuilder().create().fromJson(data, CtfGame.class);
                game.settings = game.savedSettings.cloneSettings();
                sender.sendMessage("§aReloaded successfully");
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage("§cFailed to reload the config, keeping old");
            }
            return false;
        }));
        SUB_COMMANDS.add(new CtfSubCommand("setinv", true, (sender, ignored1, ignored2, arguments) -> {
            CtfItem[] items = new CtfItem[40];
            PlayerInventory i = ((Player) sender).getInventory();
            for(int x = 0; x<36; x++) {
                CtfItem item = new CtfItem();
                ItemStack a = i.getContents()[x];
                if(a!=null) {
                    item.item = a.getTypeId();
                    item.damage = a.getDurability();
                    item.amount = a.getAmount();
                    CtfEnchantment[] enchts = new CtfEnchantment[a.getEnchantments().size()];
                    int b = 0;
                    for(Map.Entry<Enchantment, Integer> ench : a.getEnchantments().entrySet()) {
                        enchts[b] = new CtfEnchantment();
                        enchts[b].enchantment = ench.getKey().getId();
                        enchts[b].lvl = ench.getValue();
                    }
                    item.enchantments = enchts;
                }
                items[x] = item;
            }
            for(int x = 0; x<4; x++) {
                CtfItem item = new CtfItem();
                ItemStack a = i.getArmorContents()[x];
                if(a!=null) {
                    item.item = a.getTypeId();
                    item.damage = a.getDurability();
                    item.amount = a.getAmount();
                    CtfEnchantment[] enchts = new CtfEnchantment[a.getEnchantments().size()];
                    int b = 0;
                    for(Map.Entry<Enchantment, Integer> ench : a.getEnchantments().entrySet()) {
                        enchts[b] = new CtfEnchantment();
                        enchts[b].enchantment = ench.getKey().getId();
                        enchts[b].lvl = ench.getValue();
                        b++;
                    }
                    item.enchantments = enchts;
                }
                items[36+x] = item;
            }
            game.items = items;
            sender.sendMessage("§aSet inventory!");
            return false;
        }));
    }

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        String param0 = strings.length>=1?strings[0]:null;
        ArrayList<CtfSubCommand> sub = new ArrayList<>();
        for(CtfSubCommand c : SUB_COMMANDS) {
            if(c.admin) {
                if(commandSender.isOp()) sub.add(c);
            } else sub.add(c);
        }

        if(param0==null) {
            StringBuilder available = new StringBuilder();
            for(CtfSubCommand t : sub) {
                available.append("§c").append(t.name).append("§7, ");
            }
            if(available.length()!=0) available = new StringBuilder(available.substring(0, available.length() - 2));
            commandSender.sendMessage("§7Available subcommands: " + available);
            return false;
        }else {
            for(CtfSubCommand c : SUB_COMMANDS) {
                if(c.name.equalsIgnoreCase(param0)) {
                    if (c.admin) {
                        if (commandSender.isOp()) {
                            return c.executor.onCommand(commandSender, command, s, strings);
                        }
                    } else {
                        return c.executor.onCommand(commandSender, command, s, strings);
                    }
                }
            }

            StringBuilder available = new StringBuilder();
            for(CtfSubCommand t : sub) {
                available.append("§c").append(t.name).append("§7, ");
            }
            if(available.length()!=0) available = new StringBuilder(available.substring(0, available.length() - 2));
            commandSender.sendMessage("§7Available subcommands: " + available);
        }


        return false;
    }
}
