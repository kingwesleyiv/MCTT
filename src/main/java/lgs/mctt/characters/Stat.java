package lgs.mctt.characters;

public class Stat {
    
    private String value;
    private String parent;
    private int proficiency; // 0 = none, -1 = half, 1 = full, 2 = expertise
    private int advantage;   // -1 = disadvantage, 0 = normal, 1 = advantage
    
    // --- Constructors ---
    public Stat() { this.value = "0"; }
    
    public Stat(String value) {
        this.value = value;
    }
    
    public Stat(String value, int proficiency) {
        this.value = value;
        this.proficiency = proficiency;
    }
    
    public Stat(String value, int proficiency, int advantage) {
        this.value = value;
        this.proficiency = proficiency;
        this.advantage = advantage;
    }
    
    public Stat(String value, String parent) {
        this.value = value;
        this.parent = parent;
    }
    
    public Stat(String value, int proficiency, String parent) {
        this.value = value;
        this.proficiency = proficiency;
        this.parent = parent;
    }
    
    public Stat(String value, int proficiency, int advantage, String parent) {
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
    
    public int getProficiency() { return proficiency; }
    public void setProficiency(int proficiency) { this.proficiency = proficiency; }
    
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
    public int getModifier() {
        // 5e D&D standard formula: (score - 10) / 2
        try {
            return (getIntValue() - 10) / 2;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public String getLinkedAttribute() {
        return parent;
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
            int mod = getModifier();
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
