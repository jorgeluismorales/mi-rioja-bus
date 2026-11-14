package com.mibusrioja.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cardNumberKey = stringPreferencesKey("saved_card_number")
    private val lastLineKey = stringPreferencesKey("last_line_code")

    val lastLineCode: Flow<String?> = context.userPrefsDataStore.data
        .map { prefs -> prefs[lastLineKey] }

    suspend fun saveLastLineCode(codLinea: String) {
        context.userPrefsDataStore.edit { prefs -> prefs[lastLineKey] = codLinea }
    }

    val savedCardNumber: Flow<String?> = context.userPrefsDataStore.data
        .map { prefs -> prefs[cardNumberKey] }

    suspend fun saveCardNumber(number: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[cardNumberKey] = number
        }
    }

    suspend fun clearCardNumber() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(cardNumberKey)
        }
    }
}
