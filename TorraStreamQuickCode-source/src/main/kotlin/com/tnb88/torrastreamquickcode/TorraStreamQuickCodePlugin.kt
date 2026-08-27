package com.tnb88.torrastreamquickcode

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TorraStreamQuickCodePlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity
        openSettings = {
            activity?.let { host ->
                QuickCodeFragment(this).show(host.supportFragmentManager, "TorraStreamQuickCode")
            }
        }
    }
}
