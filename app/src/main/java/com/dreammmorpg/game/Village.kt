package com.dreammmorpg.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Npc(
    val name: String,
    val title: String,
    val look: NpcLook,
    val homeX: Float,
    val homeY: Float,
    val lines: List<String>,
    val wander: Float,
    val scale: Float = 1f,
    val speed: Float = 42f
) {
    var x = homeX
    var y = homeY
    var tx = homeX
    var ty = homeY
    var facing = 1
    var moving = false
    var wait = 1f + Random.nextFloat() * 3f
    var talking = false
    val phase = Random.nextFloat() * 10f
}

class Prop(
    val kind: Kind,
    val x: Float,
    val y: Float,
    val w: Float = 0f,
    val h: Float = 0f,
    val c1: Int = 0,
    val c2: Int = 0,
    val s: Float = 1f,
    val label: String = ""
) {
    enum class Kind { HOUSE, PINE, OAK, LAMP, BARREL, CRATE, STALL, WELL, FORGE, GATE, ROCK, BENCH, BOARD, BUSH, BIGTREE }
}

private fun Random.range(a: Float, b: Float): Float = a + nextFloat() * (b - a)

/** Huzur Ormanı – Başlangıç Köyü: ulu ağaç, meydan, binalar, NPC'ler. */
class Village {
    companion object {
        const val W = 2400f
        const val H = 2400f
        const val CX = 1200f
        const val CY = 1200f
        const val TREE_BASE = CY + 40f
        const val SPAWN_X = CX
        const val SPAWN_Y = CY + 380f
        private const val PLAZA_R = 432f
        private const val ROAD_HALF = 75f
    }

    val props = ArrayList<Prop>()
    val npcs = ArrayList<Npc>()
    var canopyAlpha = 1f

    private val circles = ArrayList<FloatArray>()
    private val rects = ArrayList<FloatArray>()

    private class Deco(val x: Float, val y: Float, val a: Float, val b: Float, val color: Int)

    private val patches = ArrayList<Deco>()
    private val tufts = ArrayList<Deco>()
    private val flowers = ArrayList<Deco>()
    private val cobbles = ArrayList<Deco>()
    private val canopy = ArrayList<FloatArray>()   // x, y, r, katman
    private val blossoms = ArrayList<FloatArray>() // x, y, renk
    private val flies = ArrayList<FloatArray>()    // aci, yaricap, hiz
    private val leaves = ArrayList<FloatArray>()   // x, tohum, boyut

    private val powerColors = intArrayOf(
        Color.rgb(235, 80, 70), Color.rgb(80, 150, 240), Color.rgb(90, 210, 120),
        Color.rgb(245, 200, 80), Color.rgb(170, 110, 240)
    )

    private val gp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()
    private val path = Path()

    init {
        build()
    }

    // ============================================================ kurulum

    private fun house(x: Float, baseY: Float, w: Float, h: Float, wall: Int, roof: Int, label: String) {
        props.add(Prop(Prop.Kind.HOUSE, x, baseY, w, h, wall, roof, 1f, label))
        rects.add(floatArrayOf(x - 4f, baseY - 54f, x + w + 4f, baseY))
    }

    private fun circleCol(x: Float, y: Float, r: Float) {
        circles.add(floatArrayOf(x, y, r))
    }

    fun blocked(x: Float, y: Float, r: Float): Boolean {
        for (c in circles) if (hypot(x - c[0], y - c[1]) < c[2] + r) return true
        for (rc in rects) {
            val nx = x.coerceIn(rc[0], rc[2])
            val ny = y.coerceIn(rc[1], rc[3])
            if (hypot(x - nx, y - ny) < r) return true
        }
        return false
    }

    private fun onRoadOrPlaza(x: Float, y: Float, m: Float): Boolean =
        hypot(x - CX, y - CY) < PLAZA_R + 14f + m || abs(x - CX) < ROAD_HALF + 10f + m || abs(y - CY) < ROAD_HALF + 10f + m

    private fun build() {
        val rnd = Random(7)
        val wall1 = Color.rgb(236, 220, 188)
        val wall2 = Color.rgb(226, 204, 168)
        val roofRed = Color.rgb(160, 64, 52)
        val roofBrown = Color.rgb(128, 84, 56)
        val roofBlue = Color.rgb(70, 104, 140)
        val roofGreen = Color.rgb(78, 120, 82)

        // --- yapılar
        house(1000f, 700f, 400f, 130f, wall1, roofRed, "Muhtar Evi")
        house(1650f, 900f, 240f, 100f, wall2, roofBrown, "Demirci")
        house(560f, 950f, 280f, 120f, wall1, roofBlue, "Han")
        house(620f, 1620f, 200f, 95f, wall2, roofGreen, "Şifacı")
        house(1790f, 1640f, 230f, 100f, wall1, roofBrown, "Depo")

        props.add(Prop(Prop.Kind.FORGE, 1590f, 935f))
        rects.add(floatArrayOf(1556f, 903f, 1664f - 40f, 935f))

        props.add(Prop(Prop.Kind.STALL, 1560f, 1540f, 200f, 0f, 0, 0, 1f, "Pazar"))
        rects.add(floatArrayOf(1460f, 1512f, 1660f, 1540f))
        props.add(Prop(Prop.Kind.BARREL, 1450f, 1590f)); circleCol(1450f, 1584f, 12f)
        props.add(Prop(Prop.Kind.BARREL, 1672f, 1596f)); circleCol(1672f, 1590f, 12f)
        props.add(Prop(Prop.Kind.CRATE, 1706f, 1566f)); rects.add(floatArrayOf(1691f, 1546f, 1721f, 1566f))
        props.add(Prop(Prop.Kind.CRATE, 1738f, 1580f)); rects.add(floatArrayOf(1723f, 1560f, 1753f, 1580f))

        props.add(Prop(Prop.Kind.WELL, CX - 540f, CY + 170f)); circleCol(CX - 540f, CY + 166f, 32f)
        props.add(Prop(Prop.Kind.BOARD, CX + 340f, CY - 130f, 0f, 0f, 0, 0, 1f, "Görevler"))
        rects.add(floatArrayOf(CX + 304f, CY - 146f, CX + 376f, CY - 130f))

        // lambalar
        for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
            val lx = CX + sx * 250f
            val ly = CY + sy * 250f
            props.add(Prop(Prop.Kind.LAMP, lx, ly)); circleCol(lx, ly, 8f)
        }
        props.add(Prop(Prop.Kind.LAMP, CX - 130f, 720f)); circleCol(CX - 130f, 720f, 8f)
        props.add(Prop(Prop.Kind.LAMP, CX + 130f, 720f)); circleCol(CX + 130f, 720f, 8f)

        // banklar
        props.add(Prop(Prop.Kind.BENCH, CX - 260f, CY + 120f))
        props.add(Prop(Prop.Kind.BENCH, CX + 260f, CY + 120f))
        props.add(Prop(Prop.Kind.BENCH, CX - 360f, CY - 40f))

        // köy kapısı (güney)
        props.add(Prop(Prop.Kind.GATE, CX, 2260f, 220f, 0f, 0, 0, 1f, "Köy Kapısı"))
        circleCol(CX - 110f, 2260f, 16f)
        circleCol(CX + 110f, 2260f, 16f)
        rects.add(floatArrayOf(CX - 110f, 2250f, CX + 110f, 2264f))

