package com.tesseraui;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * TesseraUI mod entry point.
 *
 * <p>On client setup this class enables {@link TesseraHotReload} when appropriate:</p>
 * <ul>
 *   <li><b>Explicit</b>: JVM flag {@code -Dtessera.hotreload=src/main/resources}</li>
 *   <li><b>Auto-detect</b>: dev environment + {@code src/main/resources/assets/} exists
 *       relative to the working directory (standard {@code ./gradlew runClient} layout)</li>
 * </ul>
 *
 * <p>On F3+T (resource reload) the template cache is cleared so stale parsed templates
 * are never served.</p>
 */
@Mod(TesseraUI.MOD_ID)
public class TesseraUI {

    public static final String MOD_ID = "tesseraui";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TesseraUI() {
        // Bus listeners registered via @EventBusSubscriber below
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ClientSetup {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Enable hot reload: explicit flag or auto-detect in dev
            TesseraHotReload.tryEnable(!FMLEnvironment.production);
        }

        /**
         * Called when the player presses F3+T (resource reload).
         * Clears the template cache so the next {@link TesseraTemplate#load} call
         * re-parses the files (from disk in hot-reload mode, from the ResourceManager otherwise).
         */
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            // PreparableReloadListener signature in MC 1.21:
            // reload(barrier, resourceManager, prepProfiler, applyProfiler, bgExecutor, gameExecutor)
            event.registerReloadListener(
                (barrier, rm, prepProfiler, applyProfiler, bgExecutor, gameExecutor) ->
                    barrier.wait(null).thenRunAsync(() -> {
                        TesseraHotReload.invalidateAll();
                        LOGGER.debug("[TesseraUI] Template cache cleared on resource reload");
                    }, gameExecutor)
            );
        }
    }
}
