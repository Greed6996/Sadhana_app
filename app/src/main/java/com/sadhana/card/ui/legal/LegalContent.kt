package com.sadhana.card.ui.legal

object LegalContent {
    const val PRIVACY_POLICY_TITLE = "Privacy Policy"
    val PRIVACY_POLICY_TEXT = """
        Effective Date: October 2026
        
        1. Offline-First Architecture & Local Storage
        Sadhana Card is built with an offline-first architecture. All daily devotional entries, sleep times, chanting rounds, study minutes, and service records are saved immediately to your device's local database. You can use the app completely offline without ever creating an account.
        
        2. Optional Google Sign-In & Cloud Synchronization
        If you choose to sign in with your Google account, your spiritual practice data and weekly summaries are synchronized with Google Cloud Firestore. This enables seamless recovery across devices and allows authorized spiritual counselors to view your weekly progress. If you do not sign in, no data is ever transmitted off your device.
        
        3. Counselor Access & Spiritual Mentorship
        When signed in as a devotee, your weekly sadhana summaries and daily breakdown become accessible in read-only format to approved spiritual counselors within the community to aid in your spiritual guidance. Counselors cannot modify or edit your records.
        
        4. Data Control & Sign Out
        You have complete control over your account. You can sign out at any time through the app settings, which returns the app to offline-only operation. Clearing app data or uninstalling the app deletes local records.
        
        5. Contact & Support
        For any questions regarding your spiritual records or mentorship program, please consult your spiritual counselor or community mentor.
    """.trimIndent()

    const val TERMS_CONDITIONS_TITLE = "Terms and Conditions"
    val TERMS_CONDITIONS_TEXT = """
        Effective Date: October 2026
        
        1. Devotional Purpose
        Sadhana Card is provided as a devotional and personal regulatory aid for practitioners (sadhakas) following the devotional standards of Gaudiya Vaishnavism and ISKCON as taught by His Divine Grace A.C. Bhaktivedanta Swami Prabhupāda.
        
        2. Voluntary Self-Regulation & Daily Logging
        The scoring metrics and time benchmarks are intended for voluntary self-discipline and reflection. They do not constitute medical, health, or psychological advice. Sadhakas should adjust their schedule under guidance of their mentors if health demands. Daily entries are editable for the current day to preserve accuracy and spiritual integrity.
        
        3. Cloud Sync & Counselor Dashboard
        By signing in with Google, you consent to synchronizing your devotional metrics with our secure cloud database and allowing verified community counselors to review your weekly sadhana completion.
        
        4. Disclaimer of Warranty
        The application is provided "as is", without warranty of any kind, express or implied. The developers are not liable for any data loss resulting from device failure, network disruption, or operating system updates.
        
        5. Acceptance
        By proceeding and accepting these terms, you acknowledge that you understand and agree to these terms for your personal spiritual practice.
    """.trimIndent()
}
