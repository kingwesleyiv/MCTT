package lgs.mctt.util;

import lgs.mctt.MCTT;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Raycaster {
	
	private static final Map<UUID, ArmorStand> playerMarkers = new HashMap<>();
	
	public static RayTraceResult spawnParticleBeam(Player player, double maxDistance) {
		float spacing = 0.5f;
		final double raySize = 0.1;
		
		RayTraceResult result = raycastFromPlayerEye(player, maxDistance, raySize);
		Location eye = player.getEyeLocation();
		World world = player.getWorld();
		Location feet = player.getLocation();
		Vector dir = eye.getDirection().normalize();
		
		Location end;
		if (result != null) {
			Vector v = result.getHitPosition();
			end = new Location(world, v.getX(), v.getY(), v.getZ());
		} else {
			end = eye.clone().add(dir.clone().multiply(maxDistance));
		}
		
		// Make rainbow dust color.
		int tRGB = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 2000) / 2000f, 1f, 1f); // 2 second (2000 ms) cycle hue
		Color rainbow = Color.fromRGB((tRGB >>16)&0xFF, (tRGB >>8)&0xFF, tRGB &0xFF);
		
		boolean hit = (result != null);
		
		Vector feetDir = end.toVector().subtract(feet.toVector()).normalize(); // vector from feet -> end
		//Vector base = feet.toVector(); // keep a stable copy of the feet vector
		
		double totalDistance = feet.distance(end);
		for (double d = 0.0; d <= totalDistance; d += Math.max(0.01, spacing)) {
			Vector pointVec = feet.toVector().clone().add(feetDir.clone().multiply(d));
			Location loc = new Location(world, pointVec.getX(), pointVec.getY()+0.1f, pointVec.getZ());
			double partDist = loc.distance(feet);
			double pdFoot = partDist * 3.28084;
			
			if (hit) {
				Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0,0,0), 0.5f);
				world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
			} else {
				Particle.DustOptions rainbowDust = new Particle.DustOptions(rainbow, 0.5f);
				world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, rainbowDust);
			}
		}
		
		if (hit) {// Spawn or teleport the marker entity.
			manageMarkerEntity(player, end.subtract(0, 0.7,0));
		} else {// Spawn crit particles at end point.
			world.spawnParticle(Particle.CRIT, end, 20, 0.3, 0.3, 0.3, 0.01);
		}
		
		return result;
	}
	
	private static void manageMarkerEntity(Player player, Location target) {
		UUID id = player.getUniqueId();
		ArmorStand marker = playerMarkers.get(id);
		
		if (marker == null) {
			marker = target.getWorld().spawn(target, ArmorStand.class, markerEntity -> {
				markerEntity.setCanTick(false);
				markerEntity.setSmall(true);
				markerEntity.setMarker(true);
				markerEntity.setInvisible(true);
				markerEntity.setGlowing(true);
				try { markerEntity.getAttribute(Attribute.SCALE).setBaseValue(0.7);
				} catch (Exception e) { player.sendMessage(e.getMessage());}
				markerEntity.setItem (EquipmentSlot.HEAD, new ItemStack(Material.GLASS));
			});
			
			
			playerMarkers.put(id, marker);
			
			// Make invisible to all players except the owner
			MCTT.HideEntityExcept(marker, Collections.singletonList(player));
			
			final ArmorStand markerRef = marker;
			// Schedule removal after 10s if not refreshed
			Bukkit.getScheduler().runTaskLater(MCTT.getPlugin(MCTT.class), () -> {
				ArmorStand current = playerMarkers.get(id);
				if (current != null && current.equals(markerRef) && !current.isDead() && current.isValid()) {
					current.remove();
					playerMarkers.remove(id);
				}
			}, 20L * 10);
			
		} else {
			marker.teleport(target);
		}
	}
	
	private static RayTraceResult raycastFromPlayerEye(Player player, double maxDistance, double raySize) {
		Location eye = player.getEyeLocation();
		World world = player.getWorld();
		
		return world.rayTrace(
			eye,
			eye.getDirection().normalize(),
			maxDistance,
			FluidCollisionMode.NEVER,
			true,
			raySize,
			(Entity e) -> !e.equals(player) && !(playerMarkers.containsValue(e))
		);
	}
}