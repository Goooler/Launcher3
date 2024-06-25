/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.util;

import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.PatternMatcher;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.android.launcher3.BuildConfig;

import java.util.function.Consumer;

public class SimpleBroadcastReceiver extends BroadcastReceiver {

    private final Consumer<Intent> mIntentConsumer;

    public SimpleBroadcastReceiver(Consumer<Intent> intentConsumer) {
        mIntentConsumer = intentConsumer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mIntentConsumer.accept(intent);
    }

    /** Helper method to register multiple actions. Caller should be on main thread. */
    @UiThread
    public void registerAsync(Context context, String... actions) {
        assertOnMainThread();
        UI_HELPER_EXECUTOR.execute(() -> registerSync(context, actions));
    }

    /** Helper method to register multiple actions. Caller should be on main thread. */
    @WorkerThread
    public void registerSync(Context context, String... actions) {
        assertOnBgThread();
        context.registerReceiver(this, getFilter(actions));
    }

    /**
     * Helper method to register multiple actions associated with a action. Caller should be from
     * main thread.
     */
    @UiThread
    public void registerPkgActionsAsync(Context context, @Nullable String pkg, String... actions) {
        assertOnMainThread();
        UI_HELPER_EXECUTOR.execute(() -> registerPkgActionsSync(context, pkg, actions));
    }

    /**
     * Helper method to register multiple actions associated with a action. Caller should be from
     * bg thread.
     */
    @WorkerThread
    public void registerPkgActionsSync(Context context, @Nullable String pkg, String... actions) {
        assertOnBgThread();
        context.registerReceiver(this, getPackageFilter(pkg, actions));
    }

    /**
     * Unregisters the receiver ignoring any errors on bg thread. Caller should be on main thread.
     */
    @UiThread
    public void unregisterReceiverSafelyAsync(Context context) {
        assertOnMainThread();
        UI_HELPER_EXECUTOR.execute(() -> unregisterReceiverSafelySync(context));
    }

    /**
     * Unregisters the receiver ignoring any errors on bg thread. Caller should be on bg thread.
     */
    @WorkerThread
    public void unregisterReceiverSafelySync(Context context) {
        assertOnBgThread();
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // It was probably never registered or already unregistered. Ignore.
        }
    }

    /**
     * Creates an intent filter to listen for actions with a specific package in the data field.
     */
    public static IntentFilter getPackageFilter(String pkg, String... actions) {
        IntentFilter filter = getFilter(actions);
        filter.addDataScheme("package");
        if (!TextUtils.isEmpty(pkg)) {
            filter.addDataSchemeSpecificPart(pkg, PatternMatcher.PATTERN_LITERAL);
        }
        return filter;
    }

    private static IntentFilter getFilter(String... actions) {
        IntentFilter filter = new IntentFilter();
        for (String action : actions) {
            filter.addAction(action);
        }
        return filter;
    }

    private static void assertOnBgThread() {
        if (BuildConfig.IS_STUDIO_BUILD && isMainThread()) {
            throw new IllegalStateException("Should not be called from main thread!");
        }
    }

    private static void assertOnMainThread() {
        if (BuildConfig.IS_STUDIO_BUILD && !isMainThread()) {
            throw new IllegalStateException("Should not be called from bg thread!");
        }
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
