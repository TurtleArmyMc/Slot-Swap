package com.turtlearmymc.slotswap.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

public class LoadCompat {


    @NotNull
    public static Action getActivationAction() {

        return getAutoHudAction();
    }

    private static Action getAutoHudAction() {
        if (FabricLoader.getInstance().isModLoaded("autohud")) {
            return new AutoHudCompatibility();
        } else {
            return () -> {};
        }
    }


    @FunctionalInterface
    public interface Action {
        void invoke();
    }
}
