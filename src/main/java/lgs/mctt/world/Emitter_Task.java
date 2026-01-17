package lgs.mctt.world;

import lgs.mctt.util.Clock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Emitter_Task extends BukkitRunnable {
	
	// Hashmap of current emitters from each world.
	private final Map<World, List<ArmorStand>> emitterCache = new HashMap<>();
	// Clock object for ticking random delays.
	Clock tick = new Clock(100, 500, System.currentTimeMillis());
	Clock update = new Clock(2000, 2000, System.currentTimeMillis());
	
	@Override
	public void run() {
		// Update the cache every 2 seconds to avoid constant global checks.
		if (update.check()){
			emitterCache.clear();
			
			for (World world : Bukkit.getWorlds()) {
				List<ArmorStand> emitters = world.getEntitiesByClass(ArmorStand.class)
					.stream()
					.filter(e -> e.getScoreboardTags().contains("campfire"))
					.toList();
				emitterCache.put(world, new ArrayList<>(emitters));
			}
		}
		
		if (tick.check()){
			for (Map.Entry<World, List<ArmorStand>> entry : emitterCache.entrySet()) {
				World world = entry.getKey();
				List<ArmorStand> emitters = entry.getValue();
				
				emitters.removeIf(e -> !e.isValid() || !e.getScoreboardTags().contains("campfire"));
				
				for (ArmorStand stand : emitters) {
					// Randomize slightly to keep it natural.
					if (Math.random() > 0.75) {continue;}
					
					Location loc = stand.getLocation().add(
						(Math.random() - 0.5)*0.3, // slight horizontal jitter
						-0.5, // Just below stand inside blocks.
						(Math.random() - 0.5)*0.3
					);
					
					world.spawnParticle(
						Particle.CAMPFIRE_SIGNAL_SMOKE,
						loc,
						0,  // 0 or 1 particle, doesn't matter
						0, 0.1, 0,  // velocity vector
						(Math.random()*0.3)+0.1,  // minimum speed
						null
					);
				}
			}
		}
	}
}