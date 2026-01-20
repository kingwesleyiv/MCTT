package lgs.mctt.commands;

import lgs.mctt.MCTT;
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
		
		player.sendMessage("§ePaused " + combatEntities.size() + " entities.");
		initScoreboard();
		rollInitiative((MCTT) plugin);
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
	
	private void rollInitiative(MCTT plugin) {
		if (initScoreObjective == null) return;
		
		List<Map.Entry<Player, Integer>> results = new ArrayList<>();
		
		for (Entity e : combatEntities) {
			if (!(e instanceof Player player)) continue;
			if (!MCTT.isPC(player)) continue;
			
			int roll = MCTT.rollCMD.rollStat(player, "Initiative", 0, 0);
			initiativeRolls.put(player.getUniqueId(), roll);
			results.add(Map.entry(player, roll));
		}
		
		results.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		
		for (Map.Entry<Player, Integer> entry : results) {
			initScoreObjective
				.getScore(entry.getKey().getName())
				.setScore(entry.getValue());
		}
	}
	
	public void clearInitiative() {
		if (initScoreObjective != null) {
			initScoreObjective.unregister();
			initScoreObjective = null;
		}
		initiativeRolls.clear();
	}
	
	public void forceInitiative(Entity entity, int modifier) {
		if (initScoreObjective == null) return;
		
		int roll;
		
		if (entity instanceof Player player && MCTT.isPC(player)) {
			roll = MCTT.rollCMD.rollStat(player, "Initiative", 0, 0);
		} else {
			roll = ThreadLocalRandom.current().nextInt(1, 21);
		}
		
		roll += modifier;
		
		String entryName = entity.getName();
		
		initScoreObjective.getScore(entryName).setScore(roll);
	}

public String getInitiativeName(Entity entity) {
	return initiativeNames.computeIfAbsent(entity.getUniqueId(), id -> {
		String base = entity instanceof Player p
			              ? p.getName() : entity.getType().name();
		
		// Try to use the main scoreboard to ensure uniqueness; fall back to UUID short if unavailable
		ScoreboardManager mgr = Bukkit.getScoreboardManager();
		Scoreboard board = mgr != null ? mgr.getMainScoreboard() : null;
		
		String name = base;
		int i = 0;
		
		if (board != null) {
			while (board.getEntries().contains(name)) {
				name = base + i++;
			}
		} else {
			// No scoreboard available (unlikely) - append part of the UUID to ensure uniqueness
			name = base + "-" + id.toString().substring(0, 8);
		}
		return name;
	});
}
	
}
