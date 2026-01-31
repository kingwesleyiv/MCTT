package lgs.mctt.characters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CharacterSheet {
    
private String name;
private int level;
private String dndClass;
private float proficiencyBonus = 1 + (float)Math.floor(((float)level - 1) / 3);
public UUID playerId;
private final Map<String, Stat> stats = new LinkedHashMap<>();

public CharacterSheet(String name, int level, String dndClass) {
    this.name = name;
    this.level = level;
    this.dndClass = dndClass;
    initializeStats();
}

// Name Strings NASTY MESSY BAD PROGRAMMER
// Core Attributes
public static final String str = "Strength";
public static final String dex = "Dexterity";
public static final String con = "Constitution";
public static final String intel = "Intelligence";
public static final String wis = "Wisdom";
public static final String cha = "Charisma";
public static final String[] attributeNames = {
    str, dex, con, intel, wis, cha
};

// Skills
public static final String acrobatics = "Acrobatics";
public static final String animalhandling = "Animal Handling";
public static final String arcana = "Arcana";
public static final String athletics = "Athletics";
public static final String deception = "Deception";
public static final String history = "History";
public static final String insight = "Insight";
public static final String intimidation = "Intimidation";
public static final String investigation = "Investigation";
public static final String medicine = "Medicine";
public static final String nature = "Nature";
public static final String perception = "Perception";
public static final String performance = "Performance";
public static final String persuasion = "Persuasion";
public static final String religion = "Religion";
public static final String sleightofhand = "Sleight of Hand";
public static final String stealth = "Stealth";
public static final String survival = "Survival";
public static final String initiative = "Initiative";
public static final String[] skillNames = {
    acrobatics, animalhandling, arcana, athletics, deception,
    history, insight, intimidation, investigation, medicine,
    nature, perception, performance, persuasion, religion,
    sleightofhand, stealth, survival, initiative
};
    
/** Initialize all default stats with base or linked attributes */
private void initializeStats() {
    
    // Core attributes
    stats.put(str, new Stat("10"));
    stats.put(dex, new Stat("10"));
    stats.put(con, new Stat("10"));
    stats.put(intel, new Stat("10"));
    stats.put(wis, new Stat("10"));
    stats.put(cha, new Stat("10"));
    
    // Derived stats
    stats.put("Armor Class", new Stat("10", dex));
    stats.put("Speed", new Stat("30"));
    stats.put("Hit Dice", new Stat("1d8"));
    stats.put("Health", new Stat("8", con));
    
    // Skills
    stats.put(acrobatics, new Stat("0", dex));
    stats.put(animalhandling, new Stat("0", wis));
    stats.put(arcana, new Stat("0", intel));
    stats.put(athletics, new Stat("0", str));
    stats.put(deception, new Stat("0", cha));
    stats.put(history, new Stat("0", intel));
    stats.put(insight, new Stat("0", wis));
    stats.put(intimidation, new Stat("0", cha));
    stats.put(investigation, new Stat("0", intel));
    stats.put(medicine, new Stat("0", wis));
    stats.put(nature, new Stat("0", intel));
    stats.put(perception, new Stat("0", wis));
    stats.put(performance, new Stat("0", cha));
    stats.put(persuasion, new Stat("0", cha));
    stats.put(religion, new Stat("0", intel));
    stats.put(sleightofhand, new Stat("0", dex));
    stats.put(stealth, new Stat("0", dex));
    stats.put(survival, new Stat("0", wis));
    stats.put(initiative, new Stat("0", dex));
    
    for (String name : skillNames) { getTotal(name); } // Update All values based on Attribute mods, proficiency and bonuses.
}

public Map<String, Stat> getAllStats() { return stats; }


// --- Basic info ---
public String getName() { return name; }
public void setName(String name) { this.name = name; }

public float getLevel() { return level; }
public void setLevel(int level) { this.level = level; }

public String getDndClass() { return dndClass; }
public void setDndClass(String newValue) { this.dndClass = newValue; }

public float calcProf(String key) {
    proficiencyBonus = 1 + (int) Math.floor(((float)level - 1) / 3);
    return proficiencyBonus * getStat(key).getProficiency();
}

// --- Stat access ---
public Stat getStat(String key) { return stats.get(key); }
public void setStat(String key, Stat stat) { stats.put(key, stat); }

public String getStatValue(String key)
{
    Stat stat = stats.get(key);
    return (stat != null) ? stat.value : "";
}
public void setStatValue(String key, String value)
{
    Stat stat = stats.get(key);
    if (stat != null) stat.setValue(value);
}

/** Gets the modifier for any numeric stat */
public int getTotal(String key) {
    Stat stat = stats.get(key);
    if (stat == null) return 0;
    if (!isSkill(key)) return stat.getIntValue();
    int total = stat.getMod() + (int) calcProf(key) + stat.bonus;
    stat.value = ""+total; // May not properly set value. Should use .put() if needed.
    return total;
}

/** Returns the linked parent attribute of a skill, if any */
public String getStatParent(String key) {
    Stat stat = stats.get(key);
    return (stat != null) ? stat.parent : null;
}

public boolean isSkill(String key) {
    for (String skillName : skillNames) {
        if (skillName.equalsIgnoreCase(key)) return true;
    }
    return false;
}

public boolean isAttribute(String key) {
    for (String attrName : attributeNames) {
        if (attrName.equalsIgnoreCase(key)) return true;
    }
    return false;
}

public int getAdvantage(String key) {
    Stat stat = stats.get(key);
    return (stat != null) ? stat.getAdvantage() : 0;
}

public void reload(){
    for (String name : skillNames) { getTotal(name); }
}

// END OF FILE ///////////////////////////////////////////////////////////////////////////////////////////////////////
}

