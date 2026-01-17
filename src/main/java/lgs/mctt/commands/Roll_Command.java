package lgs.mctt.commands;

import lgs.mctt.MCTT;
import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.characters.Stat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Roll_Command {
	
	private final MCTT plugin;
	private final Random rng = new Random();
	private final CharacterSheet_Manager manager;
	
	public Roll_Command(MCTT plugin, CharacterSheet_Manager manager) {
		this.plugin = plugin;
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
		
		sendRollMessage(player, "d" + sides, rolls, 0, 0);
		return 1;
	}
	
	/**
	 * Roll using /r <stat> for the player's assigned sheet
	 * Auto-creates a sheet if missing
	 */
	public int rollStat(Player player, String statName) {
		UUID playerId = player.getUniqueId();
		
		CharacterSheet sheet = manager.getAssignedSheet(playerId);
		if (sheet == null) {
			String defaultName = player.getName() + "_sheet";
			sheet = new CharacterSheet(defaultName, 1f);
			manager.save(sheet);
			manager.assignSheet(playerId, defaultName);
			player.sendMessage(ChatColor.DARK_GRAY + "No sheet found. Created default sheet " + ChatColor.WHITE + defaultName);
		}
		
		String normalizedName = normalizeStatName(statName);
		Stat stat = sheet.getStat(normalizedName);
		if (stat == null) {
			player.sendMessage(ChatColor.RED + "Stat '" + statName + "' not found on your sheet.");
			return 0;
		}
		
		int baseModifier = sheet.getModifier(normalizedName);
		int profBonus = stat.getProficiency();
		int advState = stat.getAdvantage();
		
		// Roll 1d20, account for advantage/disadvantage
		int roll1 = rng.nextInt(20) + 1;
		int roll2 = (advState != 0) ? rng.nextInt(20) + 1 : roll1;
		int finalRoll = roll1;
		if (advState == 1) finalRoll = Math.max(roll1, roll2);
		if (advState == -1) finalRoll = Math.min(roll1, roll2);
		
		List<Integer> rolls = advState != 0 ? List.of(roll1, roll2) : List.of(roll1);
		
		sendRollMessage(player, normalizedName, rolls, baseModifier, profBonus, advState);
		return 1;
	}
	
	/**
	 * Unified function to send roll output
	 */
	private void sendRollMessage(Player player, String label, List<Integer> rolls, int modifier, int profBonus) {
		sendRollMessage(player, label, rolls, modifier, profBonus, 0);
	}
	
	private void sendRollMessage(Player player, String label, List<Integer> rolls, int modifier, int profBonus, int advState) {
		StringBuilder rollsText = new StringBuilder();
		for (int i = 0; i < rolls.size(); i++) {
			int val = rolls.get(i);
			ChatColor rollColor = (val == 20) ? ChatColor.GOLD : (val == 1) ? ChatColor.DARK_AQUA : ChatColor.WHITE;
			rollsText.append(rollColor).append(val);
			if (i < rolls.size() - 1) rollsText.append(ChatColor.DARK_GRAY).append(", ");
		}
		
		String advText = advState == 1 ? " (Adv)" : advState == -1 ? " (Dis)" : "";
		String profText = profBonus != 0 ? (profBonus > 0 ? " +" : " ") + profBonus + " prof" : "";
		
		int total = rolls.get(0);
		if (rolls.size() > 1) total = advState == 1 ? Math.max(rolls.get(0), rolls.get(1)) : Math.min(rolls.get(0), rolls.get(1));
		total += modifier + profBonus;
		
		String modText = "";
		if (modifier != 0) {
			ChatColor modColor = (modifier > 0) ? ChatColor.GREEN : ChatColor.DARK_RED;
			modText = modColor + Integer.toString(modifier) + ChatColor.DARK_GRAY;
		}
		
		String profBonusText = "";
		if (profBonus != 0) {
			ChatColor profColor = (profBonus > 0) ? ChatColor.GREEN : ChatColor.DARK_RED;
			profBonusText = profColor + Integer.toString(profBonus) + ChatColor.DARK_GRAY;
		}
		
		player.sendMessage(ChatColor.DARK_GRAY + label + " rolled - " + rollsText + advText + profText);
		player.sendMessage(ChatColor.DARK_GRAY + "Total " +
			(modText.isEmpty() ? "" : "(+mod " + modText + ") ") +
			(profBonusText.isEmpty() ? "" : "(+prof " + profBonusText + ") ") +
			"= " + ChatColor.GOLD + total);
	}
	
	/**
	 * Normalize stat input to match CharacterSheet keys
	 */
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
