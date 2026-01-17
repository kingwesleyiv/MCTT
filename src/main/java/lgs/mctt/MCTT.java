package lgs.mctt;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import jdk.jfr.Event;
import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.commands.*;
import lgs.mctt.players.Event_Listener;
import lgs.mctt.players.StateMachine;
import lgs.mctt.world.Emitter_Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;
import java.util.List;
import java.util.UUID;

public final class MCTT extends JavaPlugin {
//*********************************************************************************************************

private final CharacterSheet_Manager charMGR = new CharacterSheet_Manager(this);
private final StartCombat_Command combatCMD = new StartCombat_Command(this);
private final Trace_Command traceCMD = new Trace_Command();
private final Roll_Command rollCMD = new Roll_Command(this, charMGR);
private final CharacterSheet_Command charCMD = new CharacterSheet_Command(this, charMGR);
private final PossessionHandler possessHandler = new PossessionHandler(this);

@Override
public void onEnable() {
	getLogger().info("Loading...");
	buildCommands();
	
	// Implement Event Listener
	getServer().getPluginManager().registerEvents(new Event_Listener(), this);
	new Emitter_Task().runTaskTimer(this, 1000L, 2L);
	//new StateMachine().runTaskTimer(this, 0L, 1L);
	
}
	
@Override
public void onLoad() {
	
	getLogger().info("MC Tabletop Loaded!");
}

@Override
public void onDisable() {

}

private void buildCommands() {
	LiteralArgumentBuilder<CommandSourceStack> possessCMD = Commands.literal("possess")
		// No arguments: toggle possession of entity player is looking at
		.executes(ctx -> {
			Player player = (Player) ctx.getSource().getSender();
			// If already possessing, stop
			if (possessHandler.isPossessing(player)) {
				possessHandler.stopPossessing(player);
				return 1;}
			// Otherwise, try to possess entity in sight
			Entity target = player.getTargetEntity(10);
			if (target != null) {
				possessHandler.handlePossess(player, target);
			} else {
				player.sendActionBar(Component.text("No Entity in Sight").color(NamedTextColor.RED));
			}
			return 1;
		})
		// Argument form: possess <entity>
		.then(Commands.argument("target", ArgumentTypes.entity())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				final EntitySelectorArgumentResolver selectedEntity = ctx.getArgument("target", EntitySelectorArgumentResolver.class);
				final List<Entity> entities = selectedEntity.resolve(ctx.getSource());
				possessHandler.handlePossess(player, entities.getFirst());
				return 1;
			})
		);
	
	// Roll command: support "/r <count> <sides>" and "/r <stat>"
	LiteralArgumentBuilder<CommandSourceStack> rollCommand = Commands.literal("r")
		// numeric dice form: /r <count> <sides>
		.then(Commands.argument("count", FloatArgumentType.floatArg())
			.then(Commands.argument("sides", FloatArgumentType.floatArg())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					int count = (int) FloatArgumentType.getFloat(ctx, "count");
					int sides = (int) FloatArgumentType.getFloat(ctx, "sides");
					return this.rollCMD.rollDice(player, count, sides);
				})
			)
		)
		// stat form: /r <stat>
		.then(Commands.argument("stat", StringArgumentType.word())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				String stat = StringArgumentType.getString(ctx, "stat");
				return this.rollCMD.rollStat(player, stat);
			})
		);
	
	
	// Combat Command for toggling players mode on entities.
	LiteralArgumentBuilder<CommandSourceStack> combatCMD = Commands.literal("combat")
		.executes(ctx -> { return this.combatCMD.onCommand(ctx.getSource().getSender(), null); })
		.then(Commands.argument("entity", ArgumentTypes.entities())
			.executes(ctx -> {
				final EntitySelectorArgumentResolver selector = ctx.getArgument("entity", EntitySelectorArgumentResolver.class);
				final List<Entity> entities = selector.resolve(ctx.getSource());
				List<LivingEntity> livingEntities = entities.stream()
					.filter(e -> e instanceof LivingEntity)
					.map(e -> (LivingEntity) e)
					.toList();
				return this.combatCMD.onCommand(ctx.getSource().getSender(), livingEntities);
			})
		);
	
	// Trace Command for finding distances.
	LiteralArgumentBuilder<CommandSourceStack> traceCMD = Commands.literal("trace")
		.executes( ctx -> this.traceCMD.onCommand(ctx.getSource().getSender()) );
	
	// Restore command for returning players to default state.
	LiteralArgumentBuilder<CommandSourceStack> healCMD = Commands.literal("heal")
		.then(Commands.argument("entity", ArgumentTypes.entities())
			.executes( ctx -> {
				final EntitySelectorArgumentResolver selector = ctx.getArgument("entity", EntitySelectorArgumentResolver.class);
				final List<Entity> entities = selector.resolve(ctx.getSource());
				List<LivingEntity> livingEntities = entities.stream()
					.filter(e -> e instanceof LivingEntity)
					.map(e -> (LivingEntity) e)
					.toList();
				return Heal_Command.full_heal(ctx.getSource().getSender(), livingEntities);
			})
		);
	
		
	// Character Command for managing stats.
	LiteralArgumentBuilder<CommandSourceStack> charCMD = Commands.literal("char")
		.then(Commands.literal("create")
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.createSheet(player, name, null);
				})
				.then(Commands.argument("level", FloatArgumentType.floatArg())
					.executes(ctx -> {
						Player player = (Player) ctx.getSource().getSender();
						String name = StringArgumentType.getString(ctx, "name");
						float level = FloatArgumentType.getFloat(ctx, "level");
						return this.charCMD.createSheet(player, name, level);
					})
				)
			)
		)
		.then(Commands.literal("delete")
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.deleteSheet(player, name);
				})
			)
		)
		.then(Commands.literal("edit")
			.then(Commands.argument("name", StringArgumentType.word())
				.then(Commands.argument("stat", StringArgumentType.word())
					.then(Commands.argument("value", StringArgumentType.word()) // or FloatArgumentType.floatArg() if numeric
						.executes(ctx -> {
							Player player = (Player) ctx.getSource().getSender();
							String name = StringArgumentType.getString(ctx, "name");
							String stat = StringArgumentType.getString(ctx, "stat");
							String value = StringArgumentType.getString(ctx, "value");
							return this.charCMD.edit(player, name, stat, value);
						})
					)
				)
			)
		)
		.then(Commands.literal("view")
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.viewSheet(player, name, null);
				})
				.then(Commands.argument("stat", StringArgumentType.word())
					.executes(ctx -> {
						Player player = (Player) ctx.getSource().getSender();
						String name = StringArgumentType.getString(ctx, "name");
						String stat = StringArgumentType.getString(ctx, "stat");
						return this.charCMD.viewSheet(player, name, stat);
					})
				)
			)
		);
	
 	this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
		commands.registrar().register(combatCMD.build());
		commands.registrar().register(healCMD.build());
	 	commands.registrar().register(traceCMD.build());
	 	commands.registrar().register(charCMD.build());
	    commands.registrar().register(rollCommand.build());
	    commands.registrar().register(possessCMD.build());
	});
}

