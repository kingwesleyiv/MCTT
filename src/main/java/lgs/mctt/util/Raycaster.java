package lgs.mctt.util;

import lgs.mctt.MCTT;

import lgs.mctt.commands.Trace_Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public final class Raycaster {
	
	public final Map<UUID, ArmorStand> playerMarkers = new HashMap<>();
	
	public void clearMarkers() {
		for (ArmorStand marker : playerMarkers.values()) {
			if (marker != null && !marker.isDead()) {
				marker.remove();
			}
		}
		playerMarkers.clear();
	}

	public RayTraceResult spawnParticleBeam(Player player, double maxDistance) {
		if (maxDistance <= 0) {
			clearMarkers();
		}

		float spacing = 0.5f;
		final double raySize = 0.1;
		
		RayTraceResult result = raycastFromPlayerEye(player, maxDistance / 3.28084, raySize);
		boolean hit = (result != null);
		
		Location eye = player.getEyeLocation();
		World world = player.getWorld();
		
		Location waist;
		try { waist = player.getLocation().add(0, (player.getAttribute(Attribute.SCALE).getValue()*1.8f)/2, 0);}
		catch (Exception e) { waist = player.getLocation().add(0, 0.90, 0); }
		
		Vector dir = eye.getDirection().normalize();
		
		Location end;
		double totalDistance = maxDistance;
		if (hit && waist.distance(result.getHitPosition().toLocation(world)) <= maxDistance / 3.28084) {
			end = result.getHitPosition().toLocation(world);
			totalDistance = waist.distance(end) * 3.28084;
		} else {
			end = waist.clone().add(dir.clone().multiply(maxDistance / 3.28084));
		}
		
		Vector waistDir = end.clone().subtract(waist).toVector().normalize();
		
		// Particle Spawning
		Particle.DustOptions dust = new Particle.DustOptions(hit?Color.fromRGB(Trace_Command.getDistRGB(totalDistance)):Color.GRAY, 1f);
		for (double d = 0.0; d <= totalDistance / 3.28084; d += Math.max(0.01, spacing)) {
			world.spawnParticle(Particle.DUST, waist.clone().add(waistDir.clone().multiply(d)), 1, 0, 0, 0, 0, dust);
		}
		
		manageMarkerEntity(player, end.add(0,0.1f,0), (float)(totalDistance), dust.getColor().asRGB());
		
		return result;
	}
	
	private void manageMarkerEntity(Player player, Location target, float distanceFeet, int distColor) {
		UUID id = player.getUniqueId();
		ArmorStand marker = playerMarkers.get(id);
		if (marker == null) {
			playerMarkers.remove(id);
			ArmorStand newMarker = target.getWorld().spawn(target, ArmorStand.class, markerEntity -> {
				markerEntity.customName(Component.text(Math.round(distanceFeet) + " ft").color(TextColor.color(distColor)).decoration(TextDecoration.BOLD, true));
				markerEntity.setCustomNameVisible(true);
				markerEntity.setMarker(true);
				markerEntity.setInvisible(true);
			});
			marker = newMarker;
			playerMarkers.put(id, newMarker);
			
			// Schedule removal after 20s if not refreshed
			Bukkit.getScheduler().runTaskLater(MCTT.getPlugin(MCTT.class), () -> {
				try { playerMarkers.get(id).remove(); } catch (Exception e) { }
				playerMarkers.remove(id);
			}, 20L * 20); // 20 seconds at 20 ticks per second
			
		} else {
			marker.teleport(target);
			marker.customName(Component.text(Math.round(distanceFeet) + " ft").color(TextColor.color(distColor)).decoration(TextDecoration.BOLD, true));
		}
	}
	
	private RayTraceResult raycastFromPlayerEye(Player player, double maxDistance, double raySize) {
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