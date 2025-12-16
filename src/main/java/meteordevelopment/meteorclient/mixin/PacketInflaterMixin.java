/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import meteordevelopment.meteorclient.asm.Asm;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.handler.PacketInflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin replacement for PacketInflaterTransformer.
 * This is used in Connector/Forge environment where ASM transformer injection is not available.
 * Prevents DecoderException when receiving large packets (AntiPacketKick functionality).
 */
@Mixin(PacketInflater.class)
public class PacketInflaterMixin {

    @Inject(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V",
            at = @At(value = "NEW", target = "io/netty/handler/codec/DecoderException", ordinal = 1),
            cancellable = true, require = 0)
    private void onDecodeThrowDecoderException(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) {
        // Only apply in Connector environment - in native Fabric, ASM Transformer handles this
        if (Asm.isConnector() && Modules.get() != null && Modules.get().isActive(AntiPacketKick.class)) {
            ci.cancel();
        }
    }
}
