package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class RepairCommand implements org.bukkit.command.TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cЭта команда доступна только игрокам.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            TextUtil.send(player, "&cВозьмите предмет в основную руку перед использованием команды.");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            TextUtil.send(player, "&cЭтот предмет нельзя починить.");
            return true;
        }

        if (damageable.getDamage() == 0) {
            TextUtil.send(player, "&eПредмет уже в идеальном состоянии.");
            return true;
        }

        damageable.setDamage(0);
        item.setItemMeta(damageable);

        TextUtil.send(player, "&aПредмет &e" + formatItemName(item) + " &aуспешно починен!");
        return true;
    }

    private String formatItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        // Convert DIAMOND_SWORD -> Diamond Sword
        String raw = item.getType().name().toLowerCase().replace("_", " ");
        String[] words = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return Collections.emptyList();
    }
}
