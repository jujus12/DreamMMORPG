package com.dreammmorpg.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class NpcLook { ELDER, BLACKSMITH, MERCHANT, HEALER, GUARD, INNKEEPER, KID }

/**
 * Tüm karakterler kod ile çizilir (resim dosyası yok).
 * Karakter yaklaşık 66 birim boyunda, ayakları (0,0) noktasında, sağa bakacak şekilde çizilir.
 * Sola bakması için tuval yatay çevrilir.
 */
object CharacterRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val oval = RectF()
    private val SKIN = Color.rgb(242, 207, 172)
    private val INK = Color.rgb(34, 30, 44)
    private val BROWN = Color.rgb(110, 75, 45)
    private const val HY = -46f

    // ---------------------------------------------------------------- yardımcılar

    private fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, rad: Float = 0f) {
        p.style = Paint.Style.FILL
        p.color = color
        if (rad > 0f) c.drawRoundRect(l, t, r, b, rad, rad, p) else c.drawRect(l, t, r, b, p)
    }

    private fun disc(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        c.drawCircle(x, y, r, p)
    }

    private fun glow(c: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = alpha.coerceIn(0, 255)
        c.drawCircle(x, y, r, p)
        p.alpha = 255
    }

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND
        p.color = color
        c.drawLine(x1, y1, x2, y2, p)
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun ovalFill(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        oval.set(l, t, r, b)
        c.drawOval(oval, p)
    }

    private fun arc(c: Canvas, l: Float, t: Float, r: Float, b: Float, start: Float, sweep: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        oval.set(l, t, r, b)
        c.drawArc(oval, start, sweep, true, p)
    }

    private fun tri(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        p.style = Paint.Style.FILL
        p.color = color
        c.drawPath(path, p)
    }

    private fun face(c: Canvas, eyeY: Float, eyeColor: Int = INK) {
        disc(c, 0f, HY, 10.5f, SKIN)
        disc(c, 3.2f, eyeY, 1.7f, eyeColor)
        disc(c, 7.6f, eyeY, 1.7f, eyeColor)
    }

    private fun legs(c: Canvas, sw: Float, leg: Int, boot: Int) {
        line(c, -4.5f, -15f, -4.5f + sw, -1f, 6f, leg)
        line(c, 4.5f, -15f, 4.5f - sw, -1f, 6f, leg)
        disc(c, -4.5f + sw, -1.5f, 3.4f, boot)
        disc(c, 4.5f - sw, -1.5f, 3.4f, boot)
    }

    private fun robe(c: Canvas, body: Int, trim: Int, sway: Float, topW: Float = 9.5f, botW: Float = 14.5f) {
        path.reset()
        path.moveTo(-topW, -39f)
        path.lineTo(topW, -39f)
        path.lineTo(botW + sway, -1f)
        path.quadTo(sway, 3f, -botW + sway, -1f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = body
        c.drawPath(path, p)
        line(c, -botW + 1.5f + sway, -2.2f, botW - 1.5f + sway, -2.2f, 3.4f, trim)
    }

    private fun begin(c: Canvas, x: Float, y: Float, s: Float, facing: Int, t: Float, moving: Boolean) {
        p.style = Paint.Style.FILL
        p.color = Color.argb(70, 0, 0, 0)
        oval.set(x - 15f * s, y - 5f * s, x + 15f * s, y + 5f * s)
        c.drawOval(oval, p)
        val bob = if (moving) abs(sin(t * 11f)) * 2.6f else sin(t * 2.2f) * 0.7f
        c.save()
        c.translate(x, y - bob * s)
        c.scale(if (facing < 0) -s else s, s)
    }

    private fun swing(t: Float, moving: Boolean): Float = if (moving) sin(t * 11f) * 5f else 0f

    // ---------------------------------------------------------------- oyuncu sınıfları

    fun drawHero(c: Canvas, cls: CharClass, x: Float, y: Float, s: Float, facing: Int, t: Float, moving: Boolean) {
        begin(c, x, y, s, facing, t, moving)
        val sw = swing(t, moving)
        when (cls) {
            CharClass.WARLORD -> warlord(c, sw, t)
            CharClass.RUNESEER -> runeseer(c, sw, t)
            CharClass.ASSASSIN -> assassin(c, sw, t)
            CharClass.MAGE -> mage(c, sw, t)
        }
        c.restore()
    }

    private fun warlord(c: Canvas, sw: Float, t: Float) {
        val steel = Color.rgb(150, 158, 172)
        val steelDark = Color.rgb(104, 110, 124)
        val red = Color.rgb(178, 44, 48)
        val gold = Color.rgb(232, 190, 72)
        legs(c, sw, Color.rgb(72, 62, 70), steelDark)
        // kalkan
        path.reset()
        path.moveTo(-19f, -41f)
        path.lineTo(-7f, -41f)
        path.lineTo(-7f, -23f)
        path.lineTo(-13f, -12f)
        path.lineTo(-19f, -23f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = steel
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.8f
        p.color = gold
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        line(c, -13f, -38f, -13f, -19f, 2.4f, red)
        line(c, -17f, -31f, -9f, -31f, 2.4f, red)
        // gövde
        rect(c, -10.5f, -39f, 10.5f, -14f, red, 5f)
        rect(c, -4.5f, -39f, 4.5f, -16f, steel, 3f)
        rect(c, -10.5f, -21f, 10.5f, -17.5f, gold)
        disc(c, 0f, -19.2f, 2.2f, steelDark)
        // omuzluklar
        disc(c, -10f, -37f, 5.8f, steelDark)
        disc(c, 10f, -37f, 5.8f, steelDark)
        disc(c, -10f, -37.8f, 4.3f, steel)
        disc(c, 10f, -37.8f, 4.3f, steel)
        // baş + miğfer
        face(c, HY + 4f)
        arc(c, -11.6f, HY - 11.6f, 11.6f, HY + 11.6f, 180f, 180f, steel)
        rect(c, -11.6f, HY - 2f, 11.6f, HY + 0.8f, steelDark)
        // tüy
        path.reset()
        path.moveTo(0f, HY - 11f)
        path.quadTo(-3f, HY - 26f + sin(t * 3f) * 1.5f, -15f, HY - 14f)
        path.quadTo(-6f, HY - 14f, 0f, HY - 11f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = red
        c.drawPath(path, p)
        // kılıç kolu
        line(c, 10f, -36f, 13.5f, -27f, 5.5f, red)
        line(c, 13.5f, -28f, 24.5f, -64f, 5.4f, Color.rgb(90, 96, 110))
        line(c, 13.5f, -28f, 24.5f, -64f, 3.2f, Color.rgb(226, 232, 244))
        line(c, 8.8f, -30f, 17.5f, -25.5f, 3f, gold)
        disc(c, 13.5f, -27f, 3.5f, steel)
    }

    private fun runeseer(c: Canvas, sw: Float, t: Float) {
        val white = Color.rgb(238, 238, 230)
        val blue = Color.rgb(60, 130, 200)
        val teal = Color.rgb(120, 230, 220)
        val silver = Color.rgb(208, 212, 228)
        val gold = Color.rgb(232, 190, 72)
        val pulse = 0.65f + 0.35f * sin(t * 4f)
        ovalFill(c, -12.5f, HY - 11f, 12.5f, HY + 16f, silver)
        robe(c, white, blue, sw * 0.3f)
        rect(c, -10.2f, -27f, 10.2f, -23.5f, blue)
        line(c, 0f, -37f, 0f, -30f, 1.4f, teal)
        line(c, 0f, -36f, 3.2f, -33.5f, 1.4f, teal)
        disc(c, -10f, -36f, 4.4f, white)
        face(c, HY + 2.5f)
        arc(c, -11f, HY - 11f, 11f, HY + 11f, 180f, 180f, silver)
        line(c, -10.4f, HY - 4f, 10.4f, HY - 4f, 1.6f, gold)
        glow(c, 4f, HY - 4f, 4.5f, teal, (90 * pulse).toInt())
        disc(c, 4f, HY - 4f, 2f, teal)
        // asa
        line(c, 13.5f, -3f, 13.5f, -63f, 2.6f, BROWN)
        line(c, 10f, -35f, 13.5f, -27f, 5f, white)
        disc(c, 13.5f, -27f, 3.2f, SKIN)
        glow(c, 13.5f, -69f, 10f, teal, (70 * pulse).toInt())
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = teal
        p.alpha = (140 + 115 * pulse).toInt().coerceIn(0, 255)
        c.drawCircle(13.5f, -69f, 5.5f, p)
        p.alpha = 255
        p.style = Paint.Style.FILL
        disc(c, 13.5f, -69f, 2.3f, Color.rgb(230, 255, 252))
    }

    private fun assassin(c: Canvas, sw: Float, t: Float) {
        val dark = Color.rgb(45, 48, 60)
        val dark2 = Color.rgb(30, 32, 42)
        val purple = Color.rgb(120, 40, 140)
        val crim = Color.rgb(220, 60, 90)
        val steel = Color.rgb(215, 222, 236)
        val w = sin(t * 7f) * 3f
        // atkı kuyruğu
        path.reset()
        path.moveTo(-7f, -41f)
        path.quadTo(-17f, -47f + w, -29f, -39f + w * 1.6f)
        path.quadTo(-18f, -38f + w, -7f, -36f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = crim
        c.drawPath(path, p)
        legs(c, sw, dark2, purple)
        // arka hançer
        line(c, -10f, -35f, -12f, -25f, 5f, dark)
        line(c, -12f, -25f, -22f, -33f, 2.6f, steel)
        disc(c, -12f, -25f, 3f, dark2)
        // gövde
        rect(c, -10f, -39f, 10f, -14f, dark, 5f)
        line(c, -9f, -37f, 9f, -18f, 3f, crim)
        rect(c, -10f, -21f, 10f, -17.5f, purple)
        disc(c, -10f, -36.5f, 4.4f, dark2)
        disc(c, 10f, -36.5f, 4.4f, dark2)
        // baş: maske + kukuleta
        face(c, HY - 1f, crim)
        arc(c, -10.5f, HY - 9.5f, 10.5f, HY + 11.5f, 0f, 180f, purple)
        arc(c, -12.5f, HY - 15f, 12.5f, HY + 7f, 180f, 180f, dark)
        rect(c, -12.5f, HY - 4f, -8f, HY + 6f, dark, 2f)
        tri(c, -9f, HY - 7f, -20f, HY - 15f, -8f, HY - 14f, dark)
        // ön hançer
        line(c, 10f, -35f, 13f, -26f, 5f, dark)
        line(c, 13f, -26f, 24f, -31f, 2.8f, steel)
        disc(c, 13f, -26f, 3f, dark2)
    }

    private fun mage(c: Canvas, sw: Float, t: Float) {
        val purple = Color.rgb(110, 60, 180)
        val purple2 = Color.rgb(70, 36, 130)
        val gold = Color.rgb(240, 200, 80)
        val orange = Color.rgb(255, 150, 60)
        val hair = Color.rgb(226, 120, 50)
        val pulse = 0.65f + 0.35f * sin(t * 4.5f)
        disc(c, -9f, HY + 3f, 5.5f, hair)
        robe(c, purple, gold, sw * 0.3f)
        rect(c, -10.2f, -27f, 10.2f, -23.5f, gold)
        disc(c, -10f, -35.5f, 4.8f, purple2)
        face(c, HY + 3f)
        // sapka
        ovalFill(c, -17f, HY - 11f, 17f, HY - 3f, purple2)
        path.reset()
        path.moveTo(-10f, HY - 7f)
        path.quadTo(-6f, HY - 30f, 9f, HY - 40f)
        path.quadTo(8f, HY - 22f, 11f, HY - 7f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = purple
        c.drawPath(path, p)
        rect(c, -10f, HY - 12f, 11f, HY - 8f, gold)
        disc(c, 1f, HY - 22f, 2.2f, gold)
        // asa
        line(c, 13.5f, -3f, 13.5f, -62f, 2.8f, BROWN)
        line(c, 13.5f, -62f, 16.8f, -66f, 2.8f, BROWN)
        glow(c, 17f, -69f, 11f, orange, (70 * pulse).toInt())
        disc(c, 17f, -69f, 5f, orange)
        disc(c, 15.6f, -70.4f, 1.8f, Color.rgb(255, 232, 196))
        for (i in 0 until 3) {
            val a = t * 3f + i * 2.1f
            glow(c, 17f + cos(a) * 10f, -69f + sin(a) * 6f, 1.3f, Color.WHITE, 220)
        }
        line(c, 10f, -35f, 13.5f, -27f, 5.5f, purple2)
        disc(c, 13.5f, -27f, 3.2f, SKIN)
    }

    // ---------------------------------------------------------------- NPC'ler

    fun drawNpc(c: Canvas, look: NpcLook, x: Float, y: Float, s: Float, facing: Int, t: Float, moving: Boolean) {
        begin(c, x, y, s, facing, t, moving)
        val sw = swing(t, moving)
        when (look) {
            NpcLook.ELDER -> elder(c, sw)
            NpcLook.BLACKSMITH -> blacksmith(c, sw)
            NpcLook.MERCHANT -> merchant(c, sw)
            NpcLook.HEALER -> healer(c, sw, t)
            NpcLook.GUARD -> guard(c, sw)
            NpcLook.INNKEEPER -> innkeeper(c, sw)
            NpcLook.KID -> kid(c, sw)
        }
        c.restore()
    }

    private fun elder(c: Canvas, sw: Float) {
        val robeC = Color.rgb(112, 84, 150)
        val trim = Color.rgb(232, 190, 72)
        val white = Color.rgb(240, 240, 240)
        robe(c, robeC, trim, sw * 0.3f)
        rect(c, -10f, -27f, 10f, -23.5f, trim)
        disc(c, -10f, -36f, 4.4f, robeC)
        disc(c, -9.5f, HY + 1f, 5f, white)
        face(c, HY + 1.5f)
        path.reset()
        path.moveTo(-8f, HY + 3f)
        path.quadTo(0f, HY + 7f, 9f, HY + 3f)
        path.lineTo(5f, HY + 22f)
        path.quadTo(0f, HY + 26f, -5f, HY + 22f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = white
        c.drawPath(path, p)
        line(c, 2f, HY - 1.5f, 5.5f, HY - 2.8f, 1.6f, white)
        line(c, 6.8f, HY - 2.8f, 9.5f, HY - 1.5f, 1.6f, white)
        line(c, 14f, -2f, 14f, -38f, 2.8f, BROWN)
        line(c, 14f, -38f, 17.5f, -40f, 2.8f, BROWN)
        line(c, 10f, -35f, 13.5f, -27f, 5f, robeC)
        disc(c, 14f, -28f, 3.2f, SKIN)
    }

    private fun blacksmith(c: Canvas, sw: Float) {
        val leather = Color.rgb(96, 64, 42)
        val shirt = Color.rgb(150, 110, 80)
        val hair = Color.rgb(70, 45, 30)
        legs(c, sw, Color.rgb(64, 54, 52), Color.rgb(40, 32, 30))
        rect(c, -11.5f, -40f, 11.5f, -14f, shirt, 6f)
        rect(c, -8f, -34f, 8f, -12f, leather, 4f)
        line(c, -11f, -36f, -13f, -25f, 6.5f, SKIN)
        line(c, 11f, -36f, 14f, -27f, 6.5f, SKIN)
        line(c, 14f, -27f, 21f, -47f, 2.8f, BROWN)
        line(c, 16.5f, -49.5f, 25f, -45.5f, 8f, Color.rgb(100, 106, 118))
        disc(c, 14f, -27f, 3.6f, SKIN)
        face(c, HY + 1.5f)
        arc(c, -11.2f, HY - 11.2f, 11.2f, HY + 11.2f, 180f, 180f, hair)
        rect(c, -11f, HY - 6f, 11f, HY - 3f, Color.rgb(190, 50, 50))
        path.reset()
        path.moveTo(-7f, HY + 3f)
        path.quadTo(0f, HY + 6f, 9f, HY + 3f)
        path.lineTo(5f, HY + 14f)
        path.quadTo(0f, HY + 17f, -4f, HY + 14f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = hair
        c.drawPath(path, p)
    }

    private fun merchant(c: Canvas, sw: Float) {
        val dress = Color.rgb(70, 140, 92)
        val trim = Color.rgb(246, 234, 200)
        val hair = Color.rgb(110, 70, 44)
        val straw = Color.rgb(226, 196, 110)
        ovalFill(c, -12f, HY - 8f, 12f, HY + 12f, hair)
        robe(c, dress, trim, sw * 0.3f, 9f, 13f)
        line(c, -7f, -38f, 7f, -21f, 2f, BROWN)
        rect(c, 3f, -26f, 12f, -17f, BROWN, 2.5f)
        disc(c, -10f, -36f, 4.2f, dress)
        face(c, HY + 2.5f)
        ovalFill(c, -16f, HY - 10f, 16f, HY - 3f, straw)
        arc(c, -9.5f, HY - 18f, 9.5f, HY - 2f, 180f, 180f, straw)
        rect(c, -9.5f, HY - 12f, 9.5f, HY - 9f, Color.rgb(200, 70, 70))
        line(c, 10f, -35f, 13.5f, -27f, 5f, dress)
        disc(c, 13.5f, -27f, 3.2f, SKIN)
        disc(c, 16f, -31f, 2.8f, Color.rgb(250, 210, 70))
    }

    private fun healer(c: Canvas, sw: Float, t: Float) {
        val dress = Color.rgb(236, 246, 240)
        val trim = Color.rgb(90, 170, 130)
        val hair = Color.rgb(236, 200, 110)
        val green = Color.rgb(120, 230, 150)
        val pulse = 0.65f + 0.35f * sin(t * 3.5f)
        ovalFill(c, -12.5f, HY - 11f, 12.5f, HY + 18f, hair)
        robe(c, dress, trim, sw * 0.3f)
        rect(c, -10.2f, -27f, 10.2f, -23.5f, trim)
        disc(c, -10f, -36f, 4.4f, dress)
        arc(c, -19f, -28f, -7f, -14f, 0f, 180f, Color.rgb(160, 110, 60))
        disc(c, -16f, -29f, 2.6f, Color.rgb(70, 170, 90))
        disc(c, -12f, -30f, 2.6f, Color.rgb(90, 190, 100))
        disc(c, -9.5f, -28.5f, 2f, Color.rgb(240, 130, 170))
        face(c, HY + 2.5f)
        arc(c, -11f, HY - 11f, 11f, HY + 11f, 180f, 180f, hair)
        disc(c, 5f, HY - 8f, 2.6f, Color.rgb(240, 130, 170))
        line(c, 10f, -35f, 13.5f, -27f, 5f, dress)
        disc(c, 13.5f, -27f, 3.2f, SKIN)
        glow(c, 16f, -32f, 8f, green, (80 * pulse).toInt())
        disc(c, 16f, -32f, 3.4f, green)
    }

    private fun guard(c: Canvas, sw: Float) {
        val steel = Color.rgb(140, 150, 168)
        val steelDark = Color.rgb(96, 104, 120)
        val blue = Color.rgb(48, 80, 150)
        val gold = Color.rgb(232, 190, 72)
        line(c, 15f, 2f, 15f, -92f, 2.8f, Color.rgb(120, 84, 50))
        tri(c, 15f, -104f, 11.5f, -90f, 18.5f, -90f, Color.rgb(220, 226, 238))
        legs(c, sw, Color.rgb(60, 66, 84), steelDark)
        rect(c, -10.5f, -39f, 10.5f, -14f, steel, 5f)
        rect(c, -7f, -39f, 7f, -15f, blue, 3f)
        line(c, 0f, -37f, 0f, -17f, 2f, gold)
        rect(c, -10.5f, -21f, 10.5f, -17.5f, steelDark)
        disc(c, -10f, -37f, 5.4f, steelDark)
        disc(c, 10f, -37f, 5.4f, steelDark)
        face(c, HY + 4f)
        arc(c, -11.6f, HY - 11.6f, 11.6f, HY + 11.6f, 180f, 180f, steel)
        rect(c, -11.6f, HY - 2f, 11.6f, HY + 0.8f, steelDark)
        rect(c, 9.2f, HY - 1f, 11.2f, HY + 9f, steelDark)
        line(c, 10f, -36f, 15f, -27f, 5.5f, steel)
        disc(c, 15f, -27f, 3.4f, steelDark)
    }

    private fun innkeeper(c: Canvas, sw: Float) {
        val shirt = Color.rgb(176, 90, 70)
        val apron = Color.rgb(240, 238, 228)
        val hair = Color.rgb(120, 80, 48)
        legs(c, sw, Color.rgb(70, 56, 50), Color.rgb(40, 32, 30))
        ovalFill(c, -13.5f, -40f, 13.5f, -12f, shirt)
        rect(c, -9.5f, -32f, 9.5f, -13f, apron, 5f)
        line(c, -11f, -36f, -13f, -26f, 6.5f, shirt)
        face(c, HY + 1.5f)
        disc(c, -9.5f, HY - 1f, 4.2f, hair)
        line(c, 2f, HY + 5f, 9.5f, HY + 5f, 2.6f, hair)
        line(c, 11f, -36f, 14f, -28f, 6f, shirt)
        disc(c, 14f, -28f, 3.4f, SKIN)
        rect(c, 12f, -37f, 19f, -27f, Color.rgb(200, 160, 90), 1.5f)
        rect(c, 12f, -39f, 19f, -36f, Color.WHITE, 1.5f)
        line(c, 19f, -35f, 21.5f, -32f, 1.8f, Color.rgb(200, 160, 90))
    }

    private fun kid(c: Canvas, sw: Float) {
        val tunic = Color.rgb(232, 140, 60)
        val hair = Color.rgb(104, 68, 40)
        legs(c, sw, Color.rgb(90, 100, 150), Color.rgb(110, 70, 40))
        rect(c, -9f, -37f, 9f, -15f, tunic, 5f)
        line(c, -9f, -20f, 9f, -20f, 2.4f, Color.rgb(120, 70, 30))
        line(c, -9f, -34f, -12f, -24f, 4.6f, SKIN)
        line(c, 9f, -34f, 12f, -24f, 4.6f, SKIN)
        face(c, HY + 2.5f)
        arc(c, -11f, HY - 11f, 11f, HY + 11f, 180f, 180f, hair)
        disc(c, 2f, HY - 12.5f, 3.2f, hair)
        disc(c, -4f, HY - 11.5f, 3f, hair)
    }
}
