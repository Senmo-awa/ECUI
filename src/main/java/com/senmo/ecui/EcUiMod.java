package com.senmo.ecui;

import com.senmo.ecui.data.HomeScanner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Essential Commands UI.
 *
 * <p>本 mod 不复制 EC 的数据结构，而是直接读取 EC 的 PlayerData / WorldData，
 * 因此 /sethome、/warp set 等操作仍然由 EC 负责，UI 只是另一个入口。</p>
 */
public final class EcUiMod implements ModInitializer {
    public static final String MOD_ID = "ec_ui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        EcUiConfig.load();
        EcUiCommands.register();
        EcCommandHook.register();
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> HomeScanner.shutdown());

        LOGGER.info("Essential Commands UI 已加载（{} / 语言包 zh_cn + en_us）。",
            net.minecraft.SharedConstants.getCurrentVersion().name());
    }
}
