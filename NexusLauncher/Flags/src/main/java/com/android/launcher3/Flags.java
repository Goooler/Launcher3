package com.android.launcher3;

public final class Flags {
    private static FeatureFlags FEATURE_FLAGS = new FeatureFlagsImpl();

    public static final String FLAG_ENABLE_LATER_IS_LOCKED_CHECK = "com.android.launcher3.enable_later_is_locked_check";
    public static boolean enableLaterIsLockedCheck() {
        return FEATURE_FLAGS.enableLaterIsLockedCheck();
    }

    public static final String FLAG_ENABLE_OVERVIEW_DESKTOP_TILE_WALLPAPER_BACKGROUND = "com.android.launcher3.enable_overview_desktop_tile_wallpaper_background";
    public static boolean enableOverviewDesktopTileWallpaperBackground() {
        return FEATURE_FLAGS.enableOverviewDesktopTileWallpaperBackground();
    }

    public static final String FLAG_ENABLE_REFACTOR_DIGITAL_WELLBEING_TOAST = "com.android.launcher3.enable_refactor_digital_wellbeing_toast";
    public static boolean enableRefactorDigitalWellbeingToast() {
        return FEATURE_FLAGS.enableRefactorDigitalWellbeingToast();
    }

    public static final String FLAG_ENABLE_SIMULTANEOUS_OVERVIEW_TRIGGER_ON_EXTENDED_DESKTOP = "com.android.launcher3.enable_simultaneous_overview_trigger_on_extended_desktop";
    public static boolean enableSimultaneousOverviewTriggerOnExtendedDesktop() {
        return FEATURE_FLAGS.enableSimultaneousOverviewTriggerOnExtendedDesktop();
    }

    public static final String FLAG_ENABLE_PREDICTIVE_BACK_IN_OVERVIEW = "com.android.launcher3.enable_predictive_back_in_overview";
    public static boolean enablePredictiveBackInOverview() {
        return FEATURE_FLAGS.enablePredictiveBackInOverview();
    }

    public static final String FLAG_ENABLE_OVERVIEW_PAGINATION = "com.android.launcher3.enable_overview_pagination";
    public static boolean enableOverviewPagination() {
        return FEATURE_FLAGS.enableOverviewPagination();
    }

    public static final String FLAG_HIDE_AUTOMATED_TASKS_IN_OVERVIEW = "com.android.launcher3.hide_automated_tasks_in_overview";
    public static boolean hideAutomatedTasksInOverview() {
        return FEATURE_FLAGS.hideAutomatedTasksInOverview();
    }

    public static final String FLAG_ENABLE_LOW_RES_THUMBNAIL_PRELOADING = "com.android.launcher3.enable_low_res_thumbnail_preloading";
    public static boolean enableLowResThumbnailPreloading() {
        return FEATURE_FLAGS.enableLowResThumbnailPreloading();
    }

    public static final String FLAG_ENABLE_TASKS_DRAG_AND_DROP_IN_OVERVIEW = "com.android.launcher3.enable_tasks_drag_and_drop_in_overview";
    public static boolean enableTasksDragAndDropInOverview() {
        return FEATURE_FLAGS.enableTasksDragAndDropInOverview();
    }

    public static final String FLAG_ENABLE_OVERVIEW_SELECT_TEXT_VIEW = "com.android.launcher3.enable_overview_select_text_view";
    public static boolean enableOverviewSelectTextView() {
        return FEATURE_FLAGS.enableOverviewSelectTextView();
    }

    public static final String FLAG_UPDATE_RECENTS_WM_WWM_CONFIGURATION = "com.android.launcher3.update_recents_wm_wwm_configuration";
    public static boolean updateRecentsWmWwmConfiguration() {
        return FEATURE_FLAGS.updateRecentsWmWwmConfiguration();
    }

    public static final String FLAG_ENABLE_RECENTS_WINDOW_BLUR = "com.android.launcher3.enable_recents_window_blur";
    public static boolean enableRecentsWindowBlur() {
        return FEATURE_FLAGS.enableRecentsWindowBlur();
    }

