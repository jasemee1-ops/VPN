package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.GermanServerCatalog
import com.example.model.ServerNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

class PesVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnelJob: Job? = null
    private var tunInterface: ParcelFileDescriptor? = null
    private val ipIdentification = AtomicInteger(1)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_CONNECT
        if (action == ACTION_DISCONNECT) {
            VpnStateHolder.userManuallyDisconnected = true
            stopTunnel()
            stopSelf()
            return START_NOT_STICKY
        }

        val nodeId = intent?.getStringExtra(EXTRA_NODE_ID)
            ?: GermanServerCatalog.defaultNodes.first().id
        val splitTunnel = intent?.getBooleanExtra(EXTRA_SPLIT_TUNNEL, false) ?: false
        val node = GermanServerCatalog.findById(nodeId)

        VpnStateHolder.userManuallyDisconnected = false
        startForegroundWithNotification(node)
        startTunnel(node, splitTunnel)
        return START_STICKY
    }

    private fun startForegroundWithNotification(node: ServerNode) {
        ensureNotificationChannel()
        val notification = buildNotification(node)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.vpn_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.vpn_notification_channel_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(node: ServerNode): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PesVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(node.nameAr)
            .setContentText("DNS ${node.primaryDns} • ${node.providerTag}")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.vpn_notification_disconnect),
                disconnectIntent
            )
            .build()
    }

    private fun startTunnel(node: ServerNode, requestSplitTunnel: Boolean) {
        stopTunnelInternal()
        VpnStateHolder.setConnecting(node, requestSplitTunnel)

        try {
            val builder = Builder()
                .setSession(node.nameEn)
                .setMtu(1500)
                .addAddress(VPN_CLIENT_IP, 32)
                .addDnsServer(VPN_DNS_GATEWAY)
                .addRoute(VPN_DNS_GATEWAY, 32)

            var splitApplied = false
            if (requestSplitTunnel && isPackageInstalled(EFOOTBALL_PACKAGE)) {
                try {
                    builder.addAllowedApplication(EFOOTBALL_PACKAGE)
                    splitApplied = true
                } catch (_: PackageManager.NameNotFoundException) {
                    splitApplied = false
                }
            }

            if (!splitApplied) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    // Ignore if package resolution fails
                }
            }

            val descriptor = builder.establish()
            if (descriptor == null) {
                VpnStateHolder.setPermissionRequired()
                stopSelf()
                return
            }

            tunInterface = descriptor
            VpnStateHolder.setConnected(node, splitApplied)

            tunnelJob = serviceScope.launch {
                runDnsForwardingLoop(descriptor, node)
            }
        } catch (ex: Exception) {
            VpnStateHolder.setDisconnected(ex.message ?: "VPN interface initialization failed")
            stopSelf()
        }
    }

    private fun runDnsForwardingLoop(descriptor: ParcelFileDescriptor, node: ServerNode) {
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)
        val packetBuffer = ByteArray(32767)

        while (serviceScope.isActive) {
            val length = try {
                inputStream.read(packetBuffer)
            } catch (_: Exception) {
                break
            }

            if (length <= 0) continue

            // Verify IPv4 packet with minimum header
            if (length < 28) continue
            val version = (packetBuffer[0].toInt() ushr 4) and 0x0F
            if (version != 4) continue

            val ihl = (packetBuffer[0].toInt() and 0x0F) * 4
            if (ihl < 20 || length < ihl + 8) continue

            val protocol = packetBuffer[9].toInt() and 0xFF
            if (protocol != UDP_PROTOCOL) continue

            val srcPort = ((packetBuffer[ihl].toInt() and 0xFF) shl 8) or
                (packetBuffer[ihl + 1].toInt() and 0xFF)
            val dstPort = ((packetBuffer[ihl + 2].toInt() and 0xFF) shl 8) or
                (packetBuffer[ihl + 3].toInt() and 0xFF)
            val udpLength = ((packetBuffer[ihl + 4].toInt() and 0xFF) shl 8) or
                (packetBuffer[ihl + 5].toInt() and 0xFF)

            if (dstPort != DNS_PORT || udpLength <= 8 || ihl + udpLength > length) continue

            val dnsQuery = packetBuffer.copyOfRange(ihl + 8, ihl + udpLength)
            val srcIp = packetBuffer.copyOfRange(12, 16)
            val dstIp = packetBuffer.copyOfRange(16, 20)
            val queriedHost = extractDnsQueryHost(dnsQuery)

            // Forward DNS query on a worker coroutine so multiple queries don't block the TUN reader
            serviceScope.launch {
                val startNs = System.nanoTime()
                val dnsResponse = forwardDnsQuery(dnsQuery, node.primaryDns, node.secondaryDns)
                val rttMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)

                if (dnsResponse != null) {
                    val responsePacket = buildIpv4UdpResponsePacket(
                        srcIp = dstIp,
                        dstIp = srcIp,
                        srcPort = DNS_PORT,
                        dstPort = srcPort,
                        payload = dnsResponse,
                        ipId = ipIdentification.getAndIncrement() and 0xFFFF
                    )
                    synchronized(outputStream) {
                        try {
                            outputStream.write(responsePacket)
                        } catch (_: Exception) {
                            // Tunnel closed during write
                        }
                    }
                    VpnStateHolder.recordDnsPacket(
                        bytesSent = dnsQuery.size,
                        bytesReceived = dnsResponse.size,
                        rttMs = rttMs,
                        host = queriedHost
                    )
                }
            }
        }
    }

    private fun forwardDnsQuery(
        query: ByteArray,
        primaryDns: String,
        secondaryDns: String
    ): ByteArray? {
        val candidates = listOf(primaryDns, secondaryDns).distinct()
        for (dnsIp in candidates) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = 2500
                if (!protect(socket)) {
                    socket.close()
                    continue
                }
                val targetAddress = InetAddress.getByName(dnsIp)
                val outPacket = DatagramPacket(query, query.size, targetAddress, DNS_PORT)
                socket.send(outPacket)

                val receiveBuf = ByteArray(4096)
                val inPacket = DatagramPacket(receiveBuf, receiveBuf.size)
                socket.receive(inPacket)
                return receiveBuf.copyOfRange(0, inPacket.length)
            } catch (_: Exception) {
                // Try secondary German/EU DNS server
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    private fun buildIpv4UdpResponsePacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
        ipId: Int
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        // IPv4 Header (20 bytes)
        packet[0] = 0x45.toByte()
        packet[1] = 0x00
        packet[2] = ((totalLength ushr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = ((ipId ushr 8) and 0xFF).toByte()
        packet[5] = (ipId and 0xFF).toByte()
        packet[6] = 0x40.toByte() // Don't fragment
        packet[7] = 0x00
        packet[8] = 64.toByte() // TTL
        packet[9] = UDP_PROTOCOL.toByte()
        packet[10] = 0
        packet[11] = 0
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        val ipChecksum = computeIpv4Checksum(packet, 0, 20)
        packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header (8 bytes)
        packet[20] = ((srcPort ushr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort ushr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        packet[24] = ((udpLength ushr 8) and 0xFF).toByte()
        packet[25] = (udpLength and 0xFF).toByte()
        packet[26] = 0
        packet[27] = 0

        System.arraycopy(payload, 0, packet, 28, payload.size)

        val udpChecksum = computeUdpChecksum(srcIp, dstIp, packet, 20, udpLength)
        packet[26] = ((udpChecksum ushr 8) and 0xFF).toByte()
        packet[27] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    private fun computeIpv4Checksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length) {
            val high = buf[i].toInt() and 0xFF
            val low = if (i + 1 < offset + length) buf[i + 1].toInt() and 0xFF else 0
            sum += ((high shl 8) or low).toLong()
            i += 2
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFFL).toInt()
    }

    private fun computeUdpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        buf: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0L
        // Pseudo-header: srcIp + dstIp + zero + proto(17) + udpLength
        sum += (((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)).toLong()
        sum += (((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)).toLong()
        sum += (((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)).toLong()
        sum += (((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)).toLong()
        sum += UDP_PROTOCOL.toLong()
        sum += udpLength.toLong()

        var i = udpOffset
        val end = udpOffset + udpLength
        while (i < end) {
            val high = buf[i].toInt() and 0xFF
            val low = if (i + 1 < end) buf[i + 1].toInt() and 0xFF else 0
            sum += ((high shl 8) or low).toLong()
            i += 2
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFFL).toInt()
        return if (result == 0) 0xFFFF else result
    }

    private fun extractDnsQueryHost(dnsQuery: ByteArray): String? {
        if (dnsQuery.size <= 13) return null
        return try {
            val labels = mutableListOf<String>()
            var idx = 12
            while (idx < dnsQuery.size) {
                val len = dnsQuery[idx].toInt() and 0xFF
                if (len == 0 || len > 63 || idx + 1 + len > dnsQuery.size) break
                labels.add(String(dnsQuery, idx + 1, len, Charsets.US_ASCII))
                idx += 1 + len
            }
            if (labels.isEmpty()) null else labels.joinToString(".")
        } catch (_: Exception) {
            null
        }
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun stopTunnelInternal() {
        tunnelJob?.cancel()
        tunnelJob = null
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
    }

    private fun stopTunnel() {
        stopTunnelInternal()
        stopForeground(STOP_FOREGROUND_REMOVE)
        VpnStateHolder.setDisconnected()
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_NODE_ID = "extra_node_id"
        const val EXTRA_SPLIT_TUNNEL = "extra_split_tunnel"
        const val EFOOTBALL_PACKAGE = "jp.konami.pesam"

        private const val VPN_CLIENT_IP = "10.88.0.2"
        private const val VPN_DNS_GATEWAY = "10.88.0.1"
        private const val UDP_PROTOCOL = 17
        private const val DNS_PORT = 53
        private const val CHANNEL_ID = "pes_vpn_germany_channel"
        private const val NOTIFICATION_ID = 1042

        fun startConnection(context: Context, nodeId: String, splitTunnel: Boolean) {
            val intent = Intent(context, PesVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_NODE_ID, nodeId)
                putExtra(EXTRA_SPLIT_TUNNEL, splitTunnel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopConnection(context: Context) {
            val intent = Intent(context, PesVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
