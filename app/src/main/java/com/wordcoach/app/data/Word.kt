package com.wordcoach.app.data

/**
 * A single vocabulary entry shown to the learner.
 *
 * @property word        The tough English word, e.g. "Ephemeral".
 * @property soundsLike  An easy phonetic respelling, e.g. "ih-FEM-er-ul".
 * @property meaning     A short, simple meaning written in plain English.
 * @property example     A simple example sentence using the word.
 */
data class Word(
    val word: String,
    val soundsLike: String,
    val meaning: String,
    val example: String
)
