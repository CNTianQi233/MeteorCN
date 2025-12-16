/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import meteordevelopment.meteorclient.utils.tooltip.MeteorTooltipData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * DrawContextMixin handles custom tooltip rendering.
 * The main tooltip component registration is handled by MeteorTooltipManager using Fabric API's
 * TooltipComponentCallback for better Connector/Forge compatibility.
 * This mixin only prevents duplicate processing of MeteorTooltipData by the vanilla system.
 */
@Mixin(value = DrawContext.class)
public class DrawContextMixin {
    /**
     * Prevents vanilla from processing MeteorTooltipData through ifPresent(),
     * since we already handle it via TooltipComponentCallback.
     */
    @ModifyReceiver(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"),
            require = 0)
    private Optional<TooltipData> onDrawTooltip_modifyIfPresentReceiver(Optional<TooltipData> data, Consumer<TooltipData> consumer) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData) return Optional.empty();
        return data;
    }
}
