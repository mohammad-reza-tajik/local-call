package com.localcall.data

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.localcall.call.Identity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Persists the device's random id and the display name the user picked. */
class IdentityRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("identity", Context.MODE_PRIVATE)

    private val _identity = MutableStateFlow(load())
    val identity: StateFlow<Identity> = _identity

    fun rename(newName: String) {
        val name = newName.trim().take(MAX_NAME_LENGTH)
        if (name.isEmpty()) return
        prefs.edit { putString(KEY_NAME, name) }
        _identity.update { it.copy(name = name) }
    }

    private fun load(): Identity {
        val id = prefs.getString(KEY_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit { putString(KEY_ID, it) } }
        val name = prefs.getString(KEY_NAME, null) ?: Build.MODEL.orEmpty().ifBlank { "My phone" }
        return Identity(id, name)
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val MAX_NAME_LENGTH = 24
    }
}
