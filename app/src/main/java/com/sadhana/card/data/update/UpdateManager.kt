package com.sadhana.card.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sadhana.card.BuildConfig
import com.sadhana.card.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val config: UpdateConfig, val apkSizeText: String? = config.apkSizeText) : UpdateState
    data class Downloading(val progressPercent: Int, val config: UpdateConfig) : UpdateState
    data class ReadyToInstall(val apkFile: File, val config: UpdateConfig) : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        const val COOLDOWN_MS = 24 * 60 * 60 * 1000L
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * Checks for approved updates from the remote update-control configuration.
     * @param isManualCheck Explicit check by user (e.g. from Settings screen). If true, ignores the 24-hour snooze cooldown.
     */
    suspend fun checkForUpdates(isManualCheck: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        try {
            val config = fetchRemoteConfig()
            if (config == null || !config.isValid) {
                val state = if (isManualCheck) UpdateState.Error("Unable to retrieve valid update information.") else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            if (!config.updateEnabled) {
                val state = if (isManualCheck) UpdateState.UpToDate(BuildConfig.VERSION_NAME) else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            if (config.approvedVersionCode <= currentVersionCode) {
                val state = if (isManualCheck) UpdateState.UpToDate(BuildConfig.VERSION_NAME) else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            // 24-Hour "Maybe Later" Snooze Cooldown with Smart Edge Cases
            if (!isManualCheck) {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val lastDismissedCode = prefs.lastDismissedVersionCode
                val lastDismissedTimestamp = prefs.lastDismissedTimestamp
                val now = System.currentTimeMillis()

                // Smart Edge Case 1: Newer Urgent Release Bypass:
                // Only suppress if the remote approvedVersionCode <= lastDismissedCode.
                // If remote approvedVersionCode > lastDismissedCode, bypass cooldown immediately!
                if (config.approvedVersionCode <= lastDismissedCode) {
                    val elapsed = now - lastDismissedTimestamp
                    if (elapsed in 0 until COOLDOWN_MS) {
                        _updateState.value = UpdateState.Idle
                        return@withContext UpdateState.Idle
                    }
                }
            }

            // Dynamic APK File Size in MB: HTTP HEAD request or optional apkSizeMb from config
            val remoteSize = fetchApkSize(config.downloadUrl)
            val resolvedSize = remoteSize ?: config.apkSizeText
            val resolvedConfig = if (resolvedSize != null) {
                config.copy(apkSizeText = resolvedSize)
            } else {
                config
            }

            val state = UpdateState.Available(config = resolvedConfig, apkSizeText = resolvedConfig.apkSizeText)
            _updateState.value = state
            state
        } catch (e: Exception) {
            val state = if (isManualCheck) UpdateState.Error(e.message ?: "Network error checking for updates.") else UpdateState.Idle
            _updateState.value = state
            state
        }
    }

    /**
     * User chose "Maybe Later". Store the dismissed version code and timestamp for 24-hour snooze cooldown.
     */
    suspend fun dismissUpdate(config: UpdateConfig) {
        _updateState.value = UpdateState.Idle
        userPreferencesRepository.recordDismissedUpdate(
            versionCode = config.approvedVersionCode,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun dismissUpdate(versionCode: Int) {
        _updateState.value = UpdateState.Idle
        userPreferencesRepository.recordDismissedUpdate(
            versionCode = versionCode,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Resets state to Idle.
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Downloads and validates the approved APK binary.
     */
    suspend fun downloadUpdate(config: UpdateConfig): Result<File> = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Downloading(0, config)
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            val targetFile = File(updatesDir, "Sadhana-Card-v${config.approvedVersion}.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val url = URL(config.downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "SadhanaCard-Android/${BuildConfig.VERSION_NAME}")
            }
            connection.connect()

            if (connection.responseCode !in 200..299) {
                val err = "Download failed with HTTP response ${connection.responseCode}"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val totalBytes = connection.contentLength
            val inputStream = BufferedInputStream(connection.inputStream)
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                if (totalBytes > 0) {
                    val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                    _updateState.value = UpdateState.Downloading(percent, config)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Validate downloaded APK
            val validationError = validateApk(targetFile, config)
            if (validationError != null) {
                targetFile.delete()
                val err = "Security verification failed: $validationError"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(SecurityException(err))
            }

            _updateState.value = UpdateState.ReadyToInstall(targetFile, config)
            Result.success(targetFile)
        } catch (e: Exception) {
            val err = e.message ?: "Failed to download update."
            _updateState.value = UpdateState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Launches Android Package Installer or Unknown Sources Settings if permission is needed.
     */
    fun startInstall(apkFile: File) {
        if (!apkFile.exists()) {
            _updateState.value = UpdateState.Error("APK file not found on disk.")
            return
        }

        // Check Unknown App Install permission on Android 8.0 (API 26) +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Could not launch package installer: ${e.message}")
        }
    }

    /**
     * Validates that the downloaded APK matches package name, version code, and production signature fingerprint.
     * Returns null if valid, or an error description string if invalid.
     */
    fun validateApk(apkFile: File, config: UpdateConfig): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return "Corrupted or unreadable APK binary."

        if (packageInfo.packageName != UpdateConfig.EXPECTED_PACKAGE_NAME) {
            return "Package name mismatch: found ${packageInfo.packageName}, expected ${UpdateConfig.EXPECTED_PACKAGE_NAME}"
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }

        if (apkVersionCode != config.approvedVersionCode) {
            return "Version code mismatch: found $apkVersionCode, expected ${config.approvedVersionCode}"
        }

        // Verify SHA-256 fingerprint
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        if (signatures.isNullOrEmpty()) {
            return "No signature found in APK archive."
        }

        val certBytes = signatures[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        val actualFingerprint = digest.joinToString("") { "%02x".format(it) }

        if (!actualFingerprint.equals(UpdateConfig.EXPECTED_SIGNATURE_SHA256, ignoreCase = true)) {
            return "Certificate fingerprint mismatch: APK is NOT signed with the permanent production key."
        }

        return null
    }

    private fun fetchRemoteConfig(): UpdateConfig? {
        val url = URL(UpdateConfig.UPDATE_CONFIG_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
        }

        return try {
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                UpdateConfig.fromJson(content)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Performs a lightweight background HTTP HEAD request to obtain the APK file size in MB.
     * Follows HTTP redirects (301, 302, 303, 307, 308) to support AWS S3/CDN downloads from GitHub Releases.
     */
    private fun fetchApkSize(downloadUrl: String): String? {
        var currentUrl = downloadUrl
        var redirects = 0
        while (redirects < 5) {
            try {
                val url = URL(currentUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 6000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "SadhanaCard-Android/${BuildConfig.VERSION_NAME}")
                }
                connection.connect()
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(url, location).toString()
                        }
                        redirects++
                        continue
                    }
                    return null
                }
                if (code in 200..299) {
                    val lengthHeader = connection.getHeaderField("Content-Length")?.toLongOrNull()
                    val totalBytes = lengthHeader ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        connection.contentLengthLong
                    } else {
                        connection.contentLength.toLong()
                    }
                    connection.disconnect()
                    if (totalBytes > 0L) {
                        val mb = totalBytes / (1024.0 * 1024.0)
                        return String.format(Locale.US, "%.1f MB", mb)
                    }
                }
                connection.disconnect()
                return null
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }
}
