package com.androbito.tilfaz.core.model

/** A rating normalised to a 0..10 scale, whatever the provider sent (0-5, 0-10 or 0-100). */
@JvmInline
value class Rating(val outOfTen: Double) {
    val outOfFive: Double get() = outOfTen / 2
    val badge: String get() = if (outOfTen >= 10) "10" else outOfTen.toInt().toString()

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
