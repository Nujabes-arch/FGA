package io.github.fate_grand_automata.scripts.models

import io.github.fate_grand_automata.scripts.enums.CardTypeEnum

enum class CardTypeSoftLimit(val preferredCount: Int?) {
    One(1),
    Two(2),
    Unlimited(null);

    companion object {
        fun of(value: String?): CardTypeSoftLimit = when (value?.trim()?.uppercase()) {
            "ONE", "1" -> One
            "TWO", "2" -> Two
            else -> Unlimited
        }
    }
}

data class CardTypeSoftLimits(
    val buster: CardTypeSoftLimit = CardTypeSoftLimit.Unlimited,
    val arts: CardTypeSoftLimit = CardTypeSoftLimit.Unlimited,
    val quick: CardTypeSoftLimit = CardTypeSoftLimit.Unlimited
) {
    private fun limitFor(type: CardTypeEnum) = when (type) {
        CardTypeEnum.Buster -> buster
        CardTypeEnum.Arts -> arts
        CardTypeEnum.Quick -> quick
        CardTypeEnum.Unknown -> null
    }

    fun apply(cards: List<ParsedCard>): List<ParsedCard> {
        if (listOf(buster, arts, quick).all { it == CardTypeSoftLimit.Unlimited }) {
            return cards
        }

        val preferred = mutableListOf<ParsedCard>()
        val overflow = mutableListOf<ParsedCard>()
        val unknown = mutableListOf<ParsedCard>()
        val counts = mutableMapOf<CardTypeEnum, Int>()

        cards.forEach { card ->
            val limit = limitFor(card.type)
            if (limit == null) {
                unknown += card
                return@forEach
            }

            val count = counts[card.type] ?: 0
            if (limit.preferredCount == null || count < limit.preferredCount) {
                preferred += card
                counts[card.type] = count + 1
            } else {
                overflow += card
            }
        }

        return preferred + overflow + unknown
    }

    override fun toString() = listOf(buster, arts, quick).joinToString(",") { it.name }

    companion object {
        val default = CardTypeSoftLimits()

        fun of(serialized: String): CardTypeSoftLimits {
            val values = serialized.split(",")
            return CardTypeSoftLimits(
                buster = CardTypeSoftLimit.of(values.getOrNull(0)),
                arts = CardTypeSoftLimit.of(values.getOrNull(1)),
                quick = CardTypeSoftLimit.of(values.getOrNull(2))
            )
        }
    }
}

class CardTypeSoftLimitsPerWave private constructor(
    private val limitsPerWave: List<CardTypeSoftLimits>
) : List<CardTypeSoftLimits> by limitsPerWave {
    fun atWave(wave: Int) = limitsPerWave[wave.coerceIn(limitsPerWave.indices)]

    override fun toString() = limitsPerWave.joinToString("\n")

    companion object {
        private const val waveCount = 3

        val default get() = from(emptyList())

        fun from(limitsPerWave: List<CardTypeSoftLimits>) = CardTypeSoftLimitsPerWave(
            limitsPerWave
                .take(waveCount)
                .let { it + List(waveCount - it.size) { CardTypeSoftLimits.default } }
        )

        fun of(serialized: String): CardTypeSoftLimitsPerWave =
            if (serialized.isBlank()) {
                default
            } else {
                from(serialized.split("\n").map(CardTypeSoftLimits::of))
            }
    }
}
