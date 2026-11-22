package br.com.ekko

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0B1220.toInt())
            setPadding(48, 120, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        fun tv(t: String, sz: Float, col: Int) = TextView(this).apply {
            text = t; textSize = sz; setTextColor(col); gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
        }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
        ll.addView(tv("Ekko", 44f, 0xFF22D3EE.toInt()))
        ll.addView(tv("Diga “Ekko” e o comando, mesmo com a tela bloqueada.", 16f, 0xFFE8F4FB.toInt()))
        ll.addView(btn("1. Permitir microfone, contatos e ligações") {
            requestPermissions(arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS), 1)
        })
        ll.addView(btn("2. Permitir sobre outros apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        ll.addView(btn("3. Ignorar economia de bateria") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        })
        ll.addView(btn("4. Ligar a Ekko") { startForegroundService(Intent(this, EkkoService::class.java)) })
        ll.addView(btn("Desligar") { stopService(Intent(this, EkkoService::class.java)) })
        setContentView(ll)
    }
}
