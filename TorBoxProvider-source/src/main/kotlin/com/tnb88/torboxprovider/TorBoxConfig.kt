package com.tnb88.torboxprovider

import android.content.Context

internal object TorBoxConfig {
    const val TORRA_PREFS = "TorraStream"
    const val LOCAL_PREFS = "TorBoxProvider"
    const val PROVIDER_KEY = "debrid_provider"
    const val API_KEY = "debrid_key"
    const val PROVIDER_NAME = "TorBox"
    const val QUICK_CODE_MANAGED = "quickcode_managed"

    private const val QUICK_CODE_BASE =
        "https://torrastream-quickcode.tongbinhnguyen9090.workers.dev"

    fun token(context: Context): String {
        val prefs = context.getSharedPreferences(TORRA_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getString(PROVIDER_KEY, "").equals(PROVIDER_NAME, ignoreCase = true)) {
            return ""
        }
        return prefs.getString(API_KEY, "").orEmpty().trim()
            .removePrefix("Bearer ").trim()
    }

    fun saveToken(context: Context, token: String, fromQuickCode: Boolean) {
        val clean = token.trim().removePrefix("Bearer ").trim()
        context.getSharedPreferences(TORRA_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PROVIDER_KEY, PROVIDER_NAME)
            .putString(API_KEY, clean)
            .apply()
        context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(QUICK_CODE_MANAGED, fromQuickCode)
            .apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(TORRA_PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(PROVIDER_KEY, "").equals(PROVIDER_NAME, ignoreCase = true)) {
            prefs.edit().remove(PROVIDER_KEY).remove(API_KEY).apply()
        }
        context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
            .edit().remove(QUICK_CODE_MANAGED).apply()
    }

    fun quickCodeUrl(): String = "$QUICK_CODE_BASE/v1/resolve"
}
