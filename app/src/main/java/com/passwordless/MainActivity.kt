package com.passwordless

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    internal var qrScanIntegrator: IntentIntegrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        qrScanIntegrator = IntentIntegrator(this)
        qrScanIntegrator?.setOrientationLocked(true)
        val scanBtn = findViewById(R.id.scanbtn) as Button
        // set on-click listener
        scanBtn.setOnClickListener { qrScanIntegrator?.initiateScan() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            // If QRCode has no data.
            if (result.contents == null) {
                Toast.makeText(this, "result null", Toast.LENGTH_LONG).show()
            } else {
                val deviceId: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val json = JSONObject()
                json.put("code", result.contents)
                json.put("device_id", deviceId)
                Fuel.post("http://9929-88-236-113-33.ngrok.io/claim")
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