        // yol uçlarındaki kayalar (doğu ve batı)
        for (dy in intArrayOf(-70, 0, 70)) {
            props.add(Prop(Prop.Kind.ROCK, 170f, CY + dy + 30f, 0f, 0f, 0, 0, 1.1f)); circleCol(170f, CY + dy + 10f, 42f)
            props.add(Prop(Prop.Kind.ROCK, W - 170f, CY + dy + 30f, 0f, 0f, 0, 0, 1.1f)); circleCol(W - 170f, CY + dy + 10f, 42f)
        }

        // ulu ağaç
        props.add(Prop(Prop.Kind.BIGTREE, CX, TREE_BASE))
        circleCol(CX, TREE_BASE - 6f, 64f)

        // --- ağaçlar
        val placed = ArrayList<FloatArray>()
        fun addTree(x: Float, y: Float, s: Float) {
            val kind = if (rnd.nextFloat() < 0.55f) Prop.Kind.OAK else Prop.Kind.PINE
            props.add(Prop(kind, x, y, 0f, 0f, 0, 0, s))
            circleCol(x, y - 4f, 11f * s)
            placed.add(floatArrayOf(x, y))
        }
        // kenar çemberi
        var x = 80f
        while (x < W - 60f) {
            if (abs(x - CX) > 120f) {
                addTree(x + rnd.range(-20f, 20f), 70f + rnd.range(0f, 50f), rnd.range(1.1f, 1.6f))
                addTree(x + rnd.range(-20f, 20f), H - 60f - rnd.range(0f, 40f), rnd.range(1.1f, 1.6f))
            }
            x += 125f
        }
        var y = 200f
        while (y < H - 120f) {
            if (abs(y - CY) > 130f) {
                addTree(70f + rnd.range(0f, 50f), y + rnd.range(-20f, 20f), rnd.range(1.1f, 1.6f))
                addTree(W - 70f - rnd.range(0f, 50f), y + rnd.range(-20f, 20f), rnd.range(1.1f, 1.6f))
            }
            y += 125f
        }
        // iç ağaçlar
        var tries = 0
        var count = 0
        while (tries < 900 && count < 80) {
            tries++
            val tx = rnd.range(120f, W - 120f)
            val ty = rnd.range(160f, H - 140f)
            if (hypot(tx - CX, ty - CY) < 580f) continue
            if (abs(tx - CX) < 130f || abs(ty - CY) < 130f) continue
            if (blocked(tx, ty, 70f)) continue
            var near = false
            for (p in placed) if (hypot(tx - p[0], ty - p[1]) < 95f) { near = true; break }
            if (near) continue
            addTree(tx, ty, rnd.range(0.9f, 1.4f))
            count++
        }
        // çalılar
        for (i in 0 until 40) {
            val bx = rnd.range(100f, W - 100f)
            val by = rnd.range(150f, H - 150f)
            if (onRoadOrPlaza(bx, by, 30f) || blocked(bx, by, 40f)) continue
            props.add(Prop(Prop.Kind.BUSH, bx, by, 0f, 0f, 0, 0, rnd.range(0.8f, 1.3f)))
        }
        props.sortBy { it.y }

        // --- zemin süsleri
        for (i in 0 until 260) {
            val c = if (i % 2 == 0) Color.argb(46, 40, 110, 50) else Color.argb(40, 160, 205, 90)
            patches.add(Deco(rnd.range(0f, W), rnd.range(0f, H), rnd.range(60f, 170f), rnd.range(30f, 80f), c))
        }
        var n = 0
        while (n < 1400) {
            n++
            val fx = rnd.range(0f, W)
            val fy = rnd.range(0f, H)
            if (onRoadOrPlaza(fx, fy, 6f)) continue
            tufts.add(Deco(fx, fy, rnd.range(6f, 12f), 0f, 0))
        }
        val flowerColors = intArrayOf(
            Color.rgb(255, 220, 90), Color.rgb(255, 255, 255), Color.rgb(240, 130, 170),
            Color.rgb(255, 160, 70), Color.rgb(170, 150, 240)
        )
        n = 0
        while (n < 420) {
            n++
            val fx = rnd.range(0f, W)
            val fy = rnd.range(0f, H)
            if (onRoadOrPlaza(fx, fy, 6f)) continue
            flowers.add(Deco(fx, fy, 3.2f, 0f, flowerColors[rnd.nextInt(flowerColors.size)]))
        }
        val cobCols = intArrayOf(Color.argb(110, 120, 112, 98), Color.argb(90, 214, 204, 184), Color.argb(100, 150, 140, 120))
        n = 0
        while (n < 900) {
            n++
            val cx = rnd.range(CX - PLAZA_R, CX + PLAZA_R)
            val cy = rnd.range(CY - PLAZA_R, CY + PLAZA_R)
            if (hypot(cx - CX, cy - CY) > PLAZA_R - 8f) continue
            val a = rnd.range(8f, 16f)
            cobbles.add(Deco(cx, cy, a, a * 0.62f, cobCols[rnd.nextInt(3)]))
        }
        for (i in 0 until 700) {
            val horizontal = i % 2 == 0
            val px = if (horizontal) rnd.range(0f, W) else CX + rnd.range(-70f, 70f)
            val py = if (horizontal) CY + rnd.range(-70f, 70f) else rnd.range(0f, H)
            val a = rnd.range(3f, 7f)
            cobbles.add(Deco(px, py, a, a * 0.6f, Color.argb(110, 130, 100, 66)))
        }

        // --- ulu ağaç tacı
        val ccx = CX
        val ccy = TREE_BASE - 390f
        fun addCircle(layer: Int, count: Int, spreadX: Float, spreadY: Float, offX: Float, offY: Float, rMin: Float, rMax: Float) {
            for (i in 0 until count) {
                val a = rnd.range(0f, (2 * PI).toFloat())
                val d = sqrtF(rnd.nextFloat())
                canopy.add(floatArrayOf(ccx + offX + cos(a) * spreadX * d, ccy + offY + sin(a) * spreadY * d, rnd.range(rMin, rMax), layer.toFloat()))
            }
        }
        addCircle(0, 18, 170f, 125f, 0f, 0f, 70f, 98f)
        addCircle(1, 16, 150f, 108f, -10f, -14f, 54f, 80f)
        addCircle(2, 12, 120f, 84f, -26f, -30f, 36f, 56f)
        addCircle(3, 10, 100f, 70f, -40f, -44f, 15f, 26f)
        for (i in 0 until 30) {
            val a = rnd.range(0f, (2 * PI).toFloat())
            val d = sqrtF(rnd.nextFloat())
            blossoms.add(floatArrayOf(ccx + cos(a) * 215f * d, ccy + sin(a) * 150f * d, if (i % 3 == 0) 1f else 0f))
        }
        for (i in 0 until 22) flies.add(floatArrayOf(rnd.range(0f, 6.28f), rnd.range(120f, 300f), rnd.range(0.08f, 0.22f)))
        for (i in 0 until 10) leaves.add(floatArrayOf(rnd.range(-200f, 200f), rnd.nextFloat(), rnd.range(3.5f, 6f)))