    public static final String FLAG_ENABLE_SAVE_ACTION_IN_OVERVIEW_SHARE = "com.android.launcher3.enable_save_action_in_overview_share";
    public static boolean enableSaveActionInOverviewShare() {
        return FEATURE_FLAGS.enableSaveActionInOverviewShare();
    }

    public static final String FLAG_ENABLE_WIDGETS_SECTION_IN_OPTIONS_MENU = "com.android.launcher3.enable_widgets_section_in_options_menu";
    public static boolean enableWidgetsSectionInOptionsMenu() {
        return FEATURE_FLAGS.enableWidgetsSectionInOptionsMenu();
    }

    public static final String FLAG_ENABLE_COMPOSE_FOR_OPTIONS_POPUP_MENU_DIALOG = "com.android.launcher3.enable_compose_for_options_popup_menu_dialog";
    public static boolean enableComposeForOptionsPopupMenuDialog() {
        return FEATURE_FLAGS.enableComposeForOptionsPopupMenuDialog();
    }

    public static final String FLAG_ENABLE_EXPANDING_PAUSE_WORK_BUTTON = "com.android.launcher3.enable_expanding_pause_work_button";
    public static boolean enableExpandingPauseWorkButton() {
        return FEATURE_FLAGS.enableExpandingPauseWorkButton();
    }

    public static final String FLAG_ENABLE_TWOLINE_ALLAPPS = "com.android.launcher3.enable_twoline_allapps";
    public static boolean enableTwolineAllapps() {
        return FEATURE_FLAGS.enableTwolineAllapps();
    }

    public static final String FLAG_ENABLE_TWOLINE_TOGGLE = "com.android.launcher3.enable_twoline_toggle";
    public static boolean enableTwolineToggle() {
        return FEATURE_FLAGS.enableTwolineToggle();
    }

    public static final String FLAG_ENABLE_RESPONSIVE_WORKSPACE = "com.android.launcher3.enable_responsive_workspace";
    public static boolean enableResponsiveWorkspace() {
        return FEATURE_FLAGS.enableResponsiveWorkspace();
    }

    public static final String FLAG_ENABLE_FOCUS_OUTLINE = "com.android.launcher3.enable_focus_outline";
    public static boolean enableFocusOutline() {
        return FEATURE_FLAGS.enableFocusOutline();
    }

    public static final String FLAG_ENABLE_TASKBAR_NO_RECREATE = "com.android.launcher3.enable_taskbar_no_recreate";
    public static boolean enableTaskbarNoRecreate() {
        return FEATURE_FLAGS.enableTaskbarNoRecreate();
    }

    public static final String FLAG_ENABLE_TASKBAR_PINNING = "com.android.launcher3.enable_taskbar_pinning";
    public static boolean enableTaskbarPinning() {
        return FEATURE_FLAGS.enableTaskbarPinning();
    }

    public static final String FLAG_ENABLE_TASKBAR_CUSTOMIZATION = "com.android.launcher3.enable_taskbar_customization";
    public static boolean enableTaskbarCustomization() {
        return FEATURE_FLAGS.enableTaskbarCustomization();
    }

    public static final String FLAG_ENABLE_TASKBAR_ICON_CONTAINER = "com.android.launcher3.enable_taskbar_icon_container";
    public static boolean enableTaskbarIconContainer() {
        return FEATURE_FLAGS.enableTaskbarIconContainer();
    }

    public static final String FLAG_ENABLE_TASKBAR_ICON_DESKTOP_CLICKS = "com.android.launcher3.enable_taskbar_icon_desktop_clicks";
    public static boolean enableTaskbarIconDesktopClicks() {
        return FEATURE_FLAGS.enableTaskbarIconDesktopClicks();
    }

    public static final String FLAG_ENABLE_TWO_PANE_LAUNCHER_SETTINGS = "com.android.launcher3.enable_two_pane_launcher_settings";
    public static boolean enableTwoPaneLauncherSettings() {
        return FEATURE_FLAGS.enableTwoPaneLauncherSettings();
    }

    public static final String FLAG_ENABLE_SUPPORT_FOR_ARCHIVING = "com.android.launcher3.enable_support_for_archiving";
    public static boolean enableSupportForArchiving() {
        return FEATURE_FLAGS.enableSupportForArchiving();
    }

