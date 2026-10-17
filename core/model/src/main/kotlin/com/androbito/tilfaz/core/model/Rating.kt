package com.androbito.tilfaz.core.model

import kotlin.math.roundToInt

/** A rating normalised to a 0..10 scale, whatever the provider sent (0-5, 0-10 or 0-100). */
@JvmInline
value class Rating(val outOfTen: Double) {
    val outOfFive: Double get() = outOfTen / 2

    /** One decimal ("6.9"), or "10" when it rounds to a perfect score. Locale-independent: a poster never reads "6,9". */
    val badge: String get() {
        val tenths = (outOfTen * 10).roundToInt()
        return if (tenths >= 100) "10" else "${tenths / 10}.${tenths % 10}"
    }

    companion object {
        fun fromRaw(value: Double?, fiveBased: Double? = null): Rating? {
            val ten = when {
                fiveBased != null && fiveBased > 0 -> fiveBased * 2
                value == null || value <= 0 -> return null
                value > 10 -> value / 10
                else -> value
            }
            return Rating(ten.coerceIn(0.0, 10.0))
        }
    }
}
