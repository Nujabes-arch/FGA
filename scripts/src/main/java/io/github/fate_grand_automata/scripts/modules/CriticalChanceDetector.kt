package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.models.CriticalChanceClassifier
import io.github.fate_grand_automata.scripts.models.CriticalChanceMatch
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class CriticalChanceDetector @Inject constructor(
    api: IFgoAutomataApi
) : IFgoAutomataApi by api {
    private companion object {
        const val blankDigitThreshold = 0.85
    }

    private val digitImages = listOf(
        0 to Images.CriticalDigit0,
        1 to Images.CriticalDigit1,
        2 to Images.CriticalDigit2,
        3 to Images.CriticalDigit3,
        4 to Images.CriticalDigit4,
        5 to Images.CriticalDigit5,
        6 to Images.CriticalDigit6,
        7 to Images.CriticalDigit7,
        8 to Images.CriticalDigit8,
        9 to Images.CriticalDigit9
    )

    fun detect(card: CommandCard.Face): Int? {
        val region = locations.attack.criticalChanceRegion(card)
        if (region !in locations.scriptArea) return null

        return region.getPattern().use { pattern ->
            val blankDigitBand = pattern.threshold(blankDigitThreshold).use { it.isBlack() }
            val matches = digitImages.flatMap { (digit, image) ->
                pattern.findMatches(images[image], CriticalChanceClassifier.searchThreshold)
                    .map { match ->
                        CriticalChanceMatch(
                            digit = digit,
                            region = match.region,
                            score = match.score
                        )
                    }
                    .toList()
            }

            CriticalChanceClassifier.classify(matches, blankDigitBand = blankDigitBand)
        }
    }
}
