package com.astrasmp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

@SuppressWarnings("deprecation")
public class ItemSerializer {

    public static String getHash(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        // Генерируем стабильный хэш: ИМЯ_МАТЕРИАЛА:CUSTOM_MODEL_DATA
        int cmd = (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) 
                  ? item.getItemMeta().getCustomModelData() : 0;
        return item.getType().name() + ":" + cmd;
    }

    public static ItemStack fromHash(String hash) {
        if (hash == null || hash.isEmpty()) return null;
        try {
            String[] parts = hash.split(":");
            org.bukkit.Material mat = org.bukkit.Material.valueOf(parts[0]);
            ItemStack item = new ItemStack(mat);
            if (parts.length > 1) {
                int cmd = Integer.parseInt(parts[1]);
                if (cmd != 0) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(cmd);
                        item.setItemMeta(meta);
                    }
                }
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static String toBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать предмет", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка сериализации инвентаря", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }
}