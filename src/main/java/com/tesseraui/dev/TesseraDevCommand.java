package com.tesseraui.dev;

import com.tesseraui.TesseraHotReload;
import com.tesseraui.TesseraUI;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Registers the {@code /tessera test} client command that opens the developer
 * test screen for TesseraUI v1.1 features.
 *
 * <p>Usage in-game: {@code /tessera test}</p>
 */
@EventBusSubscriber(modid = TesseraUI.MOD_ID, value = Dist.CLIENT)
public final class TesseraDevCommand {

    private TesseraDevCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("tessera")
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        TesseraHotReload.invalidateAll();
                        ctx.getSource().sendSystemMessage(
                            Component.literal("[TesseraUI] Template cache cleared — reopen any screen to reload."));
                        return 1;
                    })
                )
                .then(Commands.literal("test")
                    .executes(ctx -> {
                        // Must be scheduled on the main thread — brigadier may call from any thread
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreen()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-medium")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenMedium()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-low")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenLow()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-html")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenHtml()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-table")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenTable()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-radius")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenRadius()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v12")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV12()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v13")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV13()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v14")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV14()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v15")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV15()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v16")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV16()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v17")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV17()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v18")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV18()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v19")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV19()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v20")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV20()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v21")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV21()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v22")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV22()));
                        return 1;
                    })
                )
                .then(Commands.literal("test-v23")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(
                            () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV23()));
                        return 1;
                    })
                )
        );
    }
}
