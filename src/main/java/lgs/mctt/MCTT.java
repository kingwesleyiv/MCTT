package lgs.mctt;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MCTT extends JavaPlugin {
	
	private static MCTT pluginInstance;
	private static CharacterSheet_Manager charMGR;
	
	private StartCombat_Command combatCMD;
	private Trace_Command traceCMD;
	public static Roll_Command rollCMD;
	private CharacterSheet_Command charCMD;
	private PossessionHandler possessHandler;
	
	@Override
	public void onLoad() {
		pluginInstance = this;
		
		// SAFE: plugin now exists
		charMGR = new CharacterSheet_Manager(this);
		rollCMD = new Roll_Command(charMGR);
		
		getLogger().info("Loading...");
	}
	
	@Override
	public void onEnable() {
		// instance fields that need 'this'
		combatCMD = new StartCombat_Command(this);
		traceCMD = new Trace_Command();
		charCMD = new CharacterSheet_Command(charMGR);
		possessHandler = new PossessionHandler(this);
		
		buildCommands();
		
		getServer().getPluginManager().registerEvents(new Event_Listener(), this);
		new Emitter_Task().runTaskTimer(this, 1000L, 2L);
		
		getLogger().info("MCTT Enabled!");
	}
	
	public static CharacterSheet_Manager charMGR() {
		return charMGR;
	}


@Override
public void onDisable() { }

public static MCTT get() {return pluginInstance;}

// Command Building // ***********************************************************************************************

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
			Entity target;
			try {
				target = player.rayTraceEntities(10).getHitEntity();
				if (target != null) possessHandler.handlePossess(player, target);
				else player.sendActionBar(Component.text("No Entity.").color(NamedTextColor.BLACK));
			} catch (Exception e) {
				player.sendActionBar(Component.text("Command Error.").color(NamedTextColor.DARK_RED));
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
					return MCTT.rollCMD.rollDice(player, count, sides);
				})
			)
		)
		// roll from stat (d20) /r <stat>
		.then(Commands.argument("stat", StringArgumentType.word())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				String stat = StringArgumentType.getString(ctx, "stat");
				return MCTT.rollCMD.rollStat(player, stat, 0, 0);
			})
		).then(Commands.argument("bonus", IntegerArgumentType.integer())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				String stat = StringArgumentType.getString(ctx, "stat");
				int bonus = IntegerArgumentType.getInteger(ctx, "bonus");
				return MCTT.rollCMD.rollStat(player, stat, bonus, 0);
			})
		).then(Commands.argument("advantage", IntegerArgumentType.integer())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				String stat = StringArgumentType.getString(ctx, "stat");
				int bonus = IntegerArgumentType.getInteger(ctx, "bonus");
				int advantage = IntegerArgumentType.getInteger(ctx, "advantage");
				return MCTT.rollCMD.rollStat(player, stat, bonus, advantage);
			})
		);
	
	
	// Combat Command for toggling combat mode on entities.
	LiteralArgumentBuilder<CommandSourceStack> combatCMD = Commands.literal("combat")
		.executes(ctx -> { return this.combatCMD.onCommand(ctx.getSource().getSender(), null); })
		.then(Commands.literal("initiative")
			.then(Commands.argument("entity", ArgumentTypes.entities())
				.executes(ctx -> {
					final EntitySelectorArgumentResolver selector =
						ctx.getArgument("entity", EntitySelectorArgumentResolver.class);
					final List<Entity> entities = selector.resolve(ctx.getSource());
					
					for (Entity e : entities) {
						this.combatCMD.forceInitiative(e, 0);
					}
					return 1;
				})
				.then(Commands.argument("modifier", IntegerArgumentType.integer())
					.executes(ctx -> {
						final EntitySelectorArgumentResolver selector =
							ctx.getArgument("entity", EntitySelectorArgumentResolver.class);
						final List<Entity> entities = selector.resolve(ctx.getSource());
						int mod = IntegerArgumentType.getInteger(ctx, "modifier");
						
						for (Entity e : entities) {
							this.combatCMD.forceInitiative(e, mod);
						}
						return 1;
					})
				)
			)
		)
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
	player.hideEntity(MCTT.get(), entity);
}

// Hide an entity from all player except an input list.
public static void HideEntityExcept(Entity entity, List<Player> players)
{
	if(players.isEmpty()){return;}
	for(Player player : entity.getWorld().getPlayers())
		for (Player p : players)
			if (p.equals(player))
				continue;
	else player.hideEntity(MCTT.get(), entity);
}

public static void HideEntityExcept(Entity entity)
{
	for(Player player : entity.getWorld().getPlayers())
		player.hideEntity(MCTT.get(), entity);
}

// Show an entity to a specific player, or to all players if player is null.
public static void ShowEntityTo(Entity entity, @Nullable Player player)
{
	if(entity == null) return;
	if(player != null){
		player.showEntity(MCTT.get(), entity);
	}else{
		for(Player p : entity.getWorld().getPlayers()){
			p.showEntity(MCTT.get(), entity);
		}
	}
}

// Is this player a Dungeon Master?
public static boolean isDM(Player player)
{
	if (player == null) return false;
	return player.teamDisplayName().toString().contains("DM");
}
	
// Is this player a Player Character?
public static boolean isPC(Player player)
{
	if (player == null) return false;
	return player.teamDisplayName().toString().contains("players");
}

	
	
}

