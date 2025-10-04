package com.example.safezoneplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SafeZonePlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final HashSet<Location> safeZones = new HashSet<>();
    private final HashSet<Location> guardZones = new HashSet<>();
    private final HashMap<UUID, Long> combatPlayers = new HashMap<>();
    private final HashMap<UUID, UUID> tradingPlayers = new HashMap<>();
    private static final long COMBAT_TAG_DURATION = 30000; // 30초 전투 태그 지속 시간

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("safezone").setExecutor(this);
        this.getCommand("guardzone").setExecutor(this);
        this.getCommand("trade").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("safezone") && sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            safeZones.add(loc);
            player.sendMessage(ChatColor.GREEN + "안전구역이 설정되었습니다!");
            return true;
        }
        if (command.getName().equalsIgnoreCase("guardzone") && sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            guardZones.add(loc);
            player.sendMessage(ChatColor.YELLOW + "보호구역이 설정되었습니다!");
            return true;
        }
        if (command.getName().equalsIgnoreCase("trade") && sender instanceof Player && args.length > 0) {
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && player != target) {
                tradingPlayers.put(player.getUniqueId(), target.getUniqueId());
                tradingPlayers.put(target.getUniqueId(), player.getUniqueId());
                openTradeInventory(player, target);
                player.sendMessage(ChatColor.GREEN + "거래를 시작합니다!");
                target.sendMessage(ChatColor.GREEN + "거래 요청이 수락되었습니다!");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInSafeZone(event.getBlock().getLocation()) || isInGuardZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "이 구역에서는 블록을 부술 수 없습니다!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInSafeZone(event.getBlock().getLocation()) || isInGuardZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "이 구역에서는 블록을 설치할 수 없습니다!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            if (isInSafeZone(victim.getLocation()) || isInSafeZone(attacker.getLocation())) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "안전구역에서는 전투를 할 수 없습니다!");
            } else {
                combatPlayers.put(attacker.getUniqueId(), System.currentTimeMillis());
                combatPlayers.put(victim.getUniqueId(), System.currentTimeMillis());
                attacker.sendMessage(ChatColor.RED + "전투중!");
                victim.sendMessage(ChatColor.RED + "전투중!");
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInSafeZone(event.getLocation())) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();

        if (isInSafeZone(loc)) {
            player.sendMessage(ChatColor.GREEN + "[안전구역]에 진입하였습니다!");
        } else if (isInGuardZone(loc)) {
            player.sendMessage(ChatColor.YELLOW + "[보호구역]에 진입하였습니다!");
        }
    }

    private boolean isInSafeZone(Location loc) {
        return safeZones.stream().anyMatch(safeLoc -> safeLoc.getWorld().equals(loc.getWorld()) && safeLoc.distance(loc) <= 5);
    }

    private boolean isInGuardZone(Location loc) {
        return guardZones.stream().anyMatch(guardLoc -> guardLoc.getWorld().equals(loc.getWorld()) && guardLoc.distance(loc) <= 5);
    }

    private void openTradeInventory(Player player, Player target) {
        Inventory tradeInventory = Bukkit.createInventory(null, 27, "거래 창");
        player.openInventory(tradeInventory);
        target.openInventory(tradeInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("거래 창")) {
            event.setCancelled(false); // 거래 창에서 아이템 이동 가능
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("거래 창")) {
            UUID playerUUID = event.getPlayer().getUniqueId();
            tradingPlayers.remove(playerUUID);
        }
    }
}
