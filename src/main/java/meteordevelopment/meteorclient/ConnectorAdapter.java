package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.meteorclient.utils.network.Capes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.misc.CPSUtils;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shaders;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.postprocess.ChamsShader;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.commands.Commands;

public class ConnectorAdapter {
    public static void manualPreInit() {
        try {
            MeteorExecutor.init();
            Shaders.init();
            
            Utils.init();
            BlockIterator.init();
            BlockUtils.init();
            PostProcessShaders.init();
            Capes.init();
            Tabs.init();
            DamageUtils.init();
            Rotations.init();
            EChestMemory.init();
            CPSUtils.init();
            FakeClientPlayer.init();
            Names.init();
            MeteorStarscript.init();
            GuiThemes.init();
            GL.init();
            PostProcessRenderer.init();
            Renderer2D.init();
            Fonts.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void manualPostInit() {
        try {
            PlayerHeadUtils.init();
            RenderUtils.init();
            ChamsShader.load();
            RainbowColors.init();
            ChatUtils.init();
            GuiThemes.postInit();
            GuiRenderer.init();
            Commands.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
