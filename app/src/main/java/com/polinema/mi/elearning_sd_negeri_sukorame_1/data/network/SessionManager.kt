package com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val PREF_NAME = "elearning_session"
        private const val KEY_USER = "user"
    }

    fun saveUser(user: User) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return gson.fromJson(json, User::class.java)
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null && getUser() != null
    }

    fun logout() {
        auth.signOut()
        prefs.edit().clear().apply()
    }
}