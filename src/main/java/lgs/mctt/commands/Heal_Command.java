package lgs.mctt.commands;

import lgs.mctt.characters.Conditions;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;

public class Heal_Command {
	
	
	public static int full_heal(CommandSender sender, List<LivingEntity> targets) {
		
		for (LivingEntity target : targets) {
			for (String condition : Conditions.ALL) target.removeScoreboardTag(condition);
			target.heal(100);
			target.clearActivePotionEffects();
			target.setPose(Pose.STANDING);
			target.setVisualFire(false);
			target.setFireTicks(0);
			
			if (target instanceof Player p) {
				p.sendMessage("§aYou have been fully healed!");
				sender.sendMessage("§a" + p.getName() + " has been fully healed!");
				p.setFoodLevel(20);
				p.setSaturation(20);
			}
		}
		return 1;
	}
	
	public static int heal_conditions(CommandSender sender, List<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			for (String condition : Conditions.ALL) target.removeScoreboardTag(condition);
			target.clearActivePotionEffects();
			target.setPose(Pose.STANDING);
			target.setVisualFire(false);
			target.setFireTicks(0);
		}
		return 1;
	}
}
