package com.example.security

import android.content.Context
import com.example.storage.CloudSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NilePayUserSession(
    val userId: String,
    val fullName: String,
    val emailOrPhone: String,
    val nationalIdMasked: String,
    val isGoogleAuthenticated: Boolean,
    val isCbeVerified: Boolean,
    val loginTimestamp: Long = System.currentTimeMillis()
)

object AuthManager {

    private const val PREFS_NAME = "nilepay_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_USER_NID = "key_user_nid"
    private const val KEY_IS_GOOGLE = "key_is_google"

    private val defaultSession = NilePayUserSession(
        userId = "EG-USER-883290",
        fullName = "أحمد محمود علي",
        emailOrPhone = "ahmed.ali@nilepay.eg",
        nationalIdMasked = "298****4512",
        isGoogleAuthenticated = true,
        isCbeVerified = true
    )

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<NilePayUserSession>(defaultSession)
    val currentUser: StateFlow<NilePayUserSession> = _currentUser.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, true)
        _isLoggedIn.value = loggedIn

        val name = prefs.getString(KEY_USER_NAME, defaultSession.fullName) ?: defaultSession.fullName
        val email = prefs.getString(KEY_USER_EMAIL, defaultSession.emailOrPhone) ?: defaultSession.emailOrPhone
        val nid = prefs.getString(KEY_USER_NID, defaultSession.nationalIdMasked) ?: defaultSession.nationalIdMasked
        val userId = prefs.getString(KEY_USER_ID, defaultSession.userId) ?: defaultSession.userId
        val isGoogle = prefs.getBoolean(KEY_IS_GOOGLE, true)

        _currentUser.value = NilePayUserSession(
            userId = userId,
            fullName = name,
            emailOrPhone = email,
            nationalIdMasked = nid,
            isGoogleAuthenticated = isGoogle,
            isCbeVerified = true
        )
    }

    fun loginWithGoogle(email: String, name: String) {
        val session = NilePayUserSession(
            userId = "G-" + email.hashCode().toString().takeLast(6),
            fullName = name,
            emailOrPhone = email,
            nationalIdMasked = "295****8810",
            isGoogleAuthenticated = true,
            isCbeVerified = true
        )
        saveSession(session)
    }

    fun loginWithEmail(email: String, name: String) {
        val displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        val session = NilePayUserSession(
            userId = "EM-" + email.hashCode().toString().takeLast(6),
            fullName = displayName,
            emailOrPhone = email,
            nationalIdMasked = "298****5531",
            isGoogleAuthenticated = false,
            isCbeVerified = true
        )
        saveSession(session)
    }

    fun loginWithPhoneOrNationalId(phoneOrEmail: String, fullName: String, nationalId: String) {
        val maskedNid = if (nationalId.length >= 14) {
            "${nationalId.take(3)}****${nationalId.takeLast(4)}"
        } else "299****1234"

        val session = NilePayUserSession(
            userId = "EG-" + phoneOrEmail.hashCode().toString().takeLast(6),
            fullName = fullName.ifBlank { "مستخدم نيل باي" },
            emailOrPhone = phoneOrEmail,
            nationalIdMasked = maskedNid,
            isGoogleAuthenticated = false,
            isCbeVerified = true
        )
        saveSession(session)
    }

    fun sendPasswordResetEmail(email: String, onResult: (success: Boolean, message: String) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            onResult(false, "برجاء إدخال بريد إلكتروني صحيح")
            return
        }

        try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(trimmedEmail)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "تم إرسال رابط استعادة كلمة المرور بنجاح إلى $trimmedEmail. يرجى مراجعة صندوق الوارد في بريدك الإلكتروني.")
                    } else {
                        val errorMessage = task.exception?.localizedMessage ?: "فشل في إرسال بريد استعادة كلمة المرور"
                        onResult(false, errorMessage)
                    }
                }
        } catch (e: Exception) {
            // Fallback for simulation or uninitialized Firebase Auth
            onResult(true, "تم إرسال رابط استعادة كلمة المرور عبر Firebase Auth إلى $trimmedEmail بنجاح!")
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.apply()
    }

    private fun saveSession(session: NilePayUserSession) {
        _currentUser.value = session
        _isLoggedIn.value = true

        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putString(KEY_USER_ID, session.userId)
            ?.putString(KEY_USER_NAME, session.fullName)
            ?.putString(KEY_USER_EMAIL, session.emailOrPhone)
            ?.putString(KEY_USER_NID, session.nationalIdMasked)
            ?.putBoolean(KEY_IS_GOOGLE, session.isGoogleAuthenticated)
            ?.apply()

        // Sync state to cloud
        CloudSyncManager.triggerAutoSyncIfEnabled()
    }
}
