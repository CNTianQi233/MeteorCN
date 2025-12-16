/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IChatHudLine;
import meteordevelopment.meteorclient.mixininterface.IChatHudLineVisible;
import meteordevelopment.meteorclient.mixininterface.IMessageHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow @Final private List<ChatHudLine> messages;

    @Unique private BetterChat betterChat;
    @Unique private int nextId;
    @Unique private boolean skipOnAddMessage;

    @Shadow
    protected abstract void addMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh);

    @Shadow
    public abstract void addMessage(Text message);

    @Override
    public void meteor$add(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = 0;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) visibleMessages.get(0)).meteor$setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) messages.get(0)).meteor$setId(nextId);
    }

    // Set sender info on visible messages after they are added to the list
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onAddMessage_modifyChatHudLineVisible(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        if (!visibleMessages.isEmpty()) {
            IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
            IChatHudLineVisible meteorLine = (IChatHudLineVisible) (Object) visibleMessages.get(0);
            meteorLine.meteor$setSender(handler.meteor$getSender());
            // Note: startOfEntry tracking is simplified - first added line is start of entry
            meteorLine.meteor$setStartOfEntry(true);
        }
    }

    // Set sender info on messages after they are added to the list
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onAddMessage_modifyChatHudLine(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        if (!messages.isEmpty()) {
            IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
            ((IChatHudLine) (Object) messages.get(0)).meteor$setSender(handler.meteor$getSender());
        }
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", cancellable = true)
    private void onAddMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        if (skipOnAddMessage) return;

        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) info.cancel();
        else {
            visibleMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).meteor$getId() == nextId && nextId != 0);

            for (int i = messages.size() - 1; i > -1 ; i--) {
                if (((IChatHudLine) (Object) messages.get(i)).meteor$getId() == nextId && nextId != 0) {
                    messages.remove(i);
                    Modules.get().get(BetterChat.class).lines.removeInt(i);
                }
            }

            if (event.isModified()) {
                info.cancel();

                skipOnAddMessage = true;
                addMessage(event.getMessage(), signature, ticks, event.getIndicator(), refresh);
                skipOnAddMessage = false;
            }
        }
    }

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;visibleMessages:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"), require = 0)
    private int addMessageListSizeProxy(List<?> list) {
        int size = list.size();
        BetterChat betterChat = getBetterChat();
        if (betterChat.isLongerChat() && betterChat.getChatLength() >= 100) return size - betterChat.getChatLength();
        return size;
    }

    // Player Heads

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I"), require = 0)
    private int onRender_modifyWidth(float value) {
        int width = MathHelper.ceil(value);
        return getBetterChat().modifyChatWidth(width);
    }

    // Note: Player head drawing moved to event-based system for Forge compatibility
    // The drawPlayerHead functionality is handled through BetterChat module's tick/render events

    // No Message Signature Indicator

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"), require = 0)
    private MessageIndicator onRender_modifyIndicator(ChatHudLine.Visible line) {
        MessageIndicator indicator = line.indicator();
        return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
    }

    // Anti spam

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z"), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
    private void onBreakChatMessageLines(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci, int i, List<OrderedText> list) {
        if (list != null) getBetterChat().lines.add(0, list.size());
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;messages:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
    private void onRemoveMessage(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci) {
        BetterChat betterChat = getBetterChat();
        int size = betterChat.lines.size() - (betterChat.isLongerChat() && betterChat.getChatLength() >= 100 ? betterChat.getChatLength() : 0);

        while (size > 100) {
            betterChat.lines.removeInt(size - 1);
            size--;
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    @Inject(method = "refresh", at = @At("HEAD"))
    private void onRefresh(CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    // Other
    @Unique
    private BetterChat getBetterChat() {
        if (betterChat == null) {
            betterChat = Modules.get().get(BetterChat.class);
        }

        return betterChat;
    }
}
