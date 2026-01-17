package lgs.mctt.characters;

import lgs.mctt.MCTT;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CharacterSheet_Manager {
    
    private final MCTT plugin;
    private final File sheetsFile;
    private final YamlConfiguration config;
    
    // In-memory caches
    private final Map<String, CharacterSheet> sheets = new HashMap<>();
    private final Map<UUID, String> assignments = new HashMap<>();
    
    public CharacterSheet_Manager(MCTT plugin) {
        this.plugin = plugin;
        this.sheetsFile = new File(plugin.getDataFolder(), "sheets.yml");
        if (!sheetsFile.exists()) {
            try {
                sheetsFile.getParentFile().mkdirs();
                sheetsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create sheets.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(sheetsFile);
        loadAllSheets();
        loadAllAssignments();
    }

// -------------------------
// Sheet CRUD
// -------------------------
    
    public boolean create(String name, float level) {
        if (sheets.containsKey(name.toLowerCase())) return false;
        
        CharacterSheet sheet = new CharacterSheet(name, level);
        sheets.put(name.toLowerCase(), sheet);
        return save(sheet);
    }
    
    public boolean delete(String name) {
        String key = name.toLowerCase();
        if (!sheets.containsKey(key)) return false;
        sheets.remove(key); // Remove the sheet from memory
        config.set("sheets." + key, null); // Remove from config
        assignments.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(name)); // Remove any assignments pointing to this sheet
        config.set("assignments", assignments); // Save updated assignments back to YAML
        return saveFile();
    }
    
    public CharacterSheet load(String name) {
        return sheets.get(name.toLowerCase());
    }
    
    public boolean save(CharacterSheet sheet) {
        String key = sheet.getName().toLowerCase();
        sheets.put(key, sheet);
        
        String base = "sheets." + key;
        config.set(base + ".level", sheet.getLevel());
        
        for (Map.Entry<String, Stat> entry : sheet.getAllStats().entrySet()) {
            config.set(base + ".stats." + entry.getKey(), entry.getValue().getValue());
        }
        
        return saveFile();
    }

    
    // -------------------------
    // Assignments
    // -------------------------
    
    public boolean assignSheet(UUID targetUUID, String sheetName) {
        if (!sheets.containsKey(sheetName.toLowerCase())) return false;
        assignments.put(targetUUID, sheetName);
        config.set("assignments." + targetUUID.toString(), sheetName);
        return saveFile();
    }
    
    public boolean unassignSheet(UUID targetUUID) {
        if (!assignments.containsKey(targetUUID)) return false;
        assignments.remove(targetUUID);
        config.set("assignments." + targetUUID.toString(), null);
        return saveFile();
    }
    
    public CharacterSheet getAssignedSheet(UUID targetUUID) {
        String sheetName = assignments.get(targetUUID);
        if (sheetName == null) return null;
        return load(sheetName);
    }
    
    // -------------------------
    // Internal Load/Save
    // -------------------------
    
    private void loadAllSheets() {
        if (!config.contains("sheets")) return;
        
        for (String key : config.getConfigurationSection("sheets").getKeys(false)) {
            float level = (float) config.getDouble("sheets." + key + ".level", 1f);
            CharacterSheet sheet = new CharacterSheet(key, level);
            
            if (config.contains("sheets." + key + ".stats")) {
                for (String statKey : config.getConfigurationSection("sheets." + key + ".stats").getKeys(false)) {
                    String value = config.getString("sheets." + key + ".stats." + statKey);
                    sheet.getStat(statKey).setValue(value);
                }
            }
            
            sheets.put(key.toLowerCase(), sheet);
        }
    }
    
    private void loadAllAssignments() {
        if (!config.contains("assignments")) return;
        
        for (String uuidStr : config.getConfigurationSection("assignments").getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuidStr);
                String sheetName = config.getString("assignments." + uuidStr);
                if (sheetName != null && sheets.containsKey(sheetName.toLowerCase())) {
                    assignments.put(id, sheetName);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
    
    private boolean saveFile() {
        try {
            config.save(sheetsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save sheets.yml: " + e.getMessage());
            return false;
        }
    }
    
    // -------------------------
    // Helpers
    // -------------------------
    
    public Map<String, CharacterSheet> getAllSheets() {
        return sheets;
    }
    
    public Map<UUID, String> getAllAssignments() {
        return assignments;
    }
}
