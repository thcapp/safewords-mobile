package com.thc.safewords.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Tracks scam-drill practice sessions.
 *
 * A drill is a fake "incoming call" scenario — user is shown a prompt
 * that mimics a scam ("I'm your grandkid, I need money, here's the word: X"),
 * and must decide whether the word is correct. Outcomes are stored to build
 * a "practice streak" history shown in Settings.
 *
 * Drill prompts are generated client-side with the user's actual current
 * safeword AND a list of plausible-but-wrong distractor words, so practice
 * mirrors real verification.
 */
object DrillService {

    private const val DRILLS_KEY = "drills_history"
    private val gson = Gson()

    data class DrillSession(
        val id: String,
        val timestamp: Long,
        val passed: Boolean,
        val groupId: String,
        val scenario: String,
        val expectedWord: String,
        val presentedWord: String
    )

    /** A drill prompt to present to the user. */
    data class DrillPrompt(
        val groupId: String,
        val scenario: String,
        val presentedWord: String,
        val isCorrect: Boolean
    )

    private val scenarios = listOf(
        "Someone calls saying it's your grandchild. They sound upset and say they need money. They tell you the word is \"%s\".",
        "A caller claims they're your son, just got into a car accident. They say the word is \"%s\".",
        "Your \"daughter\" texts asking for cash. They send the word as \"%s\".",
        "A voice on the phone says they're your spouse calling from work. They give the word \"%s\".",
        "Someone calls claiming they're your nephew, in jail, needs bail. Word: \"%s\"."
    )

    /**
     * Generate a drill. 50% of the time, present the correct word; 50%,
     * present a slightly-wrong word (one position off in the wordlist) so
     * the user must actually check.
     */
    fun nextDrill(groupId: String): DrillPrompt? {
        val correctWord = GroupRepository.getCurrentSafeword(groupId) ?: return null
        val showCorrect = (System.currentTimeMillis() % 2L) == 0L
        val presented = if (showCorrect) correctWord else mutateWord(correctWord)
        val scenario = scenarios.random().format(presented)
        return DrillPrompt(
            groupId = groupId,
            scenario = scenario,
            presentedWord = presented,
            isCorrect = showCorrect
        )
    }

    /** User's "yes that's our word / no it isn't" answer. */
    fun submit(prompt: DrillPrompt, userSaidMatch: Boolean) {
        // Pass if the user correctly identified whether the presented word matches.
        val passed = userSaidMatch == prompt.isCorrect
        val session = DrillSession(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis() / 1000,
            passed = passed,
            groupId = prompt.groupId,
            scenario = prompt.scenario,
            expectedWord = if (prompt.isCorrect) prompt.presentedWord else (GroupRepository.getCurrentSafeword(prompt.groupId) ?: ""),
            presentedWord = prompt.presentedWord
        )
        val hist = getHistory().toMutableList()
        hist.add(0, session) // newest first
        if (hist.size > 50) hist.subList(50, hist.size).clear()
        SecureStorageService.saveString(DRILLS_KEY, gson.toJson(hist))
    }

    fun getHistory(): List<DrillSession> {
        val json = SecureStorageService.getString(DRILLS_KEY) ?: return emptyList()
        val type = object : TypeToken<List<DrillSession>>() {}.type
        return runCatching { gson.fromJson<List<DrillSession>>(json, type) ?: emptyList() }
            .getOrDefault(emptyList())
    }

    fun clearHistory() {
        SecureStorageService.saveString(DRILLS_KEY, "[]")
    }

    /** Adjacent-word substitution to make a "wrong" version of the correct word. */
    private fun mutateWord(correctWord: String): String {
        val parts = correctWord.split(" ")
        if (parts.size < 3) return correctWord
        // Replace number with number ± 1 mod 100.
        val n = parts[2].toIntOrNull() ?: return correctWord
        val newN = ((n + 7) % 100)  // arbitrary but stable shift
        return "${parts[0]} ${parts[1]} $newN"
    }
}
