package me.wesley1808.advancedchat.impl.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.wesley1808.advancedchat.api.AdvancedChatEvents;
import me.wesley1808.advancedchat.impl.AdvancedChat;
import me.wesley1808.advancedchat.impl.config.Config;
import me.wesley1808.advancedchat.mixins.filter.FilterMaskInvoker;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.TextFilter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Filter implements TextFilter {
    private final ServerPlayer player;

    public Filter(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void join() {
    }

    @Override
    public void leave() {
    }

    @Override
    public CompletableFuture<FilteredText> processStreamMessage(@NotNull String input) {
        return CompletableFuture.supplyAsync(() -> new FilteredText(input, this.parseMask(input)));
    }

    @Override
    public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> list) {
        return CompletableFuture.supplyAsync(() -> {
            ObjectArrayList<FilteredText> filtered = new ObjectArrayList<>(list.size());
            for (String input : list) {
                filtered.add(new FilteredText(input, this.parseMask(input)));
            }

            return filtered;
        });
    }

    private FilterMask parseMask(String input) {
        Config.Filter filter = Config.instance().filter;
        if (!filter.enabled || filter.filteredWords.length == 0) {
            return FilterMask.PASS_THROUGH;
        }

        BitSet filtered = new BitSet(input.length());
        for (String word : filter.filteredWords) {
            final int startIndex = StringUtils.indexOfIgnoreCase(input, word);
            for (int curr = startIndex; curr != StringUtils.INDEX_NOT_FOUND; curr = StringUtils.indexOfIgnoreCase(input, word, curr)) {
                filtered.set(curr, curr += word.length());
            }
        }

        if (!filtered.isEmpty()) {
            AdvancedChatEvents.MESSAGE_FILTERED.invoker().onMessageFiltered(this.player, input);
            if (filter.logFilteredMessages) {
                AdvancedChat.getLogger().info("[AdvancedChat] Filtered text from {}: {}", this.player.getScoreboardName(), input);
            }

            return FilterMaskInvoker.newMask(filtered);
        }

        return FilterMask.PASS_THROUGH;
    }

    public static String filterStyledChat(String input) {
        Config.Filter filter = Config.instance().filter;
        if (!filter.enabled || filter.filteredWords.length == 0) {
            return input;
        }

        String filtered = input;
        for (String word : filter.filteredWords) {
            filtered = StringUtils.replaceIgnoreCase(filtered, word, "#".repeat(word.length()));
        }

        return filtered;
    }
}
