package net.saturn.murderMystery.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemFactory {

    public static final String MURDERER_KNIFE_TAG = "murder_mystery_knife";
    public static final String SHERIFF_BOW_TAG    = "murder_mystery_bow";
    public static final String SHERIFF_ARROW_TAG  = "murder_mystery_arrow";

    public static ItemStack createMurdererKnife() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚔ The Knife", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("The instrument of death.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(Enchantment.SHARPNESS, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), MURDERER_KNIFE_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN, true
        );
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSheriffBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🏹 Sheriff's Revolver", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("One shot. Choose wisely.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Shooting an innocent is fatal.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(Enchantment.POWER, 10, true);  // one-shot kill power
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), SHERIFF_BOW_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN, true
        );
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSheriffArrow() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Justice Round", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), SHERIFF_ARROW_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN, true
        );
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMurdererKnife(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), MURDERER_KNIFE_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN
        );
    }

    public static boolean isSheriffBow(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), SHERIFF_BOW_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN
        );
    }

    public static boolean isSheriffArrow(org.bukkit.entity.Arrow arrow) {
        if (arrow == null) return false;
        return arrow.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(net.saturn.murderMystery.MurderMystery.getInstance(), SHERIFF_ARROW_TAG),
                org.bukkit.persistence.PersistentDataType.BOOLEAN
        );
    }
}