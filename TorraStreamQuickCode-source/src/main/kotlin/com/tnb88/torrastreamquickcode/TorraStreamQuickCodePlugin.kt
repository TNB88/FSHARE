package com.tnb88.torrastreamquickcode

import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TorraStreamQuickCodePlugin : Plugin() {
    private var activity: AppCompatActivity? = null
    private var lifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity
        activity?.let(::protectTorraStreamSettings)
        openSettings = {
            activity?.let { host ->
                QuickCodeFragment(this).show(host.supportFragmentManager, "TorraStreamQuickCode")
            }
        }
    }

    private fun protectTorraStreamSettings(host: AppCompatActivity) {
        if (lifecycleCallbacks != null) return
        lifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fragmentManager: FragmentManager,
                fragment: Fragment,
                view: View,
                savedInstanceState: Bundle?
            ) {
                if (fragment.javaClass.name != "com.phisher98.settings.SettingsFragment") return
                val managed = host.getSharedPreferences("TorraStreamQuickCode", 0)
                    .getBoolean("quickcode_managed", false)
                if (!managed) return

                findDebridKeyInput(view)?.apply {
                    transformationMethod = PasswordTransformationMethod.getInstance()
                    isLongClickable = false
                    setTextIsSelectable(false)
                    visibility = View.GONE
                }
            }
        }
        host.supportFragmentManager.registerFragmentLifecycleCallbacks(lifecycleCallbacks!!, true)
    }

    private fun findDebridKeyInput(view: View): EditText? {
        if (view is EditText) {
            val resourceName = runCatching {
                view.resources.getResourceEntryName(view.id)
            }.getOrNull()
            if (resourceName == "debrid_key_input" || view.hint?.toString() == "Enter API Key / URL") {
                return view
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findDebridKeyInput(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
