package lgs.mctt.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import lgs.mctt.util.Raycaster;
import lgs.mctt.MCTT;
import java.util.UUID;

public class Trace_Command{
	
	public int onCommand(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can run this command!");
			return 0;
		}
		
		RayTraceResult ray = Raycaster.spawnParticleBeam(player, 1000); // 1 mile in meters
		
		Location start = player.getLocation();
		
		if (ray != null) {
			if(ray.getHitEntity() != null){
				// Send clickable message to copy UUID
				// if player belongs to team "DM"
				if(MCTT.isDM(player)) {
					UUID hitId = ray.getHitEntity().getUniqueId();
					if (ray.getHitEntity().getName() == null) {
						player.sendMessage(Component.text("Hit Entity has no name.")
							.color(NamedTextColor.GOLD));
					} else {
						player.sendMessage(Component.text("Name: " + ray.getHitEntity().getName())
							.color(NamedTextColor.GOLD));
					}
					player.sendMessage(
						Component.text("Click to copy hit entity UUID: ")
							.color(NamedTextColor.YELLOW)
							.append(
								Component.text(hitId.toString())
									.clickEvent(ClickEvent.copyToClipboard(hitId.toString()))
									.color(NamedTextColor.AQUA)
							)
					);
				}
			}
			
			// Display distance in feet above XP bar (action bar)
			double distanceFeet = start.distance(ray.getHitPosition().toLocation(player.getWorld())) * 3.28084;
			String message = String.format("Distance: %.0f ft", distanceFeet);
			switch (
				distanceFeet <= 10 ? 1 :
				distanceFeet <= 15 ? 2 :
				distanceFeet <= 20 ? 3 :
				distanceFeet <= 30 ? 4 :
				distanceFeet <= 60 ? 5 :
				distanceFeet <= 90 ? 6 :
				distanceFeet <= 120 ? 7 :
					8) {
				case 1 -> player.sendActionBar(Component.text(message).color(TextColor.color(0,128,0)));
				case 2 -> player.sendActionBar(Component.text(message).color(TextColor.color(0,255,0)));
				case 3 -> player.sendActionBar(Component.text(message).color(TextColor.color(128,255,0)));
				case 4 -> player.sendActionBar(Component.text(message).color(TextColor.color(255,255,0)));
				case 5 -> player.sendActionBar(Component.text(message).color(TextColor.color(255,128,0)));
				case 6 -> player.sendActionBar(Component.text(message).color(TextColor.color(255,0,0)));
				case 7 -> player.sendActionBar(Component.text(message).color(TextColor.color(128,0,255)));
				default -> player.sendActionBar(Component.text(message).color(TextColor.color(128,128,128)));
			}
		}
		return 1;
	}
	
}
