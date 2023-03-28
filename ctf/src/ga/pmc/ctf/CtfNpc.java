package ga.pmc.ctf;

import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftVillager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class CtfNpc implements Listener {

    public transient CraftVillager entity;
    public transient ArrayList<LivingEntity> children = new ArrayList<>();

    public CtfLocation location;
    public String[] text = new String[0];

    private void disableThings(Entity entity) {
        net.minecraft.server.v1_8_R3.Entity nmsEnt = ((CraftEntity) entity).getHandle();
        NBTTagCompound tag = nmsEnt.getNBTTag();

        if (tag == null) {
            tag = new NBTTagCompound();
        }

        nmsEnt.c(tag);
        tag.setInt("NoAI", 1);
        tag.setInt("Silent", 1);
        nmsEnt.f(tag);
    }

    public void spawn() {
        entity = new CraftVillager((CraftServer) Bukkit.getServer(), new EntityVillager(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), 0));
        entity.teleport(location.location().add(0.5, 0, 0.5));
        disableThings(entity);
        ((CraftWorld) entity.getWorld()).addEntity(entity.getHandle(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.setProfession(Villager.Profession.FARMER);
        entity.getHandle().setProfession(0);


        Location loc = entity.getLocation().add(0, 1.65, 0);

        for (String s : text) {
            loc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            ArmorStand stand = Bukkit.getWorlds().get(0).spawn(loc.add(0, 0.25, 0), ArmorStand.class);
            stand.setGravity(false);
            stand.setCustomName(s);
            stand.setCustomNameVisible(true);
            stand.setMarker(true);
            stand.setVisible(false);
            children.add(stand);
        }
    }

    public ItemStack renameItem(ItemStack item, String name) {
        var meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack addLore(ItemStack item, String... lore) {
        var meta = item.getItemMeta();
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void despawn() {
        if (entity != null) {
            entity.setHealth(0);
            for (LivingEntity e : children) {
                e.setHealth(0);
            }
        }
    }

    @EventHandler
    public void click(CtfPlayer click) {
        var inventory = Bukkit.createInventory(null, 3 * 9, "Shop");
        ItemStack nothing = renameItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7), " ");
        for (int index = 0; index < inventory.getSize(); index++) {
            inventory.setItem(index, nothing);
            Bukkit.getServer().getConsoleSender().sendMessage("please work1");
        }
        Bukkit.getServer().getConsoleSender().sendMessage("please work2");
        inventory.setItem(11, renameItem(new ItemStack(Material.GOLD_INGOT, 1), "Item Shop"));
        inventory.setItem(15, renameItem(new ItemStack(Material.DIAMOND, 1), "Team Upgrades"));
        Bukkit.getServer().getConsoleSender().sendMessage("please work3");
        click.getPlayer().openInventory(inventory);
        Bukkit.getServer().getConsoleSender().sendMessage("please work4");

    }

    @EventHandler
    public void invClick(InventoryClickEvent e) {
        Bukkit.getServer().getConsoleSender().sendMessage(" " + e.getInventory().getName());
        if (Objects.equals(e.getInventory().getName(), "Shop")) {
            if (e.getRawSlot() == 11) {
                var inventory = Bukkit.createInventory(null, 3 * 9, "Item Shop");
                ItemStack nothing = renameItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7), " ");
                for (int index = 0; index < inventory.getSize(); index++) {
                    inventory.setItem(index, nothing);
                    Bukkit.getServer().getConsoleSender().sendMessage("please work6");
                }
                Bukkit.getServer().getConsoleSender().sendMessage("please work7");
                inventory.setItem(13, renameItem(new ItemStack(Material.FISHING_ROD, 1), "Fishing Rod"));
                e.getWhoClicked().openInventory(inventory);
                Bukkit.getServer().getConsoleSender().sendMessage("please work8");
                e.setCancelled(true);
                Bukkit.getServer().getConsoleSender().sendMessage("please work9");
            }
        }
    }

}

        /* if (!game.settings.enableBlockResetting) {
            click.player.sendMessage("§cBlock Resetting is disabled");
            return;
        }
        if (click.points >= game.settings.resetBlocksPrice || !game.settings.enablePoints) {
            if (game.settings.enablePoints) click.points -= game.settings.resetBlocksPrice;
            updateScoreboard(click);
            long time = game.settings.blockResetTime;
            Bukkit.broadcastMessage("§aBlocks Reset in §c" + time + " seconds");
            Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.broadcastMessage("§aBlocks Reset in §c5 seconds"), (time - 5) * 20);
            Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.broadcastMessage("§aBlocks Reset in §c4 seconds"), (time - 4) * 20);
            Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.broadcastMessage("§aBlocks Reset in §c3 seconds"), (time - 3) * 20);
            Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.broadcastMessage("§aBlocks Reset in §c2 seconds"), (time - 2) * 20);
            Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.broadcastMessage("§aBlocks Reset in §c1 seconds"), (time - 1) * 20);
            Bukkit.getScheduler().runTaskLater(instance, () -> {
                game.destroyPlacedBlocks();
                click.player.playSound(click.player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 0.5f);
                click.player.sendMessage("§cBlocks Reset");

            }, time * 20);
        } else {
            click.player.sendMessage("§cInsufficient Points. Required points: " + game.settings.resetBlocksPrice);
        }
 */













