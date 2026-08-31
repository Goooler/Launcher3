package com.android.systemui.shared;

public final class Flags {
    private static FeatureFlags FEATURE_FLAGS = new FeatureFlagsImpl();

    public static final String FLAG_SIDEFPS_CONTROLLER_REFACTOR = "com.android.systemui.shared.sidefps_controller_refactor";
    public static boolean sidefpsControllerRefactor() {
        return FEATURE_FLAGS.sidefpsControllerRefactor();
    }

    public static final String FLAG_AOD_INACTIVITY_DETECTION = "com.android.systemui.shared.aod_inactivity_detection";
    public static boolean aodInactivityDetection() {
        return FEATURE_FLAGS.aodInactivityDetection();
    }

    public static final String FLAG_BLUEFLAX_FIRST_PHASE = "com.android.systemui.shared.blueflax_first_phase";
    public static boolean blueflaxFirstPhase() {
        return FEATURE_FLAGS.blueflaxFirstPhase();
    }

    public static final String FLAG_BOUNCER_AREA_EXCLUSION = "com.android.systemui.shared.bouncer_area_exclusion";
    public static boolean bouncerAreaExclusion() {
        return FEATURE_FLAGS.bouncerAreaExclusion();
    }

    public static final String FLAG_BRIGHTNESS_DIALOG_ON_SYSTEM_USER = "com.android.systemui.shared.brightness_dialog_on_system_user";
    public static boolean brightnessDialogOnSystemUser() {
        return FEATURE_FLAGS.brightnessDialogOnSystemUser();
    }

    public static final String FLAG_CUE_BAR_ACE_MIGRATION = "com.android.systemui.shared.cue_bar_ace_migration";
    public static boolean cueBarAceMigration() {
        return FEATURE_FLAGS.cueBarAceMigration();
    }

    public static final String FLAG_CURSOR_HOT_CORNER = "com.android.systemui.shared.cursor_hot_corner";
    public static boolean cursorHotCorner() {
        return FEATURE_FLAGS.cursorHotCorner();
    }

    public static final String FLAG_ENABLE_AI_CLOCKS = "com.android.systemui.shared.enable_ai_clocks";
    public static boolean enableAiClocks() {
        return FEATURE_FLAGS.enableAiClocks();
    }

    public static final String FLAG_ENABLE_HOME_DELAY = "com.android.systemui.shared.enable_home_delay";
    public static boolean enableHomeDelay() {
        return FEATURE_FLAGS.enableHomeDelay();
    }

    public static final String FLAG_ENABLE_LPP_ASSIST_INVOCATION_INITIAL_RUMBLE = "com.android.systemui.shared.enable_lpp_assist_invocation_initial_rumble";
    public static boolean enableLppAssistInvocationInitialRumble() {
        return FEATURE_FLAGS.enableLppAssistInvocationInitialRumble();
    }

    public static final String FLAG_ENABLE_RECENTS_IN_TASKBAR = "com.android.systemui.shared.enable_recents_in_taskbar";
    public static boolean enableRecentsInTaskbar() {
        return FEATURE_FLAGS.enableRecentsInTaskbar();
    }

    public static final String FLAG_ENABLE_SAGE = "com.android.systemui.shared.enable_sage";
    public static boolean enableSage() {
        return FEATURE_FLAGS.enableSage();
    }

    public static final String FLAG_EXAMPLE_SHARED_FLAG = "com.android.systemui.shared.example_shared_flag";
    public static boolean exampleSharedFlag() {
        return FEATURE_FLAGS.exampleSharedFlag();
    }

    public static final String FLAG_EXTENDED_WALLPAPER_EFFECTS = "com.android.systemui.shared.extended_wallpaper_effects";
    public static boolean extendedWallpaperEffects() {
        return FEATURE_FLAGS.extendedWallpaperEffects();
    }

    public static final String FLAG_EXTENDIBLE_THEME_MANAGER = "com.android.systemui.shared.extendible_theme_manager";
    public static boolean extendibleThemeManager() {
        return FEATURE_FLAGS.extendibleThemeManager();
    }

    public static final String FLAG_LAUNCHER_ANIMATION_SHELL_MIGRATION = "com.android.systemui.shared.launcher_animation_shell_migration";
    public static boolean launcherAnimationShellMigration() {
        return FEATURE_FLAGS.launcherAnimationShellMigration();
    }

    public static final String FLAG_PAN_AND_ZOOM_IN_EXTENDED_WALLPAPER_EFFECTS = "com.android.systemui.shared.pan_and_zoom_in_extended_wallpaper_effects";
    public static boolean panAndZoomInExtendedWallpaperEffects() {
        return FEATURE_FLAGS.panAndZoomInExtendedWallpaperEffects();
    }

    public static final String FLAG_PHOTO_SHUFFLE_FLAG = "com.android.systemui.shared.photo_shuffle_flag";
    public static boolean photoShuffleFlag() {
        return FEATURE_FLAGS.photoShuffleFlag();
    }

    public static final String FLAG_SHADE_ALLOW_BACK_GESTURE = "com.android.systemui.shared.shade_allow_back_gesture";
    public static boolean shadeAllowBackGesture() {
        return FEATURE_FLAGS.shadeAllowBackGesture();
    }

    public static final String FLAG_SMARTSPACE_AQI_UPDATED_DESIGN = "com.android.systemui.shared.smartspace_aqi_updated_design";
    public static boolean smartspaceAqiUpdatedDesign() {
        return FEATURE_FLAGS.smartspaceAqiUpdatedDesign();
    }

    public static final String FLAG_SMARTSPACE_SEMANTIC_WEATHER_DATA = "com.android.systemui.shared.smartspace_semantic_weather_data";
    public static boolean smartspaceSemanticWeatherData() {
        return FEATURE_FLAGS.smartspaceSemanticWeatherData();
    }

    public static final String FLAG_SMARTSPACE_SPORTS_CARD_BACKGROUND = "com.android.systemui.shared.smartspace_sports_card_background";
    public static boolean smartspaceSportsCardBackground() {
        return FEATURE_FLAGS.smartspaceSportsCardBackground();
    }

    public static final String FLAG_SMARTSPACE_WEATHER_USE_MONOCHROME_FONT_ICONS = "com.android.systemui.shared.smartspace_weather_use_monochrome_font_icons";
    public static boolean smartspaceWeatherUseMonochromeFontIcons() {
        return FEATURE_FLAGS.smartspaceWeatherUseMonochromeFontIcons();
    }

    public static final String FLAG_THREE_BUTTON_CORNER_SWIPE = "com.android.systemui.shared.three_button_corner_swipe";
    public static boolean threeButtonCornerSwipe() {
        return FEATURE_FLAGS.threeButtonCornerSwipe();
    }

    public static final String FLAG_WORKSPACE_ITEMS_LABEL_HIDDEN = "com.android.systemui.shared.workspace_items_label_hidden";
    public static boolean workspaceItemsLabelHidden() {
        return FEATURE_FLAGS.workspaceItemsLabelHidden();
    }

}
