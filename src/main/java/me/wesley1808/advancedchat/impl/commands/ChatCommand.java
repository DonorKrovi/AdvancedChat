package me.wesley1808.advancedchat.impl.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eu.pb4.styledchat.StyledChatUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.wesley1808.advancedchat.api.AdvancedChatAPI;
import me.wesley1808.advancedchat.impl.channels.Channels;
import me.wesley1808.advancedchat.impl.channels.ChatChannel;
import me.wesley1808.advancedchat.impl.config.Config;
import me.wesley1808.advancedchat.impl.data.DataManager;
import me.wesley1808.advancedchat.impl.utils.Formatter;
import me.wesley1808.advancedchat.impl.utils.ModCompat;
import me.wesley1808.advancedchat.impl.utils.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.MessageArgument.message;

public class ChatCommand {
    private static final String MESSAGE_KEY = "message";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("chat");

        builder.then(literal("global")
                .executes(ctx -> switchChannel(ctx.getSource().getPlayerOrException(), "global", true))
                .then(argument(MESSAGE_KEY, message())
                        .executes(ctx -> sendInChannel(ctx, "global", true))
                )
        );

        builder.then(argument("channel", word())
                .suggests(availableChannels())
                .executes(ctx -> switchChannel(ctx.getSource().getPlayerOrException(), getString(ctx, "channel"), false))
                .then(argument(MESSAGE_KEY, message())
                        .executes(ctx -> sendInChannel(ctx, getString(ctx, "channel"), false))
                )
        );

        dispatcher.register(builder);
    }

    private static int switchChannel(ServerPlayer player, String name, boolean isGlobal) {
        ChatChannel channel = isGlobal ? null : Channels.get(name);
        if (!isGlobal && (channel == null || !channel.canPlayerUse(player))) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.channelNotFound.replace("${name}", name)));
            return 0;
        }

        player.sendSystemMessage(Formatter.parse(Config.instance().messages.switchedChannels.replace("${channel}", StringUtils.capitalize(name))));
        DataManager.modify(player, (data) -> data.channel = channel);
        Util.resetActionBarPacket(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int sendInChannel(CommandContext<CommandSourceStack> ctx, String name, boolean isGlobal) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ChatChannel channel = isGlobal ? null : Channels.get(name);
        if (!isGlobal && (channel == null || !channel.canPlayerUse(player))) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.channelNotFound.replace("${name}", name)));
            return 0;
        }

        MessageArgument.resolveChatMessage(ctx, MESSAGE_KEY, (message) -> {
            DataManager.modify(player, (data) -> {
                ChatChannel original = data.channel;
                data.channel = channel;

                // StyledChat support
                if (ModCompat.STYLEDCHAT) {
                    StyledChatUtils.modifyForSending(message, ctx.getSource(), ChatType.CHAT);
                }

                MutableComponent prefix = (MutableComponent) AdvancedChatAPI.getChannelPrefix(player);
                ChatType.Bound bound = ChatType.bind(ChatType.CHAT, player.server.registryAccess(), prefix.append(player.getDisplayName()));
                player.server.getPlayerList().broadcastChatMessage(message, player, bound);
                data.channel = original;
            });
        });
        return Command.SINGLE_SUCCESS;
    }

    private static SuggestionProvider<CommandSourceStack> availableChannels() {
        return (ctx, builder) -> {
            ServerPlayer source = ctx.getSource().getPlayerOrException();
            List<String> channels = new ObjectArrayList<>();
            Channels.getAll().forEach((name, channel) -> {
                if (channel.canPlayerUse(source)) {
                    channels.add(name);
                }
            });

            return Util.suggest(builder, channels);
        };
    }
}