    public static final String FLAG_ENABLE_REBOOT_UNLOCK_ANIMATION = "com.android.launcher3.enable_reboot_unlock_animation";
    public static boolean enableRebootUnlockAnimation() {
        return FEATURE_FLAGS.enableRebootUnlockAnimation();
    }

    public static final String FLAG_ENABLE_UNFOLD_STATE_ANIMATION = "com.android.launcher3.enable_unfold_state_animation";
    public static boolean enableUnfoldStateAnimation() {
        return FEATURE_FLAGS.enableUnfoldStateAnimation();
    }

    public static final String FLAG_FORCE_MONOCHROME_APP_ICONS = "com.android.launcher3.force_monochrome_app_icons";
    public static boolean forceMonochromeAppIcons() {
        return FEATURE_FLAGS.forceMonochromeAppIcons();
    }

    public static final String FLAG_ENABLE_NARROW_GRID_RESTORE = "com.android.launcher3.enable_narrow_grid_restore";
    public static boolean enableNarrowGridRestore() {
        return FEATURE_FLAGS.enableNarrowGridRestore();
    }

    public static final String FLAG_ENABLE_FALLBACK_OVERVIEW_IN_WINDOW = "com.android.launcher3.enable_fallback_overview_in_window";
    public static boolean enableFallbackOverviewInWindow() {
        return FEATURE_FLAGS.enableFallbackOverviewInWindow();
    }

    public static final String FLAG_ENABLED_FOLDERS_IN_ALL_APPS = "com.android.launcher3.enabled_folders_in_all_apps";
    public static boolean enabledFoldersInAllApps() {
        return FEATURE_FLAGS.enabledFoldersInAllApps();
    }

    public static final String FLAG_FLOATING_SEARCH_BAR = "com.android.launcher3.floating_search_bar";
    public static boolean floatingSearchBar() {
        return FEATURE_FLAGS.floatingSearchBar();
    }

    public static final String FLAG_BLURRED_HOME_ANIMATION = "com.android.launcher3.blurred_home_animation";
    public static boolean blurredHomeAnimation() {
        return FEATURE_FLAGS.blurredHomeAnimation();
    }

    public static final String FLAG_ALL_APPS_SURFACE = "com.android.launcher3.all_apps_surface";
    public static boolean allAppsSurface() {
        return FEATURE_FLAGS.allAppsSurface();
    }

    public static final String FLAG_ENABLE_MULTI_INSTANCE_MENU_TASKBAR = "com.android.launcher3.enable_multi_instance_menu_taskbar";
    public static boolean enableMultiInstanceMenuTaskbar() {
        return FEATURE_FLAGS.enableMultiInstanceMenuTaskbar();
    }

    public static final String FLAG_LETTER_FAST_SCROLLER = "com.android.launcher3.letter_fast_scroller";
    public static boolean letterFastScroller() {
        return FEATURE_FLAGS.letterFastScroller();
    }

    public static final String FLAG_IGNORE_THREE_FINGER_TRACKPAD_FOR_NAV_HANDLE_LONG_PRESS = "com.android.launcher3.ignore_three_finger_trackpad_for_nav_handle_long_press";
    public static boolean ignoreThreeFingerTrackpadForNavHandleLongPress() {
        return FEATURE_FLAGS.ignoreThreeFingerTrackpadForNavHandleLongPress();
    }

    public static final String FLAG_ONE_GRID_ROTATION_HANDLING = "com.android.launcher3.one_grid_rotation_handling";
    public static boolean oneGridRotationHandling() {
        return FEATURE_FLAGS.oneGridRotationHandling();
    }

    public static final String FLAG_ENABLE_ALL_APPS_BUTTON_IN_HOTSEAT = "com.android.launcher3.enable_all_apps_button_in_hotseat";
    public static boolean enableAllAppsButtonInHotseat() {
        return FEATURE_FLAGS.enableAllAppsButtonInHotseat();
    }

    public static final String FLAG_TASKBAR_QUIET_MODE_CHANGE_SUPPORT = "com.android.launcher3.taskbar_quiet_mode_change_support";
    public static boolean taskbarQuietModeChangeSupport() {
        return FEATURE_FLAGS.taskbarQuietModeChangeSupport();
    }

