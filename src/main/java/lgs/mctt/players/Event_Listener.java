package lgs.mctt.players;

import io.papermc.paper.event.entity.EntityMoveEvent;
import lgs.mctt.MCTT;
import lgs.mctt.characters.Conditions;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Event_Listener implements Listener {

private final double climbVelocity = 0.6; // how strong the “grab jump” is


@EventHandler
public void onPlayerJump(PlayerMoveEvent event) {
	Player player = event.getPlayer();
	if(Conditions.hasAnyTag(player.getScoreboardTags(), Conditions.ZERO_SPEED)) {
		event.setCancelled(true);
		return;
	} else if (player.isFlying() || player.getGameMode() == GameMode.SPECTATOR) return;
	
	Vector velocity = player.getVelocity();
	
	// Only attempt ledge climb if moving downward
	if(player.getPose() == Pose.SWIMMING && velocity.getY() == 0) player.setVelocity(velocity.clone().setY(1));
	if (velocity.getY() <= -0.1 && player.getCurrentInput().isJump()) {
		Vector dir = player.getLocation().getDirection();
		dir.setY(0); // ignore up/down pitch
		dir.normalize();
		
		// Position roughly half a block above feet
		Location checkPos = player.getLocation().clone().add(0, 1, 0).add(dir.multiply(0.75));
		Block blockInFront = checkPos.getBlock();
		Block blockAbove = blockInFront.getRelative(0, 1, 0);
		
		if (!blockInFront.isPassable() && blockAbove.isPassable() ) {
			// Apply upward boost
			Vector climb = velocity.clone();
			climb.setY(climbVelocity);
			player.setVelocity(climb);
		}
	}
}

@EventHandler
public void onPlayerMove(PlayerMoveEvent event){
	Player player = event.getPlayer();
	if(player.getScoreboardTags().contains("POSSESSED")) {
		event.setCancelled(true);
	}
	if (player.getScoreboardTags().contains("PAUSED")) {
		event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
		player.setVelocity(new Vector(0, 0, 0));
	}
}

@EventHandler
public void onEntityMove(EntityMoveEvent event) {
	if (event.getEntity().getScoreboardTags().contains("PAUSED") && event.hasChangedPosition()) {
		event.getEntity().setVelocity(new Vector(0,0,0));
		event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
	}
}

@EventHandler
public void onPlayerInput(PlayerInputEvent event) {
	Player player = event.getPlayer();
	if((event.getInput().isSneak() && event.getInput().isSprint())
		&& (player.getPose().equals(Pose.STANDING) || player.getPose().equals(Pose.SNEAKING))) {
		player.performCommand("crawl");
	}
}

@EventHandler
public void onPlayerDamage(EntityDamageEvent event) {
	if (!(event.getEntity() instanceof Player player)) return;
	if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getCause() == EntityDamageEvent.DamageCause.KILL) return;
	event.setCancelled(true);
}

@EventHandler
public void onPotionEffectChange(EntityPotionEffectEvent event) {
	EntityPotionEffectEvent.Action action = event.getAction();
	
	// Prevent effect removal from characters with appropriate tags.
	if (action == EntityPotionEffectEvent.Action.REMOVED || action == EntityPotionEffectEvent.Action.CLEARED) {
		if (event.getEntity() instanceof Player player && event.getOldEffect().getType() == PotionEffectType.DARKNESS) {
			if (player.getScoreboardTags().contains("BLIND") || player.getScoreboardTags().contains("UNCONSCIOUS")) {
				event.setCancelled(true);
			}
		}
	}
}

}
