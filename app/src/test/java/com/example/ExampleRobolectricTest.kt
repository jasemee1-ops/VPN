package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GermanServerCatalog
import com.example.network.GermanyPingMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read updated app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PES VPN ألمانيا", appName)
    }

    @Test
    fun `default server node is Germany Frankfurt`() {
        val defaultNode = GermanServerCatalog.defaultNodes.first()
        assertEquals("de-frankfurt-main", defaultNode.id)
        assertTrue(defaultNode.isDefaultGermany)
        assertEquals("1.1.1.1", defaultNode.primaryDns)
    }

    @Test
    fun `frankfurt window info returns valid time and score`() {
        val info = GermanyPingMonitor.getFrankfurtWindowInfo()
        assertTrue(info.formattedTime.contains(":"))
        assertTrue(info.score in 1..20)
    }
}
