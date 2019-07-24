package io.reist.zarowka

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

/**
 * Created by reist on 04.01.2018.
 */
class ZarowkaPrefs(context: Context) {

    private val sharedPrefs: SharedPreferences

    init {
        this.sharedPrefs = context.getSharedPreferences(NAME, MODE_PRIVATE)
    }

    companion object {

        const val NAME = "ZarowkaPrefs"

        const val PREF_RED = "PREF_RED"
        const val PREF_GREEN = "PREF_GREEN"
        const val PREF_BLUE = "PREF_BLUE"
        const val PREF_VISUALS = "PREF_VISUALS"

    }

    var red: Int
        get() = this.sharedPrefs.getInt(PREF_RED, 127)
        set(value) = this.sharedPrefs.edit().putInt(PREF_RED, value).apply()

    var green: Int
        get() = this.sharedPrefs.getInt(PREF_GREEN, 127)
        set(value) = this.sharedPrefs.edit().putInt(PREF_GREEN, value).apply()

    var blue: Int
        get() = this.sharedPrefs.getInt(PREF_BLUE, 127)
        set(value) = this.sharedPrefs.edit().putInt(PREF_BLUE, value).apply()

    var visuals: Boolean
        get() = this.sharedPrefs.getBoolean(PREF_VISUALS, false)
        set(value) = this.sharedPrefs.edit().putBoolean(PREF_VISUALS, value).apply()

}