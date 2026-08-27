package com.tnb88.torrastreamquickcode

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class QuickCodeFragment(
    private val plugin: TorraStreamQuickCodePlugin
) : BottomSheetDialogFragment() {
    private val workerUrl =
        "https://torrastream-quickcode.tongbinhnguyen9090.workers.dev"

    private val localPrefs by lazy {
        requireContext().getSharedPreferences("TorraStreamQuickCode", 0)
    }

    private fun torraPrefs() = requireContext().getSharedPreferences("TorraStream", 0)

    @SuppressLint("DiscouragedApi")
    private fun resourceId(name: String, type: String): Int? {
        val resources = plugin.resources ?: return null
        return resources.getIdentifier(name, type, BuildConfig.LIBRARY_PACKAGE_NAME)
            .takeIf { it != 0 }
    }

    private fun <T : View> View.byName(name: String): T? {
        val id = resourceId(name, "id") ?: return null
        return findViewById(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = resourceId("quick_code_settings", "layout") ?: return null
        return inflater.inflate(plugin.resources?.getLayout(layout), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val codeInput = view.byName<EditText>("quick_code") ?: return
        val providerSpinner = view.byName<Spinner>("provider_spinner") ?: return
        val applyButton = view.byName<Button>("apply_code") ?: return
        val clearButton = view.byName<Button>("clear_debrid") ?: return
        val status = view.byName<TextView>("status") ?: return

        val providers = listOf("TorBox", "RealDebrid")
        providerSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            providers
        )

        val currentProvider = torraPrefs().getString("debrid_provider", "TorBox")
        providerSpinner.setSelection(providers.indexOf(currentProvider).coerceAtLeast(0))

        applyButton.setOnClickListener {
            val code = codeInput.text.toString().trim().lowercase()
            val provider = providers[providerSpinner.selectedItemPosition]

            if (!code.matches(Regex("[a-z0-9_-]{2,64}"))) {
                status.text = "Mã chỉ dùng chữ thường, số, _ hoặc - (2–64 ký tự)."
                return@setOnClickListener
            }

            setBusy(applyButton, providerSpinner, true)
            status.text = "Đang kiểm tra mã…"

            Thread {
                val result = resolve(workerUrl, code, provider)
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setBusy(applyButton, providerSpinner, false)
                    result.onSuccess { resolved ->
                        torraPrefs().edit()
                            .putString("debrid_provider", resolved.provider)
                            .putString("debrid_key", resolved.key)
                            .apply()
                        localPrefs.edit().putBoolean("quickcode_managed", true).apply()
                        codeInput.text?.clear()
                        status.text = "Đã cấu hình ${resolved.provider} cho TorraStream và TorraStream-Anime. Hãy khởi động lại CloudStream."
                    }.onFailure { error ->
                        status.text = error.message ?: "Không thể nhận cấu hình."
                    }
                }
            }.start()
        }

        clearButton.setOnClickListener {
            torraPrefs().edit()
                .remove("debrid_provider")
                .remove("debrid_key")
                .apply()
            localPrefs.edit().putBoolean("quickcode_managed", false).apply()
            codeInput.text?.clear()
            status.text = "Đã xóa cấu hình debrid cục bộ của TorraStream."
        }
    }

    private fun setBusy(button: Button, spinner: Spinner, busy: Boolean) {
        button.isEnabled = !busy
        spinner.isEnabled = !busy
    }

    private fun resolve(
        endpoint: String,
        code: String,
        provider: String
    ): Result<ResolvedDebrid> = runCatching {
        val connection = URL("$endpoint/v1/resolve").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")

            val request = JSONObject()
                .put("code", code)
                .put("provider", if (provider == "TorBox") "torbox" else "real_debrid")
                .toString()
            connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (responseCode !in 200..299) {
                val message = json?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Worker trả lỗi HTTP $responseCode"
                error(message)
            }

            val returnedProvider = json?.optString("provider").orEmpty()
            val key = json?.optString("key").orEmpty()
            if (returnedProvider !in setOf("TorBox", "RealDebrid") || key.length < 8) {
                error("Phản hồi Worker không hợp lệ.")
            }
            ResolvedDebrid(returnedProvider, key)
        } finally {
            connection.disconnect()
        }
    }

    private data class ResolvedDebrid(val provider: String, val key: String)
}
