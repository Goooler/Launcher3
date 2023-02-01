package com.android.launcher3

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.annotation.VisibleForTesting
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.DeviceGridState
import com.android.launcher3.pm.InstallSessionHelper
import com.android.launcher3.provider.RestoreDbTask
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.Themes

/**
 * Use same context for shared preferences, so that we use a single cached instance
 * TODO(b/262721340): Replace all direct SharedPreference refs with LauncherPrefs / Item methods.
 */
class LauncherPrefs(private val context: Context) {

    /**
     * Retrieves the value for an [Item] from [SharedPreferences]. It handles method typing via the
     * default value type, and will throw an error if the type of the item provided is not a
     * `String`, `Boolean`, `Float`, `Int`, `Long`, or `Set<String>`.
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun <T : Any> get(item: Item<T>): T {
        val sp = context.getSharedPreferences(item.sharedPrefFile, Context.MODE_PRIVATE)

        return when (item.defaultValue::class.java) {
            String::class.java -> sp.getString(item.sharedPrefKey, item.defaultValue as String)
            Boolean::class.java,
            java.lang.Boolean::class.java ->
                sp.getBoolean(item.sharedPrefKey, item.defaultValue as Boolean)
            Int::class.java,
            java.lang.Integer::class.java -> sp.getInt(item.sharedPrefKey, item.defaultValue as Int)
            Float::class.java,
            java.lang.Float::class.java ->
                sp.getFloat(item.sharedPrefKey, item.defaultValue as Float)
            Long::class.java,
            java.lang.Long::class.java -> sp.getLong(item.sharedPrefKey, item.defaultValue as Long)
            Set::class.java -> sp.getStringSet(item.sharedPrefKey, item.defaultValue as Set<String>)
            else ->
                throw IllegalArgumentException(
                    "item type: ${item.defaultValue::class.java}" +
                        " is not compatible with sharedPref methods"
                )
        }
            as T
    }

    /**
     * Stores each of the values provided in `SharedPreferences` according to the configuration
     * contained within the associated items provided. Internally, it uses apply, so the caller
     * cannot assume that the values that have been put are immediately available for use.
     *
     * The forEach loop is necessary here since there is 1 `SharedPreference.Editor` returned from
     * prepareToPutValue(itemsToValues) for every distinct `SharedPreferences` file present in the
     * provided item configurations.
     */
    fun put(vararg itemsToValues: Pair<Item<*>, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.apply() }

    /**
     * Stores the value provided in `SharedPreferences` according to the item configuration provided
     * It is asynchronous, so the caller can't assume that the value put is immediately available.
     */
    fun <T : Any> put(item: Item<T>, value: T): Unit =
        context
            .getSharedPreferences(item.sharedPrefFile, Context.MODE_PRIVATE)
            .edit()
            .putValue(item, value)
            .apply()

    /**
     * Synchronously stores all the values provided according to their associated Item
     * configuration.
     */
    fun putSync(vararg itemsToValues: Pair<Item<*>, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.commit() }

    /**
     * Update each shared preference file with the item - value pairs provided. This method is
     * optimized to avoid retrieving the same shared preference file multiple times.
     *
     * @return `List<SharedPreferences.Editor>` 1 for each distinct shared preference file among the
     * items given as part of the itemsToValues parameter
     */
    private fun prepareToPutValues(
        itemsToValues: Array<out Pair<Item<*>, Any>>
    ): List<SharedPreferences.Editor> =
        itemsToValues
            .groupBy { it.first.sharedPrefFile }
            .map { fileToItemValueList ->
                context
                    .getSharedPreferences(fileToItemValueList.key, Context.MODE_PRIVATE)
                    .edit()
                    .apply {
                        fileToItemValueList.value.forEach { itemToValue ->
                            putValue(itemToValue.first, itemToValue.second)
                        }
                    }
            }

    /**
     * Handles adding values to `SharedPreferences` regardless of type. This method is especially
     * helpful for updating `SharedPreferences` values for `List<<Item>Any>` that have multiple
     * types of Item values.
     */
    @Suppress("UNCHECKED_CAST")
    private fun SharedPreferences.Editor.putValue(
        item: Item<*>,
        value: Any
    ): SharedPreferences.Editor =
        when (value::class.java) {
            String::class.java -> putString(item.sharedPrefKey, value as String)
            Boolean::class.java,
            java.lang.Boolean::class.java -> putBoolean(item.sharedPrefKey, value as Boolean)
            Int::class.java,
            java.lang.Integer::class.java -> putInt(item.sharedPrefKey, value as Int)
            Float::class.java,
            java.lang.Float::class.java -> putFloat(item.sharedPrefKey, value as Float)
            Long::class.java,
            java.lang.Long::class.java -> putLong(item.sharedPrefKey, value as Long)
            Set::class.java -> putStringSet(item.sharedPrefKey, value as Set<String>)
            else ->
                throw IllegalArgumentException(
                    "item type: " +
                        "${item.defaultValue!!::class} is not compatible with sharedPref methods"
                )
        }

