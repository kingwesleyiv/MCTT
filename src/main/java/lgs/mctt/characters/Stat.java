package lgs.mctt.characters;

public class Stat {

public String value = "0";
public String parent = "";
public float proficiency; // 0 = none, 0.5 = half, 1 = full, 2 = expertise
public int advantage;   // -1 = disadvantage, 0 = normal, 1 = advantage
public int bonus;       // flat bonus to add to rolls involving this stat
    
// --- Constructors ---
public Stat() { this.value = "0"; }

public Stat(String value) {
    this.value = value;
    this.proficiency = 0;
    this.advantage = 0;
    this.parent = "";
}

public Stat(String value, int proficiency) {
    this.value = value;
    this.proficiency = proficiency;
    this.advantage = 0;
    this.parent = "";
}
public Stat(String value, String parent) {
    this.value = value;
    this.proficiency = 0;
    this.advantage = 0;
    this.parent = parent;
}
public Stat(String value, String parent, int proficiency) {
    this.value = value;
    this.proficiency = proficiency;
    this.advantage = 0;
    this.parent = parent;
}
public Stat(String value, int proficiency, int advantage) {
    this.value = value;
    this.proficiency = proficiency;
    this.advantage = advantage;
    this.parent = "";
}
public Stat(String value, String parent, int proficiency, int advantage) {
    this.value = value;
    this.proficiency = proficiency;
    this.advantage = advantage;
    this.parent = parent;
}

// --- Getters & Setters ---
public String getValue() { return value; }
public void setValue(String value) { this.value = value; }

public String getParent() { return parent; }
public void setParent(String parent) { this.parent = parent; }

public int getAdvantage() { return advantage; }
public void setAdvantage(int advantage) { this.advantage = advantage; }

public float getProficiency() { return proficiency; }
public void setProficiency(float proficiency) { this.proficiency = proficiency; }

public int getBonus() { return bonus; }
public void setBonus(int bonus) { this.bonus = bonus; }

// --- Type-safe conversions ---
public int getIntValue() {
    if (value == null) return 0;
    try {
        return Integer.parseInt(value);
    } catch (NumberFormatException e) {
        return 0;
    }
}

public double getDoubleValue() {
    if (value == null) return 0.0;
    try {
        return Double.parseDouble(value);
    } catch (NumberFormatException e) {
        return 0.0;
    }
}

public boolean getBooleanValue() {
    return Boolean.parseBoolean(value);
}

// --- Derived calculations ---
public int getMod() {
    // 5e D&D standard formula: floor((score - 10) / 2)
    if (parent == null || parent == "")
        try { return (int)Math.floor((double)(Integer.parseInt(value) - 10) / 2);
        } catch (Exception e) { return 0; };
    
    try { return (int)Math.floor((double)(Integer.parseInt(parent) - 10) / 2);
    } catch (Exception e) { return 0; }
}

// --- String representation ---
public String formatString() {
    String advText = switch (advantage) {
        case 1 -> " (Adv)";
        case -1 -> " (Dis)";
        default -> advantage > 1 ? " (Adv+" + advantage + ")" :
            advantage < -1 ? " (Dis" + advantage + ")" : "";
    };
    
    if (isNumeric(value)) {
        int base = getIntValue();
        int mod = getMod();
        String modStr = (mod >= 0 ? "+" : "") + mod;
        return base + " (" + modStr + ")" + advText;
    }
    
    return (value != null ? value : "") + advText;
}

// --- Internal helper ---
private boolean isNumeric(String str) {
    if (str == null) return false;
    try {
        Integer.parseInt(str);
        return true;
    } catch (NumberFormatException e) {
        return false;
    }
}
}
