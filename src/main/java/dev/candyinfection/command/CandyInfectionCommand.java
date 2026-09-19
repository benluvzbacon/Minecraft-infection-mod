package dev.candyinfection.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionRuntime;
import dev.candyinfection.infection.InfectionStages;
import dev.candyinfection.infection.InfectionWorldState;
import dev.candyinfection.infection.PlayerInfection;
import dev.candyinfection.util.CandyLog;
import dev.candyinfection.world.gen.CandyStructures;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code /candyinfection ...} - every command the mod exposes.
 *
 * <p>Read-only subcommands need no permission; anything that changes the world
 * needs permission level 2 so it works out of the box on dedicated servers.
 */
public final class CandyInfectionCommand {
    public static final String ROOT = "candyinfection";
    private static final int PERMISSION = 2;

    private static final SuggestionProvider<ServerCommandSource> MOB_SUGGESTIONS =
            (context, builder) -> CommandSource.suggestMatching(CandyEntities.monsterNames(), builder);
    private static final SuggestionProvider<ServerCommandSource> STRUCTURE_SUGGESTIONS =
            (context, builder) -> CommandSource.suggestMatching(CandyStructures.kinds(), builder);

    private CandyInfectionCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(ROOT)
                .then(CommandManager.literal("status").executes(CandyInfectionCommand::status))
                .then(CommandManager.literal("stage")
                        .executes(context -> setStage(context, -1))
                        .then(CommandManager.argument("stage", IntegerArgumentType.integer(1, InfectionStages.MAX))
                                .requires(source -> source.hasPermissionLevel(PERMISSION))
                                .executes(context -> setStage(context, IntegerArgumentType.getInteger(context, "stage")))))
                .then(CommandManager.literal("spread")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 32))
                                .executes(context -> spread(context, IntegerArgumentType.getInteger(context, "radius"), 1.0F))))
                .then(CommandManager.literal("cure")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.literal("me").executes(context -> cure(context, null)))
                        .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> cure(context, EntityArgumentType.getPlayers(context, "player")))))
                .then(CommandManager.literal("infect")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(context -> infect(context, IntegerArgumentType.getInteger(context, "amount"), null)))
                        .then(CommandManager.argument("player", EntityArgumentType.players())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> infect(context,
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                EntityArgumentType.getPlayers(context, "player"))))))
                .then(CommandManager.literal("spawn")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("mob", StringArgumentType.word())
                                .suggests(MOB_SUGGESTIONS)
                                .executes(context -> spawn(context, StringArgumentType.getString(context, "mob"), 1))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 32))
                                        .executes(context -> spawn(context, StringArgumentType.getString(context, "mob"),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(CommandManager.literal("core")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .executes(CandyInfectionCommand::placeCore)
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> placeCore(context))))
                .then(CommandManager.literal("purge")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 128))
                                .executes(context -> purge(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(CommandManager.literal("locate")
                        .executes(CandyInfectionCommand::locate))
                .then(CommandManager.literal("event")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("event", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(InfectionRuntime.EVENTS, builder))
                                .executes(context -> event(context, StringArgumentType.getString(context, "event")))))
                .then(CommandManager.literal("structure")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .then(CommandManager.argument("kind", StringArgumentType.word())
                                .suggests(STRUCTURE_SUGGESTIONS)
                                .executes(context -> structure(context, StringArgumentType.getString(context, "kind")))))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("reload").executes(CandyInfectionCommand::reloadConfig))
                        .then(CommandManager.literal("show").executes(CandyInfectionCommand::showConfig)))
                .then(CommandManager.literal("test")
                        .requires(source -> source.hasPermissionLevel(PERMISSION))
                        .executes(CandyInfectionCommand::testZone)));
    }

    // -------------------------------------------------------------- status
    private static int status(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        InfectionWorldState state = InfectionWorldState.get(world);
        InfectionStages stage = state.stageInfo();
        CandyConfig config = CandyConfig.get();
        PlayerEntity player = source.getPlayer();

        source.sendFeedback(() -> Text.literal("=== Candy Infection ===").formatted(Formatting.LIGHT_PURPLE), false);
        send(source, "World stage: " + stage.level() + "/7 (" + stage.label() + ") - " + stage.description());
        send(source, "Infected blocks tracked: " + state.getTotalInfected()
                + " | global infection " + String.format("%.1f%%", state.globalInfectionPercent()));
        send(source, "Active cores: " + state.getCoreCount() + " | nests: " + state.getNestCount()
                + " | purge zones: " + state.purges().size());
        send(source, "Candy monsters alive (this dimension): " + InfectionRuntime.countMonsters(world)
                + " / cap " + InfectionRuntime.maxCandyMonsters(world));
        send(source, "Spread: " + (config.spreadEnabled ? "enabled" : "DISABLED") + " at "
                + config.spreadSpeedMultiplier + "x, chance " + config.baseSpreadChance);
        send(source, "Boss: " + (state.isBossSpawned() ? "active" : "not spawned")
                + " | events: " + (config.eventsEnabled ? "enabled" : "disabled")
                + (state.getActiveEvent().isEmpty() ? "" : " (running: " + state.getActiveEvent() + ")"));
        send(source, "Nearest core: " + describe(state.nearestCore(player != null ? player.getBlockPos() : BlockPos.ORIGIN)));
        send(source, "Nearest nest: " + describe(state.nearestNest(player != null ? player.getBlockPos() : BlockPos.ORIGIN)));
        if (player != null) {
            float level = PlayerInfection.get(player);
            send(source, "Your infection: " + String.format("%.1f%%", level)
                    + " (" + PlayerInfection.tierLabel(level) + ")");
        }
        return 1;
    }

    private static int setStage(CommandContext<ServerCommandSource> context, int stage) {
        ServerWorld world = context.getSource().getWorld();
        InfectionWorldState state = InfectionWorldState.get(world);
        if (stage < 0) {
            send(context.getSource(), "World stage: " + state.getStage() + "/7 ("
                    + state.stageInfo().label() + ")");
            return 1;
        }
        InfectionRuntime.setStage(world, stage);
        send(context.getSource(), "Infection stage set to " + InfectionStages.of(stage).level()
                + " (" + InfectionStages.of(stage).label() + ")");
        return 1;
    }

    private static int spread(CommandContext<ServerCommandSource> context, int radius, float chance) {
        ServerCommandSource source = context.getSource();
        BlockPos pos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        int converted = InfectionConversions.infectArea(source.getWorld(), pos, radius, chance,
                InfectionWorldState.get(source.getWorld()).getStage(), source.getWorld().random);
        send(source, "Converted " + converted + " blocks in a radius of " + radius);
        return converted;
    }

    private static int cure(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
        ServerCommandSource source = context.getSource();
        List<ServerPlayerEntity> targets = players != null ? new ArrayList<>(players) : new ArrayList<>();
        if (targets.isEmpty() && source.getPlayer() != null) {
            targets.add(source.getPlayer());
        }
        if (targets.isEmpty()) {
            send(source, "No player to cure");
            return 0;
        }
        for (ServerPlayerEntity target : targets) {
            PlayerInfection.cure(target, PlayerInfection.get(target));
            send(source, "Cured " + target.getName().getString() + " completely");
        }
        return targets.size();
    }

    private static int infect(CommandContext<ServerCommandSource> context, int amount, Collection<ServerPlayerEntity> players) {
        ServerCommandSource source = context.getSource();
        List<ServerPlayerEntity> targets = players != null ? new ArrayList<>(players) : new ArrayList<>();
        if (targets.isEmpty() && source.getPlayer() != null) {
            targets.add(source.getPlayer());
        }
        for (ServerPlayerEntity target : targets) {
            PlayerInfection.add(target, amount);
            send(source, "Added " + amount + " infection to " + target.getName().getString()
                    + " (now " + String.format("%.1f%%", PlayerInfection.get(target)) + ")");
        }
        return targets.size();
    }

    private static int spawn(CommandContext<ServerCommandSource> context, String name, int count) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Identifier id = Identifier.tryParse(name);
        EntityType<?> type = id == null ? null : Registries.ENTITY_TYPE.get(id);
        if (type == null) {
            send(source, "Unknown candy monster: " + name);
            return 0;
        }
        BlockPos pos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        int spawned = InfectionRuntime.spawnMonsters(world, pos, type, count);
        send(source, "Spawned " + spawned + "x " + name + " near " + pos.toShortString());
        return spawned;
    }

    private static int placeCore(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = context.getArgumentNames().contains("pos")
                ? BlockPosArgumentType.getBlockPos(context, "pos")
                : source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        world.setBlockState(pos, dev.candyinfection.init.CandyBlocks.INFECTION_CORE.getDefaultState(), 3);
        InfectionWorldState.get(world).addCore(pos);
        send(source, "Placed an infection core at " + pos.toShortString());
        return 1;
    }

    private static int purge(CommandContext<ServerCommandSource> context, int radius) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        InfectionWorldState.get(world).addPurge(pos, radius, radius * 40);
        send(source, "Started a purge of radius " + radius + " at " + pos.toShortString());
        return 1;
    }

    private static int locate(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        InfectionWorldState state = InfectionWorldState.get(source.getWorld());
        BlockPos pos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        send(source, "Nearest core: " + describe(state.nearestCore(pos)));
        send(source, "Nearest nest: " + describe(state.nearestNest(pos)));
        send(source, "You are " + (state.isColony(pos) ? "inside Infected Land" : "outside Infected Land")
                + " (chunk density " + state.chunkCount(pos) + ")");
        return 1;
    }

    private static int event(CommandContext<ServerCommandSource> context, String event) {
        ServerWorld world = context.getSource().getWorld();
        InfectionRuntime.forceEvent(world, event);
        send(context.getSource(), "Started event: " + event);
        return 1;
    }

    private static int structure(CommandContext<ServerCommandSource> context, String kind) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : BlockPos.ORIGIN;
        int built = CandyStructures.build(world, pos, kind, InfectionWorldState.get(world));
        send(source, "Built " + kind + ": " + built + " blocks");
        return built;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        CandyConfig.get().reload();
        send(context.getSource(), "Reloaded config/candyinfection.json");
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> context) {
        CandyConfig config = CandyConfig.get();
        send(context.getSource(), "config/candyinfection.json");
        send(context.getSource(), "  spreadSpeedMultiplier = " + config.spreadSpeedMultiplier
                + ", baseSpreadChance = " + config.baseSpreadChance);
        send(context.getSource(), "  maxSpreadOperationsPerTick = " + config.maxSpreadOperationsPerTick
                + ", maxSpreadQueueSize = " + config.maxSpreadQueueSize);
        send(context.getSource(), "  maxCandyMonsters = " + config.maxCandyMonsters
                + ", monsterSpawningEnabled = " + config.monsterSpawningEnabled);
        send(context.getSource(), "  playerInfectionEnabled = " + config.playerInfectionEnabled
                + ", infectVanillaMobs = " + config.infectVanillaMobs);
        send(context.getSource(), "  eventsEnabled = " + config.eventsEnabled
                + ", bossSpawningEnabled = " + config.bossSpawningEnabled);
        send(context.getSource(), "  structuresEnabled = " + config.structuresEnabled
                + ", difficultyScalingEnabled = " + config.difficultyScalingEnabled);
        send(context.getSource(), "  hudEnabled = " + config.hudEnabled + ", debugLogging = " + config.debugLogging);
        return 1;
    }

    /** Creates a small, self-contained infection zone for testing the mod. */
    private static int testZone(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        if (source.getPlayer() == null) {
            send(source, "This command needs a player position");
            return 0;
        }
        BlockPos pos = source.getPlayer().getBlockPos();
        InfectionWorldState state = InfectionWorldState.get(world);
        int converted = InfectionConversions.infectArea(world, pos, 8, 0.9F, 4, world.random);
        converted += CandyStructures.build(world, pos, CandyStructures.INFECTION_NEST, state);
        int spawned = InfectionRuntime.spawnMonsters(world, pos, CandyEntities.GUMMY_SPAWN, 3);
        spawned += InfectionRuntime.spawnMonsters(world, pos, CandyEntities.CANDY_CRAWLER, 2);
        send(source, "Test zone ready: " + converted + " blocks converted, " + spawned + " monsters spawned, "
                + "infection nest placed at " + pos.toShortString());
        send(source, "Use /candyinfection purge 16 to clean it up");
        CandyLog.debug("Test infection zone created at " + pos.toShortString());
        return converted;
    }

    private static String describe(BlockPos pos) {
        return pos == null ? "none found" : pos.toShortString();
    }

    private static void send(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GRAY), false);
    }

}
