package lgs.mctt.players;

import lgs.mctt.MCTT;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StateMachine extends BukkitRunnable {
	
	private final Map<UUID, TextDisplay> nameplates = new HashMap<>();
	private static final NamespacedKey SPEED_ZERO_KEY = NamespacedKey.minecraft("speed_lock");
	
	
	@Override
	public void run() {
		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity entity : world.getLivingEntities()) {
				handleEntity(entity);
			}
		}
		
		// Cleanup nameplates for entities that no longer exist
		nameplates.entrySet().removeIf(entry -> {
			Entity e = Bukkit.getEntity(entry.getKey());
			if (e == null || e.isDead()) {
				entry.getValue().remove();
				return true;
			}
			return false;
		});
	}
	
	private void handleEntity(LivingEntity entity) {
		Set<String> tags = entity.getScoreboardTags();
		
		// --- Handle pose and tag-based display ---
		List<String> displayTags = new ArrayList<>();
		for (String tag : tags) {
			switch (tag) {
				case "PRONE" -> {
					displayTags.add(Color.ORANGE + "Prone");
					if (entity.getPose() != Pose.SWIMMING) {
						if (entity instanceof Player) {
							entity.setPose(Pose.SWIMMING, false);
						} else {
							entity.setPose(Pose.SLEEPING, true);
						}
					}
				}
				case "DYING" -> {
					displayTags.add(Color.RED + "Dying");
					if (entity.getPose() != Pose.SLEEPING)
						entity.setPose(Pose.SLEEPING, false);
				}
				case "UNCONSCIOUS" -> {
					displayTags.add(Color.FUCHSIA + "Unconscious");
					if (entity.getPose() != Pose.SLEEPING)
						entity.setPose(Pose.SLEEPING, false);
				}
				case "GRAPPLED" -> displayTags.add(Color.BLUE + "Grappled");
				case "STUNNED" -> displayTags.add(Color.FUCHSIA + "Stunned");
				case "BLINDED" -> displayTags.add(Color.GRAY + "Blinded");
				case "CHARMED" -> displayTags.add(Color.FUCHSIA + "Charmed");
				case "DEAFENED" -> displayTags.add(Color.GRAY + "Deafened");
				case "FRIGHTENED" -> displayTags.add(Color.YELLOW + "Frightened");
				case "INCAPACITATED" -> displayTags.add(Color.MAROON + "Incapacitated");
				case "INVISIBLE" -> displayTags.add(Color.WHITE + "Invisible");
				case "PARALYZED" -> displayTags.add(Color.PURPLE + "Paralyzed");
				case "PETRIFIED" -> displayTags.add(Color.GRAY + "Petrified");
				case "POISONED" -> displayTags.add(Color.GREEN + "Poisoned");
				case "BURNING" -> {
					entity.setVisualFire(true);
					displayTags.add(Color.ORANGE + "Burning");
				}
				case "RESTRAINED" -> displayTags.add(Color.NAVY + "Restrained");
				case "EXHAUSTED" -> displayTags.add(Color.MAROON + "Exhausted");
				case "FROZEN" -> displayTags.add(Color.TEAL + "Frozen");
				
			}
		}
		
		updateNameplate(entity, displayTags);
		
		if (entity instanceof Player player) {
			handlePlayerStates(player, tags);
		}
		
//		// --- Handle invisibility with DM team visibility control ---
//		if (tags.contains("INVISIBLE")) {
//			if (!entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
//				entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
//			}
//			if (!entity.hasPotionEffect(PotionEffectType.GLOWING)) {
//				entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
//			}
//
//			for (Player viewer : Bukkit.getOnlinePlayers())
//				if (!viewer.getScoreboard().getEntityTeam(viewer).getName().equals("DM"))
//					if(viewer.canSee(entity)) MCTT.HideEntityFrom(entity, viewer);
//
//			// Hide nameplate for non-DM players
//			TextDisplay nameplate = nameplates.get(entity.getUniqueId());
//			if (nameplate != null && !nameplate.isDead()) {
//				nameplate.setVisibleByDefault(false);
//			}
//
//
//		} else {
//			// Remove invisibility and restore visibility
//			entity.removePotionEffect(PotionEffectType.INVISIBILITY);
//			entity.removePotionEffect(PotionEffectType.GLOWING);
//
//			MCTT.ShowEntityTo(entity, null);
//
//			// Restore nameplate visibility
//			TextDisplay nameplate = nameplates.get(entity.getUniqueId());
//			if (nameplate != null && !nameplate.isDead()) {
//				nameplate.setVisibleByDefault(true);
//			}
//		}
		
	}
	
	private void updateNameplate(LivingEntity entity, List<String> lines) {
		TextDisplay display = nameplates.get(entity.getUniqueId());
		
		if (lines.isEmpty()) {
			if (display != null) {
				display.remove();
				nameplates.remove(entity.getUniqueId());
			}
			return;
		}
		
		String text = String.join("\n", lines);
		
		if (display == null || display.isDead()) {
			display = (TextDisplay) entity.getWorld().spawnEntity(
				entity.getLocation().add(0, entity.getHeight() + 0.5, 0),
				EntityType.TEXT_DISPLAY
			);
			display.setBillboard(Display.Billboard.CENTER);
			display.setSeeThrough(true);
			display.setPersistent(false);
			display.setDefaultBackground(false);
			nameplates.put(entity.getUniqueId(), display);
		}
		
		display.setText(text);
		display.teleport(entity.getLocation().add(0, entity.getHeight() + 0.5, 0));
	}
	
	private void handlePlayerStates(Player player, Set<String> tags) {
		AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
		
		// --- UNCONSCIOUS ---
		if (tags.contains("UNCONSCIOUS")) {
			if (!player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
			}
			
			if (speedAttr != null && speedAttr.getModifier(SPEED_ZERO_KEY) == null) {
				speedAttr.addModifier(new AttributeModifier(SPEED_ZERO_KEY, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
			}
			
			player.setPose(Pose.SLEEPING, false);
		} else {
			if (speedAttr != null) {
				AttributeModifier mod = speedAttr.getModifier(SPEED_ZERO_KEY);
				if (mod != null) speedAttr.removeModifier(mod);
			}
		}
		
		// --- PRONE ---
		if (tags.contains("PRONE")) {
			if (player.getPose() != Pose.SWIMMING) {
				player.setPose(Pose.SWIMMING, true);
			}
		} else if (tags.contains("SITTING")) {
			if (player.getPose() != Pose.SITTING) {
				player.setPose(Pose.SITTING, true);
			}
		}
		
		// --- FLYING ---
		if (tags.contains("FLYING")) {
			if (!player.getGameMode().equals(GameMode.SPECTATOR)) {
				if (!player.isFlying()) {
					player.setAllowFlight(true);
					player.setFlying(true);
				}
			}
		} else if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
			if (player.isFlying()) player.setFlying(false);
	}
}
