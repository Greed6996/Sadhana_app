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
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val config: UpdateConfig) : UpdateState
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
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * Checks for approved updates from the remote update-control configuration.
     * @param isManual Explicit check by user (e.g. from Settings screen). If true, ignores previously dismissed version.
     */
    suspend fun checkForUpdates(isManual: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        try {
            val config = fetchRemoteConfig()
            if (config == null || !config.isValid) {
                val state = if (isManual) UpdateState.Error("Unable to retrieve valid update information.") else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            if (!config.updateEnabled) {
                val state = if (isManual) UpdateState.UpToDate(BuildConfig.VERSION_NAME) else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            if (config.approvedVersionCode <= currentVersionCode) {
                val state = if (isManual) UpdateState.UpToDate(BuildConfig.VERSION_NAME) else UpdateState.Idle
                _updateState.value = state
                return@withContext state
            }

            // If not manual, respect previously dismissed version
            if (!isManual) {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                if (config.approvedVersionCode <= prefs.dismissedUpdateVersion) {
                    _updateState.value = UpdateState.Idle
                    return@withContext UpdateState.Idle
                }
            }

            val state = UpdateState.Available(config)
            _updateState.value = state
            state
        } catch (e: Exception) {
            val state = if (isManual) UpdateState.Error(e.message ?: "Network error checking for updates.") else UpdateState.Idle
            _updateState.value = state
            state
        }
    }

    /**
     * User chose "Later". Store the dismissed version code so they aren't interrupted repeatedly.
     */
    suspend fun dismissUpdate(config: UpdateConfig) {
        _updateState.value = UpdateState.Idle
        userPreferencesRepository.setDismissedUpdateVersion(config.approvedVersionCode)
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
}
