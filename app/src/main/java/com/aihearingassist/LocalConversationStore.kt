package com.aihearingassist

import android.content.Context

class LocalConversationStore(private val context: Context) {
    private val prefsName = "ai_hearing_assist_conversation"

    fun saveTranscript(text: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("transcript", text)
            .apply()
    }

    fun loadTranscript(): String {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString("transcript", "")
            ?: ""
    }

    fun clearTranscript() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .remove("transcript")
            .apply()
    }
}
