package com.example.prog7313poepart1

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveFirebaseUser(uid: String, email: String?) {
        prefs.edit()
            .putString("firebaseUid", uid)
            .putString("email", email ?: "")
            .apply()
    }

    fun getFirebaseUid(): String? {
        return prefs.getString("firebaseUid", null)
    }

    fun getUserEmail(): String {
        return prefs.getString("email", "") ?: ""
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun saveUserId(userId: Int) {
        prefs.edit().putInt("legacyUserId", userId).apply()
    }

    fun getUserId(): Int {
        return prefs.getInt("legacyUserId", 1)
    }
}