package com.sunrtp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SunRtp extends JavaPlugin {
    private FileConfiguration config;
    private Map<Player, Long> cooldowns = new HashMap<>();
    private long cooldownTime; // Время задержки в миллисекундах

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        // Загрузка времени задержки из конфигурации
        cooldownTime = config.getLong("cooldown") * 1000; // Конвертирование секунд в миллисекунды
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("rtp")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("sunrtp.reload")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет разрешения на использование этой команды.");
                    return true;
                }

                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Конфигурация плагина SunRtp перезагружена.");
                // Обновление времени задержки из конфигурации
                cooldownTime = config.getLong("cooldown") * 1000;
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Команда доступна только игрокам.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("sunrtp.teleport")) {
                player.sendMessage(ChatColor.RED + "У вас нет разрешения на использование этой команды.");
                return true;
            }

            if (cooldowns.containsKey(player)) {
                long timeLeft = (cooldowns.get(player) + cooldownTime - System.currentTimeMillis()) / 1000;
                if (timeLeft > 0) {
                    player.sendMessage(ChatColor.RED + "Подождите " + timeLeft + " секунд перед следующим использованием команды.");
                    return true;
                }
            }

            String worldName = config.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(ChatColor.RED + "Указанный мир не найден.");
                return true;
            }

            int minX = config.getInt("minX");
            int maxX = config.getInt("maxX");
            int minZ = config.getInt("minZ");
            int maxZ = config.getInt("maxZ");

            Random random = new Random();
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = world.getHighestBlockYAt(x, z);

            Location location = new Location(world, x, y, z);
            player.teleport(location);

            String teleportMessage = ChatColor.translateAlternateColorCodes('&', config.getString("teleportMessage"));
            teleportMessage = teleportMessage.replace("%world%", worldName);
            player.sendMessage(teleportMessage);

            // Установка задержки для игрока
            cooldowns.put(player, System.currentTimeMillis());
        }
        return true;
    }
}