    public static final String FLAG_ENABLE_RECENTS_WINDOW_PROTO_LOG = "com.android.launcher3.enable_recents_window_proto_log";
    public static boolean enableRecentsWindowProtoLog() {
        return FEATURE_FLAGS.enableRecentsWindowProtoLog();
    }

    public static final String FLAG_ENABLE_STATE_MANAGER_PROTO_LOG = "com.android.launcher3.enable_state_manager_proto_log";
    public static boolean enableStateManagerProtoLog() {
        return FEATURE_FLAGS.enableStateManagerProtoLog();
    }

    public static final String FLAG_ENABLE_LAUNCHER_OVERVIEW_IN_WINDOW = "com.android.launcher3.enable_launcher_overview_in_window";
    public static boolean enableLauncherOverviewInWindow() {
        return FEATURE_FLAGS.enableLauncherOverviewInWindow();
    }

    public static final String FLAG_ENABLE_CONTRAST_TILES = "com.android.launcher3.enable_contrast_tiles";
    public static boolean enableContrastTiles() {
        return FEATURE_FLAGS.enableContrastTiles();
    }

    public static final String FLAG_MSDL_FEEDBACK = "com.android.launcher3.msdl_feedback";
    public static boolean msdlFeedback() {
        return FEATURE_FLAGS.msdlFeedback();
    }

    public static final String FLAG_ENABLE_LAUNCHER_ICON_SHAPES = "com.android.launcher3.enable_launcher_icon_shapes";
    public static boolean enableLauncherIconShapes() {
        return FEATURE_FLAGS.enableLauncherIconShapes();
    }

    public static final String FLAG_RESTORE_ARCHIVED_APP_ICONS_FROM_DB = "com.android.launcher3.restore_archived_app_icons_from_db";
    public static boolean restoreArchivedAppIconsFromDb() {
        return FEATURE_FLAGS.restoreArchivedAppIconsFromDb();
    }

    public static final String FLAG_ENABLE_MOUSE_INTERACTION_CHANGES = "com.android.launcher3.enable_mouse_interaction_changes";
    public static boolean enableMouseInteractionChanges() {
        return FEATURE_FLAGS.enableMouseInteractionChanges();
    }

    public static final String FLAG_ENABLE_STRICT_MODE = "com.android.launcher3.enable_strict_mode";
    public static boolean enableStrictMode() {
        return FEATURE_FLAGS.enableStrictMode();
    }

    public static final String FLAG_ENABLE_ALT_TAB_KQS_FLATENNING = "com.android.launcher3.enable_alt_tab_kqs_flatenning";
    public static boolean enableAltTabKqsFlatenning() {
        return FEATURE_FLAGS.enableAltTabKqsFlatenning();
    }

    public static final String FLAG_ENABLE_TASKBAR_BEHIND_SHADE = "com.android.launcher3.enable_taskbar_behind_shade";
    public static boolean enableTaskbarBehindShade() {
        return FEATURE_FLAGS.enableTaskbarBehindShade();
    }

    public static final String FLAG_ENABLE_SCALABILITY_FOR_DESKTOP_EXPERIENCE = "com.android.launcher3.enable_scalability_for_desktop_experience";
    public static boolean enableScalabilityForDesktopExperience() {
        return FEATURE_FLAGS.enableScalabilityForDesktopExperience();
    }

    public static final String FLAG_ENABLE_TASKBAR_UI_THREAD = "com.android.launcher3.enable_taskbar_ui_thread";
    public static boolean enableTaskbarUiThread() {
        return FEATURE_FLAGS.enableTaskbarUiThread();
    }

    public static final String FLAG_ENABLE_EXPRESSIVE_FOLDER_EXPANSION = "com.android.launcher3.enable_expressive_folder_expansion";
    public static boolean enableExpressiveFolderExpansion() {
        return FEATURE_FLAGS.enableExpressiveFolderExpansion();
    }

    public static final String FLAG_ENABLE_FOLDER_REFACTOR = "com.android.launcher3.enable_folder_refactor";
    public static boolean enableFolderRefactor() {
        return FEATURE_FLAGS.enableFolderRefactor();
    }

