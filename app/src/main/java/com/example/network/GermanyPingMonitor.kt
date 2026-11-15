package com.example.network

import com.example.model.ServerNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.random.Random

data class ProbeResult(
    val avgPingMs: Int?,
    val jitterMs: Int?,
    val packetLossPercent: Int
)

data class GermanyWindowInfo(
    val formattedTime: String,
    val score: Int,
    val statusAr: String,
    val statusEn: String
)

object GermanyPingMonitor {

    /**
     * Measures real round-trip latency to a DNS/routing server by sending real UDP DNS queries
     * (falling back to TCP socket connect on port 53 if UDP is restricted on the network).
     */
    suspend fun probeNode(node: ServerNode, attempts: Int = 3): ProbeResult =
        withContext(Dispatchers.IO) {
            val samples = mutableListOf<Int>()
            repeat(attempts) {
                val rtt = measureSingleDnsUdpRtt(node.primaryDns)
                    ?: measureSingleTcpRtt(node.primaryDns, 53)
                if (rtt != null) {
                    samples.add(rtt)
                }
            }

            if (samples.isEmpty()) {
                return@withContext ProbeResult(
                    avgPingMs = null,
                    jitterMs = null,
                    packetLossPercent = 100
                )
            }

            val avg = samples.average().toInt().coerceAtLeast(1)
            val jitter = if (samples.size > 1) {
                var diffSum = 0
                for (i in 1 until samples.size) {
                    diffSum += abs(samples[i] - samples[i - 1])
                }
                (diffSum / (samples.size - 1)).coerceAtLeast(1)
            } else {
                2
            }
            val lossPercent = (((attempts - samples.size).toDouble() / attempts) * 100).toInt()
            ProbeResult(avgPingMs = avg, jitterMs = jitter, packetLossPercent = lossPercent)
        }

    private fun measureSingleDnsUdpRtt(dnsIp: String): Int? {
        var socket: DatagramSocket? = null
        return try {
            val query = buildStandardDnsQuery()
            val address = InetAddress.getByName(dnsIp)
            socket = DatagramSocket()
            socket.soTimeout = 1600
            val outPacket = DatagramPacket(query, query.size, address, 53)
            val receiveBuf = ByteArray(512)
            val inPacket = DatagramPacket(receiveBuf, receiveBuf.size)

            val startNs = System.nanoTime()
            socket.send(outPacket)
            socket.receive(inPacket)
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt()
            elapsedMs.coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun measureSingleTcpRtt(hostIp: String, port: Int): Int? {
        var socket: Socket? = null
        return try {
            socket = Socket()
            val startNs = System.nanoTime()
            socket.connect(InetSocketAddress(hostIp, port), 1600)
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt()
            elapsedMs.coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Constructs a minimal standard DNS A-record query for example.com (29 bytes).
     */
    private fun buildStandardDnsQuery(): ByteArray {
        val txId = Random.nextInt(1, 65535)
        return byteArrayOf(
            ((txId ushr 8) and 0xFF).toByte(), (txId and 0xFF).toByte(), // Transaction ID
            0x01, 0x00, // Standard query, recursion desired
            0x00, 0x01, // QDCOUNT = 1
            0x00, 0x00, // ANCOUNT = 0
            0x00, 0x00, // NSCOUNT = 0
            0x00, 0x00, // ARCOUNT = 0
            0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(),
            'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0x00,       // End of QNAME
            0x00, 0x01, // QTYPE = A
            0x00, 0x01  // QCLASS = IN
        )
    }

    fun getFrankfurtWindowInfo(): GermanyWindowInfo {
        val berlinTz = TimeZone.getTimeZone("Europe/Berlin")
        val cal = Calendar.getInstance(berlinTz)
        val formatter = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = berlinTz
        }
        val formattedTime = formatter.format(cal.time)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 1..7 -> GermanyWindowInfo(
                formattedTime = formattedTime,
                score = 20,
                statusAr = "نافذة هدوء الفجر في فرانكفورت (ضغط منخفض جداً)",
                statusEn = "Frankfurt off-peak dawn window (minimal load)"
            )
            in 8..15 -> GermanyWindowInfo(
                formattedTime = formattedTime,
                score = 16,
                statusAr = "استقرار نهاري متوازن على سيرفر ألمانيا",
                statusEn = "Balanced daytime stability on Germany route"
            )
            in 16..22 -> GermanyWindowInfo(
                formattedTime = formattedTime,
                score = 12,
                statusAr = "ساعات الذروة الأوروبية (التوجيه المباشر مفيد)",
                statusEn = "EU peak hours (direct German DNS route active)"
            )
            else -> GermanyWindowInfo(
                formattedTime = formattedTime,
                score = 18,
                statusAr = "بداية هدوء السيرفرات الأوروبية الليلي",
                statusEn = "Late-night EU server calm window"
            )
        }
    }
}
