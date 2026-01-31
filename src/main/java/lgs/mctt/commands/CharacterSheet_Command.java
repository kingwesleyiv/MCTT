package lgs.mctt.commands;

import lgs.mctt.MCTT;
import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.characters.Stat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CharacterSheet_Command {
	
	public final CharacterSheet_Manager manager;
	
	public CharacterSheet_Command(CharacterSheet_Manager manager) {
		this.manager = manager;
	}
	
	// --- Create a new sheet ---
	public int createSheet(Entity target, String sheetName, int level, String dndclass) {
		CharacterSheet existing = manager.load(sheetName);
		if (existing != null) {
			target.sendMessage("§cSheet '" + sheetName + "' already exists.");
			return 0;
		}
		
		CharacterSheet sheet = new CharacterSheet(sheetName, level, dndclass);
		manager.save(sheet);
		manager.assignSheet(target.getUniqueId(), sheetName); // assign to player
		target.sendMessage("§aCreated and assigned sheet §b" + sheetName);
		return 1;
	}
	
	// --- Delete a sheet ---
	public int deleteSheet(Player player, String sheetName) {
		CharacterSheet sheet = manager.load(sheetName);
		if (sheet == null) {
			player.sendMessage("§cSheet '" + sheetName + "' does not exist.");
			return 0;
		}
		for (Map.Entry<UUID, String> entry : manager.assignedSheets.entrySet()) {
			if (entry.getValue().equals(sheetName)) {
				manager.assignedSheets.remove(entry.getKey());
			}
		}
		
		manager.delete(sheetName);
		player.sendMessage("§cDeleted sheet §b" + sheetName);
		return 1;
	}
	
	// --- View a sheet ---
	public int viewSheet(Player player, String sheetName, String statFilter) {
		CharacterSheet sheet = manager.load(sheetName);
		if (sheet == null) {
			player.sendMessage("§cSheet '" + sheetName + "' not found.");
			return 0;
		}
		
		player.sendMessage("§6===== " + sheet.getName() + " (Lvl " + sheet.getLevel() + ") =====");
		
		Map<String, Stat> stats = sheet.getAllStats();
		boolean found = false;
		
		for (Map.Entry<String, Stat> entry : stats.entrySet()) {
			String key = entry.getKey();
			Stat value = entry.getValue();
			
			if (statFilter != null && !key.equals(statFilter)) continue;
			found = true;
			
			player.sendMessage("§e" + key + ": §b" + value.formatString());
		}
		
		if (statFilter != null && !found) {
			player.sendMessage("§cStat '" + statFilter + "' not found.");
		}
		
		return 1;
	}
	
	// --- Edit a stat or level ---
	public int edit(Player player, String sheetName, String statName, String newValue) {
		CharacterSheet sheet = manager.load(sheetName);
		if (sheet == null) {
			player.sendMessage("§cSheet '" + sheetName + "' does not exist.");
			return 0;
		}
		if (sheet.playerId != player.getUniqueId() && !MCTT.isDM(player)) {
			player.sendMessage("§cYou do not have permission to edit this sheet.");
			return 0;
		}
		
		if (statName.equals("level")) {
			int lvl = Integer.parseInt(newValue);
			sheet.setLevel(lvl);
			manager.save(sheet);
			player.sendMessage("§aUpdated level of §b" + sheetName + " §ato §b" + newValue);
			return 1;
		}

		if (statName.equals("name")){
			String oldName = sheet.getName();
			sheet.setName(newValue);
			manager.save(sheet);
			player.sendMessage("§aUpdated name of sheet §b" + oldName + " §ato §b" + newValue);
			return 1;
		}

		if (statName.equals("class")){
			sheet.setDndClass(newValue);
			manager.save(sheet);
			player.sendMessage("§aUpdated class of sheet §b" + sheetName + " §ato §b" + newValue);
			return 1;
		}
		
		// Normalize input to lower case for lookup
		Stat stat = sheet.getAllStats().entrySet().stream()
			.filter(e -> e.getKey().equals(statName))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElse(null);
		
		if (stat == null) {
			player.sendMessage("§cStat '" + statName + "' not found on sheet.");
			return 0;
		}

		if (sheet.isSkill(statName)) {

			int bonus = 0;
			try { bonus = Integer.parseInt(newValue); }
			catch (NumberFormatException e) {
				player.sendMessage("§cInvalid bonus value. Must be an integer.");
				return 0;
			}
			stat.setBonus(bonus);
			player.sendMessage("§aUpdated '" + statName + "' bonus value.");
			return 1;
		}

		stat.setValue(newValue);
		manager.save(sheet);
		player.sendMessage("§aUpdated stat §b" + statName + " §aof sheet §b" + sheetName + " §ato §b" + newValue);
		return 1;
	}
	
	public int assignSheet(CommandSender sender, Entity target, String sheetName) {
		CharacterSheet sheet = manager.load(sheetName);
		if (sheet == null) {
			sender.sendMessage("§cSheet '" + sheetName + "' does not exist.");
			return 0;
		}
		manager.assignSheet(target.getUniqueId(), sheetName);
		sender.sendMessage("§aAssigned sheet §b" + sheetName + " §ato §b" + target.getName());
		
		return 1;
	}
	
	// --- Get or auto-create a sheet for a player ---
	public CharacterSheet getOrCreateSheetForPlayer(Player player) {
		UUID uuid = player.getUniqueId();
		CharacterSheet sheet = manager.getAssignedSheet(uuid);
		if (sheet == null) {
			// Auto-create default sheet with player name
			String defaultName = player.getName() + "_sheet";
			sheet = new CharacterSheet(defaultName, 1, "NPC");
			manager.save(sheet);
			manager.assignSheet(uuid, defaultName);
			player.sendMessage("§aNo sheet found. Created default sheet §b" + defaultName);
		}
		return sheet;
	}
}
