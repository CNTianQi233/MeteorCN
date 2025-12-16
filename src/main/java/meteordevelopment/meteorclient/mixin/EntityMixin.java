/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Redirect(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"), require = 0)
    private Vec3d redirectFluidStateGetVelocity(FluidState fluidState, BlockView world, BlockPos pos) {
        Vec3d vec = fluidState.getVelocity(world, pos);
        Velocity velocity = Modules.get().get(Velocity.class);
        if ((Object) this == mc.player && velocity.isActive() && velocity.liquids.get()) {
            vec = vec.multiply(velocity.getHorizontal(velocity.liquidsHorizontal), velocity.getVertical(velocity.liquidsVertical), velocity.getHorizontal(velocity.liquidsHorizontal));
        }
        return vec;
    }

    @Inject(method = "isTouchingWater", at = @At(value = "HEAD"), cancellable = true)
    private void isTouchingWater(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this == mc.player && Modules.get().get(NoSlow.class).fluidDrag()) info.setReturnValue(false);
    }

    @Inject(method = "isInLava", at = @At(value = "HEAD"), cancellable = true)
    private void isInLava(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this == mc.player && Modules.get().get(NoSlow.class).fluidDrag()) info.setReturnValue(false);
    }

    @Redirect(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSubmergedInWater()Z"), require = 0)
    private boolean redirectIsSubmergedInWater(Entity entity) {
        if ((Object) this == mc.player && Modules.get().get(NoSlow.class).fluidDrag()) return false;
        return entity.isSubmergedInWater();
    }

    @ModifyArgs(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"), require = 0)
    private void onPushAwayFrom(Args args, Entity entity) {
        Velocity velocity = Modules.get().get(Velocity.class);

        // Velocity
        if ((Object) this == mc.player && velocity.isActive() && velocity.entityPush.get()) {
            double multiplier = velocity.entityPushAmount.get();
            args.set(0, (double) args.get(0) * multiplier);
            args.set(2, (double) args.get(2) * multiplier);
        }
        // FakePlayerEntity
        else if (entity instanceof FakePlayerEntity player && player.doNotPush) {
            args.set(0, 0.0);
            args.set(2, 0.0);
        }
    }

    @Inject(method = "getJumpVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void onGetJumpVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == mc.player) {
            JumpVelocityMultiplierEvent event = MeteorClient.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
            cir.setReturnValue(cir.getReturnValue() * event.multiplier);
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this == mc.player) {
            MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(type, movement));
        }
        else if ((Object) this instanceof LivingEntity) {
            MeteorClient.EVENT_BUS.post(LivingEntityMoveEvent.get((LivingEntity) (Object) this, movement));
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (PostProcessShaders.rendering) {
            Color color = Modules.get().get(ESP.class).getColor((Entity) (Object) this);
            if (color != null) info.setReturnValue(color.getPacked());
        }
    }

    @Redirect(method = "getVelocityMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"), require = 0)
    private Block getVelocityMultiplierGetBlockProxy(BlockState blockState) {
        if ((Object) this != mc.player) return blockState.getBlock();
        if (blockState.getBlock() == Blocks.SOUL_SAND && Modules.get().get(NoSlow.class).soulSand()) return Blocks.STONE;
        if (blockState.getBlock() == Blocks.HONEY_BLOCK && Modules.get().get(NoSlow.class).honeyBlock()) return Blocks.STONE;
        return blockState.getBlock();
    }

    @Inject(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void isInvisibleToCanceller(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.canUpdate()) return;
        ESP esp = Modules.get().get(ESP.class);
        if (Modules.get().get(NoRender.class).noInvisibility() || esp.isActive() && !esp.shouldSkip((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void isGlowing(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(NoRender.class).noGlowing()) info.setReturnValue(false);
    }

    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> info) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue((Entity) (Object) this);
        if (v != 0) info.setReturnValue((float) v);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (player == null) info.setReturnValue(false);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void getPoseHook(CallbackInfoReturnable<EntityPose> info) {
        if ((Object) this == mc.player && Modules.get().get(ElytraFly.class).canPacketEfly()) {
            info.setReturnValue(EntityPose.FALL_FLYING);
        }
    }

    @Inject(method = "bypassesLandingEffects", at = @At("RETURN"), cancellable = true)
    private void cancelBounce(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(NoFall.class).cancelBounce()) {
            cir.setReturnValue(true);
        }
    }
}