    public static final String FLAG_MODEL_REPOSITORY = "com.android.launcher3.model_repository";
    public static boolean modelRepository() {
        return FEATURE_FLAGS.modelRepository();
    }

    public static final String FLAG_SIMPLIFIED_LAUNCHER_MODEL_BINDING = "com.android.launcher3.simplified_launcher_model_binding";
    public static boolean simplifiedLauncherModelBinding() {
        return FEATURE_FLAGS.simplifiedLauncherModelBinding();
    }

    public static final String FLAG_HOME_SCREEN_EDIT_IMPROVEMENTS = "com.android.launcher3.home_screen_edit_improvements";
    public static boolean homeScreenEditImprovements() {
        return FEATURE_FLAGS.homeScreenEditImprovements();
    }

    public static final String FLAG_ENABLE_REVERSIBLE_HOME_ACTION_CORNER = "com.android.launcher3.enable_reversible_home_action_corner";
    public static boolean enableReversibleHomeActionCorner() {
        return FEATURE_FLAGS.enableReversibleHomeActionCorner();
    }

    public static final String FLAG_AVOID_DISPLAY_CUTOUT_BUBBLE_BAR = "com.android.launcher3.avoid_display_cutout_bubble_bar";
    public static boolean avoidDisplayCutoutBubbleBar() {
        return FEATURE_FLAGS.avoidDisplayCutoutBubbleBar();
    }

    public static final String FLAG_ENABLE_TASKBAR_DRAG_AND_DROP = "com.android.launcher3.enable_taskbar_drag_and_drop";
    public static boolean enableTaskbarDragAndDrop() {
        return FEATURE_FLAGS.enableTaskbarDragAndDrop();
    }

    public static final String FLAG_ENABLE_TASKBAR_DRAG_TO_REMOVE = "com.android.launcher3.enable_taskbar_drag_to_remove";
    public static boolean enableTaskbarDragToRemove() {
        return FEATURE_FLAGS.enableTaskbarDragToRemove();
    }

    public static final String FLAG_SHOW_FILES_ON_HOME_SCREEN = "com.android.launcher3.show_files_on_home_screen";
    public static boolean showFilesOnHomeScreen() {
        return FEATURE_FLAGS.showFilesOnHomeScreen();
    }

    public static final String FLAG_ENABLE_SYSTEM_DRAG = "com.android.launcher3.enable_system_drag";
    public static boolean enableSystemDrag() {
        return FEATURE_FLAGS.enableSystemDrag();
    }

    public static final String FLAG_ENABLE_SYSTEM_DRAG_TO_OTHER_APPS = "com.android.launcher3.enable_system_drag_to_other_apps";
    public static boolean enableSystemDragToOtherApps() {
        return FEATURE_FLAGS.enableSystemDragToOtherApps();
    }

    public static final String FLAG_FORCE_MONOCHROME_APP_ICONS_ADAPT_COLORS = "com.android.launcher3.force_monochrome_app_icons_adapt_colors";
    public static boolean forceMonochromeAppIconsAdaptColors() {
        return FEATURE_FLAGS.forceMonochromeAppIconsAdaptColors();
    }

    public static final String FLAG_ENABLE_CUSTOM_HEIGHT_FOR_ALL_APPS_ON_CD = "com.android.launcher3.enable_custom_height_for_all_apps_on_cd";
    public static boolean enableCustomHeightForAllAppsOnCd() {
        return FEATURE_FLAGS.enableCustomHeightForAllAppsOnCd();
    }

    public static final String FLAG_EXPANDABLE_LONG_PRESS_MENU = "com.android.launcher3.expandable_long_press_menu";
    public static boolean expandableLongPressMenu() {
        return FEATURE_FLAGS.expandableLongPressMenu();
    }

    public static final String FLAG_ENABLE_CURSOR_DRIVEN_WORKFLOWS = "com.android.launcher3.enable_cursor_driven_workflows";
    public static boolean enableCursorDrivenWorkflows() {
        return FEATURE_FLAGS.enableCursorDrivenWorkflows();
    }

    public static final String FLAG_ENABLE_WORKSPACE_SELECTION = "com.android.launcher3.enable_workspace_selection";
    public static boolean enableWorkspaceSelection() {
        return FEATURE_FLAGS.enableWorkspaceSelection();
    }

