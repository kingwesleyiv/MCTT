package lgs.mctt.characters;

import java.util.Set;

public class Conditions {
	
	public static final Set<String> ALL = Set.of(
		"PRONE", "DYING", "UNCONSCIOUS", "BLIND", "POISONED", "STUNNED", "GRAPPLED", "RESTRAINED", "PARALYZED", "SLOWED",
		"CHARMED", "FRIGHTENED", "PETRIFIED", "FROZEN", "DEAD", "INVISIBLE", "HIDDEN", "SILENCED", "EXHAUSTION", "POISON",
		"BLEEDING", "PAUSED", "DEAFENED", "CURSED", "BLESSED", "INSPIRED", "HEROIC_INSPIRED", "COMBAT"
	);
	public static final Set<String> ZERO_SPEED = Set.of(
		"UNCONSCIOUS", "STUNNED", "GRAPPLED", "RESTRAINED", "PARALYZED", "PETRIFIED", "FROZEN", "DEAD"
	);
	
	public static boolean hasAnyTag(Set<String> tags, String... toCheck) {
		for (String tag : toCheck) { if (tags.contains(tag)) return true; }
		return false;
	}
	public static boolean hasAnyTag(Set<String> tags, Set<String> toCheck) {
		for (String tag : toCheck) { if (tags.contains(tag)) return true; }
		return false;
	}
	
}
