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

    fun setConnecting(node: ServerNode, splitTunnel: Boolean) {
        _telemetry.update {
            it.copy(
                state = VpnConnectionState.CONNECTING,
                activeNode = node,
                isSplitTunnelActive = splitTunnel,
                errorMessage = null
            )
        }
    }

    fun setConnected(node: ServerNode, splitTunnel: Boolean) {
        _telemetry.update {
            it.copy(
                state = VpnConnectionState.CONNECTED,
                activeNode = node,
                connectedSinceEpochMs = System.currentTimeMillis(),
                isSplitTunnelActive = splitTunnel,
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

    fun recordDnsPacket(bytesSent: Int, bytesReceived: Int, rttMs: Int, host: String?) {
        _telemetry.update { current ->
            current.copy(
                dnsQueriesHandled = current.dnsQueriesHandled + 1,
                bytesOut = current.bytesOut + bytesSent,
                bytesIn = current.bytesIn + bytesReceived,
                lastDnsLatencyMs = rttMs,
                lastResolvedHost = host ?: current.lastResolvedHost
            )
        }
    }

    fun updateActiveNode(node: ServerNode) {
        _telemetry.update {
            if (it.activeNode.id == node.id) it.copy(activeNode = node) else it
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
