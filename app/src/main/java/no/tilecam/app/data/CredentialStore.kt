package no.tilecam.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Per-camera username/password, stored with Jetpack Security (AES-256). */
data class Credentials(val username: String, val password: String)

class CredentialStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "tilecam_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(cameraId: String, creds: Credentials) {
        prefs.edit()
            .putString("$cameraId.user", creds.username)
            .putString("$cameraId.pass", creds.password)
            .apply()
    }

    fun get(cameraId: String): Credentials? {
        val user = prefs.getString("$cameraId.user", null) ?: return null
        val pass = prefs.getString("$cameraId.pass", "") ?: ""
        return Credentials(user, pass)
    }

    fun delete(cameraId: String) {
        prefs.edit()
            .remove("$cameraId.user")
            .remove("$cameraId.pass")
            .apply()
    }
}
