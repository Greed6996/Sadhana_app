package com.sadhana.card.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateConfigTest {

    @Test
    fun testParseValidUpdateConfig() {
        val json = """
            {
              "updateEnabled": true,
              "approvedVersion": "1.0.13",
              "approvedVersionCode": 13,
              "downloadUrl": "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.13/Sadhana-Card.apk",
              "releaseNotes": "Category-based scoring and Sanskrit dedication.",
              "updateMessage": "A new version of My Sadhana Card is available."
            }
        """.trimIndent()

        val config = UpdateConfig.fromJson(json)
        assertNotNull(config)
        config!!
        assertTrue(config.updateEnabled)
        assertEquals("1.0.13", config.approvedVersion)
        assertEquals(13, config.approvedVersionCode)
        assertEquals(
            "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.13/Sadhana-Card.apk",
            config.downloadUrl
        )
        assertEquals("Category-based scoring and Sanskrit dedication.", config.releaseNotes)
        assertEquals("A new version of My Sadhana Card is available.", config.updateMessage)
        assertTrue(config.isValid)
    }

    @Test
    fun testParseDisabledUpdateConfig() {
        val json = """
            {
              "updateEnabled": false,
              "approvedVersion": "1.0.13",
              "approvedVersionCode": 13,
              "downloadUrl": "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.13/Sadhana-Card.apk",
              "releaseNotes": "",
              "updateMessage": ""
            }
        """.trimIndent()

        val config = UpdateConfig.fromJson(json)
        assertNotNull(config)
        assertFalse(config!!.updateEnabled)
        assertTrue(config.isValid)
    }

    @Test
    fun testInvalidUrlsAndEmptyVersionsAreInvalid() {
        val invalidUrlJson = """
            {
              "updateEnabled": true,
              "approvedVersion": "1.0.13",
              "approvedVersionCode": 13,
              "downloadUrl": "http://insecure.site/apk.apk",
              "releaseNotes": "",
              "updateMessage": ""
            }
        """.trimIndent()
        val config1 = UpdateConfig.fromJson(invalidUrlJson)
        assertNotNull(config1)
        assertFalse(config1!!.isValid)

        val invalidCodeJson = """
            {
              "updateEnabled": true,
              "approvedVersion": "1.0.13",
              "approvedVersionCode": -1,
              "downloadUrl": "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.13/Sadhana-Card.apk"
            }
        """.trimIndent()
        val config2 = UpdateConfig.fromJson(invalidCodeJson)
        assertNotNull(config2)
        assertFalse(config2!!.isValid)
    }

    @Test
    fun testMalformedJsonReturnsNull() {
        val malformed = "not a json object"
        assertNull(UpdateConfig.fromJson(malformed))

        val empty = ""
        assertNull(UpdateConfig.fromJson(empty))
    }

    @Test
    fun testSecurityConstants() {
        assertEquals("com.sadhana.card", UpdateConfig.EXPECTED_PACKAGE_NAME)
        assertEquals(
            "75e471ee7feb55afe62bdc75f4f14a69a76c49a5c184e4664e60a6676886b5aa",
            UpdateConfig.EXPECTED_SIGNATURE_SHA256
        )
        assertEquals(
            "https://raw.githubusercontent.com/Greed6996/Sadhana_app/main/update.json",
            UpdateConfig.UPDATE_CONFIG_URL
        )
    }

    @Test
    fun testUpgradeEligibilityRule() {
        // Installed app is at versionCode 12
        val currentVersionCode = 12

        // Config offers approved versionCode 13 with updateEnabled = true
        val approvedUpdate = UpdateConfig(
            updateEnabled = true,
            approvedVersion = "1.0.13",
            approvedVersionCode = 13,
            downloadUrl = "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.13/Sadhana-Card.apk",
            releaseNotes = "Notes",
            updateMessage = "Message"
        )
        val shouldOffer1 = approvedUpdate.updateEnabled && approvedUpdate.approvedVersionCode > currentVersionCode
        assertTrue(shouldOffer1)

        // If installed app is already at 13 -> do not offer
        val shouldOffer2 = approvedUpdate.updateEnabled && approvedUpdate.approvedVersionCode > 13
        assertFalse(shouldOffer2)

        // If installed app is newer (e.g. 14) -> do not offer downgrade
        val shouldOffer3 = approvedUpdate.updateEnabled && approvedUpdate.approvedVersionCode > 14
        assertFalse(shouldOffer3)

        // If updates are disabled -> do not offer
        val disabledUpdate = approvedUpdate.copy(updateEnabled = false)
        val shouldOffer4 = disabledUpdate.updateEnabled && disabledUpdate.approvedVersionCode > currentVersionCode
        assertFalse(shouldOffer4)
    }

    @Test
    fun testApkSizeParsingAndFormatting() {
        val json = """
            {
              "updateEnabled": true,
              "approvedVersion": "1.0.28",
              "approvedVersionCode": 28,
              "downloadUrl": "https://github.com/Greed6996/Sadhana_app/releases/download/v1.0.28/Sadhana-Card.apk",
              "releaseNotes": "Notes",
              "updateMessage": "Message",
              "apkSizeMb": "14.2 MB"
            }
        """.trimIndent()
        val config = UpdateConfig.fromJson(json)
        assertNotNull(config)
        assertEquals("14.2 MB", config?.apkSizeMb)
        assertEquals("14.2 MB", config?.apkSizeText)

        // Test numeric normalization
        assertEquals("15.0 MB", UpdateConfig.formatApkSizeMb("15"))
        assertEquals("14.2 MB", UpdateConfig.formatApkSizeMb("14.2"))
        assertEquals("14.2 MB", UpdateConfig.formatApkSizeMb("14.2 mb"))
        assertNull(UpdateConfig.formatApkSizeMb(null))
        assertNull(UpdateConfig.formatApkSizeMb(""))
    }
}
