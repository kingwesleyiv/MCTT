package lgs.mctt.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StartCombat_Command{

	private final JavaPlugin plugin;
	private final Set<LivingEntity> combatEntities = new HashSet<>();
	private BukkitTask combatTask;

	// Modifier names for attribute modifiers
//	private static final NamespacedKey MOVEMENT_SPEED_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_movement_speed");
//	private static final NamespacedKey FLYING_SPEED_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_flying_speed");
//	private static final NamespacedKey JUMP_STRENGTH_FREEZE_KEY = NamespacedKey.minecraft("mctt_freeze_jump_strength");

	public StartCombat_Command(JavaPlugin plugin) {
		this.plugin = plugin;
	}



	public int onCommand(CommandSender sender, List<LivingEntity> target) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§aOnly players can run this command!");
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
	
}
