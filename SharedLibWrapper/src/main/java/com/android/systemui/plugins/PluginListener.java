package com.android.systemui.plugins;

import android.content.Context;

public interface PluginListener<T extends Plugin> {
    void onPluginConnected(T plugin, Context pluginContext);
    default void onPluginDisconnected(T plugin) {}
    default void onPluginLoaded(T plugin, Context pluginContext, Object manager) {}
}
