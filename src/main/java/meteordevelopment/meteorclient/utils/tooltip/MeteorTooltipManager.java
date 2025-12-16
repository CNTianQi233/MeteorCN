/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;

/**
 * Manages custom tooltip components using Fabric API's TooltipComponentCallback.
 * This approach is more compatible with Sinytra Connector than Mixin injection.
 */
public class MeteorTooltipManager {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register callback for converting MeteorTooltipData to TooltipComponent
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof MeteorTooltipData meteorData) {
                return meteorData.getComponent();
            }
            return null; // Return null to let other handlers process it
        });
    }
}
