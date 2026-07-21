package com.indianservers.aiexplorer.persistence

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.indianservers.aiexplorer.AppSettings
import com.indianservers.aiexplorer.SavedWorkspace
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.mathSettingsDataStore by preferencesDataStore("maths_product_settings")

private class MathWorkspaceDb(context: Context) : SQLiteOpenHelper(context, "maths-workspaces.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE projects(id TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,module TEXT NOT NULL,archive TEXT NOT NULL,updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE recovery(slot INTEGER PRIMARY KEY CHECK(slot=1),id TEXT NOT NULL,name TEXT NOT NULL,module TEXT NOT NULL,archive TEXT NOT NULL,updated_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS recovery(slot INTEGER PRIMARY KEY CHECK(slot=1),id TEXT NOT NULL,name TEXT NOT NULL,module TEXT NOT NULL,archive TEXT NOT NULL,updated_at INTEGER NOT NULL)")
    }
}

/** Transactional local project library, crash-recovery journal, and DataStore-backed preferences. */
class DurableMathStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val database = MathWorkspaceDb(applicationContext)

    suspend fun saveRecovery(state: WorkspaceState) = withContext(Dispatchers.IO) {
        database.writableDatabase.transaction {
            insertWithOnConflict("recovery", null, state.values(slot = true), SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    suspend fun loadRecovery(): WorkspaceState? = withContext(Dispatchers.IO) {
        database.readableDatabase.query("recovery", columns, "slot=1", null, null, null, null).use { cursor ->
            cursor.takeIf(Cursor::moveToFirst)?.decodeState()
        }
    }

    suspend fun replaceProjects(saved: List<SavedWorkspace>) = withContext(Dispatchers.IO) {
        database.writableDatabase.transaction {
            delete("projects", null, null)
            saved.take(24).forEach { item ->
                val state = item.snapshot.copy(id = item.id, name = item.name, module = item.module, modifiedAt = item.updatedAt)
                insertWithOnConflict("projects", null, state.values(), SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    suspend fun loadProjects(): List<SavedWorkspace> = withContext(Dispatchers.IO) {
        database.readableDatabase.query("projects", columns, null, null, null, null, "updated_at DESC", "24").use { cursor ->
            buildList { while (cursor.moveToNext()) cursor.decodeSaved()?.let(::add) }
        }
    }

    suspend fun saveSettings(value: AppSettings) {
        applicationContext.mathSettingsDataStore.edit { preferences ->
            preferences[haptics] = value.haptics; preferences[snap] = value.snap
            preferences[highContrast] = value.highContrast; preferences[reducedMotion] = value.reducedMotion
            preferences[spokenMath] = value.spokenMath; preferences[graphSonification] = value.graphSonification
            preferences[largeTouchTargets] = value.largeTouchTargets; preferences[decimalPrecision] = value.decimalPrecision
        }
    }

    suspend fun loadSettings(): AppSettings {
        val preferences = applicationContext.mathSettingsDataStore.data.first()
        return AppSettings(
            haptics = preferences[haptics] ?: true, snap = preferences[snap] ?: true,
            highContrast = preferences[highContrast] ?: false, reducedMotion = preferences[reducedMotion] ?: false,
            spokenMath = preferences[spokenMath] ?: false, graphSonification = preferences[graphSonification] ?: false,
            largeTouchTargets = preferences[largeTouchTargets] ?: false,
            decimalPrecision = (preferences[decimalPrecision] ?: 2).coerceIn(0, 10),
        )
    }

    private fun WorkspaceState.values(slot: Boolean = false) = ContentValues().apply {
        if (slot) put("slot", 1)
        put("id", id); put("name", name); put("module", module.name)
        put("archive", WorkspaceProjectCodec.encode(this@values)); put("updated_at", modifiedAt)
    }

    private fun Cursor.decodeState(): WorkspaceState? = WorkspaceProjectCodec.decode(getString(getColumnIndexOrThrow("archive"))).state

    private fun Cursor.decodeSaved(): SavedWorkspace? {
        val state = decodeState() ?: return null
        val id = getString(getColumnIndexOrThrow("id")); val name = getString(getColumnIndexOrThrow("name"))
        val module = runCatching { MathModule.valueOf(getString(getColumnIndexOrThrow("module"))) }.getOrDefault(state.module)
        val updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
        return SavedWorkspace(id, name, module, state.copy(id = id, name = name, module = module), WorkspaceProjectCodec.encode(state), updatedAt)
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try { block(); setTransactionSuccessful() } finally { endTransaction() }
    }

    private companion object {
        val columns = arrayOf("id", "name", "module", "archive", "updated_at")
        val haptics = booleanPreferencesKey("haptics")
        val snap = booleanPreferencesKey("snap")
        val highContrast = booleanPreferencesKey("high_contrast")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val spokenMath = booleanPreferencesKey("spoken_math")
        val graphSonification = booleanPreferencesKey("graph_sonification")
        val largeTouchTargets = booleanPreferencesKey("large_touch_targets")
        val decimalPrecision = intPreferencesKey("decimal_precision")
    }
}
