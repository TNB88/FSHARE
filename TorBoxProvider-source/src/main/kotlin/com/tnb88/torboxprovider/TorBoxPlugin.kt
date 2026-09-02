package com.tnb88.torboxprovider

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TorBoxPlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        val appContext = context.applicationContext
        activity = context as? AppCompatActivity
        TorBoxConfig.applyVietnameseSubtitlePreference(appContext)
        VietnameseSubtitleProxy.start()
        registerMainAPI(TorBoxProvider(appContext))

        openSettings = {
            activity?.let { host ->
                TorBoxSettingsFragment(this).show(
                    host.supportFragmentManager,
                    "TorBoxProviderSettings"
                )
            }
        }
    }
}