// Helper Functions // ************************************************************************************************

// Hide an entity from a specific player.
public static void HideEntityFrom(Entity entity, Player player)
{
	player.hideEntity(MCTT.getPlugin(MCTT.class), entity);
}

// Hide an entity from all player except an input list.
public static void HideEntityExcept(Entity entity, List<Player> players)
{
	if(players.isEmpty()){return;}
	for(Player player : entity.getWorld().getPlayers())
		player.hideEntity(MCTT.getPlugin(MCTT.class), entity);
}

public static void HideEntityExcept(Entity entity)
{
	for(Player player : entity.getWorld().getPlayers())
		player.hideEntity(MCTT.getPlugin(MCTT.class), entity);
}

// Show an entity to a specific player, or to all players if player is null.
public static void ShowEntityTo(Entity entity, @Nullable Player player)
{
	if(entity == null) return;
	if(player != null){
		player.showEntity(MCTT.getPlugin(MCTT.class), entity);
	}else{
		for(Player p : entity.getWorld().getPlayers()){
			p.showEntity(MCTT.getPlugin(MCTT.class), entity);
		}
	}
}

// Is this player a Dungeon Master?
public static boolean isDM(Player player) {
	return Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player).getName() == "DM"; }
	
// Is this player a Player Character?
public static boolean isPC(Player player) {
	return Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player).getName() == "DM"; }

	
	
}

