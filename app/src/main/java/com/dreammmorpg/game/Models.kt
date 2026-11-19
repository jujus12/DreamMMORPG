package com.dreammmorpg.game

import android.content.Context
import android.graphics.Color

enum class CharClass(
    val title: String,
    val role: String,
    val desc: String,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val str: Int,
    val def: Int,
    val mag: Int,
    val spd: Int
) {
    WARLORD(
        "Warlord", "Savaşçı",
        "Ağır zırhlı ön saf savaşçısı. Kılıcı ve kalkanıyla düşman hattını tek başına tutar.",
        Color.rgb(178, 44, 48), Color.rgb(150, 158, 172), Color.rgb(232, 190, 72),
        5, 5, 1, 2
    ),
    RUNESEER(
        "Runeseer", "Destek",
        "Kadim rünlerin gücüyle dostlarını iyileştirir, güçlendirir ve savaşın akışını değiştirir.",
        Color.rgb(238, 238, 230), Color.rgb(60, 130, 200), Color.rgb(120, 230, 220),
        2, 3, 4, 3
    ),
    ASSASSIN(
        "Assassin", "Ninja",
        "Gölgelerde dolaşan çevik suikastçı. Hızlı, sessiz ve ölümcül hançerlerin ustası.",
        Color.rgb(45, 48, 60), Color.rgb(120, 40, 140), Color.rgb(220, 60, 90),
        4, 2, 1, 5
    ),
    MAGE(
        "Mage", "Büyücü",
        "Yıkıcı büyülerle uzaktan hasar veren kırılgan ama çok güçlü bir büyücü.",
        Color.rgb(110, 60, 180), Color.rgb(60, 90, 220), Color.rgb(255, 150, 60),
        1, 1, 5, 3
    )
}

class Hero(val name: String, val cls: CharClass)

/** Karakterleri cihazda saklar (4 slot). */
object Save {
    private const val PREFS = "dream_save"
    const val SLOTS = 4

    fun load(ctx: Context): Array<Hero?> {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val out = arrayOfNulls<Hero>(SLOTS)
        for (i in 0 until SLOTS) {
            val s = p.getString("slot$i", null) ?: continue
            val parts = s.split("|")
            if (parts.size != 2) continue
            val cls = try {
                CharClass.valueOf(parts[1])
            } catch (e: IllegalArgumentException) {
                continue
            }
            out[i] = Hero(parts[0], cls)
        }
        return out
    }

    fun store(ctx: Context, slot: Int, hero: Hero?) {
        val e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (hero == null) e.remove("slot$slot") else e.putString("slot$slot", hero.name + "|" + hero.cls.name)
        e.apply()
    }
}
