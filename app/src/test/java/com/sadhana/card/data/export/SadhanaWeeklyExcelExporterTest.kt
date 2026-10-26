package com.sadhana.card.data.export

import android.content.Context
import android.content.ContextWrapper
import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipInputStream

class SadhanaWeeklyExcelExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var exporter: SadhanaWeeklyExcelExporter

    @Before
    fun setUp() {
        mockContext = ContextWrapper(null)
        exporter = SadhanaWeeklyExcelExporter(mockContext)
    }

    @Test
    fun testSafeFileNameGeneration() {
        val startOfWeek = LocalDate.of(2026, 10, 11) // Sunday to Oct 17 Saturday

        assertEquals(
            "Sadhana_Report_Sundar_Gopal_Das_2026-10-11_to_2026-10-17.xlsx",
            exporter.buildSafeFileName("Sundar Gopal Das", startOfWeek)
        )

        assertEquals(
            "Sadhana_Report_Sadhaka_2026-10-11_to_2026-10-17.xlsx",
            exporter.buildSafeFileName("   ", startOfWeek)
        )

        assertEquals(
            "Sadhana_Report_Radha_Krishna_Das_2026-10-11_to_2026-10-17.xlsx",
            exporter.buildSafeFileName("Radha-Krishna (Das)!", startOfWeek)
        )
    }

    @Test
    fun testWorksheetXmlGeneration_populatesAllFieldsAndReplacesAllPlaceholders() {
        val startOfWeek = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            SadhanaEntry(
                date = startOfWeek, // Sunday
                bedtime = LocalTime.of(21, 15), // <= 21:30 -> 25 pts
                wakeTime = LocalTime.of(2, 45), // < 03:00 -> 25 pts
                napMinutes = 25, // <= 30 -> 25 pts
                trainJourney = false,
                japaTime = LocalTime.of(7, 0), // < 09:00 -> 25 pts
                pathanMinutes = 45,
                sravanMinutes = 30
            ),
            SadhanaEntry(
                date = startOfWeek.plusDays(1), // Monday
                bedtime = LocalTime.of(21, 50), // 21:31-22:00 -> 20 pts
                wakeTime = LocalTime.of(3, 15), // 03:00-03:30 -> 20 pts
                napMinutes = 80, // > 60 -> 0 pts
                trainJourney = true,
                japaTime = LocalTime.of(8, 30), // <= 09:00 -> 25 pts
                pathanMinutes = 30,
                sravanMinutes = 45
            )
        )

        val xml = exporter.buildWorksheetXml(
            sadhakaName = "Bhakta Krishna",
            counselorName = "H.G. Sundar Gopal Das",
            startOfWeek = startOfWeek,
            entries = entries
        )

        // Verify no placeholders from template remain
        assertFalse("Template placeholder [enter value] was found", xml.contains("[enter value]"))
        assertFalse("Template placeholder [points] was found", xml.contains("[points]"))
        assertFalse("Template placeholder [minutes] was found", xml.contains("[minutes]"))
        assertFalse("Template placeholder [activity] was found", xml.contains("[activity]"))
        assertFalse("Template placeholder [total] was found", xml.contains("[total]"))
        assertFalse("Template placeholder [Sadhaka Name] was found", xml.contains("[Sadhaka Name]"))
        assertFalse("Template placeholder [Counselor Name] was found", xml.contains("[Counselor Name]"))
        assertFalse("Template placeholder [Sunday Date] was found", xml.contains("[Sunday Date]"))
        assertFalse("Template placeholder [Saturday Date] was found", xml.contains("[Saturday Date]"))

        // Verify headers, dedication, and verse
        assertTrue("Header Sri Sri Guru Gouranga Jayate missing", xml.contains("Sri Sri Guru Gouranga Jayate"))
        assertTrue("Name Bhakta Krishna missing", xml.contains("Name: Bhakta Krishna"))
        assertTrue("Counselor H.G. Sundar Gopal Das missing", xml.contains("Counselor: H.G. Sundar Gopal Das"))
        assertTrue("Dates range missing", xml.contains("Dates: From Sun, 11 Oct 2026 to Sat, 17 Oct 2026"))
        assertTrue("Title missing", xml.contains("Sadhana Card for Sadhaka"))
        assertTrue("Dedication missing", xml.contains("ISKCON BONGAIGAON NAMHATTA Sangha"))
        assertTrue("Verse missing", xml.contains("yukta ahara viharasya"))

        // Verify Sunday data populated accurately
        assertTrue("Bedtime 9:15 PM missing", xml.contains("9:15 PM"))
        assertTrue("Wake time 2:45 AM missing", xml.contains("2:45 AM"))
        assertTrue("Nap 25 mins missing", xml.contains("25 mins"))
        assertTrue("Japa 7:00 AM missing", xml.contains("7:00 AM"))
        assertTrue("Pathan 45 mins missing", xml.contains("45 mins"))
        assertTrue("Sravan 30 mins missing", xml.contains("30 mins"))
        assertTrue("Sunday total score 150 / 150 missing", xml.contains("150 / 150"))

        // Verify Monday train journey bonus
        assertTrue("Train journey bonus missing", xml.contains("+60 mins bonus"))

        // Verify summary row
        assertTrue("Summary total score missing", xml.contains("Total Score = 265 / 1050 pts"))
    }

    @Test
    fun testWorksheetXmlGeneration_exportsCategoryLabelsAccurately() {
        val startOfWeek = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            SadhanaEntry(
                date = startOfWeek, // Sunday
                bedtimeCategory = BedtimeCategory.BY_9_30_PM,
                wakeUpCategory = WakeUpCategory.BEFORE_3_00_AM,
                daySleepCategory = DaySleepCategory.UP_TO_30_MIN,
                japaCategory = JapaCategory.BY_9_00_AM,
                japaCompleted = true,
                pathanMinutes = 45,
                sravanMinutes = 30
            ),
            SadhanaEntry(
                date = startOfWeek.plusDays(1), // Monday with incomplete japa
                bedtimeCategory = BedtimeCategory.BEFORE_10_00_PM,
                wakeUpCategory = WakeUpCategory.BEFORE_3_30_AM,
                daySleepCategory = DaySleepCategory.FROM_31_TO_45_MIN,
                japaCategory = JapaCategory.AFTER_9_00_PM_OR_INCOMPLETE,
                japaCompleted = false,
                pathanMinutes = 30,
                sravanMinutes = 45
            )
        )

        val xml = exporter.buildWorksheetXml(
            sadhakaName = "Arjuna Das",
            counselorName = "H.G. Gauranga Das",
            startOfWeek = startOfWeek,
            entries = entries
        )

        // Verify Sunday category labels and 150/150 points
        assertTrue("Bedtime category label missing", xml.contains("9:30 PM or before"))
        assertTrue("Wake-up category label missing", xml.contains("Before 3:00 AM"))
        assertTrue("Day sleep category label missing", xml.contains("30 minutes or less"))
        assertTrue("Japa category label missing", xml.contains("Before 9:00 AM"))
        assertTrue("Sunday total 150 / 150 missing", xml.contains("150 / 150"))

        // Verify Monday incomplete japa label and 105/150 points (20+20+20-5+25+25)
        assertTrue("Monday bedtime category missing", xml.contains("Before 10:00 PM"))
        assertTrue("Monday wake-up category missing", xml.contains("Before 3:30 AM"))
        assertTrue("Monday incomplete japa missing", xml.contains("(Incomplete)"))
        assertTrue("Monday total 105 / 150 missing", xml.contains("105 / 150"))

        // Verify weekly total: 150 + 105 = 255
        assertTrue("Weekly total score missing", xml.contains("Total Score = 255 / 1050 pts"))
    }

    @Test
    fun testWorksheetXmlGeneration_handlesEmptyWeekHonestlyWithoutFakeZeros() {
        val startOfWeek = LocalDate.of(2026, 10, 11)
        val emptyEntries = emptyList<SadhanaEntry>()

        val xml = exporter.buildWorksheetXml(
            sadhakaName = "",
            counselorName = "",
            startOfWeek = startOfWeek,
            entries = emptyEntries
        )

        // Missing sadhaka/counselor fallbacks gracefully
        assertTrue(xml.contains("Name: Not Specified"))
        assertTrue(xml.contains("Counselor: Not Specified"))

        // Ensure unlogged days show em-dash '—' and NOT '0' or '0 points'
        assertTrue(xml.contains("—"))
        assertFalse(xml.contains("[enter value]"))
        assertFalse(xml.contains("[total]"))
        assertTrue(xml.contains("Total Score = 0 / 1050 pts"))
    }

    @Test
    fun testGenerateXlsxWorkbook_producesValidZipWithAllParts() {
        val templateFile = listOf(
            File("src/main/assets/templates/Sadhana_Weekly_Excel_Template.xlsx"),
            File("app/src/main/assets/templates/Sadhana_Weekly_Excel_Template.xlsx"),
            File("../templates/Sadhana_Weekly_Excel_Template.xlsx"),
            File("templates/Sadhana_Weekly_Excel_Template.xlsx"),
            File("../Sadhana_Weekly_Excel_Template.xlsx"),
            File("Sadhana_Weekly_Excel_Template.xlsx")
        ).firstOrNull { it.exists() } ?: error("Template file not found")
        val outputFile = tempFolder.newFile("test_output_report.xlsx")

        val startOfWeek = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            SadhanaEntry(
                date = startOfWeek,
                bedtime = LocalTime.of(21, 30),
                wakeTime = LocalTime.of(3, 0),
                napMinutes = 30,
                japaTime = LocalTime.of(7, 15)
            )
        )

        FileInputStream(templateFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                exporter.generateXlsxWorkbook(
                    templateInput = input,
                    outputStream = output,
                    sadhakaName = "Gouranga Das",
                    counselorName = "Radha Raman Das",
                    startOfWeek = startOfWeek,
                    entries = entries
                )
            }
        }

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 1000L)

        // Verify ZIP contents
        val zipEntries = mutableListOf<String>()
        var foundSheetXml = false
        var sheetXmlContent = ""

        ZipInputStream(FileInputStream(outputFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                zipEntries.add(entry.name)
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    foundSheetXml = true
                    sheetXmlContent = zis.bufferedReader(Charsets.UTF_8).readText()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue("sheet1.xml was not found in generated xlsx", foundSheetXml)
        assertTrue(zipEntries.contains("xl/styles.xml"))
        assertTrue(zipEntries.contains("xl/workbook.xml"))
        assertTrue(zipEntries.contains("[Content_Types].xml"))
        assertTrue(zipEntries.contains("_rels/.rels"))

        // Verify dynamic contents in sheet1.xml
        assertTrue(sheetXmlContent.contains("Name: Gouranga Das"))
        assertTrue(sheetXmlContent.contains("Counselor: Radha Raman Das"))
        assertFalse(sheetXmlContent.contains("[enter value]"))
        assertFalse(sheetXmlContent.contains("[points]"))
    }
}
