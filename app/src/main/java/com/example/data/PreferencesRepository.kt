package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.GermanServerCatalog
import com.example.model.PackPullRecord
import com.example.model.PackTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pes_vpn_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_CONNECT_GERMANY = booleanPreferencesKey("auto_connect_germany")
        val SELECTED_NODE_ID = stringPreferencesKey("selected_node_id")
        val SPLIT_TUNNEL_EFOOTBALL = booleanPreferencesKey("split_tunnel_efootball")
        val LUCK_STABILITY_MODE = booleanPreferencesKey("luck_stability_mode")
        val IS_ARABIC = booleanPreferencesKey("is_arabic")
        val PACK_PULL_HISTORY = stringPreferencesKey("pack_pull_history")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            autoConnectGermany = prefs[Keys.AUTO_CONNECT_GERMANY] ?: true,
            selectedNodeId = prefs[Keys.SELECTED_NODE_ID]
                ?: GermanServerCatalog.defaultNodes.first().id,
            splitTunnelEfootballOnly = prefs[Keys.SPLIT_TUNNEL_EFOOTBALL] ?: false,
            luckStabilityMode = prefs[Keys.LUCK_STABILITY_MODE] ?: true,
            isArabic = prefs[Keys.IS_ARABIC] ?: true
        )
    }

    val packHistoryFlow: Flow<List<PackPullRecord>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PACK_PULL_HISTORY].orEmpty()
        decodeRecords(raw)
    }

    suspend fun setAutoConnectGermany(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CONNECT_GERMANY] = enabled }
    }

    suspend fun setSelectedNodeId(nodeId: String) {
        context.dataStore.edit { it[Keys.SELECTED_NODE_ID] = nodeId }
    }

    suspend fun setSplitTunnelEfootballOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPLIT_TUNNEL_EFOOTBALL] = enabled }
    }

    suspend fun setLuckStabilityMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LUCK_STABILITY_MODE] = enabled }
    }

    suspend fun setIsArabic(isArabic: Boolean) {
        context.dataStore.edit { it[Keys.IS_ARABIC] = isArabic }
    }

    suspend fun addPackPullRecord(record: PackPullRecord) {
        context.dataStore.edit { prefs ->
            val current = decodeRecords(prefs[Keys.PACK_PULL_HISTORY].orEmpty())
            val updated = (listOf(record) + current).take(30)
            prefs[Keys.PACK_PULL_HISTORY] = encodeRecords(updated)
        }
    }

    suspend fun clearPackHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PACK_PULL_HISTORY)
        }
    }

    private fun encodeRecords(records: List<PackPullRecord>): String {
        return records.joinToString("\n") { r ->
            val safeName = r.packName.replace("|", " ").replace("\n", " ")
            "${r.id}|$safeName|${r.tier.name}|${r.coinsSpent}|${r.pingAtPullMs ?: -1}|${r.timestampMs}"
        }
    }

    private fun decodeRecords(raw: String): List<PackPullRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val packName = parts[1]
            val tier = runCatching { PackTier.valueOf(parts[2]) }.getOrDefault(PackTier.STANDARD)
            val coins = parts[3].toIntOrNull() ?: 100
            val ping = parts[4].toIntOrNull()?.takeIf { it >= 0 }
            val timestamp = parts[5].toLongOrNull() ?: 0L
            PackPullRecord(
                id = id,
                packName = packName,
                tier = tier,
                coinsSpent = coins,
                pingAtPullMs = ping,
                timestampMs = timestamp
            )
        }
    }
}