    public static final String FLAG_ENABLE_WORKSPACE_PAGE_ANIMATION = "com.android.launcher3.enable_workspace_page_animation";
    public static boolean enableWorkspacePageAnimation() {
        return FEATURE_FLAGS.enableWorkspacePageAnimation();
    }

    public static final String FLAG_APP_LAUNCH_BLUR = "com.android.launcher3.app_launch_blur";
    public static boolean appLaunchBlur() {
        return FEATURE_FLAGS.appLaunchBlur();
    }

    public static final String FLAG_TOOLTIP_EDU_COMBINATOR = "com.android.launcher3.tooltip_edu_combinator";
    public static boolean tooltipEduCombinator() {
        return FEATURE_FLAGS.tooltipEduCombinator();
    }

    public static final String FLAG_BLUR_ON_MORE_SURFACES = "com.android.launcher3.blur_on_more_surfaces";
    public static boolean blurOnMoreSurfaces() {
        return FEATURE_FLAGS.blurOnMoreSurfaces();
    }

    public static final String FLAG_ENABLE_NEW_TOUCHPAD_GESTURES = "com.android.launcher3.enable_new_touchpad_gestures";
    public static boolean enableNewTouchpadGestures() {
        return FEATURE_FLAGS.enableNewTouchpadGestures();
    }

    public static final String FLAG_ORIENTATION_FRIENDLY_DESKTOP_GRID_SPEC = "com.android.launcher3.orientation_friendly_desktop_grid_spec";
    public static boolean orientationFriendlyDesktopGridSpec() {
        return FEATURE_FLAGS.orientationFriendlyDesktopGridSpec();
    }

    public static final String FLAG_FIX_WIDGET_SINGLE_PTR_RESIZE = "com.android.launcher3.fix_widget_single_ptr_resize";
    public static boolean fixWidgetSinglePtrResize() {
        return FEATURE_FLAGS.fixWidgetSinglePtrResize();
    }

    public static final String FLAG_ENABLE_APP_AUTOMATION_INDICATOR = "com.android.launcher3.enable_app_automation_indicator";
    public static boolean enableAppAutomationIndicator() {
        return FEATURE_FLAGS.enableAppAutomationIndicator();
    }

    public static final String FLAG_DISABLE_APP_AUTOMATION_BLUR = "com.android.launcher3.disable_app_automation_blur";
    public static boolean disableAppAutomationBlur() {
        return FEATURE_FLAGS.disableAppAutomationBlur();
    }

    public static final String FLAG_FALLBACK_REVEAL_ANIMATION = "com.android.launcher3.fallback_reveal_animation";
    public static boolean fallbackRevealAnimation() {
        return FEATURE_FLAGS.fallbackRevealAnimation();
    }

    public static final String FLAG_ENABLE_SWIPE_UP_MAGNETIC_DETACH = "com.android.launcher3.enable_swipe_up_magnetic_detach";
    public static boolean enableSwipeUpMagneticDetach() {
        return FEATURE_FLAGS.enableSwipeUpMagneticDetach();
    }

    public static final String FLAG_BIND_MODEL_USING_REPOSITORY = "com.android.launcher3.bind_model_using_repository";
    public static boolean bindModelUsingRepository() {
        return FEATURE_FLAGS.bindModelUsingRepository();
    }

    public static final String FLAG_ENABLE_HOME_SCREEN_FILES_TRASHING = "com.android.launcher3.enable_home_screen_files_trashing";
    public static boolean enableHomeScreenFilesTrashing() {
        return FEATURE_FLAGS.enableHomeScreenFilesTrashing();
    }

    public static final String FLAG_ENABLE_ALL_APPS_EDU_FOR_OVERSWIPE = "com.android.launcher3.enable_all_apps_edu_for_overswipe";
    public static boolean enableAllAppsEduForOverswipe() {
        return FEATURE_FLAGS.enableAllAppsEduForOverswipe();
    }

    public static final String FLAG_SHOW_CREATE_WIDGET_BTN_IN_PICKER = "com.android.launcher3.show_create_widget_btn_in_picker";
    public static boolean showCreateWidgetBtnInPicker() {
        return FEATURE_FLAGS.showCreateWidgetBtnInPicker();
    }

