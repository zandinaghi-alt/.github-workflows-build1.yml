package com.mahdi.voiceassistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.animation.LinearInterpolator
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mahdi.voiceassistant.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeech

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE
    )

    private val speechLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.get(0) ?: ""
                binding.transcriptText.text = spokenText
                handleCommand(spokenText)
            }
        }

    private val permissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
            val allGranted = grantResults.values.all { it }
            binding.statusText.text = if (allGranted)
                "آماده‌ام، دکمه رو بزن و صحبت کن"
            else
                "برای کار کردن باید همه‌ی دسترسی‌ها رو بدی"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        startRobotAnimation()

        binding.micButton.setOnClickListener {
            if (hasAllPermissions()) {
                startListening()
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }

        binding.settingsButton.setOnClickListener {
            showApiKeyDialog()
        }

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }

        if (ClaudeApiHelper.getApiKey(this).isBlank()) {
            showApiKeyDialog()
        }
    }

    /**
     * انیمیشن ساده و آروم برای ربات: بالا و پایین رفتن + کمی چرخش،
     * تا حس زنده بودن بده حتی وقتی کاری نمی‌کنه.
     */
    private fun startRobotAnimation() {
        val floatUp = ObjectAnimator.ofFloat(binding.robotImage, "translationY", 0f, -20f, 0f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        val wobble = ObjectAnimator.ofFloat(binding.robotImage, "rotation", -4f, 4f, -4f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
        }
        floatUp.start()
        wobble.start()
    }

    private fun showApiKeyDialog() {
        val input = EditText(this)
        input.hint = "کلید API رو اینجا پیست کن (sk-ant-...)"
        input.setText(ClaudeApiHelper.getApiKey(this))

        AlertDialog.Builder(this)
            .setTitle("تنظیمات هوش مصنوعی")
            .setMessage(
                "برای اینکه دستیار بتونه به هر سوالی جواب بده (نه فقط دستورهای ثابت)، " +
                        "یه کلید API از console.anthropic.com لازم داری. " +
                        "این کلید فقط رو خود گوشیت ذخیره می‌شه."
            )
            .setView(input)
            .setPositiveButton("ذخیره") { _, _ ->
                val key = input.text.toString().trim()
                ClaudeApiHelper.saveApiKey(this, key)
                binding.statusText.text = "کلید ذخیره شد. آماده‌ام!"
            }
            .setNegativeButton("بی‌خیال", null)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("fa", "IR")
        }
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startListening() {
        binding.statusText.text = "دارم گوش می‌دم..."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "الان صحبت کن")
        }
        speechLauncher.launch(intent)
    }

    private fun handleCommand(spokenText: String) {
        binding.statusText.text = "در حال پردازش..."
        CommandProcessor.process(this, spokenText) { response ->
            binding.statusText.text = response
            speak(response)
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
