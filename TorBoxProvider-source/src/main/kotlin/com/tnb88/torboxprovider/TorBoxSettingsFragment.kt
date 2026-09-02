package com.tnb88.torboxprovider

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TorBoxSettingsFragment(
    private val plugin: TorBoxPlugin
) : BottomSheetDialogFragment() {

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
        val layout = resourceId("torbox_settings", "layout") ?: return null
        return inflater.inflate(plugin.resources?.getLayout(layout), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val codeInput = view.byName<EditText>("quick_code") ?: return
        val apiInput = view.byName<EditText>("api_key") ?: return
        val applyCode = view.byName<Button>("apply_code") ?: return
        val saveApi = view.byName<Button>("save_api") ?: return
        val clear = view.byName<Button>("clear_torbox") ?: return
        val autoVietnamese = view.byName<CheckBox>("auto_vietnamese_subtitle") ?: return
        val status = view.byName<TextView>("status") ?: return

        autoVietnamese.isChecked = TorBoxConfig.autoVietnameseSubtitle(requireContext())
        autoVietnamese.setOnCheckedChangeListener { _, enabled ->
            TorBoxConfig.setAutoVietnameseSubtitle(requireContext(), enabled)
            status.text = if (enabled) {
                "Đã ưu tiên và tự chọn phụ đề tiếng Việt trong trình phát."
            } else {
                "Đã trả cài đặt phụ đề tự động về lựa chọn trước đó."
            }
        }
        refreshStatus(status)

        applyCode.setOnClickListener {
            val code = codeInput.text.toString().trim().lowercase()
            if (!code.matches(Regex("[a-z0-9_-]{2,64}"))) {
                status.text = "Mã chỉ dùng chữ thường, số, _ hoặc - (2–64 ký tự)."
                return@setOnClickListener
            }
            setBusy(true, applyCode, saveApi, clear)
            status.text = "Đang kiểm tra mã…"
            Thread {
                val result = resolveQuickCode(code)
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setBusy(false, applyCode, saveApi, clear)
                    result.onSuccess { token ->
                        TorBoxConfig.saveToken(requireContext(), token, true)
                        codeInput.text?.clear()
                        apiInput.text?.clear()
                        status.text = "Đã kích hoạt TorBox thành công. Có thể xem phim ngay."
                    }.onFailure { error ->
                        status.text = error.message ?: "Không thể nhận cấu hình TorBox."
                    }
                }
            }.start()
        }

        saveApi.setOnClickListener {
            val key = apiInput.text.toString().trim().removePrefix("Bearer ").trim()
            if (key.length < 8) {
                status.text = "API key TorBox không hợp lệ."
                return@setOnClickListener
            }
            TorBoxConfig.saveToken(requireContext(), key, false)
            apiInput.text?.clear()
            codeInput.text?.clear()
            status.text = "Đã lưu API TorBox trên thiết bị. Có thể xem phim ngay."
        }

        clear.setOnClickListener {
            TorBoxConfig.clear(requireContext())
            codeInput.text?.clear()
            apiInput.text?.clear()
            status.text = "Đã xóa cấu hình TorBox trên thiết bị."
        }
    }

    private fun setBusy(busy: Boolean, vararg buttons: Button) {
        buttons.forEach { it.isEnabled = !busy }
    }

    private fun refreshStatus(status: TextView) {
        status.text = if (TorBoxConfig.token(requireContext()).isBlank()) {
            "Chưa kích hoạt TorBox."
        } else {
            "TorBox đã được cấu hình trên thiết bị. API key đang được ẩn."
        }
    }

    private fun resolveQuickCode(code: String): Result<String> = runCatching {
        val connection = URL(TorBoxConfig.quickCodeUrl()).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            val body = JSONObject().put("code", code).put("provider", "torbox").toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(responseBody) }.getOrNull()
            if (responseCode !in 200..299) {
                error(json?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Máy chủ trả lỗi HTTP $responseCode")
            }

            val provider = json?.optString("provider").orEmpty()
            val key = json?.optString("key").orEmpty().trim()
            if (!provider.equals("TorBox", true) || key.length < 8) {
                error("Mã không có cấu hình TorBox hợp lệ.")
            }
            key
        } finally {
            connection.disconnect()
        }
    }
}
