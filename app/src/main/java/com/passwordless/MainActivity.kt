package com.passwordless

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    internal var qrScanIntegrator: IntentIntegrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "Passwordless",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token: String? = sharedPreferences.getString("token", "")
        if (token == "") {
            val secureStorage = sharedPreferences.edit()
            secureStorage.putString(
                "token",
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            )
            secureStorage.commit()
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Development Env.
        Toast.makeText(applicationContext, token, Toast.LENGTH_SHORT).show()
        qrScanIntegrator = IntentIntegrator(this)
        qrScanIntegrator?.setOrientationLocked(true)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    qrScanIntegrator?.initiateScan()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        val scanBtn = findViewById(R.id.scanbtn) as Button
        // set on-click listener
        scanBtn.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "result null", Toast.LENGTH_LONG).show()
            } else {
                if (!result.contents.startsWith("passwordless://")) {
                    Toast.makeText(
                        this@MainActivity,
                        "No valid content detected!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val deviceId: String =
                    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val json = JSONObject()
                json.put("code", result.contents)
                json.put("device_id", deviceId)
                Fuel.post("https://flowsys.cloud/claim")
                    .jsonBody(json.toString())
                    .also { println(it) }
                    .response { result -> }
                Toast.makeText(this@MainActivity, result.contents, Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

