package lgs.mctt.hud;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Set;

public class PlayerHUD {

public static void showConditions(Player player) {
	// Create a new scoreboard for this player
	Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
	
	// Register an objective for the sidebar
	Objective obj = board.registerNewObjective("conditions", "dummy", "§eConditions");
	obj.setDisplaySlot(DisplaySlot.SIDEBAR);
	
	// Grab their tags
	Set<String> tags = player.getScoreboardTags();
	
	if (tags.isEmpty()) {
		Score none = obj.getScore("§7None");
		none.setScore(1);
	} else {
		int line = tags.size();
		for (String tag : tags) {
			Score score = obj.getScore("§c" + tag);
			score.setScore(line);
			line--;
		}
	}
	
	// Apply scoreboard to player
	player.setScoreboard(board);
}
}
