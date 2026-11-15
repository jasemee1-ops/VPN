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

class PesVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

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
        val bypassEfootball = intent?.getBooleanExtra(EXTRA_BYPASS_EFOOTBALL, true) ?: true
        val node = GermanServerCatalog.findById(nodeId)

        VpnStateHolder.userManuallyDisconnected = false
        startForegroundWithNotification(node)
        startTunnel(node, bypassEfootball)
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

    private fun startTunnel(node: ServerNode, bypassEfootball: Boolean) {
        stopTunnelInternal()
        VpnStateHolder.setConnecting(node, bypassEfootball)

        try {
            // Configure non-blocking DNS servers + allowBypass() without hijacking 10.88.0.1 UDP-only,
            // so Android Private DNS (DoT 853), TCP DNS, and eFootball Konami sockets never stall or fail.
            val builder = Builder()
                .setSession(node.nameEn)
                .setMtu(1500)
                .addAddress(VPN_CLIENT_IP, 32)
                .addDnsServer(node.primaryDns)
                .addDnsServer(node.secondaryDns)
                .allowBypass()

            var efootballExcluded = false
            if (bypassEfootball && isPackageInstalled(EFOOTBALL_PACKAGE)) {
                try {
                    builder.addDisallowedApplication(EFOOTBALL_PACKAGE)
                    efootballExcluded = true
                } catch (_: PackageManager.NameNotFoundException) {
                    efootballExcluded = false
                }
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                // Ignore if package resolution fails
            }

            val descriptor = builder.establish()
            if (descriptor == null) {
                VpnStateHolder.setPermissionRequired()
                stopSelf()
                return
            }

            tunInterface = descriptor
            VpnStateHolder.setConnected(node, efootballExcluded)
        } catch (ex: Exception) {
            VpnStateHolder.setDisconnected(ex.message ?: "VPN interface initialization failed")
            stopSelf()
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
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_NODE_ID = "extra_node_id"
        const val EXTRA_BYPASS_EFOOTBALL = "extra_bypass_efootball"
        const val EFOOTBALL_PACKAGE = "jp.konami.pesam"

        private const val VPN_CLIENT_IP = "10.88.0.2"
        private const val CHANNEL_ID = "pes_vpn_germany_channel"
        private const val NOTIFICATION_ID = 1042

        fun startConnection(context: Context, nodeId: String, bypassEfootball: Boolean) {
            val intent = Intent(context, PesVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_NODE_ID, nodeId)
                putExtra(EXTRA_BYPASS_EFOOTBALL, bypassEfootball)
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
