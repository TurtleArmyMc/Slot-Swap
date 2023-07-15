package com.turtlearmymc.slotswap.compat;

import mod.crend.autohud.component.Component;

public class AutoHudCompatibility implements LoadCompat.Action {

    @Override
    public void invoke() {
        Component.Hotbar.revealCombined();
    }
}
