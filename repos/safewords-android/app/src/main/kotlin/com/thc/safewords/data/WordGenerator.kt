package com.thc.safewords.data

import java.security.SecureRandom

object WordGenerator {

    enum class Style { WordNumber, TwoWords, ThreeWords }

    private val rng = SecureRandom()

    fun adjective(): String = WordLists.adjectives[rng.nextInt(WordLists.adjectives.size)]
    fun noun(): String = WordLists.nouns[rng.nextInt(WordLists.nouns.size)]
    fun number(): Int = rng.nextInt(100)

    fun phrase(style: Style = Style.WordNumber): String {
        val a = adjective().replaceFirstChar { it.uppercase() }
        val n = noun().replaceFirstChar { it.uppercase() }
        return when (style) {
            Style.WordNumber -> "$a $n ${number()}"
            Style.TwoWords -> "$a $n"
            Style.ThreeWords -> {
                val n2 = noun().replaceFirstChar { it.uppercase() }
                "$a $n $n2"
            }
        }
    }
}
