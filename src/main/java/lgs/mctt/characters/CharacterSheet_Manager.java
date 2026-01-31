package lgs.mctt.characters;

import lgs.mctt.MCTT;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CharacterSheet_Manager {
    
    private final File sheetsFile;
    private final YamlConfiguration config;
    
    // In-memory caches
    public final Map<String, CharacterSheet> sheets = new HashMap<>();
    public final Map<UUID, String> assignedSheets = new HashMap<>();
    
    public CharacterSheet_Manager(MCTT plugin) {
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
    
    public boolean create(String name, int level, String dndclass) {
        if (sheets.containsKey(name)) return false;
        
        CharacterSheet sheet = new CharacterSheet(name, level, dndclass);
        sheets.put(name, sheet);
        return save(sheet);
    }
    
    public boolean delete(String name) {
        String key = name;
        if (!sheets.containsKey(key)) return false;
        sheets.remove(key); // Remove the sheet from memory
        config.set("sheets." + key, null); // Remove from config
        assignedSheets.entrySet().removeIf(e -> e.getValue().equals(name)); // Remove any assignedSheets pointing to this sheet
        config.set("assignedSheets", assignedSheets); // Save updated assignedSheets back to YAML
        return saveFile();
    }
    
    public CharacterSheet load(String name) {
        return sheets.get(name);
    }
    
    public boolean save(CharacterSheet sheet) {
        String key = sheet.getName();
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
    
    public int assignSheet(UUID targetUUID, String sheetName) {
        if (!sheets.containsKey(sheetName)) return 0;
        if (assignedSheets.containsKey(targetUUID) && assignedSheets.get(targetUUID).equals(sheetName))
            return 1; // Already assigned to this sheet
        else {
            assignedSheets.put(targetUUID, sheetName);
            config.set("assignedSheets." + targetUUID.toString(), sheetName);
        }
        return saveFile() ? 1 : 0;
    }
    
    public int unassignSheet(UUID targetUUID) {
        if (!assignedSheets.containsKey(targetUUID)) return 0;
        assignedSheets.remove(targetUUID);
        config.set("assignedSheets." + targetUUID.toString(), null);
        return saveFile() ? 1 : 0;
    }
    
    public CharacterSheet getAssignedSheet(UUID targetUUID) {
        String sheetName = assignedSheets.get(targetUUID);
        if (sheetName == null) return null;
        return load(sheetName);
    }
    
    // -------------------------
    // Internal Load/Save
    // -------------------------
    
    private void loadAllSheets() {
        if (!config.contains("sheets")) return;
        
        for (String key : config.getConfigurationSection("sheets").getKeys(false)) {
            int level = config.getInt("sheets." + key + ".level");
            String dndclass = config.getString("sheets." + key + ".class");
            CharacterSheet sheet = new CharacterSheet(key, level, dndclass);
            
            if (config.contains("sheets." + key + ".stats")) {
                for (String statKey : config.getConfigurationSection("sheets." + key + ".stats").getKeys(false)) {
                    String value = config.getString("sheets." + key + ".stats." + statKey);
                    sheet.getStat(statKey).setValue(value);
                }
            }
            
            sheets.put(key, sheet);
        }
    }
    
    private void loadAllAssignments() {
        if (!config.contains("assignedSheets")) return;
        
        for (String uuidStr : config.getConfigurationSection("assignedSheets").getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuidStr);
                String sheetName = config.getString("assignedSheets." + uuidStr);
                if (sheetName != null && sheets.containsKey(sheetName)) {
                    assignedSheets.put(id, sheetName);
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
            MCTT.get().getLogger().warning("Failed to save sheets.yml: " + e.getMessage());
            return false;
        }
    }

    public boolean saveSheets() {
        config.set("sheets", null);
        for (CharacterSheet sheet : sheets.values()) {
            String key = sheet.getName();
            String base = "sheets." + key;
            config.set(base + ".level", sheet.getLevel());
            config.set(base + ".class", sheet.getDndClass());
            for (Map.Entry<String, Stat> entry : sheet.getAllStats().entrySet()) {
                config.set(base + ".stats." + entry.getKey(), entry.getValue().getValue());
            }
        }
        config.set("assignedSheets", null);
        for (Map.Entry<UUID, String> entry : assignedSheets.entrySet()) {
            config.set("assignedSheets." + entry.getKey().toString(), entry.getValue());
        }
        return saveFile();
    }

    public void reloadSheets() {
        sheets.clear();
        assignedSheets.clear();
        config.options().copyDefaults(false);
        YamlConfiguration fresh = YamlConfiguration.loadConfiguration(sheetsFile);
        config.set("sheets", fresh.getConfigurationSection("sheets"));
        config.set("assignedSheets", fresh.getConfigurationSection("assignedSheets"));
        loadAllSheets();
        loadAllAssignments();
    }

    public static void reloadSheet(String name) {

    }
    
    public Map<String, CharacterSheet> getAllSheets() {
        return sheets;
    }
    
    public Map<UUID, String> getAllAssignments() {
        return assignedSheets;
    }

    public CharacterSheet getSheetByUUID(UUID uuid) {
        String sheetName = assignedSheets.get(uuid);
        if (sheetName == null) return null;
        return sheets.get(sheetName);
    }

    public CharacterSheet getSheetByName(String name) {
        return sheets.get(name);
    }

}