        // --- NPC'ler
        npcs.add(
            Npc(
                "Eldrin", "Köy Muhtarı", NpcLook.ELDER, CX + 20f, CY + 200f,
                listOf(
                    "Huzur Ormanı'na ve köyümüze hoş geldin, yolcu.",
                    "Şu ulu ağacı görüyor musun? Beş kadim kahramanın gücü onda saklıdır. Gövdesindeki ışıklar bu yüzden parlıyor.",
                    "Köyü gez, herkesle tanış. Pazar, demirci, han ve depo hep meydanın çevresinde. Görev panosuna da göz at."
                ), 40f
            )
        )
        npcs.add(
            Npc(
                "Borga", "Demirci", NpcLook.BLACKSMITH, 1700f, 960f,
                listOf(
                    "Hrrm! Ateş sönmedi, çekiç hazır.",
                    "Silahını ve zırhını bana getir; eskisinden de sağlam çıkar.",
                    "Şimdilik sadece kömür kokusu var burada. Ama işler yakında hareketlenecek!"
                ), 30f
            )
        )
        npcs.add(
            Npc(
                "Selma", "Tüccar", NpcLook.MERCHANT, 1560f, 1585f,
                listOf(
                    "Pazara hoş geldin! Taze iksirler, nadir otlar, her şey burada.",
                    "Tezgâhım şu an biraz boş ama yakında envanterim dolacak. Sonra gel, pazarlık yaparız!"
                ), 30f
            )
        )
        npcs.add(
            Npc(
                "Liora", "Şifacı", NpcLook.HEALER, 730f, 1680f,
                listOf(
                    "Işık yolunu aydınlatsın, gezgin.",
                    "Yaralarını sararım, ruhunu dinlendiririm. Savaşa gitmeden önce yanıma uğra."
                ), 34f
            )
        )
        npcs.add(
            Npc(
                "Rüstem", "Hancı", NpcLook.INNKEEPER, 720f, 1000f,
                listOf(
                    "Han'a hoş geldin! Sıcak çorba, soğuk içecek, yumuşak yatak.",
                    "Maceracılar çoğaldıkça buranın neşesi de artacak, göreceksin."
                ), 30f
            )
        )
        npcs.add(
            Npc(
                "Kaan", "Köy Muhafızı", NpcLook.GUARD, CX - 160f, 2190f,
                listOf(
                    "Köy kapısı benim nöbetimde güvende.",
                    "Orman yolları şimdilik kapalı. Hazır olduğunda seni haberdar ederiz."
                ), 0f
            )
        )
        npcs.add(
            Npc(
                "Demir", "Köy Muhafızı", NpcLook.GUARD, CX + 160f, 2190f,
                listOf(
                    "Kapıdan öteye geçmek için henüz erken, yolcu.",
                    "Önce köyü tanı. Orman her zaman yerinde duracak."
                ), 0f
            )
        )
        npcs.add(
            Npc(
                "Pipo", "Köy Çocuğu", NpcLook.KID, CX - 250f, CY + 280f,
                listOf(
                    "Ağacın ışıkları geceleri daha da güzel parlıyor, biliyor muydun?",
                    "Yakalamaca oynayalım mı? Hadi, beni yakala!"
                ), 220f, 0.72f, 75f
            )
        )
    }

    private fun sqrtF(v: Float): Float = kotlin.math.sqrt(v)

    // ============================================================ mantık

    fun update(dt: Float, px: Float, py: Float) {
        val target = if (py < TREE_BASE - 20f && py > TREE_BASE - 560f && abs(px - CX) < 300f) 0.38f else 1f
        canopyAlpha += (target - canopyAlpha) * min(1f, dt * 6f)

        for (n in npcs) {
            if (n.talking) {
                n.moving = false
                n.facing = if (px >= n.x) 1 else -1
                continue
            }
            if (n.wander <= 0f) {
                n.moving = false
                continue
            }
            if (n.moving) {
                val dx = n.tx - n.x
                val dy = n.ty - n.y
                val d = hypot(dx, dy)
                val step = n.speed * dt
                if (d <= step) {
                    n.x = n.tx
                    n.y = n.ty
                    n.moving = false
                    n.wait = 2f + Random.nextFloat() * 4f
                } else {
                    n.x += dx / d * step
                    n.y += dy / d * step
                    if (abs(dx) > 1f) n.facing = if (dx > 0f) 1 else -1
                }
            } else {
                n.wait -= dt
                if (n.wait <= 0f) {
                    var found = false
                    for (k in 0 until 8) {
                        val a = Random.nextFloat() * 6.2831f
                        val dist = Random.nextFloat() * n.wander
                        val tx = n.homeX + cos(a) * dist
                        val ty = n.homeY + sin(a) * dist
                        if (!blocked(tx, ty, 26f) && tx > 90f && tx < W - 90f && ty > 110f && ty < H - 130f) {
                            n.tx = tx
                            n.ty = ty
                            n.moving = true
                            found = true
                            break
                        }
                    }
                    if (!found) n.wait = 2f
                }
            }
        }
    }

    /** pos[0], pos[1] konumunu engellerden dışarı iter. */
    fun resolve(pos: FloatArray, r: Float) {
        pos[0] = pos[0].coerceIn(70f, W - 70f)
        // köy kapısının (y=2260) ötesine geçilemez
        pos[1] = pos[1].coerceIn(110f, 2236f)
        for (iter in 0 until 2) {
            for (c in circles) {
                var dx = pos[0] - c[0]
                var dy = pos[1] - c[1]
                var d = hypot(dx, dy)
                val mind = c[2] + r
                if (d < mind) {
                    if (d < 0.001f) {
                        dx = 0f; dy = 1f; d = 1f
                    }
                    pos[0] += dx / d * (mind - d)
                    pos[1] += dy / d * (mind - d)
                }
            }
            for (rc in rects) {
                val nx = pos[0].coerceIn(rc[0], rc[2])
                val ny = pos[1].coerceIn(rc[1], rc[3])
                val dx = pos[0] - nx
                val dy = pos[1] - ny
                val d = hypot(dx, dy)
                if (d < r) {
                    if (d < 0.001f) {
                        val l = pos[0] - rc[0]
                        val rr = rc[2] - pos[0]
                        val u = pos[1] - rc[1]
                        val b = rc[3] - pos[1]
                        val m = minOf(minOf(l, rr), minOf(u, b))
                        when (m) {
                            l -> pos[0] = rc[0] - r
                            rr -> pos[0] = rc[2] + r
                            u -> pos[1] = rc[1] - r
                            else -> pos[1] = rc[3] + r
                        }
                    } else {
                        pos[0] += dx / d * (r - d)
                        pos[1] += dy / d * (r - d)
                    }
                }
            }
        }
    }

    fun npcAt(wx: Float, wy: Float): Npc? {
        var best: Npc? = null
        var bestD = 1e9f
        for (n in npcs) {
            val s = 1.1f * n.scale
            if (abs(wx - n.x) < 32f * s && wy > n.y - 80f * s && wy < n.y + 16f * s) {
                val d = abs(wx - n.x) + abs(wy - n.y)
                if (d < bestD) {
                    bestD = d
                    best = n
                }
            }
        }
        return best
    }

    fun nearestNpc(px: Float, py: Float, range: Float): Npc? {
        var best: Npc? = null
        var bestD = range
        for (n in npcs) {
            val d = hypot(px - n.x, py - n.y)
            if (d < bestD) {
                bestD = d
                best = n
            }
        }
        return best
    }

    // ============================================================ çizim

    private class Dyn(val y: Float, val npc: Npc?)

    fun draw(
        c: Canvas, camX: Float, camY: Float, vw: Float, vh: Float, t: Float,
        playerY: Float, drawPlayer: (Canvas) -> Unit
    ) {
        drawGround(c, camX, camY, vw, vh)

        val dyn = ArrayList<Dyn>(npcs.size + 1)
        for (n in npcs) dyn.add(Dyn(n.y, n))
        dyn.add(Dyn(playerY, null))
        dyn.sortBy { it.y }

        var di = 0
        for (pr in props) {
            while (di < dyn.size && dyn[di].y <= pr.y) {
                drawDyn(c, dyn[di], t, camX, camY, vw, vh, drawPlayer)
                di++
            }
            val ext = if (pr.kind == Prop.Kind.BIGTREE) 520f else 160f + pr.w
            val top = if (pr.kind == Prop.Kind.BIGTREE) 760f else 340f
            if (pr.x + ext < camX || pr.x - 300f > camX + vw || pr.y + 60f < camY || pr.y - top > camY + vh) continue
            drawProp(c, pr, t)
        }
        while (di < dyn.size) {
            drawDyn(c, dyn[di], t, camX, camY, vw, vh, drawPlayer)
            di++
        }
    }

    private fun drawDyn(c: Canvas, d: Dyn, t: Float, camX: Float, camY: Float, vw: Float, vh: Float, drawPlayer: (Canvas) -> Unit) {
        val n = d.npc
        if (n == null) {
            drawPlayer(c)
            return
        }
        if (n.x < camX - 100f || n.x > camX + vw + 100f || n.y < camY - 60f || n.y > camY + vh + 200f) return
        val s = 1.1f * n.scale
        CharacterRenderer.drawNpc(c, n.look, n.x, n.y, s, n.facing, t + n.phase, n.moving)
        nameTag(c, n.name, n.x, n.y - 72f * s - 6f, 13f, Color.rgb(255, 236, 160))
    }

    fun nameTag(c: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
        gp.textSize = size
        gp.textAlign = Paint.Align.CENTER
        gp.typeface = Typeface.DEFAULT_BOLD
        gp.style = Paint.Style.STROKE
        gp.strokeWidth = size * 0.28f
        gp.strokeJoin = Paint.Join.ROUND
        gp.color = Color.argb(200, 20, 16, 12)
        c.drawText(text, x, y, gp)
        gp.style = Paint.Style.FILL
        gp.color = color
        c.drawText(text, x, y, gp)
    }

    private fun fillR(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, rad: Float = 0f) {
        gp.style = Paint.Style.FILL
        gp.color = color
        if (rad > 0f) c.drawRoundRect(l, t, r, b, rad, rad, gp) else c.drawRect(l, t, r, b, gp)
    }

    private fun fillO(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        gp.style = Paint.Style.FILL
        gp.color = color
        rf.set(l, t, r, b)
        c.drawOval(rf, gp)
    }

    private fun disc(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        gp.style = Paint.Style.FILL
        gp.color = color
        c.drawCircle(x, y, r, gp)
    }

    private fun ln(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, color: Int) {
        gp.style = Paint.Style.STROKE
        gp.strokeWidth = w
        gp.strokeCap = Paint.Cap.ROUND
        gp.color = color
        c.drawLine(x1, y1, x2, y2, gp)
        gp.style = Paint.Style.FILL
        gp.strokeCap = Paint.Cap.BUTT
    }

    private fun tri(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        gp.style = Paint.Style.FILL
        gp.color = color
        c.drawPath(path, gp)
    }

    private fun shadow(c: Canvas, x: Float, y: Float, rx: Float, ry: Float) {
        fillO(c, x - rx, y - ry, x + rx, y + ry, Color.argb(55, 0, 0, 0))
    }

    private fun drawGround(c: Canvas, cx: Float, cy: Float, vw: Float, vh: Float) {
        c.drawColor(Color.rgb(104, 168, 82))
        val l = cx - 130f
        val r = cx + vw + 130f
        val top = cy - 130f
        val bot = cy + vh + 130f

        gp.style = Paint.Style.FILL
        for (d in patches) {
            if (d.x < l - 170f || d.x > r + 170f || d.y < top - 80f || d.y > bot + 80f) continue
            gp.color = d.color
            rf.set(d.x - d.a, d.y - d.b, d.x + d.a, d.y + d.b)
            c.drawOval(rf, gp)
        }

        // yollar
        fillR(c, CX - ROAD_HALF - 10f, 0f, CX + ROAD_HALF + 10f, H, Color.rgb(150, 116, 76))
        fillR(c, 0f, CY - ROAD_HALF - 10f, W, CY + ROAD_HALF + 10f, Color.rgb(150, 116, 76))
        fillR(c, CX - ROAD_HALF, 0f, CX + ROAD_HALF, H, Color.rgb(188, 154, 108))
        fillR(c, 0f, CY - ROAD_HALF, W, CY + ROAD_HALF, Color.rgb(188, 154, 108))

        // meydan
        disc(c, CX, CY, PLAZA_R + 16f, Color.rgb(128, 116, 98))
        disc(c, CX, CY, PLAZA_R, Color.rgb(182, 172, 150))

        for (d in cobbles) {
            if (d.x < l || d.x > r || d.y < top || d.y > bot) continue
            gp.color = d.color
            rf.set(d.x - d.a, d.y - d.b, d.x + d.a, d.y + d.b)
            c.drawOval(rf, gp)
        }

        // meydan deseni: beş güç
        gp.style = Paint.Style.STROKE
        gp.strokeWidth = 6f
        gp.color = Color.argb(120, 150, 136, 112)
        c.drawCircle(CX, CY, 330f, gp)
        c.drawCircle(CX, CY, 262f, gp)
        gp.strokeWidth = 3f
        gp.color = Color.argb(90, 120, 106, 86)
        for (i in 0 until 5) {
            val a1 = (-90.0 + i * 72.0) * PI / 180.0
            val a2 = (-90.0 + ((i + 2) % 5) * 72.0) * PI / 180.0
            c.drawLine(
                CX + cos(a1).toFloat() * 262f, CY + sin(a1).toFloat() * 262f,
                CX + cos(a2).toFloat() * 262f, CY + sin(a2).toFloat() * 262f, gp
            )
        }
        gp.style = Paint.Style.FILL
        for (i in 0 until 5) {
            val a = (-90.0 + i * 72.0) * PI / 180.0
            val px = CX + cos(a).toFloat() * 262f
            val py = CY + sin(a).toFloat() * 262f
            disc(c, px, py, 20f, Color.argb(110, 90, 80, 66))
            disc(c, px, py, 13f, powerColors[i])
            disc(c, px - 3f, py - 3f, 4f, Color.argb(150, 255, 255, 255))
        }

        // çimen ve çiçekler
        gp.style = Paint.Style.STROKE
        gp.strokeWidth = 2f
        gp.color = Color.rgb(70, 130, 58)
        for (d in tufts) {
            if (d.x < l || d.x > r || d.y < top || d.y > bot) continue
            val h = d.a
            c.drawLine(d.x, d.y, d.x - 4f, d.y - h, gp)
            c.drawLine(d.x, d.y, d.x, d.y - h * 1.25f, gp)
            c.drawLine(d.x, d.y, d.x + 4f, d.y - h, gp)
        }
        gp.style = Paint.Style.FILL
        for (d in flowers) {
            if (d.x < l || d.x > r || d.y < top || d.y > bot) continue
            disc(c, d.x, d.y, d.a, d.color)
            disc(c, d.x, d.y, 1.2f, Color.rgb(250, 200, 60))
        }

        // ulu ağaç gölgesi ve çiçeklik
        fillO(c, CX - 330f + 40f, TREE_BASE - 110f + 10f, CX + 330f + 40f, TREE_BASE + 130f + 10f, Color.argb(60, 0, 0, 0))
        fillO(c, CX - 185f, TREE_BASE - 105f, CX + 185f, TREE_BASE + 105f, Color.rgb(92, 146, 70))
        for (i in 0 until 28) {
            val a = 2.0 * PI * i / 28.0
            val sx = CX + cos(a).toFloat() * 172f
            val sy = TREE_BASE + sin(a).toFloat() * 98f
            fillO(c, sx - 12f, sy - 8f, sx + 12f, sy + 8f, Color.rgb(146, 140, 130))
            fillO(c, sx - 9f, sy - 7f, sx + 9f, sy + 4f, Color.rgb(176, 170, 158))
            val a2 = 2.0 * PI * (i + 0.5) / 28.0
            val fx = CX + cos(a2).toFloat() * 152f
            val fy = TREE_BASE + sin(a2).toFloat() * 86f
            disc(c, fx, fy, 4f, powerColors[i % 5])
            disc(c, fx, fy, 1.5f, Color.rgb(255, 245, 200))
        }
    }

    private fun drawProp(c: Canvas, p: Prop, t: Float) {
        when (p.kind) {
            Prop.Kind.HOUSE -> drawHouse(c, p, t)
            Prop.Kind.PINE -> drawPine(c, p)
            Prop.Kind.OAK -> drawOak(c, p)
            Prop.Kind.LAMP -> drawLamp(c, p, t)
            Prop.Kind.BARREL -> drawBarrel(c, p)
            Prop.Kind.CRATE -> drawCrate(c, p)
            Prop.Kind.STALL -> drawStall(c, p)
            Prop.Kind.WELL -> drawWell(c, p)
            Prop.Kind.FORGE -> drawForge(c, p, t)
            Prop.Kind.GATE -> drawGate(c, p)
            Prop.Kind.ROCK -> drawRock(c, p)
            Prop.Kind.BENCH -> drawBench(c, p)
            Prop.Kind.BOARD -> drawBoard(c, p)
            Prop.Kind.BUSH -> drawBush(c, p)
            Prop.Kind.BIGTREE -> drawBigTree(c, t)
        }
    }

    private fun drawHouse(c: Canvas, p: Prop, t: Float) {
        val x = p.x
        val b = p.y
        val w = p.w
        val h = p.h
        shadow(c, x + w * 0.5f + 20f, b + 4f, w * 0.55f, 11f)

        // baca ve duman
        val chx = x + w * 0.72f
        fillR(c, chx, b - h - 128f, chx + 26f, b - h - 40f, Color.rgb(128, 120, 112))
        fillR(c, chx - 3f, b - h - 134f, chx + 29f, b - h - 124f, Color.rgb(100, 94, 88))
        for (i in 0 until 3) {
            val k = (t * 0.3f + i / 3f) % 1f
            disc(c, chx + 13f + sin(k * 6f + i) * 7f, b - h - 140f - k * 70f, 8f + k * 12f, Color.argb((110 * (1f - k)).toInt(), 225, 225, 225))
        }

        fillR(c, x - 4f, b - 16f, x + w + 4f, b, Color.rgb(122, 118, 112))
        fillR(c, x, b - h, x + w, b - 16f, p.c1)

        // ahşap kirişler
        val beams = (w / 90f).toInt().coerceAtLeast(2)
        for (i in 0..beams) {
            val bx = x + w * i / beams
            fillR(c, bx - 3f, b - h, bx + 3f, b - 16f, Color.rgb(112, 78, 50))
        }
        fillR(c, x, b - h + 30f, x + w, b - h + 36f, Color.rgb(112, 78, 50))

        // kapı
        val dx = x + w / 2f - 20f
        fillR(c, dx - 4f, b - 86f, dx + 44f, b - 16f, Color.rgb(112, 78, 50), 12f)
        fillR(c, dx, b - 82f, dx + 40f, b - 16f, Color.rgb(92, 60, 38), 10f)
        ln(c, dx + 20f, b - 80f, dx + 20f, b - 16f, 1.6f, Color.rgb(60, 38, 24))
        disc(c, dx + 32f, b - 48f, 2.4f, Color.rgb(236, 196, 90))

        // pencereler (sıcak ışık)
        val winX = if (w >= 300f) floatArrayOf(w * 0.10f, w * 0.28f, w * 0.64f, w * 0.82f) else floatArrayOf(w * 0.14f, w * 0.86f - 38f)
        for (wx in winX) {
            val wl = x + wx
            val wt = b - h + 46f
            disc(c, wl + 19f, wt + 19f, 34f, Color.argb(34, 255, 214, 120))
            fillR(c, wl - 3f, wt - 3f, wl + 41f, wt + 41f, Color.rgb(112, 78, 50), 3f)
            fillR(c, wl, wt, wl + 38f, wt + 38f, Color.rgb(255, 226, 150), 2f)
            fillR(c, wl + 17f, wt, wl + 21f, wt + 38f, Color.rgb(112, 78, 50))
            fillR(c, wl, wt + 17f, wl + 38f, wt + 21f, Color.rgb(112, 78, 50))
        }

        // çatı
        path.reset()
        path.moveTo(x - 26f, b - h + 4f)
        path.lineTo(x + w + 26f, b - h + 4f)
        path.lineTo(x + w - 34f, b - h - 96f)
        path.lineTo(x + 34f, b - h - 96f)
        path.close()
        gp.style = Paint.Style.FILL
        gp.color = p.c2
        c.drawPath(path, gp)
        for (i in 1..4) {
            val k = i * 20f
            val yy = b - h + 4f - k
            ln(c, x - 26f + 0.6f * k, yy, x + w + 26f - 0.6f * k, yy, 1.6f, Color.argb(70, 0, 0, 0))
        }
        fillR(c, x + 30f, b - h - 102f, x + w - 30f, b - h - 94f, Color.argb(110, 0, 0, 0), 3f)
        fillR(c, x - 26f, b - h + 4f, x + w + 26f, b - h + 12f, Color.argb(70, 0, 0, 0))

        // tabela
        if (p.label.isNotEmpty()) {
            fillR(c, x + w / 2f - 52f, b - h - 28f, x + w / 2f + 52f, b - h - 4f, Color.rgb(132, 90, 54), 5f)
            fillR(c, x + w / 2f - 49f, b - h - 25f, x + w / 2f + 49f, b - h - 7f, Color.rgb(196, 152, 96), 4f)
            nameTag(c, p.label, x + w / 2f, b - h - 11f, 14f, Color.rgb(255, 250, 235))
        }
    }

    private fun drawPine(c: Canvas, p: Prop) {
        val s = p.s
        val x = p.x
        val y = p.y
        shadow(c, x + 6f * s, y + 2f, 30f * s, 8f * s)
        fillR(c, x - 6f * s, y - 34f * s, x + 6f * s, y, Color.rgb(98, 66, 42))
        val hw = floatArrayOf(40f, 32f, 24f)
        val bt = floatArrayOf(24f, 58f, 88f)
        val tp = floatArrayOf(86f, 112f, 138f)
        for (i in 0 until 3) {
            tri(c, x, y - tp[i] * s, x - hw[i] * s, y - bt[i] * s, x + hw[i] * s, y - bt[i] * s, Color.rgb(38, 100, 62))
            tri(c, x, y - tp[i] * s, x - hw[i] * s, y - bt[i] * s, x, y - bt[i] * s, Color.rgb(58, 132, 78))
        }
    }

    private fun drawOak(c: Canvas, p: Prop) {
        val s = p.s
        val x = p.x
        val y = p.y
        shadow(c, x + 8f * s, y + 2f, 34f * s, 9f * s)
        fillR(c, x - 7f * s, y - 46f * s, x + 7f * s, y, Color.rgb(98, 66, 42))
        disc(c, x, y - 74f * s, 40f * s, Color.rgb(50, 118, 62))
        disc(c, x - 28f * s, y - 54f * s, 30f * s, Color.rgb(50, 118, 62))
        disc(c, x + 28f * s, y - 54f * s, 30f * s, Color.rgb(50, 118, 62))
        disc(c, x - 6f * s, y - 82f * s, 32f * s, Color.rgb(72, 150, 78))
        disc(c, x - 18f * s, y - 90f * s, 17f * s, Color.rgb(112, 186, 98))
    }

    private fun drawBush(c: Canvas, p: Prop) {
        val s = p.s
        shadow(c, p.x + 4f, p.y + 2f, 22f * s, 6f * s)
        disc(c, p.x, p.y - 14f * s, 16f * s, Color.rgb(54, 128, 64))
        disc(c, p.x - 14f * s, p.y - 8f * s, 12f * s, Color.rgb(64, 142, 72))
        disc(c, p.x + 14f * s, p.y - 8f * s, 12f * s, Color.rgb(64, 142, 72))
        disc(c, p.x - 4f * s, p.y - 20f * s, 6f * s, Color.rgb(250, 190, 90))
        disc(c, p.x + 9f * s, p.y - 12f * s, 4.5f * s, Color.rgb(240, 130, 170))
    }

    private fun drawLamp(c: Canvas, p: Prop, t: Float) {
        val x = p.x
        val y = p.y
        shadow(c, x + 4f, y + 2f, 14f, 4f)
        disc(c, x, y - 4f, 9f, Color.rgb(110, 106, 100))
        ln(c, x, y - 4f, x, y - 86f, 5f, Color.rgb(54, 46, 42))
        val g = 46f + 8f * sin(t * 2.2f + x)
        disc(c, x, y - 100f, g, Color.argb(40, 255, 214, 120))
        disc(c, x, y - 100f, g * 0.55f, Color.argb(50, 255, 214, 120))
        fillR(c, x - 8f, y - 112f, x + 8f, y - 90f, Color.rgb(255, 214, 120), 3f)
        fillR(c, x - 10f, y - 116f, x + 10f, y - 110f, Color.rgb(54, 46, 42))
        fillR(c, x - 10f, y - 92f, x + 10f, y - 88f, Color.rgb(54, 46, 42))
    }

    private fun drawBarrel(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        shadow(c, x + 5f, y + 2f, 16f, 5f)
        fillR(c, x - 12f, y - 30f, x + 12f, y, Color.rgb(146, 100, 58), 6f)
        fillR(c, x - 12f, y - 23f, x + 12f, y - 20f, Color.rgb(70, 52, 38))
        fillR(c, x - 12f, y - 10f, x + 12f, y - 7f, Color.rgb(70, 52, 38))
        fillO(c, x - 12f, y - 34f, x + 12f, y - 26f, Color.rgb(178, 132, 82))
    }

    private fun drawCrate(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        shadow(c, x + 4f, y + 2f, 20f, 5f)
        fillR(c, x - 15f, y - 26f, x + 15f, y, Color.rgb(172, 128, 78), 2f)
        ln(c, x - 13f, y - 24f, x + 13f, y - 2f, 2.4f, Color.rgb(112, 80, 50))
        ln(c, x + 13f, y - 24f, x - 13f, y - 2f, 2.4f, Color.rgb(112, 80, 50))
    }

    private fun drawStall(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        val w = p.w
        val l = x - w / 2f
        val r = x + w / 2f
        shadow(c, x + 10f, y + 4f, w * 0.55f, 10f)
        fillR(c, l + 4f, y - 132f, l + 10f, y, Color.rgb(112, 78, 50))
        fillR(c, r - 10f, y - 132f, r - 4f, y, Color.rgb(112, 78, 50))
        fillR(c, l, y - 44f, r, y, Color.rgb(150, 104, 60))
        fillR(c, l - 4f, y - 50f, r + 4f, y - 42f, Color.rgb(182, 134, 82))
        val cols = intArrayOf(Color.rgb(220, 70, 70), Color.rgb(70, 130, 230), Color.rgb(90, 200, 110), Color.rgb(240, 190, 70))
        var gx = l + 22f
        var i = 0
        while (gx < r - 16f) {
            if (i % 3 == 2) {
                disc(c, gx, y - 58f, 8f, Color.rgb(230, 130, 50))
                disc(c, gx - 2f, y - 60f, 2.4f, Color.argb(150, 255, 255, 255))
            } else {
                fillR(c, gx - 5f, y - 66f, gx + 5f, y - 50f, cols[i % 4], 4f)
                fillR(c, gx - 2f, y - 72f, gx + 2f, y - 66f, Color.rgb(200, 200, 210))
            }
            gx += 28f
            i++
        }
        val n = 8
        val sw = (w + 24f) / n
        for (k in 0 until n) {
            val a = l - 12f + k * sw
            val bb = a + sw
            path.reset()
            path.moveTo(a, y - 134f)
            path.lineTo(bb, y - 134f)
            path.lineTo(bb, y - 100f)
            path.quadTo((a + bb) / 2f, y - 86f, a, y - 100f)
            path.close()
            gp.style = Paint.Style.FILL
            gp.color = if (k % 2 == 0) Color.rgb(204, 66, 58) else Color.rgb(246, 234, 206)
            c.drawPath(path, gp)
        }
        fillR(c, l - 12f, y - 140f, r + 12f, y - 132f, Color.rgb(120, 80, 52), 3f)
        if (p.label.isNotEmpty()) nameTag(c, p.label, x, y - 146f, 14f, Color.rgb(255, 236, 160))
    }

    private fun drawWell(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        shadow(c, x + 10f, y + 4f, 44f, 9f)
        fillO(c, x - 34f, y - 26f, x + 34f, y + 4f, Color.rgb(120, 118, 114))
        fillO(c, x - 34f, y - 30f, x + 34f, y - 6f, Color.rgb(160, 158, 152))
        fillO(c, x - 24f, y - 26f, x + 24f, y - 10f, Color.rgb(52, 92, 124))
        fillR(c, x - 32f, y - 74f, x - 26f, y - 12f, Color.rgb(112, 78, 50))
        fillR(c, x + 26f, y - 74f, x + 32f, y - 12f, Color.rgb(112, 78, 50))
        tri(c, x - 46f, y - 72f, x + 46f, y - 72f, x, y - 104f, Color.rgb(156, 70, 52))
        ln(c, x, y - 72f, x, y - 48f, 1.6f, Color.rgb(70, 52, 38))
        fillR(c, x - 6f, y - 50f, x + 6f, y - 38f, Color.rgb(146, 100, 58), 2f)
    }

    private fun drawForge(c: Canvas, p: Prop, t: Float) {
        val x = p.x
        val y = p.y
        shadow(c, x + 20f, y + 3f, 70f, 8f)
        fillR(c, x - 12f, y - 78f, x + 12f, y - 40f, Color.rgb(112, 106, 100))
        for (i in 0 until 3) {
            val k = (t * 0.35f + i / 3f) % 1f
            disc(c, x + sin(k * 5f + i) * 6f, y - 84f - k * 60f, 7f + k * 11f, Color.argb((100 * (1f - k)).toInt(), 90, 90, 90))
        }
        fillR(c, x - 32f, y - 42f, x + 32f, y, Color.rgb(122, 118, 112), 3f)
        fillR(c, x - 20f, y - 32f, x + 20f, y - 4f, Color.rgb(34, 26, 24), 4f)
        val fl = 0.7f + 0.3f * sin(t * 11f)
        disc(c, x, y - 12f, 16f, Color.argb((120 * fl).toInt(), 255, 140, 40))
        tri(c, x - 10f, y - 6f, x, y - 26f - 4f * fl, x + 10f, y - 6f, Color.rgb(255, 150, 50))
        tri(c, x - 5f, y - 6f, x, y - 18f, x + 5f, y - 6f, Color.rgb(255, 220, 110))
        fillR(c, x + 40f, y - 22f, x + 68f, y - 12f, Color.rgb(54, 56, 66), 2f)
        fillR(c, x + 48f, y - 12f, x + 60f, y - 4f, Color.rgb(48, 50, 58))
        fillR(c, x + 44f, y - 4f, x + 64f, y, Color.rgb(40, 42, 50))
    }

    private fun drawGate(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        val l = x - p.w / 2f
        val r = x + p.w / 2f
        shadow(c, x, y + 4f, p.w * 0.6f, 9f)
        fillR(c, l - 14f, y - 146f, l + 14f, y, Color.rgb(112, 78, 50), 3f)
        fillR(c, r - 14f, y - 146f, r + 14f, y, Color.rgb(112, 78, 50), 3f)
        fillR(c, l - 18f, y - 14f, l + 18f, y, Color.rgb(122, 118, 112), 3f)
        fillR(c, r - 18f, y - 14f, r + 18f, y, Color.rgb(122, 118, 112), 3f)
        fillR(c, l - 22f, y - 164f, r + 22f, y - 140f, Color.rgb(132, 90, 54), 4f)
        fillR(c, l - 22f, y - 164f, r + 22f, y - 158f, Color.rgb(160, 114, 70), 3f)
        for (bx in floatArrayOf(l + 22f, r - 40f)) {
            path.reset()
            path.moveTo(bx, y - 140f)
            path.lineTo(bx + 18f, y - 140f)
            path.lineTo(bx + 18f, y - 90f)
            path.lineTo(bx + 9f, y - 100f)
            path.lineTo(bx, y - 90f)
            path.close()
            gp.style = Paint.Style.FILL
            gp.color = Color.rgb(186, 52, 52)
            c.drawPath(path, gp)
            disc(c, bx + 9f, y - 122f, 5f, Color.rgb(236, 196, 90))
        }
        if (p.label.isNotEmpty()) nameTag(c, p.label, x, y - 146f, 15f, Color.rgb(255, 244, 210))
    }

    private fun drawRock(c: Canvas, p: Prop) {
        val s = p.s
        val x = p.x
        val y = p.y
        shadow(c, x + 8f, y + 2f, 52f * s, 9f * s)
        fillO(c, x - 48f * s, y - 52f * s, x + 48f * s, y + 2f, Color.rgb(116, 120, 126))
        fillO(c, x - 40f * s, y - 52f * s, x + 22f * s, y - 20f * s, Color.rgb(150, 154, 160))
        fillO(c, x - 26f * s, y - 50f * s, x - 2f * s, y - 36f * s, Color.rgb(184, 188, 194))
    }

    private fun drawBench(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        shadow(c, x + 4f, y + 2f, 36f, 5f)
        fillR(c, x - 28f, y - 14f, x - 23f, y, Color.rgb(98, 66, 42))
        fillR(c, x + 23f, y - 14f, x + 28f, y, Color.rgb(98, 66, 42))
        fillR(c, x - 32f, y - 38f, x + 32f, y - 30f, Color.rgb(150, 104, 60), 2f)
        fillR(c, x - 32f, y - 24f, x + 32f, y - 14f, Color.rgb(172, 124, 76), 2f)
    }

    private fun drawBoard(c: Canvas, p: Prop) {
        val x = p.x
        val y = p.y
        shadow(c, x + 6f, y + 2f, 50f, 7f)
        fillR(c, x - 34f, y - 96f, x - 28f, y, Color.rgb(98, 66, 42))
        fillR(c, x + 28f, y - 96f, x + 34f, y, Color.rgb(98, 66, 42))
        tri(c, x - 46f, y - 94f, x + 46f, y - 94f, x, y - 118f, Color.rgb(140, 70, 50))
        fillR(c, x - 40f, y - 92f, x + 40f, y - 36f, Color.rgb(112, 78, 50), 3f)
        fillR(c, x - 36f, y - 88f, x + 36f, y - 40f, Color.rgb(176, 130, 80), 2f)
        fillR(c, x - 30f, y - 82f, x - 8f, y - 60f, Color.rgb(246, 238, 214))
        fillR(c, x - 2f, y - 84f, x + 28f, y - 66f, Color.rgb(240, 230, 200))
        fillR(c, x - 24f, y - 56f, x + 4f, y - 44f, Color.rgb(246, 238, 214))
        fillR(c, x + 10f, y - 62f, x + 30f, y - 44f, Color.rgb(236, 226, 196))
        disc(c, x - 19f, y - 81f, 2f, Color.rgb(200, 50, 50))
        disc(c, x + 13f, y - 83f, 2f, Color.rgb(200, 50, 50))
        if (p.label.isNotEmpty()) nameTag(c, p.label, x, y - 124f, 14f, Color.rgb(255, 236, 160))
    }

    private fun drawBigTree(c: Canvas, t: Float) {
        val bx = CX
        val by = TREE_BASE
        val pulse = 0.65f + 0.35f * sin(t * 2.4f)

        // gövde ve kökler
        path.reset()
        path.moveTo(bx - 150f, by + 16f)
        path.quadTo(bx - 80f, by - 6f, bx - 66f, by - 90f)
        path.quadTo(bx - 58f, by - 200f, bx - 50f, by - 300f)
        path.lineTo(bx + 50f, by - 300f)
        path.quadTo(bx + 58f, by - 200f, bx + 66f, by - 90f)
        path.quadTo(bx + 80f, by - 6f, bx + 150f, by + 16f)
        path.quadTo(bx + 60f, by + 30f, bx, by + 24f)
        path.quadTo(bx - 60f, by + 30f, bx - 150f, by + 16f)
        path.close()
        gp.style = Paint.Style.FILL
        gp.color = Color.rgb(104, 68, 42)
        c.drawPath(path, gp)

        path.reset()
        path.moveTo(bx + 22f, by - 300f)
        path.lineTo(bx + 50f, by - 300f)
        path.quadTo(bx + 58f, by - 200f, bx + 66f, by - 90f)
        path.quadTo(bx + 80f, by - 6f, bx + 150f, by + 16f)
        path.quadTo(bx + 60f, by + 30f, bx + 40f, by + 26f)
        path.quadTo(bx + 36f, by - 100f, bx + 22f, by - 300f)
        path.close()
        gp.color = Color.argb(64, 30, 10, 0)
        c.drawPath(path, gp)

        path.reset()
        path.moveTo(bx - 62f, by - 100f)
        path.quadTo(bx - 56f, by - 200f, bx - 48f, by - 300f)
        path.lineTo(bx - 28f, by - 300f)
        path.quadTo(bx - 40f, by - 200f, bx - 44f, by - 90f)
        path.close()
        gp.color = Color.argb(46, 255, 224, 170)
        c.drawPath(path, gp)

        gp.style = Paint.Style.STROKE
        gp.strokeWidth = 2.6f
        gp.strokeCap = Paint.Cap.ROUND
        gp.color = Color.argb(80, 40, 20, 8)
        for (i in 0..6) {
            val x0 = bx - 48f + i * 16f
            val dir = if (i % 2 == 0) 6f else -6f
            path.reset()
            path.moveTo(x0, by - 290f)
            path.quadTo(x0 + dir, by - 160f, x0 + dir * 0.4f, by - 12f)
            c.drawPath(path, gp)
        }
        gp.style = Paint.Style.FILL
        gp.strokeCap = Paint.Cap.BUTT

        // beş kadim güç: gövdedeki parlayan rünler
        for (i in 0 until 5) {
            val rx = bx - 34f + i * 17f
            val ry = by - 150f - (2 - abs(i - 2)) * 14f
            val ph = 0.6f + 0.4f * sin(t * 2.4f + i * 1.3f)
            disc(c, rx, ry, 13f, Color.argb((70 * ph).toInt(), Color.red(powerColors[i]), Color.green(powerColors[i]), Color.blue(powerColors[i])))
            disc(c, rx, ry, 4.4f, powerColors[i])
            disc(c, rx - 1.2f, ry - 1.2f, 1.5f, Color.argb(200, 255, 255, 255))
        }

        // dallar
        ln(c, bx - 30f, by - 280f, bx - 170f, by - 340f, 30f, Color.rgb(104, 68, 42))
        ln(c, bx + 30f, by - 280f, bx + 170f, by - 345f, 30f, Color.rgb(104, 68, 42))
        ln(c, bx - 6f, by - 290f, bx - 64f, by - 420f, 28f, Color.rgb(104, 68, 42))
        ln(c, bx + 6f, by - 290f, bx + 72f, by - 430f, 28f, Color.rgb(104, 68, 42))

        // taç (oyuncu altındaysa yarı saydam)
        val fade = canopyAlpha < 0.98f
        if (fade) c.saveLayerAlpha(bx - 440f, by - 740f, bx + 440f, by - 90f, (canopyAlpha * 255f).toInt())

        val layerColors = intArrayOf(
            Color.rgb(44, 108, 58), Color.rgb(62, 138, 70), Color.rgb(94, 170, 86), Color.rgb(146, 206, 112)
        )
        for (layer in 0 until 4) {
            gp.color = layerColors[layer]
            gp.style = Paint.Style.FILL
            for (cc in canopy) {
                if (cc[3].toInt() != layer) continue
                c.drawCircle(cc[0], cc[1], cc[2], gp)
            }
        }
        for (b in blossoms) {
            val warm = b[2] > 0.5f
            disc(c, b[0], b[1], 3.4f, if (warm) Color.rgb(255, 176, 120) else Color.rgb(255, 226, 140))
        }

        // fenerler
        val ccy = by - 390f
        for (i in 0 until 7) {
            val a = (0.14 + i * 0.12) * PI
            val px = bx + cos(a).toFloat() * 195f
            val py = ccy + sin(a).toFloat() * 150f
            val lx = px + sin(t * 1.4f + i) * 5f
            val ly = py + 44f
            ln(c, px, py - 8f, lx, ly - 10f, 1.6f, Color.rgb(80, 60, 40))
            disc(c, lx, ly, 28f, Color.argb((46 + 14 * pulse).toInt(), 255, 214, 120))
            fillR(c, lx - 6f, ly - 10f, lx + 6f, ly + 7f, Color.rgb(255, 214, 120), 3f)
            fillR(c, lx - 7f, ly - 13f, lx + 7f, ly - 9f, Color.rgb(70, 52, 38))
            fillR(c, lx - 7f, ly + 6f, lx + 7f, ly + 9f, Color.rgb(70, 52, 38))
        }

        // ateş böcekleri
        for ((i, f) in flies.withIndex()) {
            val a = f[0] + t * f[2]
            val fx = bx + cos(a) * f[1]
            val fy = ccy + 20f + sin(a * 1.3f) * f[1] * 0.55f
            val tw = 0.5f + 0.5f * sin(t * 2.2f + i * 1.7f)
            disc(c, fx, fy, 7f, Color.argb((50 * tw).toInt(), 255, 236, 140))
            disc(c, fx, fy, 2.2f, Color.argb((140 + 110 * tw).toInt().coerceIn(0, 255), 255, 244, 170))
        }
        if (fade) c.restore()

        // düşen altın yapraklar
        for (lf in leaves) {
            val k = (t * 0.07f + lf[1]) % 1f
            val lx = bx + lf[0] + sin(k * 14f + lf[1] * 9f) * 22f
            val ly = ccy + 100f + k * 360f
            rf.set(lx - lf[2], ly - lf[2] * 0.55f, lx + lf[2], ly + lf[2] * 0.55f)
            gp.color = Color.argb((230 * (1f - k * 0.5f)).toInt(), 236, 190, 80)
            c.save()
            c.rotate(k * 400f, lx, ly)
            c.drawOval(rf, gp)
            c.restore()
        }
    }
}
