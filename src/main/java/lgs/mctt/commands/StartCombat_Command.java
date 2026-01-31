package lgs.mctt.commands;

import lgs.mctt.MCTT;
import lgs.mctt.characters.CharacterSheet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class StartCombat_Command{

	private final JavaPlugin plugin;
	private final Set<LivingEntity> combatEntities = new HashSet<>();
	// Scoreboard objective for tracking initiative
	private Objective initScoreObjective;
	private final Map<UUID, Integer> initiativeRolls = new HashMap<>();
	private final Map<UUID, String> initiativeNames = new HashMap<>();
	
	private BukkitTask combatTask;

	// Modifier names for attribute modifiers
//	private static final NamespacedKey MOVEMENT_SPEED_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_movement_speed");
//	private static final NamespacedKey FLYING_SPEED_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_flying_speed");
//	private static final NamespacedKey JUMP_STRENGTH_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_jump_strength");

	public StartCombat_Command(JavaPlugin plugin) {
		this.plugin = plugin;
	}



	public int onCommand(CommandSender sender, List<LivingEntity> target) {
		if (!(sender instanceof Player player && MCTT.isDM(player))) {
			sender.sendMessage("§aOnly DMs can run this command!");
			return 0;
		}
		
		if (target != null) {
			for (LivingEntity entity : target) {
				boolean wasPaused = combatEntities.contains(entity);
				toggleFreeze(entity);
				if (entity instanceof Player p) {
					entity.sendMessage("§eYou have been " + (wasPaused ? "un-paused!" : "paused!"));
					player.sendMessage("§a" + entity.getName() + (wasPaused ? " un-paused!" : " paused!"));
				}
			}
			return 1;
		}
		
		// Default: area toggle with range 20
		toggleAreaCombat(player, 100);
		return 1;
	}

	// Toggle freeze for a single entity
	private void toggleFreeze(LivingEntity entity) {
		if (combatEntities.contains(entity)) {
			unpauseEntity(entity);
		} else {
			pauseEntity(entity);
		}
	}

	private void toggleAreaCombat(Player player, int range) {
		if (combatTask != null) {
			stopCombat();
			player.sendMessage("§aEntities un-paused.");
			return;
		}
		
		Location center = player.getLocation();
		for (Entity e : center.getWorld().getNearbyEntities(center, range, range, range)) {
			if (!(e instanceof LivingEntity living)) continue;
			if (living.equals(player)) continue;
			
			pauseEntity(living);
		}
		
		player.sendMessage("§ePaused " + combatEntities.size() + " entities. Rolling initiative...");
		initScoreboard();
		rollAll();
	}

	// Add entity to frozen set and start freeze task if needed
	private void pauseEntity(LivingEntity entity) {
		combatEntities.add(entity);
		entity.addScoreboardTag("PAUSED");
		entity.addScoreboardTag("COMBAT");
		
		if (combatTask == null) {
			// Send affected players a message that players has begun
			for (LivingEntity e : combatEntities) {
				if (e instanceof Player p) {
					p.sendMessage("§ePaused for Initiative");
				}
			}
			combatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::combatTick, 0L, 1L);
		}
	}

	// Remove entity from frozen set and reset movement/gravity
	private void unpauseEntity(LivingEntity entity) {
		combatEntities.remove(entity);

		if (entity.isValid()) {
			entity.removeScoreboardTag("PAUSED");
			entity.removeScoreboardTag("COMBAT");
		}
		
		// Cancel task if no entities remain
		if (combatEntities.isEmpty() && combatTask != null) {
			combatTask.cancel();
			combatTask = null;
		}
	}

	// Apply freezing effects each tick
	private void combatTick() {
		Iterator<LivingEntity> it = combatEntities.iterator();
		while (it.hasNext()) {
			LivingEntity entity = it.next();
			if (!entity.isValid() || entity.isDead()) { it.remove(); continue;}
			
		}
	}
	
	private void stopCombat() {
		clearInitiative();
		
		if (combatTask != null) {
			combatTask.cancel();
			combatTask = null;
		}
		for (LivingEntity entity : new HashSet<>(combatEntities)) {
			if (!entity.isValid()) continue;
			unpauseEntity(entity);
		}
		
		combatEntities.clear();
	}
	
	private void initScoreboard() {
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		
		if (initScoreObjective != null) {
			initScoreObjective.unregister();
		}
		
		initScoreObjective = board.registerNewObjective(
			"initiative",
			Criteria.DUMMY,
			Component.text("Initiative").color(NamedTextColor.GOLD)
		);
		
		initScoreObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		initiativeRolls.clear();
	}
	
	private void rollAll() {
		if (initScoreObjective == null) return;
		
		for (Entity e : combatEntities) {
			if (!(e instanceof Player player)) continue;
			if (!MCTT.isPC(player)) continue;
			
			int roll = rollInitiative(player, 0);
			initiativeRolls.put(player.getUniqueId(), roll);
		}
	}
	
	public void clearInitiative() {
		if (initScoreObjective != null) {
			initScoreObjective.unregister();
			initScoreObjective = null;
		}
		initiativeRolls.clear();
	}

	public void removeInitiativeByName(String name) {
		initScoreObjective.getScore(name).resetScore();
	}
	public void removeInitiative(Entity entity) {
		initScoreObjective.getScoreFor(entity).resetScore();
	}
	
	public int rollInitiative(Entity target, int modifier) {
		if (initScoreObjective == null) return 0;

		int roll = ThreadLocalRandom.current().nextInt(1, 21) + modifier;
		String name = getInitiativeName(target);

		CharacterSheet character = MCTT.charMGR().getSheetByUUID(target.getUniqueId());
		if (character != null) {
			name = character.getName();
			int bonus = character.getTotal("Initiative");
			int advantage = character.getAdvantage("Initiative");
			if (advantage != 0) {
				int[] advRolls = new int[Math.abs(advantage)+1];
				advRolls[0] = roll + bonus;
				for (int i = Math.abs(advantage); i > 0; i--) {
					advRolls[i] = ThreadLocalRandom.current().nextInt(1, 21) + bonus + modifier;
				}
				if (advantage > 0) {
					Bukkit.getServer().sendMessage(Component.text(
						name + " rolled initiative (With Advantage): " + advRolls.toString()).color(NamedTextColor.YELLOW));
					roll = Arrays.stream(advRolls).max().getAsInt();
				} else {
					Bukkit.getServer().sendMessage(Component.text(
						name + " rolled initiative (With Disadvantage): " + advRolls.toString()).color(NamedTextColor.DARK_RED));
					roll = Arrays.stream(advRolls).min().getAsInt();
				}
			}
		} else {
			Bukkit.getServer().sendMessage(Component.text(
				name + " rolled initiative: " + roll).color(NamedTextColor.WHITE));
		}
		initScoreObjective.getScore(name).setScore(roll);
		return roll;
	}

public String getInitiativeName(Entity entity) {
	return initiativeNames.computeIfAbsent(entity.getUniqueId(), id -> {
		String base = entity instanceof Player p ? p.getName() :
			entity.customName() == null || entity.customName().examinableName().equals("") ?
				entity.getType().name() : entity.customName().examinableName();
		
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		
		String name = base;
		int i = 0;
		
		if (board != null) {
			if (board.getEntries().contains(name))
				while (board.getEntries().contains(name))
					name = base + "_" + i++;
		} else {
			// No scoreboard available (unlikely) - append part of the UUID to ensure uniqueness
			name = base + "_" + id.toString().substring(0, 4);
		}
		return name;
	});
}
	
}
