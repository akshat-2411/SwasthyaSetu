package com.example.swasthyasetu.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.swasthyasetu.R
import com.example.swasthyasetu.databinding.ActivityAlertDetailBinding
import com.example.swasthyasetu.util.LocaleHelper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class AlertDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private val client = OkHttpClient()

    companion object {
        const val EXTRA_ALERT_TITLE = "extra_alert_title"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"

        private const val API_KEY = "AIzaSyDwQVr121magYzPhUZW8r17jcoQpp_R41U"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val alertTitle =
            intent.getStringExtra(EXTRA_ALERT_TITLE)
                ?: getString(R.string.alert_detail_title)

        val alertType =
            intent.getStringExtra(EXTRA_ALERT_TYPE)
                ?: "general medical emergency"

        binding.tvAlertStatusTitle.text = alertTitle

        setupHelplineButton()
        fetchAdvisory(alertType)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupHelplineButton() {
        binding.btnCallHelpline.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:104"))
            )
        }
    }

    // -----------------------------
    // 🔥 UPDATED API CALL
    // -----------------------------
    private fun fetchAdvisory(alertType: String) {

        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutAiContent.visibility = View.GONE

        val currentLang = LocaleHelper.getSavedLanguage(this)

        val langInstruction = if (currentLang == "hi") {
            "Reply ONLY in simple Hindi (Devanagari)."
        } else {
            "Reply in simple English."
        }

        val prompt = """
            You are a public health official.

            Emergency: $alertType

            Return ONLY valid JSON:
            {
              "what_is_happening": "...",
              "symptoms": "...",
              "precautions": "..."
            }

            $langInstruction
        """.trimIndent()

        val json = """
        {
          "contents": [
            {
              "parts": [
                { "text": "$prompt" }
              ]
            }
          ]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY")
            .post(RequestBody.create("application/json".toMediaType(), json))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showError() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                try {
                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        runOnUiThread { showError() }
                        return
                    }

                    val jsonResponse = JSONObject(body)

                    val text = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val data = JSONObject(text)

                    runOnUiThread {
                        binding.tvWhatIsHappening.text =
                            data.optString("what_is_happening", "N/A")

                        binding.tvSymptoms.text =
                            data.optString("symptoms", "N/A")

                        binding.tvPrecautions.text =
                            data.optString("precautions", "N/A")

                        binding.layoutLoading.visibility = View.GONE
                        binding.layoutAiContent.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    runOnUiThread { showError() }
                }
            }
        })
    }

    private fun showError() {
        binding.layoutLoading.visibility = View.GONE
        Toast.makeText(
            this,
            getString(R.string.alert_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}