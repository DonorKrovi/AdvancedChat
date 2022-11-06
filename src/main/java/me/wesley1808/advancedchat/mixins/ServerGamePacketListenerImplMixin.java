package me.wesley1808.advancedchat.mixins;

import me.wesley1808.advancedchat.common.utils.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatType;bind(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/ChatType$Bound;"))
    private ChatType.Bound advancedchat$addChannelPrefix(ResourceKey<ChatType> key, Entity entity) {
        MutableComponent prefix = (MutableComponent) Util.getChannelPrefix(this.player);
        prefix = prefix != null ? Util.addHoverText(prefix, this.player) : Component.empty();
        return ChatType.bind(key, entity.level.registryAccess(), prefix.append(entity.getDisplayName()));
    }
}