    /**
     * After calling this method, the listener will be notified of any future updates to the
     * `SharedPreferences` files associated with the provided list of items. The listener will need
     * to filter update notifications so they don't activate for non-relevant updates.
     */
    fun addListener(listener: OnSharedPreferenceChangeListener, vararg items: Item<*>) {
        items
            .map { it.sharedPrefFile }
            .distinct()
            .forEach {
                context
                    .getSharedPreferences(it, Context.MODE_PRIVATE)
                    .registerOnSharedPreferenceChangeListener(listener)
            }
    }

    /**
     * Stops the listener from getting notified of any more updates to any of the
     * `SharedPreferences` files associated with any of the provided list of [Item].
     */
    fun removeListener(listener: OnSharedPreferenceChangeListener, vararg items: Item<*>) {
        // If a listener is not registered to a SharedPreference, unregistering it does nothing
        items
            .map { it.sharedPrefFile }
            .distinct()
            .forEach {
                context
                    .getSharedPreferences(it, Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(listener)
            }
    }

    /**
     * Checks if all the provided [Item] have values stored in their corresponding
     * `SharedPreferences` files.
     */
    fun has(vararg items: Item<*>): Boolean {
        items
            .groupBy { it.sharedPrefFile }
            .forEach { (file, itemsSublist) ->
                val prefs: SharedPreferences =
                    context.getSharedPreferences(file, Context.MODE_PRIVATE)
                if (!itemsSublist.none { !prefs.contains(it.sharedPrefKey) }) return false
            }
        return true
    }

    /**
     * Asynchronously removes the [Item]'s value from its corresponding `SharedPreferences` file.
     */
    fun remove(vararg items: Item<*>) = prepareToRemove(items).forEach { it.apply() }

    /** Synchronously removes the [Item]'s value from its corresponding `SharedPreferences` file. */
    fun removeSync(vararg items: Item<*>) = prepareToRemove(items).forEach { it.commit() }

    /**
     * Creates `SharedPreferences.Editor` transactions for removing all the provided [Item] values
     * from their respective `SharedPreferences` files. These returned `Editors` can then be
     * committed or applied for synchronous or async behavior.
     */
    private fun prepareToRemove(items: Array<out Item<*>>): List<SharedPreferences.Editor> =
        items
            .groupBy { it.sharedPrefFile }
            .map { (file, items) ->
                context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().also { editor ->
                    items.forEach { item -> editor.remove(item.sharedPrefKey) }
                }
            }

    companion object {
        @JvmField var INSTANCE = MainThreadInitializedObject { LauncherPrefs(it) }

        @JvmStatic fun get(context: Context): LauncherPrefs = INSTANCE.get(context)

        @JvmField val ICON_STATE = nonRestorableItem(LauncherAppState.KEY_ICON_STATE, "")
        @JvmField val THEMED_ICONS = backedUpItem(Themes.KEY_THEMED_ICONS, false)
        @JvmField val PROMISE_ICON_IDS = backedUpItem(InstallSessionHelper.PROMISE_ICON_IDS, "")
        @JvmField val WORK_EDU_STEP = backedUpItem(WorkProfileManager.KEY_WORK_EDU_STEP, 0)
        @JvmField val WORKSPACE_SIZE = backedUpItem(DeviceGridState.KEY_WORKSPACE_SIZE, "")
        @JvmField val HOTSEAT_COUNT = backedUpItem(DeviceGridState.KEY_HOTSEAT_COUNT, -1)
        @JvmField
        val DEVICE_TYPE =
            backedUpItem(DeviceGridState.KEY_DEVICE_TYPE, InvariantDeviceProfile.TYPE_PHONE)
        @JvmField val DB_FILE = backedUpItem(DeviceGridState.KEY_DB_FILE, "")
        @JvmField
        val RESTORE_DEVICE =
            backedUpItem(RestoreDbTask.RESTORED_DEVICE_TYPE, InvariantDeviceProfile.TYPE_PHONE)
        @JvmField val APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_IDS, "")
        @JvmField val OLD_APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_OLD_IDS, "")

        @VisibleForTesting
        @JvmStatic
        fun <T> backedUpItem(sharedPrefKey: String, defaultValue: T): Item<T> =
            Item(sharedPrefKey, LauncherFiles.SHARED_PREFERENCES_KEY, defaultValue)

        @VisibleForTesting
        @JvmStatic
        fun <T> nonRestorableItem(sharedPrefKey: String, defaultValue: T): Item<T> =
            Item(sharedPrefKey, LauncherFiles.DEVICE_PREFERENCES_KEY, defaultValue)

        @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
        @JvmStatic
        fun getPrefs(context: Context): SharedPreferences {
            // Use application context for shared preferences, so we use single cached instance
            return context.applicationContext.getSharedPreferences(
                LauncherFiles.SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
        }

        @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
        @JvmStatic
        fun getDevicePrefs(context: Context): SharedPreferences {
            // Use application context for shared preferences, so we use a single cached instance
            return context.applicationContext.getSharedPreferences(
                LauncherFiles.DEVICE_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
        }
    }
}

data class Item<T>(val sharedPrefKey: String, val sharedPrefFile: String, val defaultValue: T) {
    fun to(value: T): Pair<Item<T>, T> = Pair(this, value)
}
