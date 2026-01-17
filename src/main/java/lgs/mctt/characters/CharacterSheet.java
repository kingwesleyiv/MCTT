package lgs.mctt.characters;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterSheet {
    
    private String name;
    private float level;
    private final Map<String, Stat> stats = new LinkedHashMap<>();
    
    public CharacterSheet(String name, float level) {
        this.name = name;
        this.level = level;
        initializeStats();
    }
    
    /** Initialize all default stats with base or linked attributes */
    private void initializeStats() {
        // Core attributes
        stats.put("Strength", new Stat("10"));
        stats.put("Dexterity", new Stat("10"));
        stats.put("Constitution", new Stat("10"));
        stats.put("Intelligence", new Stat("10"));
        stats.put("Wisdom", new Stat("10"));
        stats.put("Charisma", new Stat("10"));
        
        // Skills
        stats.put("Acrobatics", new Stat("0", "Dexterity"));
        stats.put("Animal Handling", new Stat("0", "Wisdom"));
        stats.put("Arcana", new Stat("0", "Intelligence"));
        stats.put("Athletics", new Stat("0", "Strength"));
        stats.put("Deception", new Stat("0", "Charisma"));
        stats.put("History", new Stat("0", "Intelligence"));
        stats.put("Insight", new Stat("0", "Wisdom"));
        stats.put("Intimidation", new Stat("0", "Charisma"));
        stats.put("Investigation", new Stat("0", "Intelligence"));
        stats.put("Medicine", new Stat("0", "Wisdom"));
        stats.put("Nature", new Stat("0", "Intelligence"));
        stats.put("Perception", new Stat("0", "Wisdom"));
        stats.put("Performance", new Stat("0", "Charisma"));
        stats.put("Persuasion", new Stat("0", "Charisma"));
        stats.put("Religion", new Stat("0", "Intelligence"));
        stats.put("Sleight of Hand", new Stat("0", "Dexterity"));
        stats.put("Stealth", new Stat("0", "Dexterity"));
        stats.put("Survival", new Stat("0", "Wisdom"));
        
        // Derived stats
        stats.put("Armor Class", new Stat("10", "Dexterity"));
        stats.put("Initiative", new Stat("0", "Dexterity"));
        stats.put("Speed", new Stat("30"));
        stats.put("Hit Dice", new Stat("1d8"));
        stats.put("Health", new Stat("8"));
    }
    
    // --- Basic info ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public float getLevel() { return level; }
    public void setLevel(float level) { this.level = level; }
    
    // --- Stat access ---
    public Stat getStat(String key) { return stats.get(key); }
    
    public void setStat(String key, Stat stat) { stats.put(key, stat); }
    
    public Map<String, Stat> getAllStats() { return stats; }
    
    /** Gets the modifier for any numeric stat */
    public int getModifier(String key) {
        Stat stat = stats.get(key);
        if (stat == null) return 0;
        return stat.getModifier();
    }
    
    /** Returns the value of a stat as an integer (for rolls) */
    public int getStatValue(String key) {
        Stat stat = stats.get(key);
        if (stat == null) return 0;
        try {
            return Integer.parseInt(stat.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /** Returns the linked parent attribute of a skill, if any */
    public String getLinkedAttribute(String key) {
        Stat stat = stats.get(key);
        return (stat != null) ? stat.getLinkedAttribute() : null;
    }
}
