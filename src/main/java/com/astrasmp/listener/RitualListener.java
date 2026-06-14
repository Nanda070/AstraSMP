package com.astrasmp.listener;

import com.astrasmp.items.ItemRegistry;
import com.astrasmp.rituals.RitualService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RitualListener implements Listener {

    private final RitualService ritualService;

    public RitualListener(RitualService ritualService) {
        this.ritualService = ritualService;
    }

    @EventHandler
    public void onRiftInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        
        if (ItemRegistry.is(hand, "cleansingTotem")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
            Location riftLoc = com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().getNearbyRift(loc, 5.0);
            
            if (riftLoc != null) {
                event.setCancelled(true);
                hand.setAmount(hand.getAmount() - 1);
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().closeRift(riftLoc);
                player.sendMessage("§e[Очищение] §fВы закрыли Врата Бездны и остановили распространение Скверны.");
            }
        }
    }

    @EventHandler
    public void onAltarInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Проверяем, что кликнули пустой рукой по Плачущему обсидиану (или кастомному блоку алтаря)
        if (block.getType() != org.bukkit.Material.CRYING_OBSIDIAN) return;

        int tier = ritualService.getCircleManager().getCircleTier(block.getLocation());
        if (tier > 0) {
            // Если игрок держит ритуальный кинжал, не прерываем (возможно он готовится), 
            // но если пустая рука - рисуем пентаграмму.
            if (hand.getType().isAir()) {
                ritualService.getCircleManager().drawPentagram(block.getLocation(), tier);
                player.sendMessage("§5[Темная Магия] §7Алтарь " + tier + " уровня готов к ритуалу.");
            }
        }
    }

    @EventHandler
    public void onSacrifice(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!ItemRegistry.is(weapon, "sacrificialDagger")) return;

        Location deathLoc = event.getEntity().getLocation();

        // Ищем ближайший алтарь в радиусе 5 блоков
        // Для простоты сканируем блоки вокруг, но в идеале нужно проверять закэшированные координаты
        Location altarLoc = findNearbyAltar(deathLoc);
        if (altarLoc != null) {
            int tier = ritualService.getCircleManager().getCircleTier(altarLoc);
            if (tier > 0) {
                boolean success = ritualService.attemptRitual(altarLoc, tier, event.getEntity().getType(), killer);
                if (success) {
                    // Ритуал успешен
                    killer.sendMessage("§4[Ритуал] §cЖертвоприношение принято!");
                }
            }
        }
    }

    private Location findNearbyAltar(Location loc) {
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == org.bukkit.Material.CRYING_OBSIDIAN) {
                        return b.getLocation();
                    }
                }
            }
        }
        return null;
    }
}
