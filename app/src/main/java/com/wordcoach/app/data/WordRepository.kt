package com.wordcoach.app.data

import android.content.Context
import org.json.JSONArray

/**
 * Loads the bundled word list from assets/words.json.
 *
 * Everything is packaged inside the app, so this works fully offline.
 */
object WordRepository {

    private const val ASSET_NAME = "words.json"

    @Volatile
    private var cached: List<Word>? = null

    /** Returns the full word list, loading and caching it on first call. */
    fun getWords(context: Context): List<Word> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = loadFromAssets(context)
            cached = loaded
            return loaded
        }
    }

    private fun loadFromAssets(context: Context): List<Word> {
        val json = context.assets.open(ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }

        val array = JSONArray(json)
        val result = ArrayList<Word>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Word(
                    word = obj.getString("word"),
                    soundsLike = obj.optString("soundsLike", ""),
                    meaning = obj.getString("meaning"),
                    example = obj.optString("example", "")
                )
            )
        }
        return result
    }
}