    public static final String FLAG_CENTER_SPRING_LOADED_STATE_VERTICALLY = "com.android.launcher3.center_spring_loaded_state_vertically";
    public static boolean centerSpringLoadedStateVertically() {
        return FEATURE_FLAGS.centerSpringLoadedStateVertically();
    }

    public static final String FLAG_ENABLE_APP_LOCK_SHORTCUT = "com.android.launcher3.enable_app_lock_shortcut";
    public static boolean enableAppLockShortcut() {
        return FEATURE_FLAGS.enableAppLockShortcut();
    }

    public static final String FLAG_ENABLE_RESTOREDBTASK_MODELWRITER_REFACTOR = "com.android.launcher3.enable_restoredbtask_modelwriter_refactor";
    public static boolean enableRestoredbtaskModelwriterRefactor() {
        return FEATURE_FLAGS.enableRestoredbtaskModelwriterRefactor();
    }

    public static final String FLAG_ENABLE_TRANSACTIONAL_MODEL_WRITER = "com.android.launcher3.enable_transactional_model_writer";
    public static boolean enableTransactionalModelWriter() {
        return FEATURE_FLAGS.enableTransactionalModelWriter();
    }

    public static final String FLAG_ENABLE_FILE_SYSTEM_FOLDERS_AS_DROP_TARGETS = "com.android.launcher3.enable_file_system_folders_as_drop_targets";
    public static boolean enableFileSystemFoldersAsDropTargets() {
        return FEATURE_FLAGS.enableFileSystemFoldersAsDropTargets();
    }

    public static final String FLAG_ENABLE_TASKBAR_A11Y_MORE_OPTIONS_BUTTON = "com.android.launcher3.enable_taskbar_a11y_more_options_button";
    public static boolean enableTaskbarA11YMoreOptionsButton() {
        return FEATURE_FLAGS.enableTaskbarA11YMoreOptionsButton();
    }

    public static final String FLAG_ENABLE_WIDGET_PICKER_BLUR = "com.android.launcher3.enable_widget_picker_blur";
    public static boolean enableWidgetPickerBlur() {
        return FEATURE_FLAGS.enableWidgetPickerBlur();
    }

    public static final String FLAG_QSB_ANIMATION_MINOR_FIXES = "com.android.launcher3.qsb_animation_minor_fixes";
    public static boolean qsbAnimationMinorFixes() {
        return FEATURE_FLAGS.qsbAnimationMinorFixes();
    }

    public static final String FLAG_MOVE_TO_REST_STATE_FOR_BACKGROUND_APP = "com.android.launcher3.move_to_rest_state_for_background_app";
    public static boolean moveToRestStateForBackgroundApp() {
        return FEATURE_FLAGS.moveToRestStateForBackgroundApp();
    }

    public static final String FLAG_WIDGET_RETURN_ANIMATION_MINOR_FIXES = "com.android.launcher3.widget_return_animation_minor_fixes";
    public static boolean widgetReturnAnimationMinorFixes() {
        return FEATURE_FLAGS.widgetReturnAnimationMinorFixes();
    }

    public static final String FLAG_CONDO_PLANNER = "com.android.launcher3.condo_planner";
    public static boolean condoPlanner() {
        return FEATURE_FLAGS.condoPlanner();
    }

    public static final String FLAG_DOUBLE_TAP_TO_SLEEP = "com.android.launcher3.double_tap_to_sleep";
    public static boolean doubleTapToSleep() {
        return FEATURE_FLAGS.doubleTapToSleep();
    }

    public static final String FLAG_REDUCE_WORKSPACE_BLUR_USAGE = "com.android.launcher3.reduce_workspace_blur_usage";
    public static boolean reduceWorkspaceBlurUsage() {
        return FEATURE_FLAGS.reduceWorkspaceBlurUsage();
    }

    public static final String FLAG_ENABLE_HOME_SCREEN_FILES_RENAMING = "com.android.launcher3.enable_home_screen_files_renaming";
    public static boolean enableHomeScreenFilesRenaming() {
        return FEATURE_FLAGS.enableHomeScreenFilesRenaming();
    }

