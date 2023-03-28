package ga.pmc.ctf;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class CtfItem {

    public int item;
    public int amount;
    public int damage;
    public CtfEnchantment[] enchantments;

    public String toString() {
        return "CtfItem{" +
                "item=" + item +
                ", damage=" + damage +
                ", enchantments=" + Arrays.toString(enchantments) +
                '}';
    }

    public ItemStack toItem() {
        ItemStack i = new ItemStack(item, amount, (short) damage);
        if(enchantments!=null) {
            for(CtfEnchantment e : enchantments) {
                i.addUnsafeEnchantment(Enchantment.getById(e.enchantment), e.lvl);
            }
        }
        ItemMeta a = i.getItemMeta();
        if(a!=null) {
            if(i.getType().getMaxDurability() > 0) {
                a.spigot().setUnbreakable(true);
                i.setItemMeta(a);
            }
        }
        return i;
    }

}
