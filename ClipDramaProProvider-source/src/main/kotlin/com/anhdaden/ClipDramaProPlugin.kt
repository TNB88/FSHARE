package com.anhdaden

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ClipDramaProPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ClipDramaProProvider())
    }
}
