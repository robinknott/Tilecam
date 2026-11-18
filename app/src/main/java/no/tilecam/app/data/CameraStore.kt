package no.tilecam.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.tilecam.app.data.model.Camera

private val Context.dataStore by preferencesDataStore(name = "tilecam_cameras")

/** Persists the user's saved camera wall as JSON in DataStore. */
class CameraStore(private val context: Context) {

    private val key = stringPreferencesKey("cameras_json")
    private val json = Json { ignoreUnknownKeys = true }

    val cameras: Flow<List<Camera>> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<List<Camera>>(it) }.getOrNull() }
            ?.sortedBy { it.position }
            ?: emptyList()
    }

    suspend fun save(cameras: List<Camera>) {
        val reindexed = cameras.mapIndexed { i, c -> c.copy(position = i) }
        context.dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(reindexed)
        }
    }

    suspend fun upsert(current: List<Camera>, camera: Camera): List<Camera> {
        val idx = current.indexOfFirst { it.id == camera.id }
        val next = if (idx >= 0) current.toMutableList().apply { this[idx] = camera }
        else current + camera
        save(next)
        return next
    }

    suspend fun remove(current: List<Camera>, cameraId: String): List<Camera> {
        val next = current.filterNot { it.id == cameraId }
        save(next)
        return next
    }
}
