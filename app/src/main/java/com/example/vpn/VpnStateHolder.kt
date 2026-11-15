package com.example.vpn

import com.example.model.GermanServerCatalog
import com.example.model.ServerNode
import com.example.model.VpnConnectionState
import com.example.model.VpnTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnStateHolder {
    private val _telemetry = MutableStateFlow(VpnTelemetry())
    val telemetry: StateFlow<VpnTelemetry> = _telemetry.asStateFlow()

    @Volatile
    var userManuallyDisconnected: Boolean = false

    fun setConnecting(node: ServerNode, bypassEfootball: Boolean) {
        _telemetry.update {
            it.copy(
                state = VpnConnectionState.CONNECTING,
                activeNode = node,
                isEfootballBypassed = bypassEfootball,
                errorMessage = null
            )
        }
    }

    fun setConnected(node: ServerNode, bypassEfootball: Boolean) {
        _telemetry.update {
            it.copy(
                state = VpnConnectionState.CONNECTED,
                activeNode = node,
                connectedSinceEpochMs = System.currentTimeMillis(),
                isEfootballBypassed = bypassEfootball,
                errorMessage = null
            )
        }
    }

    fun setDisconnected(error: String? = null) {
        _telemetry.update {
            it.copy(
                state = if (error != null) VpnConnectionState.ERROR else VpnConnectionState.DISCONNECTED,
                connectedSinceEpochMs = null,
                errorMessage = error
            )
        }
    }

    fun setPermissionRequired() {
        _telemetry.update {
            it.copy(
                state = VpnConnectionState.PERMISSION_REQUIRED,
                connectedSinceEpochMs = null
            )
        }
    }

    fun updateActiveNode(node: ServerNode) {
        _telemetry.update { current ->
            if (current.activeNode.id == node.id) {
                current.copy(
                    activeNode = node,
                    lastMeasuredLatencyMs = node.measuredPingMs ?: current.lastMeasuredLatencyMs
                )
            } else {
                current
            }
        }
    }

    fun resetIfIdle(selectedNodeId: String) {
        _telemetry.update {
            if (it.state == VpnConnectionState.DISCONNECTED) {
                it.copy(activeNode = GermanServerCatalog.findById(selectedNodeId))
            } else {
                it
            }
        }
    }
}
