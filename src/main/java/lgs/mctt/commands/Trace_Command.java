package lgs.mctt.commands;

import lgs.mctt.MCTT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import lgs.mctt.util.Raycaster;

import java.util.UUID;

public class Trace_Command{

	public Raycaster raycaster = new Raycaster();
	
	public int onCommand(CommandSender sender, int range) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can run this command!");
			return 0;
		}
		
		RayTraceResult ray = raycaster.spawnParticleBeam(player, range);
		
		double hitDistFeet;
		if (ray != null) {
			hitDistFeet = getMidDist(player, ray.getHitPosition().toLocation(player.getWorld()));
		} else {
			hitDistFeet = range;
		}
		
		TextColor distColor = TextColor.color(getDistRGB(hitDistFeet));
		
		// Display distance in feet above XP bar
		if (ray == null) {
			player.sendActionBar(
				Component.text("No Hit ( >").color(NamedTextColor.GRAY)
					.append(Component.text(range).color(TextColor.color(getDistRGB(range))))
					.append(Component.text(" ft.)").color(NamedTextColor.GRAY))
			);
		} else {
			if (ray.getHitEntity() != null && MCTT.isDM(player)) {
				UUID hitId = ray.getHitEntity().getUniqueId();
				player.sendMessage(Component.text("Name: ").append(Component.text(ray.getHitEntity().getName())).color(distColor));
				player.sendMessage(Component.text("Click Here to copy UUID. ").color(NamedTextColor.AQUA)
					.clickEvent(ClickEvent.copyToClipboard(hitId.toString()))
					.color(NamedTextColor.AQUA)
				);
			}
			player.sendActionBar(Component.text(String.format("Distance: %.0f ft", hitDistFeet)).color(distColor));
		}
		
		
		return 1;
	}

public static int getDistRGB(double distanceFeet) {
	int color;
	switch (
		distanceFeet <= 10 ? 1 :
			distanceFeet <= 15 ? 2 :
				distanceFeet <= 20 ? 3 :
					distanceFeet <= 30 ? 4 :
						distanceFeet <= 60 ? 5 :
							distanceFeet <= 90 ? 6 :
								distanceFeet <= 120 ? 7 :
									8) {
		case 1 -> color = Color.fromRGB(0, 128, 0).asRGB();
		case 2 -> color = Color.fromRGB(0, 255, 0).asRGB();
		case 3 -> color = Color.fromRGB(128, 255, 0).asRGB();
		case 4 -> color = Color.fromRGB(255, 255, 0).asRGB();
		case 5 -> color = Color.fromRGB(255, 128, 0).asRGB();
		case 6 -> color = Color.fromRGB(255, 0, 0).asRGB();
		case 7 -> color = Color.fromRGB(128, 0, 255).asRGB();
		default -> color = Color.fromRGB(255, 255, 255).asRGB();
	}
	return color;
}

public static float getMidDist(Player player, Location target) {
		Location start;
		try { start = player.getLocation().add(0, (player.getAttribute(Attribute.SCALE).getValue()*1.8f)/2, 0);}
		catch (Exception e) { start = player.getLocation().add(0, 0.90, 0); }
		
		return (float)start.distance(target) * 3.28084f;
	}
	
}
