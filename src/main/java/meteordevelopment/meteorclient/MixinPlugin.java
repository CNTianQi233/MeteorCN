/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.asm.Asm;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String mixinPackage = "meteordevelopment.meteorclient.mixin";

    private static boolean loaded;
    private static boolean isConnectorEnvironment;

    private static boolean isOriginsPresent;
    private static boolean isIndigoPresent;
    public static boolean isSodiumPresent;
    private static boolean isCanvasPresent;
    private static boolean isLithiumPresent;
    public static boolean isIrisPresent;
    private static boolean isIndiumPresent;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;

        // Detect if running under Sinytra Connector (Forge environment)
        isConnectorEnvironment = detectConnectorEnvironment();

        if (!isConnectorEnvironment) {
            // Original Fabric ASM transformer injection
            try {
                // Get class loader
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Class<?> classLoaderClass = classLoader.getClass();

                // Get delegate
                Field delegateField = classLoaderClass.getDeclaredField("delegate");
                delegateField.setAccessible(true);
                Object delegate = delegateField.get(classLoader);
                Class<?> delegateClass = delegate.getClass();

                // Get mixinTransformer field
                Field mixinTransformerField = delegateClass.getDeclaredField("mixinTransformer");
                mixinTransformerField.setAccessible(true);

                // Get unsafe
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                Unsafe unsafe = (Unsafe) unsafeField.get(null);

                // Create Asm
                Asm.init();

                // Change delegate
                Asm.Transformer mixinTransformer = (Asm.Transformer) unsafe.allocateInstance(Asm.Transformer.class);
                mixinTransformer.delegate = (IMixinTransformer) mixinTransformerField.get(delegate);

                mixinTransformerField.set(delegate, mixinTransformer);
            }
            catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            // In Connector environment, just initialize Asm without transformer injection
            // The ASM transformations will be handled differently or skipped
            Asm.init();
        }

        // Safe mod detection that works in both environments
        isIndigoPresent = isModLoaded("fabric-renderer-indigo");
        isOriginsPresent = isModLoaded("origins");
        isSodiumPresent = isModLoaded("sodium") || isModLoaded("embeddium") || isModLoaded("rubidium");
        isCanvasPresent = isModLoaded("canvas");
        isLithiumPresent = isModLoaded("lithium");
        isIrisPresent = isModLoaded("iris") || isModLoaded("oculus");
        isIndiumPresent = isModLoaded("indium");

        loaded = true;
    }

    private static boolean detectConnectorEnvironment() {
        try {
            // Check for Sinytra Connector
            Class.forName("org.sinytra.connector.ConnectorEarlyLoader");
            return true;
        } catch (ClassNotFoundException e) {
            // Not in Connector environment
        }

        try {
            // Check for Forge
            Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            return true;
        } catch (ClassNotFoundException e) {
            // Not in Forge environment
        }

        return false;
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            // Fallback for edge cases
            return false;
        }
    }

    public static boolean isConnector() {
        return isConnectorEnvironment;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(mixinPackage)) {
            throw new RuntimeException("Mixin " + mixinClassName + " is not in the mixin package");
        }
        else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
            return !isOriginsPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".sodium")) {
            // In Connector environment, sodium mixins target Embeddium/Rubidium
            return isSodiumPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".indigo")) {
            // Indigo is Fabric-specific, disable in Connector
            return isIndigoPresent && !isConnectorEnvironment;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".canvas")) {
            return isCanvasPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".lithium")) {
            return isLithiumPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".indium")) {
            // Indium is Fabric-specific, disable in Connector
            return isIndiumPresent && !isConnectorEnvironment;
        }


        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
