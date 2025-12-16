/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.util.math.Vec3d;

/**
 * Event posted when fluid velocity is about to be applied to the player.
 * This event allows modifying the fluid velocity before it affects the player.
 */
public class FluidPushEvent {
    private static final FluidPushEvent INSTANCE = new FluidPushEvent();

    public Vec3d velocity;
    public double multiplierX = 1.0;
    public double multiplierY = 1.0;
    public double multiplierZ = 1.0;

    public static FluidPushEvent get(Vec3d velocity) {
        INSTANCE.velocity = velocity;
        INSTANCE.multiplierX = 1.0;
        INSTANCE.multiplierY = 1.0;
        INSTANCE.multiplierZ = 1.0;
        return INSTANCE;
    }

    public Vec3d getModifiedVelocity() {
        return velocity.multiply(multiplierX, multiplierY, multiplierZ);
    }
}
