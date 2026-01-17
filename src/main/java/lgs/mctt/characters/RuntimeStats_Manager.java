package lgs.mctt.characters;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the runtime (current) stats for an online player.
 */
public class RuntimeStats_Manager {

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Stat>> activeStats = new HashMap<>();

    public RuntimeStats_Manager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Load runtime stats for a player when they join */
    public void load(Player player, CharacterSheet defaults) {
        UUID id = player.getUniqueId();
        File file = getRuntimeFile(id);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        Map<String, Stat> stats = new HashMap<>();

        for (Map.Entry<String, Stat> entry : defaults.getAllStats().entrySet()) {
            String key = entry.getKey();
            Stat defaultStat = entry.getValue();
            String val = String.valueOf(config.get(key, defaultStat.getValue()));
            int adv = config.getInt(key + ".adv", defaultStat.getAdvantage());

            Stat runtimeStat = new Stat(val, adv);
            stats.put(key, runtimeStat);
        }

        activeStats.put(id, stats);
    }

    /** Save runtime stats for a player */
    public void save(Player player) {
        UUID id = player.getUniqueId();
        Map<String, Stat> stats = activeStats.get(id);
        if (stats == null) return;

        File file = getRuntimeFile(id);
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Stat> entry : stats.entrySet()) {
            config.set(entry.getKey() + ".value", entry.getValue().getValue());
            config.set(entry.getKey() + ".adv", entry.getValue().getAdvantage());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Get a runtime stat */
    public Stat getStat(Player player, String key) {
        Map<String, Stat> stats = activeStats.get(player.getUniqueId());
        return stats != null ? stats.get(key) : null;
    }

    /** Update a runtime stat */
    public void setStat(Player player, String key, Object value) {
        Map<String, Stat> stats = activeStats.get(player.getUniqueId());
        if (stats != null && stats.containsKey(key)) {
            stats.get(key).setValue(value != null ? value.toString() : null);
        }
    }

    /** Remove stats when a player leaves */
    public void unload(Player player) {
        save(player);
        activeStats.remove(player.getUniqueId());
    }

    private File getRuntimeFile(UUID id) {
        return new File(plugin.getDataFolder(), id.toString() + "_runtime.yml");
    }
}
