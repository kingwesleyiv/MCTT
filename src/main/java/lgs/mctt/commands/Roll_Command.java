package lgs.mctt.commands;

import lgs.mctt.MCTT;
import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.characters.Stat;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
			player.sendMessage(NamedTextColor.RED + "Invalid dice. Usage: /r <count> <sides>");
			return 0;
		}
		
		List<Integer> rolls = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			rolls.add(rng.nextInt(sides) + 1);
		}
		
		int total = rolls.stream().mapToInt(Integer::intValue).sum();
		
		sendRollMessage(player, "d" + sides, rolls, 0, 0, 0);
		return 1;
	}
	
// Use /r to roll random numbers. Can use /r <stat> to roll a character stat (only supports d20 stat rolls at the moment.)
	public int rollStat(Player player, String statName, int bonus, int advantage) {
		int baseModifier = 0;
		int profBonus = bonus;
		int advState = advantage;
		
		UUID playerId = player.getUniqueId();
		CharacterSheet sheet = manager.getAssignedSheet(playerId);
		String normalizedName = "";
		
		if ( sheet == null)
			player.sendMessage(NamedTextColor.DARK_RED + "No sheet found.");
		if (sheet != null) {
			normalizedName = normalizeStatName(statName);
			Stat stat = sheet.getStat(normalizedName);
			if (stat == null) {
				player.sendMessage(NamedTextColor.RED + "Stat '" + statName + "' not found on your sheet.");
				return 0;
			}
			baseModifier = sheet.getModifier(normalizedName);
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
		
		sendRollMessage(player, normalizedName, rolls, baseModifier, profBonus, advState);
		return 1;
	}
	
	private void sendRollMessage(Player player, String label, List<Integer> rolls, int modifier, int profBonus, int advState) {
		sendRollMessage(player, label, rolls, 1, 20, modifier, profBonus, 0);
	}
	
	private void sendRollMessage(Player player, String label, List<Integer> rolls, int min, int max, int modifier, int profBonus, int advState) {
		StringBuilder rollsText = new StringBuilder();
		for (int i = 0; i < rolls.size(); i++) {
			int val = rolls.get(i);
			NamedTextColor rollColor = (val == 20) ? NamedTextColor.GOLD : (val == 1) ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
			rollsText.append(rollColor).append(val);
			if (i < rolls.size() - 1) rollsText.append(NamedTextColor.DARK_GRAY).append(", ");
		}
		
		String advText = advState > 0 ? NamedTextColor.DARK_GREEN+ " (Adv)" : advState < 0 ? NamedTextColor.DARK_RED+ " (Dis)" : "";
		String profText = profBonus != 0 ? (profBonus > 0 ? NamedTextColor.DARK_GREEN+ " +" : NamedTextColor.DARK_RED+ " -") + profBonus + " prof" : "";
		
		int total = rolls.stream().mapToInt(Integer::intValue).sum() + ((modifier + profBonus) * rolls.size());
		
		String modText = "";
		if (modifier != 0) {
			NamedTextColor modColor = (modifier > 0) ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;
			modText = modColor + Integer.toString(modifier) + NamedTextColor.DARK_GRAY;
		}
		
		String profBonusText = "";
		if (profBonus != 0) {
			NamedTextColor profColor = (profBonus > 0) ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;
			profBonusText = profColor + Integer.toString(profBonus) + NamedTextColor.DARK_GRAY;
		}
		
	// Output the actual chat text: //
		Bukkit.getServer().sendRichMessage(player.getName()+ " rolled " + label + ": " + rollsText + advText + profText);
		Bukkit.getServer().sendRichMessage(NamedTextColor.GRAY + "Total " +
			(modText.isEmpty() ? "" : "(mod " + modText + ") ") +
			(profBonusText.isEmpty() ? "" : "(prof " + profBonusText + ") ") +
			"= " + NamedTextColor.GOLD + total);
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
