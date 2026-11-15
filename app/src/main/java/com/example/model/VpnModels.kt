package com.example.model

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSION_REQUIRED,
    ERROR
}

enum class PackTier(val labelAr: String, val labelEn: String) {
    EPIC_SHOWTIME("إيبك / شو تايم", "Epic / Showtime"),
    FEATURED("مميّز / نجوم الأسبوع", "Featured / POTW"),
    STANDARD("عادي", "Standard")
}

data class ServerNode(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val cityAr: String,
    val cityEn: String,
    val providerTag: String,
    val primaryDns: String,
    val secondaryDns: String,
    val isDefaultGermany: Boolean = false,
    val measuredPingMs: Int? = null,
    val jitterMs: Int? = null,
    val packetLossPercent: Int = 0,
    val isProbing: Boolean = false
)

object GermanServerCatalog {
    val defaultNodes = listOf(
        ServerNode(
            id = "de-frankfurt-main",
            nameAr = "ألمانيا — فرانكفورت (تلقائي)",
            nameEn = "Germany — Frankfurt (Auto)",
            cityAr = "فرانكفورت • المسار الأساسي",
            cityEn = "Frankfurt • Primary Route",
            providerTag = "DNS.Watch DE",
            primaryDns = "84.200.69.80",
            secondaryDns = "84.200.70.40",
            isDefaultGermany = true
        ),
        ServerNode(
            id = "de-berlin-core",
            nameAr = "ألمانيا — برلين الاحتياطي",
            nameEn = "Germany — Berlin Core",
            cityAr = "برلين • نود مزدوج",
            cityEn = "Berlin • Dual Node",
            providerTag = "DNS.Watch DE-2",
            primaryDns = "84.200.70.40",
            secondaryDns = "84.200.69.80"
        ),
        ServerNode(
            id = "de-frankfurt-privacy",
            nameAr = "ألمانيا — سيرفر الخصوصية DE",
            nameEn = "Germany — Privacy Node DE",
            cityAr = "فرانكفورت • تشفير بدون سجل",
            cityEn = "Frankfurt • No-Log DNS",
            providerTag = "Mullvad DE",
            primaryDns = "194.242.2.2",
            secondaryDns = "84.200.69.80"
        ),
        ServerNode(
            id = "de-munich-gaming",
            nameAr = "ألمانيا — ميونخ للألعاب",
            nameEn = "Germany — Munich Gaming",
            cityAr = "ميونخ • استجابة سريعة",
            cityEn = "Munich • Anycast EU",
            providerTag = "Quad9 EU",
            primaryDns = "9.9.9.9",
            secondaryDns = "149.112.112.112"
        ),
        ServerNode(
            id = "de-dusseldorf-edge",
            nameAr = "ألمانيا — دوسلدورف فائق السرعة",
            nameEn = "Germany — Düsseldorf Fast",
            cityAr = "دوسلدورف • أقل بنق",
            cityEn = "Düsseldorf • Ultra Edge",
            providerTag = "Cloudflare EU",
            primaryDns = "1.1.1.1",
            secondaryDns = "1.0.0.1"
        )
    )

    fun findById(id: String): ServerNode =
        defaultNodes.firstOrNull { it.id == id } ?: defaultNodes.first()
}

data class VpnTelemetry(
    val state: VpnConnectionState = VpnConnectionState.DISCONNECTED,
    val activeNode: ServerNode = GermanServerCatalog.defaultNodes.first(),
    val connectedSinceEpochMs: Long? = null,
    val dnsQueriesHandled: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val lastDnsLatencyMs: Int? = null,
    val lastResolvedHost: String? = null,
    val isSplitTunnelActive: Boolean = false,
    val errorMessage: String? = null
)

data class LuckMetrics(
    val overallScore: Int = 20,
    val tunnelScore: Int = 0,
    val pingScore: Int = 0,
    val jitterScore: Int = 0,
    val windowScore: Int = 20,
    val germanyTimeFormatted: String = "--:--",
    val windowStatusAr: String = "جارٍ فحص التوقيت الألماني...",
    val windowStatusEn: String = "Checking Frankfurt server time...",
    val isBoostRunning: Boolean = false,
    val lastBurstSummaryAr: String? = null,
    val lastBurstSummaryEn: String? = null
)

data class PackPullRecord(
    val id: Long,
    val packName: String,
    val tier: PackTier,
    val coinsSpent: Int,
    val pingAtPullMs: Int?,
    val timestampMs: Long
)

data class AppSettings(
    val autoConnectGermany: Boolean = true,
    val selectedNodeId: String = GermanServerCatalog.defaultNodes.first().id,
    val splitTunnelEfootballOnly: Boolean = false,
    val luckStabilityMode: Boolean = true,
    val isArabic: Boolean = true
)
