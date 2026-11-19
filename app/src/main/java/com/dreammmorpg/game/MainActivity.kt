package com.dreammmorpg.game

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity(), GameView.Listener {

    private lateinit var game: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        game = GameView(this)
        game.listener = this
        setContentView(game)
        game.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    // ---- klavye (WASD / oklar / E)

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (game.keyDown(keyCode, event.repeatCount)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (game.keyUp(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (!game.handleBack()) super.onBackPressed()
    }

    // ---- oyundan gelen istekler (iletişim kutuları)

    override fun askName(cls: CharClass, onName: (String) -> Unit) {
        runOnUiThread {
            val input = EditText(this).apply {
                hint = "Karakter adı (2-12 harf)"
                setSingleLine(true)
                filters = arrayOf(InputFilter.LengthFilter(12))
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle(cls.title + " için bir isim seç")
                .setView(input)
                .setPositiveButton("Oluştur") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.length < 2 || !name.all { it.isLetterOrDigit() }) {
                        Toast.makeText(this, "İsim en az 2 karakter olmalı, sadece harf ve rakam içermeli.", Toast.LENGTH_LONG).show()
                        askName(cls, onName)
                    } else {
                        onName(name)
                    }
                }
                .setNegativeButton("Vazgeç", null)
                .create()
            dialog.setOnDismissListener { hideSystemBars() }
            dialog.show()
        }
    }

    override fun askDelete(hero: Hero, onYes: () -> Unit) {
        runOnUiThread {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Karakteri sil")
                .setMessage(hero.name + " (" + hero.cls.title + ") kalıcı olarak silinsin mi?")
                .setPositiveButton("Sil") { _, _ -> onYes() }
                .setNegativeButton("Vazgeç", null)
                .create()
            dialog.setOnDismissListener { hideSystemBars() }
            dialog.show()
        }
    }
}
