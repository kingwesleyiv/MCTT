package lgs.mctt;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lgs.mctt.characters.CharacterSheet;
import lgs.mctt.characters.CharacterSheet_Manager;
import lgs.mctt.commands.*;
import lgs.mctt.players.Event_Listener;
import lgs.mctt.world.Emitter_Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MCTT extends JavaPlugin {
	
	private static MCTT pluginInstance;
	public static CharacterSheet_Manager charMGR;
	
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

		// Hacky Emitter setup.
		getServer().getPluginManager().registerEvents(new Event_Listener(), this);
		new Emitter_Task().runTaskTimer(this, 1000L, 2L);
		
		getLogger().info("MCTT Enabled!");
	}
	
	public static CharacterSheet_Manager charMGR() { return charMGR; }


@Override
public void onDisable() {
	this.traceCMD.raycaster.clearMarkers();
}

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
				target = player.rayTraceEntities(20).getHitEntity();
				if (target == null) { target = player.getTargetEntity(20);
					if (target == null) { player.sendActionBar(Component.text("No Entity.").color(NamedTextColor.BLACK));
						return 1;
					}
				}
				possessHandler.handlePossess(player, target);
			} catch (Exception e) { player.sendActionBar(Component.text("Command Error.").color(NamedTextColor.DARK_RED));}
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
		).then(Commands.literal("clear")
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				possessHandler.stopAllPossessions();
				player.sendActionBar(Component.text("All possessions cleared.").color(NamedTextColor.GREEN));
				return 1;
			})
		);
	
	// Roll command: support "/r <count> <sides>" and "/r <stat>"
	LiteralArgumentBuilder<CommandSourceStack> rollCommand = Commands.literal("r")
		// numeric dice form: /r <count> <sides>
		.then(Commands.argument("count", FloatArgumentType.floatArg())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				int count = (int) FloatArgumentType.getFloat(ctx, "count");
				return MCTT.rollCMD.rollDice(player, 1, count);
			})
			.then(Commands.argument("sides", FloatArgumentType.floatArg())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					int count = (int) FloatArgumentType.getFloat(ctx, "count");
					int sides = (int) FloatArgumentType.getFloat(ctx, "sides");
					return MCTT.rollCMD.rollDice(player, count, sides);
				})
			)
		)
		// roll from stat (d20) /r <stat> <bonus> <advantage>
		.then(Commands.argument("stat", StringArgumentType.word())
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				String stat = StringArgumentType.getString(ctx, "stat");
				return MCTT.rollCMD.rollStat(player, stat, 0, 0);
			})
			.then(Commands.argument("bonus", IntegerArgumentType.integer())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String stat = StringArgumentType.getString(ctx, "stat");
					int bonus = IntegerArgumentType.getInteger(ctx, "bonus");
					return MCTT.rollCMD.rollStat(player, stat, bonus, 0);
				})
				.then(Commands.argument("advantage", IntegerArgumentType.integer())
					.executes(ctx -> {
						Player player = (Player) ctx.getSource().getSender();
						String stat = StringArgumentType.getString(ctx, "stat");
						int bonus = IntegerArgumentType.getInteger(ctx, "bonus");
						int advantage = IntegerArgumentType.getInteger(ctx, "advantage");
						return MCTT.rollCMD.rollStat(player, stat, bonus, advantage);
					})
				)
			)
		);
	
	
	// Combat Command for toggling combat mode on entities.
	LiteralArgumentBuilder<CommandSourceStack> combatCMD = Commands.literal("combat")
		.executes(ctx -> { return this.combatCMD.onCommand(ctx.getSource().getSender(), null); })
		.then(Commands.literal("initiative")
			.executes(ctx -> {
				Entity target = possessor((Player)ctx.getSource().getSender());
				this.combatCMD.rollInitiative(target, getInitiative(target));
				return 1;
			})
			.then(Commands.argument("entities", ArgumentTypes.entities())
				.executes(ctx -> {
					final EntitySelectorArgumentResolver selector = ctx.getArgument("entities", EntitySelectorArgumentResolver.class);
					final List<Entity> entities = selector.resolve(ctx.getSource());
					for (Entity e : entities)
						this.combatCMD.rollInitiative(e, getInitiative(e));
					return 1;
				})
				.then(Commands.argument("modifier", IntegerArgumentType.integer())
					.executes(ctx -> {
						final EntitySelectorArgumentResolver selector = ctx.getArgument("entities", EntitySelectorArgumentResolver.class);
						final List<Entity> entities = selector.resolve(ctx.getSource());
						int mod = IntegerArgumentType.getInteger(ctx, "modifier");
						
						for (Entity e : entities) {
							this.combatCMD.rollInitiative(e, mod);
						}
						return 1;
					})
				)
			)
			.then(Commands.literal("remove")
				.executes(ctx -> {
					Entity target = possessor((Player)ctx.getSource().getSender());
					this.combatCMD.removeInitiative(target);
					return 1;
				})
				.then(Commands.argument("entities", ArgumentTypes.entities())
					.executes(ctx -> {
						final EntitySelectorArgumentResolver selector = ctx.getArgument("entities", EntitySelectorArgumentResolver.class);
						final List<Entity> entities = selector.resolve(ctx.getSource());
						for (Entity e : entities)
							this.combatCMD.removeInitiative(e);
						return 1;
					})
				)
				.then(Commands.argument("name", StringArgumentType.string())
					.executes(ctx -> {
							this.combatCMD.removeInitiativeByName(StringArgumentType.getString(ctx, "name"));
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
		.then(Commands.argument("range (ft.)", IntegerArgumentType.integer())
			.executes( ctx -> {
				return this.traceCMD.onCommand(ctx.getSource().getSender(), IntegerArgumentType.getInteger(ctx, "range (ft.)"));
			} )
		);
	
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

	LiteralArgumentBuilder<CommandSourceStack> foodCMD = Commands.literal("food")
		// /food <int>
		.then(Commands.argument("value", FloatArgumentType.floatArg(0, 20))
			.executes(ctx -> {
				Player player = (Player) ctx.getSource().getSender();
				int value = (int) FloatArgumentType.getFloat(ctx, "value");
				player.setFoodLevel(value);
				return 1;
			})
		)
		// /food absorb <int>
		.then(Commands.literal("absorb")
			.then(Commands.argument("value", FloatArgumentType.floatArg(0))
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					int value = (int) FloatArgumentType.getFloat(ctx, "value");
					player.setAbsorptionAmount(value);
					return 1;
				})
			)
		);



	// Character Command for managing stats.
	LiteralArgumentBuilder<CommandSourceStack> charCMD = Commands.literal("char")
		.then(Commands.literal("save").executes(ctx -> { charMGR.saveSheets(); return 1; }))
		.then(Commands.literal("reload").executes(ctx -> { charMGR.reloadSheets(); return 1; })
			.then(Commands.argument("sheet", StringArgumentType.string())
				.executes(ctx -> { charMGR.getSheetByName(StringArgumentType.getString(ctx, "sheet")).reload(); return 1;})
			)
		)
		.then(Commands.literal("create")
			.then(Commands.argument("name", StringArgumentType.string())
				.executes(ctx -> {
					Entity target = possessor((Player)ctx.getSource().getSender());
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.createSheet(target, name, 1, "NPC");
				})
				.then(Commands.argument("level", IntegerArgumentType.integer())
					.executes(ctx -> {
						Entity target = possessor((Player)ctx.getSource().getSender());
						String name = StringArgumentType.getString(ctx, "name");
						int level = IntegerArgumentType.getInteger(ctx, "level");
						return this.charCMD.createSheet(target, name, level, "NPC");
					})
					.then(Commands.argument("class", StringArgumentType.string())
						.executes(ctx -> {
							Entity target = possessor((Player)ctx.getSource().getSender());
							String name = StringArgumentType.getString(ctx, "name");
							int level = IntegerArgumentType.getInteger(ctx, "level");
							String charClass = StringArgumentType.getString(ctx, "class");
							return this.charCMD.createSheet(target, name, level, charClass);
						})
						.then(Commands.argument("target", ArgumentTypes.entities())
							.executes(ctx -> {
								EntitySelectorArgumentResolver selector = ctx.getArgument("entity", EntitySelectorArgumentResolver.class);
								final Entity target = selector.resolve(ctx.getSource()).getFirst();
								if (target == null) {
									ctx.getSource().getSender().sendMessage("§cInvalid target.");
									return 0;
								}

								String name = StringArgumentType.getString(ctx, "name");
								int level = IntegerArgumentType.getInteger(ctx, "level");
								String charClass = StringArgumentType.getString(ctx, "class");
								return this.charCMD.createSheet(target, name, level, charClass);
							})
						)
					)
				)
			)
		).then(Commands.literal("delete")
			.then(Commands.argument("name", StringArgumentType.string())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.deleteSheet(player, name);
				})
			)
		).then(Commands.literal("edit")
			.then(Commands.argument("character", StringArgumentType.string())
				.then(Commands.argument("stat", StringArgumentType.string())
					.then(Commands.argument("value", StringArgumentType.string())
						.executes(ctx -> {
							Player player = (Player) ctx.getSource().getSender();
							String name = StringArgumentType.getString(ctx, "character");
							String stat = StringArgumentType.getString(ctx, "stat");
							String value = StringArgumentType.getString(ctx, "value");
							return this.charCMD.edit(player, name, stat, value);
						})
					)
				)
			)
		).then(Commands.literal("view")
			.then(Commands.argument("name", StringArgumentType.string())
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();
					String name = StringArgumentType.getString(ctx, "name");
					return this.charCMD.viewSheet(player, name, null);
				})
				.then(Commands.argument("stat", StringArgumentType.string())
					.executes(ctx -> {
						Player player = (Player) ctx.getSource().getSender();
						String name = StringArgumentType.getString(ctx, "name");
						String stat = StringArgumentType.getString(ctx, "stat");
						return this.charCMD.viewSheet(player, name, stat);
					})
				)
			)
		).then(Commands.literal("assign")
			.then(Commands.argument("target", ArgumentTypes.entity())
				.then(Commands.argument("sheet", StringArgumentType.string())
					.executes(ctx -> {
						final EntitySelectorArgumentResolver selector =
							ctx.getArgument("target", EntitySelectorArgumentResolver.class);
						final Entity entity = selector.resolve(ctx.getSource()).getFirst();
						if (entity == null) {
							ctx.getSource().getSender().sendMessage("§cInvalid target.");
							return 0;
						}
						String sheetName = StringArgumentType.getString(ctx, "sheet");
						if (entity instanceof Player p)
							return this.charCMD.assignSheet(ctx.getSource().getSender(), possessor(p), sheetName);
						return this.charCMD.assignSheet(ctx.getSource().getSender(), entity, sheetName);
					})
				)
			)
		).then(Commands.literal("remove")
			.then(Commands.argument("target", ArgumentTypes.entity())
				.executes(ctx -> {
					final EntitySelectorArgumentResolver selector =
						ctx.getArgument("target", EntitySelectorArgumentResolver.class);
					final Entity entity = selector.resolve(ctx.getSource()).getFirst();
					if (entity == null) {
						ctx.getSource().getSender().sendMessage("§cInvalid target.");
						return 0;
					}
					if (entity instanceof Player p)
						return this.charCMD.manager.unassignSheet(possessor(p).getUniqueId());
					return this.charCMD.manager.unassignSheet(entity.getUniqueId());
				})
			)
		);
		
	
	this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
	{
		commands.registrar().register(foodCMD.build());
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

public static void sendInvisTarget(Player viewer, LivingEntity target)
{
	ProtocolManager pm = ProtocolLibrary.getProtocolManager();
	PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_EFFECT);
	// Entity ID
	packet.getIntegers().write(0, target.getEntityId());
	// Effect type (NOT an ID)
	packet.getEffectTypes().write(0, PotionEffectType.INVISIBILITY);
	// Amplifier (0 = level I)
	packet.getBytes().write(0, (byte) 0);
	// Duration (large = effectively infinite)
	packet.getIntegers().write(1, 32767);
	// Flags: ambient, particles, icon
	packet.getBooleans().write(0, false);
	packet.getBooleans().write(1, false);
	packet.getBooleans().write(2, false);
	
	try { pm.sendServerPacket(viewer, packet);}
	catch (Exception ignored) { }
}

public static void removeInvisTarget(Player viewer, LivingEntity target)
{
	ProtocolManager pm = ProtocolLibrary.getProtocolManager();
	PacketContainer packet = pm.createPacket(PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
	
	// Entity ID
	packet.getIntegers().write(0, target.getEntityId());
	// Effect type to remove
	packet.getEffectTypes().write(0, PotionEffectType.INVISIBILITY);
	
	try { pm.sendServerPacket(viewer, packet);}
	catch (Exception ignored) { }
}

// Returns the UUID of the player or the entity they are possessing.
public static Entity possessor(Player player)
{
	if (MCTT.get().possessHandler.isPossessing(player))
		return MCTT.get().possessHandler.activeSessionsData.get(player.getUniqueId()).target();
	return player;
}

public static int getInitiative(Entity entity)
{
	CharacterSheet sheet = charMGR.getSheetByUUID(entity.getUniqueId());
	if (sheet == null) return 0;
	return sheet.getTotal("Initiative");
}


}

