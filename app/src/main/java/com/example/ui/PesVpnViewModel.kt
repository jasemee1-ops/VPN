package com.example.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.model.AppSettings
import com.example.model.GermanServerCatalog
import com.example.model.LuckMetrics
import com.example.model.PackPullRecord
import com.example.model.PackTier
import com.example.model.ServerNode
import com.example.model.VpnConnectionState
import com.example.model.VpnTelemetry
import com.example.network.GermanyPingMonitor
import com.example.vpn.PesVpnService
import com.example.vpn.VpnStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val telemetry: VpnTelemetry = VpnTelemetry(),
    val nodes: List<ServerNode> = GermanServerCatalog.defaultNodes,
    val luckMetrics: LuckMetrics = LuckMetrics(),
    val packHistory: List<PackPullRecord> = emptyList(),
    val elapsedSeconds: Long = 0L,
    val isEfootballInstalled: Boolean = false,
    val isProbingAll: Boolean = false,
    val packTimerSecondsRemaining: Int = 25,
    val isPackTimerRunning: Boolean = false,
    val ritualStepsDone: Set<Int> = emptySet(),
    val pendingVpnConsentIntent: Intent? = null,
    val autoConnectAttempted: Boolean = false
)

class PesVpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PreferencesRepository(application.applicationContext)

    private val _nodes = MutableStateFlow(GermanServerCatalog.defaultNodes)
    private val _elapsedSeconds = MutableStateFlow(0L)
    private val _isProbingAll = MutableStateFlow(false)
    private val _isBoostRunning = MutableStateFlow(false)
    private val _burstSummary = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val _packTimerSeconds = MutableStateFlow(25)
    private val _isPackTimerRunning = MutableStateFlow(false)
    private val _ritualStepsDone = MutableStateFlow<Set<Int>>(emptySet())
    private val _pendingConsentIntent = MutableStateFlow<Intent?>(null)
    private val _autoConnectAttempted = MutableStateFlow(false)
    private val _isEfootballInstalled = MutableStateFlow(checkEfootballInstalled())

    private var timerJob: Job? = null

    val uiState: StateFlow<MainUiState> = combine(
        repo.settingsFlow,
        VpnStateHolder.telemetry,
        _nodes,
        repo.packHistoryFlow,
        _elapsedSeconds
    ) { settings, telemetry, nodes, history, elapsed ->
        val selectedNode = nodes.firstOrNull { it.id == settings.selectedNodeId } ?: nodes.first()
        val luckMetrics = computeLuckMetrics(
            telemetry = telemetry,
            selectedNode = selectedNode,
            settings = settings
        )
        MainUiState(
            settings = settings,
            telemetry = telemetry,
            nodes = nodes,
            luckMetrics = luckMetrics,
            packHistory = history,
            elapsedSeconds = elapsed,
            isEfootballInstalled = _isEfootballInstalled.value,
            isProbingAll = _isProbingAll.value,
            packTimerSecondsRemaining = _packTimerSeconds.value,
            isPackTimerRunning = _isPackTimerRunning.value,
            ritualStepsDone = _ritualStepsDone.value,
            pendingVpnConsentIntent = _pendingConsentIntent.value,
            autoConnectAttempted = _autoConnectAttempted.value
        )
    }.combine(_isBoostRunning) { state, boostRunning ->
        state.copy(luckMetrics = state.luckMetrics.copy(isBoostRunning = boostRunning))
    }.combine(_burstSummary) { state, burstPair ->
        state.copy(
            luckMetrics = state.luckMetrics.copy(
                lastBurstSummaryAr = burstPair.first,
                lastBurstSummaryEn = burstPair.second
            )
        )
    }.combine(_isProbingAll) { state, probing ->
        state.copy(isProbingAll = probing)
    }.combine(_packTimerSeconds) { state, secs ->
        state.copy(packTimerSecondsRemaining = secs)
    }.combine(_isPackTimerRunning) { state, running ->
        state.copy(isPackTimerRunning = running)
    }.combine(_ritualStepsDone) { state, steps ->
        state.copy(ritualStepsDone = steps)
    }.combine(_pendingConsentIntent) { state, intent ->
        state.copy(pendingVpnConsentIntent = intent)
    }.combine(_autoConnectAttempted) { state, attempted ->
        state.copy(autoConnectAttempted = attempted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        startElapsedTicker()
        probeAllNodes(initialFast = true)
        triggerAutoConnectIfEnabled()
    }

    private fun startElapsedTicker() {
        viewModelScope.launch {
            while (isActive) {
                val since = VpnStateHolder.telemetry.value.connectedSinceEpochMs
                _elapsedSeconds.value = if (since != null &&
                    VpnStateHolder.telemetry.value.state == VpnConnectionState.CONNECTED
                ) {
                    ((System.currentTimeMillis() - since) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                delay(1000L)
            }
        }
    }

    private fun triggerAutoConnectIfEnabled() {
        viewModelScope.launch {
            repo.settingsFlow.collect { settings ->
                if (!_autoConnectAttempted.value) {
                    _autoConnectAttempted.value = true
                    if (settings.autoConnectGermany &&
                        !VpnStateHolder.userManuallyDisconnected &&
                        VpnStateHolder.telemetry.value.state == VpnConnectionState.DISCONNECTED
                    ) {
                        val germanNodeId = settings.selectedNodeId.ifBlank {
                            GermanServerCatalog.defaultNodes.first().id
                        }
                        connectToNode(germanNodeId, settings.splitTunnelEfootballOnly)
                    }
                }
            }
        }
    }

    fun onToggleConnection() {
        val currentState = VpnStateHolder.telemetry.value.state
        val settings = uiState.value.settings
        if (currentState == VpnConnectionState.CONNECTED ||
            currentState == VpnConnectionState.CONNECTING
        ) {
            disconnect()
        } else {
            connectToNode(settings.selectedNodeId, settings.splitTunnelEfootballOnly)
        }
    }

    fun connectToNode(nodeId: String, splitTunnel: Boolean) {
        val context = getApplication<Application>()
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            VpnStateHolder.setPermissionRequired()
            _pendingConsentIntent.value = prepareIntent
        } else {
            _pendingConsentIntent.value = null
            PesVpnService.startConnection(context, nodeId, splitTunnel)
        }
    }

    fun onVpnConsentResult(granted: Boolean) {
        _pendingConsentIntent.value = null
        val settings = uiState.value.settings
        if (granted) {
            PesVpnService.startConnection(
                getApplication(),
                settings.selectedNodeId,
                settings.splitTunnelEfootballOnly
            )
        } else {
            VpnStateHolder.setDisconnected()
        }
    }

    fun disconnect() {
        VpnStateHolder.userManuallyDisconnected = true
        PesVpnService.stopConnection(getApplication())
    }

    fun selectNode(nodeId: String) {
        viewModelScope.launch {
            repo.setSelectedNodeId(nodeId)
            VpnStateHolder.resetIfIdle(nodeId)
            val state = VpnStateHolder.telemetry.value.state
            if (state == VpnConnectionState.CONNECTED || state == VpnConnectionState.CONNECTING) {
                connectToNode(nodeId, uiState.value.settings.splitTunnelEfootballOnly)
            }
        }
    }

    fun setAutoConnectGermany(enabled: Boolean) {
        viewModelScope.launch {
            repo.setAutoConnectGermany(enabled)
        }
    }

    fun setSplitTunnelEfootball(enabled: Boolean) {
        viewModelScope.launch {
            repo.setSplitTunnelEfootballOnly(enabled)
            if (VpnStateHolder.telemetry.value.state == VpnConnectionState.CONNECTED) {
                connectToNode(uiState.value.settings.selectedNodeId, enabled)
            }
        }
    }

    fun setLuckStabilityMode(enabled: Boolean) {
        viewModelScope.launch {
            repo.setLuckStabilityMode(enabled)
        }
    }

    fun setIsArabic(isArabic: Boolean) {
        viewModelScope.launch {
            repo.setIsArabic(isArabic)
        }
    }

    fun probeAllNodes(initialFast: Boolean = false) {
        if (_isProbingAll.value) return
        viewModelScope.launch {
            _isProbingAll.value = true
            _nodes.update { list -> list.map { it.copy(isProbing = true) } }
            val attempts = if (initialFast) 2 else 3
            val updated = coroutineScope {
                _nodes.value.map { node ->
                    async {
                        val result = GermanyPingMonitor.probeNode(node, attempts = attempts)
                        val refreshed = node.copy(
                            measuredPingMs = result.avgPingMs,
                            jitterMs = result.jitterMs,
                            packetLossPercent = result.packetLossPercent,
                            isProbing = false
                        )
                        VpnStateHolder.updateActiveNode(refreshed)
                        refreshed
                    }
                }.awaitAll()
            }
            _nodes.value = updated
            _isProbingAll.value = false
        }
    }

    fun runLuckBoostCalibration() {
        if (_isBoostRunning.value) return
        viewModelScope.launch {
            _isBoostRunning.value = true
            val settings = uiState.value.settings
            val selected = _nodes.value.firstOrNull { it.id == settings.selectedNodeId }
                ?: _nodes.value.first()

            // Connect automatically to Germany if not connected
            if (VpnStateHolder.telemetry.value.state != VpnConnectionState.CONNECTED) {
                connectToNode(selected.id, settings.splitTunnelEfootballOnly)
            }

            // Run a 5-packet burst latency & jitter check to Germany
            val result = GermanyPingMonitor.probeNode(selected, attempts = 5)
            _nodes.update { list ->
                list.map {
                    if (it.id == selected.id) {
                        it.copy(
                            measuredPingMs = result.avgPingMs,
                            jitterMs = result.jitterMs,
                            packetLossPercent = result.packetLossPercent
                        )
                    } else it
                }
            }
            VpnStateHolder.updateActiveNode(
                selected.copy(
                    measuredPingMs = result.avgPingMs,
                    jitterMs = result.jitterMs,
                    packetLossPercent = result.packetLossPercent
                )
            )

            val pingStr = result.avgPingMs?.let { "${it}ms" } ?: "--"
            val jitterStr = result.jitterMs?.let { "${it}ms" } ?: "--"
            _burstSummary.value =
                "اكتمل فحص مسار ألمانيا: البنق $pingStr • التذبذب $jitterStr • فقدان الحزم ${result.packetLossPercent}%" to
                "Germany route calibrated: Ping $pingStr • Jitter $jitterStr • Loss ${result.packetLossPercent}%"

            _ritualStepsDone.update { it + 0 + 1 }
            _isBoostRunning.value = false
        }
    }

    fun toggleRitualStep(index: Int) {
        _ritualStepsDone.update { current ->
            if (current.contains(index)) current - index else current + index
        }
    }

    fun togglePackTimer() {
        if (_isPackTimerRunning.value) {
            timerJob?.cancel()
            _isPackTimerRunning.value = false
        } else {
            if (_packTimerSeconds.value <= 0) _packTimerSeconds.value = 25
            _isPackTimerRunning.value = true
            timerJob = viewModelScope.launch {
                while (_packTimerSeconds.value > 0 && isActive) {
                    delay(1000L)
                    _packTimerSeconds.update { (it - 1).coerceAtLeast(0) }
                }
                _isPackTimerRunning.value = false
                if (_packTimerSeconds.value == 0) {
                    _ritualStepsDone.update { it + 2 }
                }
            }
        }
    }

    fun resetPackTimer() {
        timerJob?.cancel()
        _isPackTimerRunning.value = false
        _packTimerSeconds.value = 25
    }

    fun logPackPull(packName: String, tier: PackTier, coinsSpent: Int) {
        val currentPing = VpnStateHolder.telemetry.value.lastDnsLatencyMs
            ?: uiState.value.nodes.firstOrNull { it.id == uiState.value.settings.selectedNodeId }?.measuredPingMs
        viewModelScope.launch {
            repo.addPackPullRecord(
                PackPullRecord(
                    id = System.currentTimeMillis(),
                    packName = packName.ifBlank { "Epic / Showtime Box" },
                    tier = tier,
                    coinsSpent = coinsSpent,
                    pingAtPullMs = currentPing,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearPackHistory() {
        viewModelScope.launch {
            repo.clearPackHistory()
        }
    }

    fun launchEfootball(): Boolean {
        val context = getApplication<Application>()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PesVpnService.EFOOTBALL_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            _isEfootballInstalled.value = false
            false
        }
    }

    fun refreshEfootballInstalled() {
        _isEfootballInstalled.value = checkEfootballInstalled()
    }

    private fun checkEfootballInstalled(): Boolean {
        return try {
            getApplication<Application>().packageManager.getPackageInfo(
                PesVpnService.EFOOTBALL_PACKAGE,
                0
            )
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun computeLuckMetrics(
        telemetry: VpnTelemetry,
        selectedNode: ServerNode,
        settings: AppSettings
    ): LuckMetrics {
        val isConnected = telemetry.state == VpnConnectionState.CONNECTED
        val tunnelScore = if (isConnected) 35 else 0

        val ping = telemetry.lastDnsLatencyMs ?: selectedNode.measuredPingMs
        val pingScore = when {
            ping == null -> 8
            ping <= 65 -> 25
            ping <= 105 -> 21
            ping <= 150 -> 15
            else -> 9
        }

        val jitter = selectedNode.jitterMs
        val jitterScore = when {
            jitter == null -> 8
            jitter <= 8 -> 20
            jitter <= 18 -> 16
            jitter <= 35 -> 11
            else -> 6
        }

        val windowInfo = GermanyPingMonitor.getFrankfurtWindowInfo()
        val stabilityBonus = if (settings.luckStabilityMode && isConnected) 5 else 0
        val total = (tunnelScore + pingScore + jitterScore + windowInfo.score + stabilityBonus)
            .coerceIn(0, 100)

        return LuckMetrics(
            overallScore = total,
            tunnelScore = tunnelScore,
            pingScore = pingScore,
            jitterScore = jitterScore,
            windowScore = windowInfo.score,
            germanyTimeFormatted = windowInfo.formattedTime,
            windowStatusAr = windowInfo.statusAr,
            windowStatusEn = windowInfo.statusEn
        )
    }
}