    public static final String FLAG_GESTURE_NAV_TOUCH_INPUT_OPTIMIZATIONS = "com.android.launcher3.gesture_nav_touch_input_optimizations";
    public static boolean gestureNavTouchInputOptimizations() {
        return FEATURE_FLAGS.gestureNavTouchInputOptimizations();
    }

    public static final String FLAG_ENABLE_CUE_BAR_DESKTOP_FORM_FACTOR = "com.android.launcher3.enable_cue_bar_desktop_form_factor";
    public static boolean enableCueBarDesktopFormFactor() {
        return FEATURE_FLAGS.enableCueBarDesktopFormFactor();
    }

    public static final String FLAG_ENABLE_DRAG_START_END_MULTI_DISPATCH = "com.android.launcher3.enable_drag_start_end_multi_dispatch";
    public static boolean enableDragStartEndMultiDispatch() {
        return FEATURE_FLAGS.enableDragStartEndMultiDispatch();
    }

    public static final String FLAG_ENABLE_KQS_FORCE_TAKE_RUNNING_TASK_THUMBNAIL = "com.android.launcher3.enable_kqs_force_take_running_task_thumbnail";
    public static boolean enableKqsForceTakeRunningTaskThumbnail() {
        return FEATURE_FLAGS.enableKqsForceTakeRunningTaskThumbnail();
    }

    public static final String FLAG_ENABLE_FIXED_QSB_AT_BOTTOM = "com.android.launcher3.enable_fixed_qsb_at_bottom";
    public static boolean enableFixedQsbAtBottom() {
        return FEATURE_FLAGS.enableFixedQsbAtBottom();
    }

    public static final String FLAG_TRANSLATE_IMESWITCHER_3BUTTONS_WITH_BUBBLE = "com.android.launcher3.translate_imeswitcher_3buttons_with_bubble";
    public static boolean translateImeswitcher3ButtonsWithBubble() {
        return FEATURE_FLAGS.translateImeswitcher3ButtonsWithBubble();
    }

    public static final String FLAG_ENABLE_HOME_SCREEN_FILES_COPY_PASTE = "com.android.launcher3.enable_home_screen_files_copy_paste";
    public static boolean enableHomeScreenFilesCopyPaste() {
        return FEATURE_FLAGS.enableHomeScreenFilesCopyPaste();
    }

    public static final String FLAG_ENABLE_COLLAPSE_SYSUI_PANELS_ON_TASKBAR_CLICK = "com.android.launcher3.enable_collapse_sysui_panels_on_taskbar_click";
    public static boolean enableCollapseSysuiPanelsOnTaskbarClick() {
        return FEATURE_FLAGS.enableCollapseSysuiPanelsOnTaskbarClick();
    }

    public static final String FLAG_ENABLE_GROWTH_NUDGE = "com.android.launcher3.enable_growth_nudge";
    public static boolean enableGrowthNudge() {
        return FEATURE_FLAGS.enableGrowthNudge();
    }

    public static final String FLAG_ENABLE_PRIVATE_SPACE = "com.android.launcher3.enable_private_space";
    public static boolean enablePrivateSpace() {
        return FEATURE_FLAGS.enablePrivateSpace();
    }

    public static final String FLAG_PRIVATE_SPACE_ANIMATION = "com.android.launcher3.private_space_animation";
    public static boolean privateSpaceAnimation() {
        return FEATURE_FLAGS.privateSpaceAnimation();
    }

    public static final String FLAG_NUDGE_PILL = "com.android.launcher3.nudge_pill";
    public static boolean nudgePill() {
        return FEATURE_FLAGS.nudgePill();
    }

    public static final String FLAG_PRIVATE_SPACE_ADD_FLOATING_MASK_VIEW = "com.android.launcher3.private_space_add_floating_mask_view";
    public static boolean privateSpaceAddFloatingMaskView() {
        return FEATURE_FLAGS.privateSpaceAddFloatingMaskView();
    }

    public static final String FLAG_ENABLE_QSB_ON_HOTSEAT = "com.android.launcher3.enable_qsb_on_hotseat";
    public static boolean enableQsbOnHotseat() {
        return FEATURE_FLAGS.enableQsbOnHotseat();
    }

}
