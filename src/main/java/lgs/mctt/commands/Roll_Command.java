package lgs.mctt.commands;

import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.characters.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class Roll_Command {
	
	private final Random rng = new Random();
	private final CharacterSheet_Manager manager;
	
	public Roll_Command(CharacterSheet_Manager manager) {
		this.manager = manager;
	}
	
	/**
	 * Roll numeric dice: /r <count> <sides>
	 */
	public int rollDice(Player player, int count, int sides) {
		if (count < 1 || sides < 1) {
			player.sendMessage(ChatColor.RED + "Invalid dice. Usage: /r <count> <sides>");
			return 0;
		}
		
		List<Integer> rolls = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			rolls.add(rng.nextInt(sides) + 1);
		}
		
		int total = rolls.stream().mapToInt(Integer::intValue).sum();
		
		sendRollMessage(player, "d" + sides, rolls, 0, 1, sides);
		return 1;
	}
	
// Use /r to roll random numbers. Can use /r <stat> to roll a character stat (only supports d20 stat rolls at the moment.)
	public int rollStat(Player player, String statName, int bonus, int advantage) {
		int baseModifier = 0;
		int profBonus = bonus;
		int advState = advantage;
		
		UUID playerId = player.getUniqueId();
		CharacterSheet sheet = manager.getAssignedSheet(playerId);
		
		if ( sheet == null) {
			player.sendMessage(ChatColor.DARK_RED + "No sheet found.");
			return 0;
		}
		else {
			Stat stat = sheet.getStat(statName);
			if (stat == null) {
				player.sendMessage(ChatColor.RED + "Stat '" + statName + "' not found on your sheet.");
				return 0;
			}
			baseModifier = sheet.getTotal(statName);
			profBonus += stat.getProficiency();
			advState += stat.getAdvantage();
		}
		
	// Roll and account for Advantage / Disadvantage.
		List<Integer> rolls = new ArrayList<>();
		rolls.add(rng.nextInt(20) + 1);
		
		// We need to roll extra dice and sort them accordingly if we have Advantage / Disadvantage.
		// We allow multiple Advantage / Disadvantage stacks here for now because homebrew rules.
		if (advState != 0) {
			for (int i = Math.abs(advState); i > 0; i -= 1) {
				rolls.add(rng.nextInt(20) + 1);
			}
			if (advState > 0) rolls.sort(null);
			if (advState < 0) rolls.sort(Collections.reverseOrder());
		}
		
		sendRollMessage(player, statName, rolls, baseModifier, profBonus, advState);
		return 1;
	}
	
	private void sendRollMessage(Player player, String label, List<Integer> rolls, int modifier, int min, int max) {
		sendRollMessage(player, label, rolls, modifier, 0, 0, min, max);
	}

	private void sendRollMessage(Player player, String label, List<Integer> rolls, int modifier, int profBonus, int advState, int min, int max) {
		// Build the colored rolls component
		Component rollsComponent = Component.empty();
		for (int i = 0; i < rolls.size(); i++) {
			int val = rolls.get(i);
			NamedTextColor rollColor = (val == max) ? NamedTextColor.YELLOW : (val == min) ? NamedTextColor.RED : NamedTextColor.WHITE;
			rollsComponent = rollsComponent.append(Component.text(Integer.toString(val), rollColor));
			if (i < rolls.size() - 1) {
				rollsComponent = rollsComponent.append(Component.text(", ", NamedTextColor.DARK_GRAY));
			}
		}
		
		// Advantage / Disadvantage component
		Component advComponent = advState > 0
			? Component.text(" (Adv)", NamedTextColor.DARK_GREEN)
			: advState < 0
			? Component.text(" (Dis)", NamedTextColor.DARK_RED)
			: Component.empty();
		
		// Proficiency component
		Component profComponent = Component.empty();
		if (profBonus != 0) {
			NamedTextColor profColor = (profBonus > 0) ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED;
			profComponent = Component.text((profBonus > 0 ? " +" : " -") + Math.abs(profBonus) + " prof", profColor);
		}
		
		// Compose and send the roll line
		Component header = Component
			.text(player.getName() + " rolled " + label + ": ", NamedTextColor.GRAY)
			.append(rollsComponent)
			.append(advComponent)
			.append(profComponent);
		
		Bukkit.getServer().sendMessage(header);
		
		// Total line: include optional (mod ...) and (prof ...) segments
		Component modsComponent = Component.empty();
		if (modifier != 0) {
			NamedTextColor modColor = (modifier > 0) ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;
			modsComponent = modsComponent
				.append(Component.text("(mod ", NamedTextColor.GRAY))
				.append(Component.text(Integer.toString(modifier), modColor))
				.append(Component.text(") ", NamedTextColor.GRAY));
		}
		if (profBonus != 0) {
			NamedTextColor profColor = (profBonus > 0) ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;
			modsComponent = modsComponent
				.append(Component.text("(prof ", NamedTextColor.GRAY))
				.append(Component.text(Integer.toString(profBonus), profColor))
				.append(Component.text(") ", NamedTextColor.GRAY));
		}
		
		int total = rolls.stream().mapToInt(Integer::intValue).sum() + ((modifier + profBonus) * rolls.size());
		
		Component totalComponent = Component
			.text("Total ", NamedTextColor.GRAY)
			.append(modsComponent)
			.append(Component.text("= ", NamedTextColor.GRAY))
			.append(Component.text(Integer.toString(total), NamedTextColor.GOLD));
		
		Bukkit.getServer().sendMessage(totalComponent);
	}
	
// Normalize stat input to match CharacterSheet keys
	private String normalizeStatName(String input) {
		String[] parts = input.trim().toLowerCase().split(" ");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].isEmpty()) continue;
			sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
			if (i < parts.length - 1) sb.append(" ");
		}
		return sb.toString();
	}
}
