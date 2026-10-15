package com.sadhana.card.data.update

/**
 * Remote update configuration schema for manually approved in-app updates.
 */
data class UpdateConfig(
    val updateEnabled: Boolean,
    val approvedVersion: String,
    val approvedVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val updateMessage: String
) {
    companion object {
        const val UPDATE_CONFIG_URL =
            "https://raw.githubusercontent.com/Greed6996/Sadhana_app/main/update.json"

        const val EXPECTED_PACKAGE_NAME = "com.sadhana.card"
        const val EXPECTED_SIGNATURE_SHA256 =
            "75e471ee7feb55afe62bdc75f4f14a69a76c49a5c184e4664e60a6676886b5aa"

        /**
         * Safely parses JSON string without external library dependencies,
         * ensuring compatibility across both Android runtime and JVM unit tests.
         */
        fun fromJson(jsonStr: String): UpdateConfig? {
            if (jsonStr.isBlank() || !jsonStr.trim().startsWith("{") || !jsonStr.trim().endsWith("}")) {
                return null
            }
            return try {
                fun extractString(key: String): String {
                    val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
                    val raw = pattern.find(jsonStr)?.groupValues?.get(1) ?: return ""
                    return raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "")
                }

                fun extractInt(key: String): Int {
                    val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
                    return pattern.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                }

                fun extractBoolean(key: String): Boolean {
                    val pattern = Regex("\"$key\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
                    return pattern.find(jsonStr)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: false
                }

                val config = UpdateConfig(
                    updateEnabled = extractBoolean("updateEnabled"),
                    approvedVersion = extractString("approvedVersion"),
                    approvedVersionCode = extractInt("approvedVersionCode"),
                    downloadUrl = extractString("downloadUrl"),
                    releaseNotes = extractString("releaseNotes"),
                    updateMessage = extractString("updateMessage")
                )
                if (config.approvedVersion.isBlank() && config.approvedVersionCode == -1) {
                    null
                } else {
                    config
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Checks if this configuration is structurally valid.
     */
    val isValid: Boolean
        get() = approvedVersion.isNotBlank() &&
                approvedVersionCode > 0 &&
                downloadUrl.startsWith("https://")
}
