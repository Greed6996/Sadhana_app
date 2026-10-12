package com.sadhana.card.ui.legal

object LegalContent {
    const val PRIVACY_POLICY_TITLE = "Privacy Policy"
    val PRIVACY_POLICY_TEXT = """
        Effective Date: October 2026
        
        1. 100% Offline & Device-Only Storage
        Sadhana Card is strictly an offline-first application. It does not request or possess network permissions (android.permission.INTERNET is completely absent). All records, including sleep times, wake-up times, chanting rounds, study minutes, service records, sadhaka names, and counselor names, are stored solely in an encrypted-capable local SQLite database on your physical device.
        
        2. No Personal Data Collection or Transmission
        We do not collect, transmit, sell, or analyze your personal information. There are no tracking scripts, advertising SDKs, analytics frameworks, or cloud backups.
        
        3. Local Data Deletion & Control
        You have exclusive and absolute control over your information. Clearing the application's data through Android Settings or uninstalling the app permanently erases all stored records from your device.
        
        4. Permissions
        This application requires zero runtime permissions (no camera, no microphone, no location, no contacts, no network).
        
        5. Contact & Support
        For any inquiries or feedback regarding your spiritual records, consult your spiritual counselor or mentor locally.
    """.trimIndent()

    const val TERMS_CONDITIONS_TITLE = "Terms and Conditions"
    val TERMS_CONDITIONS_TEXT = """
        Effective Date: October 2026
        
        1. Devotional Purpose
        Sadhana Card is provided as a devotional and personal regulatory aid for practitioners (sadhakas) following the devotional standards of Gaudiya Vaishnavism and ISKCON as taught by His Divine Grace A.C. Bhaktivedanta Swami Prabhupāda.
        
        2. Voluntary Self-Regulation
        The scoring metrics and time benchmarks (e.g., bedtime before 21:30, wake-up before 03:30, and completing 16 rounds before 07:15) are intended for voluntary self-discipline and reflection. They do not constitute medical, health, or psychological advice. Sadhakas should adjust their schedule under guidance of their mentors if health demands.
        
        3. Disclaimer of Warranty
        The application is provided "as is", without warranty of any kind, express or implied. The developers are not liable for any data loss resulting from device failure, operating system updates, or device clearing.
        
        4. Acceptance
        By ticking the consent checkboxes and proceeding, you acknowledge that you have read, understood, and agreed to these terms for your personal spiritual practice.
    """.trimIndent()
}
