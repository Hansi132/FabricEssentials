package org.server_utilities.essentials.command.impl.warp;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.message.api.Message;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.server_utilities.essentials.command.Command;
import org.server_utilities.essentials.command.CommandProperties;
import org.server_utilities.essentials.command.util.CommandUtil;
import org.server_utilities.essentials.storage.DataStorage;
import org.server_utilities.essentials.storage.ServerData;
import org.server_utilities.essentials.util.teleportation.Warp;

import java.util.Map;

public class WarpCommand extends Command {

    public static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Message.message("fabric-essentials.commands.warp.unknown"));

    public WarpCommand() {
        super(CommandProperties.create("warp", 0));
    }

    @Override
    protected void registerArguments(LiteralArgumentBuilder<CommandSourceStack> literal, CommandBuildContext commandBuildContext) {
        RequiredArgumentBuilder<CommandSourceStack, String> name = Commands.argument("warp", StringArgumentType.string()).suggests(WARPS_PROVIDER);
        name.executes(this::teleport);
        literal.then(name);
    }

    private int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "warp");
        ServerPlayer serverPlayer = src.getPlayerOrException();
        ServerData essentialsData = DataStorage.STORAGE.getServerData();
        Map<String, Warp> warps = essentialsData.getWarps();
        Warp warp = warps.get(name);
        if (warp == null) throw UNKNOWN.create();
        ServerLevel targetLevel = warp.location().getLevel(src.getServer());
        if (targetLevel != null) {
            CommandUtil.asyncTeleport(src, targetLevel, warp.location().chunkPos(), config().warps.waitingPeriod).whenCompleteAsync((chunkAccess, throwable) -> {
                if (chunkAccess == null) return;
                ctx.getSource().sendSuccess(Message.message("fabric-essentials.commands.warp", warp.placeholders(name)), false);
                warp.location().teleport(serverPlayer);
            }, src.getServer());
        } else {
            throw WORLD_UNKNOWN.create();
        }
        return SUCCESS;
    }

    public static final SuggestionProvider<CommandSourceStack> WARPS_PROVIDER = (ctx, builder) -> SharedSuggestionProvider.suggest(DataStorage.STORAGE.getServerData().getWarps().keySet(), builder);

}
