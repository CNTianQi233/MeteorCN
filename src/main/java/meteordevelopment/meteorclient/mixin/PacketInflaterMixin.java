/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.asm.Asm;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.PacketInflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin replacement for PacketInflaterTransformer.
 * This is used in Connector/Forge environment where ASM transformer injection is not available.
 * Prevents DecoderException when receiving large packets (AntiPacketKick functionality).
 *
 * The ASM transformer wraps the throw statement for oversized packets. Since Mixin cannot
 * directly wrap throw statements, we instead modify the size comparison to always pass
 * when AntiPacketKick is active.
 */
@Mixin(PacketInflater.class)
public class PacketInflaterMixin {

    /**
     * Modifies the packet size check in decode() method.
     * When AntiPacketKick is active in Connector environment, makes the comparison always false
     * to prevent the DecoderException from being thrown.
     */
    @ModifyExpressionValue(
        method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Lio/netty/buffer/ByteBuf;readableBytes()I", ordinal = 1),
        require = 0
    )
    private int modifyReadableBytesForSizeCheck(int original) {
        // Only apply in Connector environment - in native Fabric, ASM Transformer handles this
        if (Asm.isConnector() && Modules.get() != null && Modules.get().isActive(AntiPacketKick.class)) {
            // Return 0 to make the size check pass (0 is never > threshold)
            return 0;
        }
        return original;
    }
}
