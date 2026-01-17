package lgs.mctt.commands;

import lgs.mctt.MCTT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * PossessionHandler - simplified, functional possession system
 */
public class PossessionHandler {
	
	private final Plugin plugin;
	
	/** player UUID -> active possession session */
	private final Map<UUID, PossessionSession> activeSessions = new HashMap<>();
	
	/** target UUID -> possessor player UUID */
	private final Map<UUID, UUID> targetToPlayer = new HashMap<>();
	
	private record PossessionSession(
		Player player,
		Entity target,
		BukkitTask task,
		long startedAt,
		UUID sessionId
	) {}
	
	public PossessionHandler(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public boolean isPossessing(Player player) {
		return activeSessions.containsKey(player.getUniqueId());
	}
	
	/** Start or stop possession */
	public void handlePossess(Player player, Entity target) {
		if (isPossessing(player)) { stopPossessing(player); return; }
		if (target == null || !target.isValid()) { player.sendMessage("§7No valid target."); return; }
		if (target instanceof Player && !MCTT.isDM(player)) { player.sendMessage("§cOnly a DM can possess other players."); return; }
		
		UUID targetId = target.getUniqueId();
		if (targetToPlayer.containsKey(targetId)) {
			player.sendMessage("§cEntity already possessed!"); return;
		}
		
		// Clone entity
		//Entity cloneEntity = cloneEntity(target);
		//if (cloneEntity == null) { player.sendMessage("§cFailed to clone entity."); return; }
		
		// Hide real target and show clone only to possessor
		MCTT.HideEntityExcept(player);
		MCTT.HideEntityFrom(target, player);
		
		// Toggle player physics/collision
		player.setCollidable(false);
		target.setNoPhysics(true);
		target.setGravity(false);
		
		player.teleport(target.getLocation());
		player.sendActionBar(Component.text("You are now possessing " + target.getName() + ".").color(NamedTextColor.LIGHT_PURPLE));
		
		// Task: sync target + clone to player + copy items
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (!player.isOnline() || !target.isValid()) { stopPossessing(player); return; }
			
			Location loc = player.getLocation();
			
			// Teleport target + clone
			try { target.teleport(loc); target.setVelocity(new org.bukkit.util.Vector(0,0,0)); } catch (Exception ignored) {}
			
			// Copy main/off-hand if needed
			if (target instanceof LivingEntity livingTarget) {
				ItemStack main = player.getInventory().getItemInMainHand();
				ItemStack off  = player.getInventory().getItemInOffHand();
				if (!main.equals(livingTarget.getEquipment().getItemInMainHand()))
					livingTarget.getEquipment().setItemInMainHand(main);
				if (!off.equals(livingTarget.getEquipment().getItemInOffHand()))
					livingTarget.getEquipment().setItemInOffHand(off);
			}
			
		}, 1L, 1L);
		
		PossessionSession session = new PossessionSession(player, target, task, System.currentTimeMillis(), UUID.randomUUID());
		activeSessions.put(player.getUniqueId(), session);
		targetToPlayer.put(targetId, player.getUniqueId());
	}
	
	/** Stop possession by player */
	public void stopPossessing(Player player) {
		PossessionSession session = activeSessions.remove(player.getUniqueId());
		if (session == null) return;
		
		try { if (session.task() != null) session.task().cancel(); } catch (Exception ignored) {}
		
		MCTT.ShowEntityTo(session.target(), player);
		MCTT.ShowEntityTo(session.player(), null);
		
		try {
			session.target().setNoPhysics(false);
			session.target().setGravity(true);
			session.player().setCollidable(true);
		} catch (Exception ignored) {}
		
		targetToPlayer.remove(session.target().getUniqueId());
		player.sendActionBar(Component.text("The Possession Ends...").color(NamedTextColor.DARK_GRAY));
	}
	
//	/** Clone entity with same class, copying equipment if living */
//	private Entity cloneEntity(Entity original) {
//		if (original == null || !original.isValid()) return null;
//		World world = original.getWorld();
//		Entity cloneEntity;
//		try {
//			cloneEntity = world.spawn(original.getLocation(), original.getType().getEntityClass());
//		} catch (Exception ex) { return null; }
//
//		if (cloneEntity instanceof LivingEntity livingClone && original instanceof LivingEntity livingOrig) {
//			try {
//				livingClone.getEquipment().setItemInMainHand(livingOrig.getEquipment().getItemInMainHand());
//				livingClone.getEquipment().setItemInOffHand(livingOrig.getEquipment().getItemInOffHand());
//			} catch (Exception ignored) {}
//			livingClone.setAI(false);
//			livingClone.setCollidable(false);
//		}
//
//		cloneEntity.teleport(original.getLocation().add(0, 0.01, 0));
//		return cloneEntity;
//	}
	
	/** Convenience: is player a DM */

	
	/** Cleanup invalid sessions */
	public void cleanupSessions() {
		List<UUID> remove = new ArrayList<>();
		for (var e : activeSessions.entrySet()) {
			PossessionSession s = e.getValue();
			if (!s.player().isOnline() || !s.target().isValid()) {
				try { if (s.task() != null) s.task().cancel(); } catch (Exception ignored) {}
				//try { if (s.cloneEntity() != null && s.cloneEntity.isValid()) s.cloneEntity().remove(); } catch (Exception ignored) {}
				remove.add(e.getKey());
				targetToPlayer.remove(s.target().getUniqueId());
			}
		}
		for (UUID u : remove) activeSessions.remove(u);
	}
}
