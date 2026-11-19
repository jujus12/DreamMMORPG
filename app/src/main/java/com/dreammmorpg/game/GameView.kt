package com.dreammmorpg.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tüm oyun tek bir SurfaceView içinde çizilir.
 * Sanal ekran: yükseklik her zaman 720 birim, genişlik ekran oranına göre değişir.
 */
class GameView(private val ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {

    interface Listener {
        fun askName(cls: CharClass, onName: (String) -> Unit)
        fun askDelete(hero: Hero, onYes: () -> Unit)
    }

    var listener: Listener? = null

    private enum class Screen { SELECT, CREATE, VILLAGE }

    private class Btn(val id: String, val x: Float, val y: Float, val w: Float, val h: Float)

    // ---- genel durum
    private var thread: Thread? = null
    @Volatile private var running = false
    private val events = ConcurrentLinkedQueue<() -> Unit>()
    private val btns = ArrayList<Btn>()
    @Volatile private var screen = Screen.SELECT
    private val heroes: Array<Hero?> = Save.load(ctx)
    private var selected = -1
    private var createSlot = 0
    private var createCls = CharClass.WARLORD
    private var time = 0f
    @Volatile private var errorText: String? = null

    @Volatile private var sc = 1f
    @Volatile private var vw = 1280f
    private val vh = 720f
    @Volatile private var vigShader: Shader? = null

    // ---- köy durumu
    private var village: Village? = null
    private var hero: Hero? = null
    private var px = 0f
    private var py = 0f
    private var pFacing = 1
    private var pMoving = false
    private var hasTarget = false
    private var tx = 0f
    private var ty = 0f
    private var stuck = 0f
    private var pressing = false
    private var pendingTalk: Npc? = null
    private var dlgNpc: Npc? = null
    private var dlgIdx = 0
    private var camX = 0f
    private var camY = 0f
    private var zoneT = 0f
    private var kUp = false
    private var kDown = false
    private var kLeft = false
    private var kRight = false
    private val pos = FloatArray(2)

    // ---- boyalar
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgp = Paint()
    private val vigPaint = Paint()
    private val rf = RectF()
    private val path = Path()
    private val stars = FloatArray(90 * 3)
    private val powerColors = intArrayOf(
        Color.rgb(235, 80, 70), Color.rgb(80, 150, 240), Color.rgb(90, 210, 120),
        Color.rgb(245, 200, 80), Color.rgb(170, 110, 240)
    )
    private val gold = Color.rgb(246, 206, 98)
    private val soft = Color.rgb(205, 210, 236)

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        val r = Random(3)
        for (i in 0 until 90) {
            stars[i * 3] = r.nextFloat()
            stars[i * 3 + 1] = r.nextFloat() * 0.72f
            stars[i * 3 + 2] = r.nextFloat() * 6.28f
        }
        bgp.shader = LinearGradient(
            0f, 0f, 0f, 720f,
            intArrayOf(Color.rgb(14, 16, 46), Color.rgb(54, 34, 96), Color.rgb(190, 96, 100)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        for (i in heroes.indices) if (heroes[i] != null) {
            selected = i
            break
        }
    }

    // ============================================================ yaşam döngüsü

    override fun surfaceCreated(h: SurfaceHolder) {
        if (running) return
        running = true
        thread = Thread(this, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, hh: Int) {
        if (hh <= 0) return
        val s = hh / 720f
        sc = s
        vw = w / s
        vigShader = RadialGradient(
            vw / 2f, vh / 2f, hypot(vw / 2f, vh / 2f),
            intArrayOf(Color.argb(0, 8, 10, 30), Color.argb(0, 8, 10, 30), Color.argb(120, 8, 10, 30)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try {
            thread?.join(800)
        } catch (e: InterruptedException) {
        }
        thread = null
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1e9f
            last = now
            if (dt > 0.05f) dt = 0.05f
            while (true) {
                val e = events.poll() ?: break
                try {
                    e()
                } catch (t: Throwable) {
                    errorText = describe(t)
                }
            }
            try {
                update(dt)
            } catch (t: Throwable) {
                errorText = describe(t)
            }
            val h = holder
            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= 26) h.lockHardwareCanvas() else h.lockCanvas()
                if (canvas != null) {
                    try {
                        render(canvas)
                    } catch (t: Throwable) {
                        errorText = describe(t)
                        drawError(canvas)
                    }
                }
            } catch (t: Throwable) {
                errorText = describe(t)
            } finally {
                if (canvas != null) {
                    try {
                        h.unlockCanvasAndPost(canvas)
                    } catch (t: Throwable) {
                    }
                }
            }
            val spent = (System.nanoTime() - now) / 1_000_000L
            if (spent < 16L) {
                try {
                    Thread.sleep(16L - spent)
                } catch (e: InterruptedException) {
                }
            }
        }
    }

    private fun describe(t: Throwable): String {
        val top = t.stackTrace.firstOrNull()
        return t.javaClass.simpleName + ": " + (t.message ?: "") + (if (top != null) " @ " + top.fileName + ":" + top.lineNumber else "")
    }

    // ============================================================ girdi

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = sc
        val x = e.x / s
        val y = e.y / s
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> events.add { onDown(x, y) }
            MotionEvent.ACTION_MOVE -> events.add { onMove(x, y) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> events.add { pressing = false }
        }
        return true
    }

    fun keyDown(code: Int, repeat: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> events.add { kUp = true }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> events.add { kDown = true }
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> events.add { kLeft = true }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> events.add { kRight = true }
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                if (repeat == 0) events.add { interact() }
            }
            else -> return false
        }
        return true
    }

    fun keyUp(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> events.add { kUp = false }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> events.add { kDown = false }
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> events.add { kLeft = false }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> events.add { kRight = false }
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
            }
            else -> return false
        }
        return true
    }

    /** Geri tuşu: true dönerse oyun kendisi halletti. */
    fun handleBack(): Boolean {
        return when (screen) {
            Screen.VILLAGE -> {
                events.add { leaveVillage() }
                true
            }
            Screen.CREATE -> {
                events.add { screen = Screen.SELECT }
                true
            }
            Screen.SELECT -> false
        }
    }

    private fun onDown(x: Float, y: Float) {
        var hit: Btn? = null
        for (b in btns) if (x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h) hit = b
        if (hit != null) {
            onButton(hit.id)
            return
        }
        if (screen == Screen.VILLAGE) {
            pressing = true
            groundTap(x, y, true)
        }
    }

    private fun onMove(x: Float, y: Float) {
        if (screen == Screen.VILLAGE && pressing && pendingTalk == null) groundTap(x, y, false)
    }

    private fun groundTap(sx: Float, sy: Float, fresh: Boolean) {
        val v = village ?: return
        val wx = sx + camX
        val wy = sy + camY
        if (fresh) {
            val npc = v.npcAt(wx, wy)
            if (npc != null) {
                pendingTalk = npc
                tx = npc.x
                ty = npc.y
                hasTarget = true
                stuck = 0f
                return
            }
            pendingTalk = null
        }
        tx = wx
        ty = wy
        hasTarget = true
    }

    private fun onButton(id: String) {
        when {
            id.startsWith("slot") -> {
                val i = id.substring(4).toInt()
                if (heroes[i] == null) {
                    createSlot = i
                    createCls = CharClass.WARLORD
                    screen = Screen.CREATE
                } else {
                    selected = i
                }
            }
            id == "enter" -> enterVillage()
            id == "delete" -> {
                val i = selected
                val h = if (i >= 0) heroes[i] else null
                if (h != null) {
                    listener?.askDelete(h) {
                        events.add {
                            heroes[i] = null
                            Save.store(ctx, i, null)
                            if (selected == i) selected = -1
                        }
                    }
                }
            }
            id.startsWith("cls") -> createCls = CharClass.values()[id.substring(3).toInt()]
            id == "create" -> {
                val cls = createCls
                val slot = createSlot
                listener?.askName(cls) { name ->
                    events.add {
                        val h = Hero(name, cls)
                        heroes[slot] = h
                        Save.store(ctx, slot, h)
                        selected = slot
                        screen = Screen.SELECT
                    }
                }
            }
            id == "back" -> screen = Screen.SELECT
            id == "exit" -> leaveVillage()
            id == "dlg" -> advanceDialog()
        }
    }

    // ============================================================ oyun mantığı

    private fun enterVillage() {
        val h = if (selected >= 0) heroes[selected] else null
        if (h == null) return
        hero = h
        val v = village ?: Village().also { village = it }
        px = Village.SPAWN_X
        py = Village.SPAWN_Y
        pFacing = 1
        hasTarget = false
        pendingTalk = null
        dlgNpc = null
        pressing = false
        kUp = false; kDown = false; kLeft = false; kRight = false
        zoneT = 0f
        camX = (px - vw / 2f).coerceIn(0f, max(0f, Village.W - vw))
        camY = (py - vh / 2f - 30f).coerceIn(0f, max(0f, Village.H - vh))
        v.canopyAlpha = 1f
        screen = Screen.VILLAGE
    }

    private fun leaveVillage() {
        closeDialog()
        hasTarget = false
        pendingTalk = null
        screen = Screen.SELECT
    }

    private fun startTalk(n: Npc) {
        closeDialog()
        dlgNpc = n
        dlgIdx = 0
        n.talking = true
        pFacing = if (n.x >= px) 1 else -1
    }

    private fun advanceDialog() {
        val n = dlgNpc ?: return
        dlgIdx++
        if (dlgIdx >= n.lines.size) closeDialog()
    }

    private fun closeDialog() {
        dlgNpc?.talking = false
        dlgNpc = null
    }

    private fun interact() {
        if (screen != Screen.VILLAGE) return
        if (dlgNpc != null) {
            advanceDialog()
            return
        }
        val v = village ?: return
        val n = v.nearestNpc(px, py, 130f) ?: return
        startTalk(n)
    }

    private fun update(dt: Float) {
        time += dt
        if (screen != Screen.VILLAGE) return
        val v = village ?: return
        zoneT += dt

        var dx = 0f
        var dy = 0f
        if (kUp) dy -= 1f
        if (kDown) dy += 1f
        if (kLeft) dx -= 1f
        if (kRight) dx += 1f
        val speed = 200f
        var moving = false

        if (dx != 0f || dy != 0f) {
            hasTarget = false
            pendingTalk = null
            val len = hypot(dx, dy)
            dx /= len
            dy /= len
            pos[0] = px + dx * speed * dt
            pos[1] = py + dy * speed * dt
            v.resolve(pos, 14f)
            px = pos[0]
            py = pos[1]
            moving = true
            if (dx != 0f) pFacing = if (dx > 0f) 1 else -1
        } else if (hasTarget) {
            val pt = pendingTalk
            if (pt != null) {
                tx = pt.x
                ty = pt.y
            }
            val ddx = tx - px
            val ddy = ty - py
            val d = hypot(ddx, ddy)
            val stopDist = if (pt != null) 80f else 4f
            if (d <= stopDist) {
                hasTarget = false
                if (pt != null) {
                    pendingTalk = null
                    startTalk(pt)
                }
            } else {
                val step = min(speed * dt, d)
                pos[0] = px + ddx / d * step
                pos[1] = py + ddy / d * step
                v.resolve(pos, 14f)
                val moved = hypot(pos[0] - px, pos[1] - py)
                px = pos[0]
                py = pos[1]
                if (moved < step * 0.25f) stuck += dt else stuck = 0f
                if (stuck > 0.35f) {
                    hasTarget = false
                    pendingTalk = null
                    stuck = 0f
                }
                moving = true
                if (kotlin.math.abs(ddx) > 1f) pFacing = if (ddx > 0f) 1 else -1
            }
        }
        pMoving = moving

        val dn = dlgNpc
        if (dn != null && hypot(px - dn.x, py - dn.y) > 200f) closeDialog()

        v.update(dt, px, py)

        val txCam = (px - vw / 2f).coerceIn(0f, max(0f, Village.W - vw))
        val tyCam = (py - vh / 2f - 30f).coerceIn(0f, max(0f, Village.H - vh))
        val k = min(1f, dt * 9f)
        camX += (txCam - camX) * k
        camY += (tyCam - camY) * k
    }

    // ============================================================ çizim yardımcıları

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.CENTER, bold: Boolean = true) {
        tp.style = Paint.Style.FILL
        tp.textSize = size
        tp.color = color
        tp.textAlign = align
        tp.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        c.drawText(s, x, y, tp)
    }

    private fun rrect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        fp.style = Paint.Style.FILL
        fp.color = color
        c.drawRoundRect(l, t, r, b, rad, rad, fp)
    }

    private fun srect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, w: Float, color: Int) {
        fp.style = Paint.Style.STROKE
        fp.strokeWidth = w
        fp.color = color
        c.drawRoundRect(l, t, r, b, rad, rad, fp)
        fp.style = Paint.Style.FILL
    }

    private fun disc(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        fp.style = Paint.Style.FILL
        fp.color = color
        c.drawCircle(x, y, r, fp)
    }

    private fun oval(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        fp.style = Paint.Style.FILL
        fp.color = color
        rf.set(l, t, r, b)
        c.drawOval(rf, fp)
    }

    private fun button(c: Canvas, id: String, x: Float, y: Float, w: Float, h: Float, label: String, enabled: Boolean, primary: Boolean) {
        val bg = when {
            !enabled -> Color.argb(120, 60, 62, 80)
            primary -> Color.rgb(214, 140, 40)
            else -> Color.argb(200, 52, 58, 104)
        }
        rrect(c, x, y, x + w, y + h, h / 2.6f, bg)
        if (enabled) {
            rrect(c, x + 3f, y + 3f, x + w - 3f, y + h * 0.5f, h / 3f, Color.argb(if (primary) 70 else 40, 255, 255, 255))
            srect(c, x, y, x + w, y + h, h / 2.6f, 2f, if (primary) Color.rgb(255, 224, 150) else Color.argb(160, 190, 200, 255))
            btns.add(Btn(id, x, y, w, h))
        }
        text(c, label, x + w / 2f, y + h / 2f + h * 0.17f, h * 0.38f, if (enabled) Color.WHITE else Color.argb(130, 255, 255, 255))
    }

    private fun wrap(s: String, maxW: Float, size: Float): List<String> {
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT
        val out = ArrayList<String>()
        var line = ""
        for (word in s.split(" ")) {
            val cand = if (line.isEmpty()) word else "$line $word"
            if (tp.measureText(cand) > maxW && line.isNotEmpty()) {
                out.add(line)
                line = word
            } else {
                line = cand
            }
        }
        if (line.isNotEmpty()) out.add(line)
        return out
    }

    private fun drawError(c: Canvas) {
        val e = errorText ?: return
        c.save()
        c.scale(sc, sc)
        rrect(c, 10f, vh - 70f, vw - 10f, vh - 10f, 8f, Color.argb(220, 120, 0, 0))
        text(c, "Hata: $e", 22f, vh - 34f, 15f, Color.WHITE, Paint.Align.LEFT, false)
        c.restore()
    }

    // ============================================================ render

    private fun render(c: Canvas) {
        btns.clear()
        c.save()
        c.scale(sc, sc)
        when (screen) {
            Screen.SELECT -> drawSelect(c)
            Screen.CREATE -> drawCreate(c)
            Screen.VILLAGE -> drawVillageScreen(c)
        }
        c.restore()
        if (errorText != null) drawError(c)
    }

    private fun drawMenuBg(c: Canvas) {
        val w = vw
        c.drawRect(0f, 0f, w, vh, bgp)
        // yıldızlar
        for (i in 0 until 90) {
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * 1.6f + stars[i * 3 + 2]))
            disc(c, stars[i * 3] * w, stars[i * 3 + 1] * vh, 1.2f + (i % 3) * 0.6f, Color.argb((200 * a).toInt(), 255, 255, 235))
        }
        // ay
        disc(c, w * 0.84f, 118f, 90f, Color.argb(26, 255, 244, 210))
        disc(c, w * 0.84f, 118f, 56f, Color.argb(60, 255, 244, 210))
        disc(c, w * 0.84f, 118f, 44f, Color.rgb(255, 246, 214))
        disc(c, w * 0.84f - 10f, 108f, 9f, Color.argb(40, 200, 180, 140))
        // tepeler
        path.reset()
        path.moveTo(0f, 566f)
        path.quadTo(w * 0.22f, 470f, w * 0.48f, 566f)
        path.quadTo(w * 0.74f, 650f, w, 540f)
        path.lineTo(w, vh)
        path.lineTo(0f, vh)
        path.close()
        fp.style = Paint.Style.FILL
        fp.color = Color.rgb(34, 30, 70)
        c.drawPath(path, fp)
        path.reset()
        path.moveTo(0f, 640f)
        path.quadTo(w * 0.3f, 590f, w * 0.58f, 650f)
        path.quadTo(w * 0.8f, 690f, w, 630f)
        path.lineTo(w, vh)
        path.lineTo(0f, vh)
        path.close()
        fp.color = Color.rgb(20, 20, 46)
        c.drawPath(path, fp)
        // beş kadim güç: süzülen ışıklar
        for (i in 0 until 5) {
            val ox = w * (0.12f + 0.19f * i) + sin(time * 0.7f + i * 1.9f) * 26f
            val oy = 600f + cos(time * 0.9f + i * 1.3f) * 22f
            val col = powerColors[i]
            disc(c, ox, oy, 22f, Color.argb(36, Color.red(col), Color.green(col), Color.blue(col)))
            disc(c, ox, oy, 5f, col)
        }
    }

    private fun drawSelect(c: Canvas) {
        drawMenuBg(c)
        text(c, "DREAM MMORPG", vw / 2f + 2f, 94f, 54f, Color.argb(150, 0, 0, 0))
        text(c, "DREAM MMORPG", vw / 2f, 92f, 54f, gold)
        text(c, "Karakterini seç", vw / 2f, 128f, 22f, soft, Paint.Align.CENTER, false)

        val gap = 26f
        val cw = min(240f, (vw - 80f - gap * 3f) / 4f)
        val ch = 350f
        val sx = (vw - (cw * 4f + gap * 3f)) / 2f
        val sy = 160f
        for (i in 0 until Save.SLOTS) {
            val x = sx + i * (cw + gap)
            val h = heroes[i]
            val sel = i == selected && h != null
            rrect(c, x, sy, x + cw, sy + ch, 18f, Color.argb(if (sel) 190 else 140, 12, 14, 38))
            srect(c, x, sy, x + cw, sy + ch, 18f, if (sel) 3.5f else 2f, if (sel) gold else Color.argb(110, 160, 170, 220))
            btns.add(Btn("slot$i", x, sy, cw, ch))
            val mx = x + cw / 2f
            if (h != null) {
                val col = h.cls.accent
                disc(c, mx, sy + 190f, 90f, Color.argb(34, Color.red(col), Color.green(col), Color.blue(col)))
                oval(c, mx - 74f, sy + 252f, mx + 74f, sy + 286f, Color.argb(90, Color.red(col), Color.green(col), Color.blue(col)))
                CharacterRenderer.drawHero(c, h.cls, mx, sy + 270f, 2.4f, 1, time + i, false)
                text(c, h.name, mx, sy + 312f, 24f, Color.WHITE)
                text(c, "Lv.1 · " + h.cls.title, mx, sy + 338f, 16f, col)
            } else {
                disc(c, mx, sy + 150f, 46f, Color.argb(50, 255, 255, 255))
                text(c, "+", mx, sy + 172f, 62f, Color.argb(200, 255, 255, 255))
                text(c, "Boş Slot", mx, sy + 250f, 22f, Color.WHITE)
                text(c, "Karakter Oluştur", mx, sy + 280f, 16f, soft, Paint.Align.CENTER, false)
            }
        }

        val canPlay = selected >= 0 && heroes[selected] != null
        button(c, "enter", vw / 2f - 150f, 560f, 300f, 64f, "Oyuna Gir", canPlay, true)
        button(c, "delete", vw / 2f + 190f, 568f, 170f, 48f, "Karakteri Sil", canPlay, false)
        text(c, "Boş bir slota dokunarak yeni karakter oluştur", vw / 2f, 664f, 16f, Color.argb(200, 205, 210, 236), Paint.Align.CENTER, false)
    }

    private fun drawCreate(c: Canvas) {
        drawMenuBg(c)
        text(c, "Karakter Oluştur", 40f, 80f, 36f, gold, Paint.Align.LEFT)
        button(c, "back", vw - 190f, 40f, 150f, 46f, "← Geri", true, false)

        val classes = CharClass.values()
        for (i in classes.indices) {
            val cls = classes[i]
            val x = 40f
            val y = 124f + i * 118f
            val sel = cls == createCls
            rrect(c, x, y, x + 290f, y + 104f, 16f, Color.argb(if (sel) 200 else 130, 12, 14, 38))
            srect(c, x, y, x + 290f, y + 104f, 16f, if (sel) 3.5f else 2f, if (sel) gold else Color.argb(110, 160, 170, 220))
            btns.add(Btn("cls$i", x, y, 290f, 104f))
            disc(c, x + 54f, y + 52f, 32f, cls.primary)
            srect(c, x + 22f, y + 20f, x + 86f, y + 84f, 32f, 3f, cls.accent)
            text(c, cls.title.substring(0, 1), x + 54f, y + 64f, 34f, if (cls == CharClass.RUNESEER) Color.rgb(60, 130, 200) else Color.WHITE)
            text(c, cls.title, x + 106f, y + 46f, 26f, Color.WHITE, Paint.Align.LEFT)
            text(c, cls.role, x + 106f, y + 76f, 18f, cls.accent, Paint.Align.LEFT)
        }

        val panelX = vw - 380f
        val mid = (330f + panelX) / 2f
        val col = createCls.accent
        disc(c, mid, 390f, 190f, Color.argb(30, Color.red(col), Color.green(col), Color.blue(col)))
        oval(c, mid - 150f, 526f, mid + 150f, 594f, Color.argb(70, Color.red(col), Color.green(col), Color.blue(col)))
        oval(c, mid - 110f, 536f, mid + 110f, 584f, Color.argb(50, 255, 255, 255))
        CharacterRenderer.drawHero(c, createCls, mid, 560f, 4.2f, 1, time, false)

        rrect(c, panelX, 124f, panelX + 340f, 584f, 18f, Color.argb(170, 12, 14, 38))
        srect(c, panelX, 124f, panelX + 340f, 584f, 18f, 2f, Color.argb(110, 160, 170, 220))
        text(c, createCls.title, panelX + 22f, 176f, 38f, Color.WHITE, Paint.Align.LEFT)
        rrect(c, panelX + 22f, 192f, panelX + 22f + 120f, 222f, 15f, Color.argb(160, Color.red(col), Color.green(col), Color.blue(col)))
        text(c, createCls.role, panelX + 82f, 214f, 17f, Color.rgb(20, 20, 30))
        val lines = wrap(createCls.desc, 296f, 17f)
        for ((i, l) in lines.withIndex()) text(c, l, panelX + 22f, 258f + i * 24f, 17f, soft, Paint.Align.LEFT, false)

        val labels = arrayOf("Güç", "Savunma", "Büyü", "Hız")
        val vals = intArrayOf(createCls.str, createCls.def, createCls.mag, createCls.spd)
        for (i in 0 until 4) {
            val yy = 420f + i * 40f
            text(c, labels[i], panelX + 22f, yy, 17f, Color.WHITE, Paint.Align.LEFT)
            for (k in 0 until 5) {
                val bx = panelX + 118f + k * 40f
                rrect(c, bx, yy - 15f, bx + 34f, yy + 1f, 5f, if (k < vals[i]) col else Color.argb(60, 255, 255, 255))
            }
        }
        button(c, "create", panelX + 20f, 604f, 300f, 64f, "Oluştur", true, true)
    }

    private fun drawVillageScreen(c: Canvas) {
        val v = village ?: return
        val hr = hero ?: return

        c.save()
        c.translate(-camX, -camY)
        v.draw(c, camX, camY, vw, vh, time, py) { cv ->
            CharacterRenderer.drawHero(cv, hr.cls, px, py, 1.1f, pFacing, time, pMoving)
            v.nameTag(cv, hr.name, px, py - 72f * 1.1f - 6f, 14f, Color.rgb(190, 255, 200))
        }
        if (hasTarget && pendingTalk == null) {
            val r = 11f + 3f * sin(time * 8f)
            fp.style = Paint.Style.STROKE
            fp.strokeWidth = 3f
            fp.color = Color.argb(210, 255, 240, 150)
            rf.set(tx - r, ty - r * 0.6f, tx + r, ty + r * 0.6f)
            c.drawOval(rf, fp)
            fp.style = Paint.Style.FILL
        }
        c.restore()

        // vinyet
        vigPaint.shader = vigShader
        c.drawRect(0f, 0f, vw, vh, vigPaint)

        drawHud(c, v, hr)
    }

    private fun drawHud(c: Canvas, v: Village, hr: Hero) {
        val cls = hr.cls
        // karakter kartı
        rrect(c, 16f, 16f, 300f, 112f, 14f, Color.argb(160, 12, 14, 36))
        srect(c, 16f, 16f, 300f, 112f, 14f, 2f, Color.argb(120, 200, 210, 255))
        disc(c, 58f, 62f, 30f, cls.primary)
        srect(c, 28f, 32f, 88f, 92f, 30f, 3f, cls.accent)
        text(c, cls.title.substring(0, 1), 58f, 74f, 32f, if (cls == CharClass.RUNESEER) Color.rgb(60, 130, 200) else Color.WHITE)
        text(c, hr.name, 102f, 48f, 22f, Color.WHITE, Paint.Align.LEFT)
        text(c, "Lv.1 · " + cls.role, 102f, 70f, 15f, cls.accent, Paint.Align.LEFT)
        rrect(c, 102f, 80f, 286f, 91f, 5f, Color.argb(120, 0, 0, 0))
        rrect(c, 102f, 80f, 286f, 91f, 5f, Color.rgb(214, 64, 64))
        rrect(c, 102f, 96f, 286f, 107f, 5f, Color.argb(120, 0, 0, 0))
        rrect(c, 102f, 96f, 286f, 107f, 5f, Color.rgb(70, 120, 230))

        // mini harita
        val ms = 170f
        val mx = vw - 16f - ms
        val my = 16f
        val k = ms / Village.W
        rrect(c, mx - 3f, my - 3f, mx + ms + 3f, my + ms + 3f, 12f, Color.argb(190, 20, 24, 40))
        c.save()
        c.clipRect(mx, my, mx + ms, my + ms)
        fp.style = Paint.Style.FILL
        fp.color = Color.rgb(70, 130, 64)
        c.drawRect(mx, my, mx + ms, my + ms, fp)
        fp.color = Color.rgb(176, 144, 100)
        c.drawRect(mx + (Village.CX - 75f) * k, my, mx + (Village.CX + 75f) * k, my + ms, fp)
        c.drawRect(mx, my + (Village.CY - 75f) * k, mx + ms, my + (Village.CY + 75f) * k, fp)
        disc(c, mx + Village.CX * k, my + Village.CY * k, 432f * k, Color.rgb(190, 180, 158))
        for (p in v.props) {
            if (p.kind == Prop.Kind.HOUSE) {
                fp.color = Color.rgb(150, 70, 56)
                c.drawRect(mx + p.x * k, my + (p.y - 90f) * k, mx + (p.x + p.w) * k, my + p.y * k, fp)
            } else if (p.kind == Prop.Kind.BIGTREE) {
                disc(c, mx + p.x * k, my + (p.y - 30f) * k, 11f, Color.rgb(46, 112, 58))
                disc(c, mx + p.x * k, my + (p.y - 30f) * k, 5f, Color.rgb(112, 190, 100))
            }
        }
        for (n in v.npcs) disc(c, mx + n.x * k, my + n.y * k, 2.6f, Color.rgb(255, 220, 90))
        val pr = 4f + 1.5f * sin(time * 5f)
        disc(c, mx + px * k, my + py * k, pr + 3f, Color.argb(90, 255, 255, 255))
        disc(c, mx + px * k, my + py * k, 3.4f, Color.WHITE)
        c.restore()
        srect(c, mx, my, mx + ms, my + ms, 10f, 2f, Color.argb(200, 220, 230, 255))
        text(c, "Huzur Ormanı", mx + ms / 2f, my + ms + 20f, 14f, Color.WHITE)
        button(c, "exit", mx, my + ms + 32f, ms, 38f, "Karakter Seç", true, false)

        // bölge başlığı
        if (zoneT < 5f) {
            val a = min(1f, min(zoneT / 0.8f, (5f - zoneT) / 1f))
            val al = (255 * a).toInt()
            text(c, "Huzur Ormanı", vw / 2f + 2f, 122f, 44f, Color.argb((150 * a).toInt(), 0, 0, 0))
            text(c, "Huzur Ormanı", vw / 2f, 120f, 44f, Color.argb(al, 255, 236, 170))
            text(c, "Başlangıç Köyü", vw / 2f, 152f, 21f, Color.argb(al, 255, 255, 255), Paint.Align.CENTER, false)
        }

        // ipucu
        if (zoneT < 20f) {
            val a = min(1f, (20f - zoneT) / 2f)
            val al = (220 * a).toInt()
            text(c, "Yürü: WASD / ok tuşları veya ekrana dokun (basılı tut)", 20f, vh - 52f, 15f, Color.argb(al, 255, 255, 255), Paint.Align.LEFT, false)
            text(c, "Konuş: NPC'ye dokun veya E", 20f, vh - 30f, 15f, Color.argb(al, 255, 255, 255), Paint.Align.LEFT, false)
        }

        drawDialog(c)
    }

    private fun drawDialog(c: Canvas) {
        val n = dlgNpc ?: return
        val pw = min(900f, vw - 60f)
        val x = (vw - pw) / 2f
        val y = vh - 196f
        val h = 164f
        rrect(c, x, y, x + pw, y + h, 18f, Color.argb(230, 18, 16, 36))
        srect(c, x, y, x + pw, y + h, 18f, 2.5f, gold)
        btns.add(Btn("dlg", x, y, pw, h))
        rrect(c, x + 20f, y - 20f, x + 280f, y + 18f, 12f, Color.rgb(150, 96, 40))
        text(c, n.name, x + 34f, y + 6f, 22f, Color.WHITE, Paint.Align.LEFT)
        text(c, n.title, x + 270f, y + 5f, 14f, Color.rgb(255, 230, 170), Paint.Align.RIGHT, false)
        val idx = min(dlgIdx, n.lines.size - 1)
        val lines = wrap(n.lines[idx], pw - 56f, 21f)
        for ((i, l) in lines.withIndex()) text(c, l, x + 28f, y + 56f + i * 29f, 21f, Color.WHITE, Paint.Align.LEFT, false)
        text(c, "${idx + 1}/${n.lines.size}  ·  devam: dokun / E", x + pw - 22f, y + h - 16f, 14f, Color.argb(200, 205, 210, 236), Paint.Align.RIGHT, false)
    }
}
