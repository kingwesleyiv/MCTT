package lgs.mctt.commands;

import lgs.mctt.MCTT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * PossessionHandler - simplified, functional possession system
 */
public class PossessionHandler {
	
	private final Plugin plugin;
	
// player UUID, Possession map.
	public final Map<UUID, PossessionSession> activeSessionsData = new HashMap<>();
// Player UUID, Target UUID map.
	public final Map<UUID, UUID> possessionMap = new HashMap<>();
	
	public record PossessionSession(
		Player player,
		Entity target,
		BukkitTask task,
		long startedAt,
		UUID sessionId
	) { }
	
	public PossessionHandler(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public boolean isPossessing(Player player) {
		return activeSessionsData.containsKey(player.getUniqueId());
	}
	
	/** Start or stop possession */
	public void handlePossess(Player player, Entity target) {
		if (isPossessing(player)) { stopPossessing(player); return; }
		if (target == null || !target.isValid()) { player.sendMessage("§7Invalid target."); return; }
		if (target instanceof Player && !MCTT.isDM(player)) { player.sendMessage("§cOnly a DM can possess other players."); return; }
		
		if (possessionMap.containsValue(target.getUniqueId())) {
			player.sendMessage("§cEntity already possessed!");
			return;
		}
		
		// Hide target from possessor.
		if (target instanceof Player) {
			MCTT.sendInvisTarget(player, (LivingEntity)target);
		} else MCTT.HideEntityFrom(target, player);
		
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
		
		// Toggle player physics/collision
		player.setCollidable(false);
		target.setNoPhysics(true);
		target.setInvulnerable(true);
		target.setGravity(false);
		
		player.teleport(target.getLocation());
		player.sendActionBar(Component.text("You are now possessing " + target.getName() + ".").color(NamedTextColor.LIGHT_PURPLE));
		
		// Task: sync target + copy items
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (!player.isOnline() || !target.isValid()) { stopPossessing(player); return; }
			
			Location loc = player.getLocation();
			
			// Teleport target
			try { target.teleport(loc); target.setVelocity(new org.bukkit.util.Vector(0,0,0)); } catch (Exception ignored) {}
			
			// Copy main/off-hand if needed
			try{if (target instanceof LivingEntity livingTarget) {
				ItemStack main = player.getInventory().getItemInMainHand();
				ItemStack off = player.getInventory().getItemInOffHand();
				ItemStack head = player.getInventory().getHelmet();
				ItemStack body = player.getInventory().getChestplate();
				ItemStack legs = player.getInventory().getLeggings();
				ItemStack feet = player.getInventory().getBoots();
				
				if (!main.equals(livingTarget.getEquipment().getItemInMainHand()))
					livingTarget.getEquipment().setItemInMainHand(main);
				if (!off.equals(livingTarget.getEquipment().getItemInOffHand()))
					livingTarget.getEquipment().setItemInOffHand(off);
				if (!head.equals(livingTarget.getEquipment().getHelmet()))
					livingTarget.getEquipment().setHelmet(head);
				if (!body.equals(livingTarget.getEquipment().getChestplate()))
					livingTarget.getEquipment().setChestplate(body);
				if (!legs.equals(livingTarget.getEquipment().getLeggings()))
					livingTarget.getEquipment().setLeggings(legs);
				if (!feet.equals(livingTarget.getEquipment().getBoots()))
					livingTarget.getEquipment().setBoots(feet);
			}} catch (Exception ignored) {}
			
		}, 1L, 1L);
		
		PossessionSession session = new PossessionSession(player, target, task, System.currentTimeMillis(), UUID.randomUUID());
		activeSessionsData.put(player.getUniqueId(), session);
		possessionMap.put(player.getUniqueId(), target.getUniqueId());
	}
	
	/** Stop possession by player */
	public void stopPossessing(Player player) {
		PossessionSession session = activeSessionsData.remove(player.getUniqueId());
		if (session == null) return;
		
		try { if (session.task() != null) session.task().cancel(); } catch (Exception ignored) {}
		
		MCTT.ShowEntityTo(session.target(), player);
		MCTT.ShowEntityTo(session.player(), null);
		if(session.target() instanceof Player) MCTT.removeInvisTarget(player, (LivingEntity)session.target());
		
		try {
			session.target().setNoPhysics(false);
			session.target().setGravity(true);
			session.player().setCollidable(true);
		} catch (Exception ignored) {}
		
		possessionMap.remove(player.getUniqueId());
		player.sendActionBar(Component.text("The Possession Ends...").color(NamedTextColor.DARK_GRAY));
	}
	
	public void stopAllPossessions() {
		for (UUID u : new ArrayList<>(activeSessionsData.keySet())) {
			Player p = Bukkit.getPlayer(u);
			if (p != null) stopPossessing(p);
		}
		cleanupSessions();
	}
	
	/** Cleanup invalid sessions */
	public void cleanupSessions() {
		for (var e : activeSessionsData.entrySet()) {
			PossessionSession s = e.getValue();
			try { if (s.task() != null) s.task().cancel(); } catch (Exception ignored) {}
			possessionMap.remove(s.player().getUniqueId());
		}
		activeSessionsData.clear();
		possessionMap.clear();
	}
}
