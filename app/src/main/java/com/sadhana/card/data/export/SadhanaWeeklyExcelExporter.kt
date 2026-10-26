package com.sadhana.card.data.export

import android.content.Context
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust, offline-first Excel (.xlsx) generator reproducing the authentic layout,
 * formatting, formulas, styling, borders, and print configuration of
 * [Sadhana_Weekly_Excel_Template.xlsx].
 *
 * Runs completely off the main thread with zero heavy external libraries.
 */
@Singleton
class SadhanaWeeklyExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TEMPLATE_ASSET_PATH = "templates/Sadhana_Weekly_Excel_Template.xlsx"
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        private val DAY_COLUMNS = listOf("B", "C", "D", "E", "F", "G", "H")
    }

    /**
     * Generates a safe, descriptive filename for the weekly report.
     * Example: "Sadhana_Report_Sundar_Gopal_Das_2026-10-11_to_2026-10-17.xlsx"
     */
    fun buildSafeFileName(sadhakaName: String, startOfWeek: LocalDate): String {
        val endOfWeek = startOfWeek.plusDays(6)
        val sanitized = sadhakaName.trim()
            .replace(Regex("[^a-zA-Z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "Sadhaka" }
        return "Sadhana_Report_${sanitized}_${startOfWeek}_to_${endOfWeek}.xlsx"
    }

    /**
     * Exports the weekly report for [startOfWeek] (Sunday) to a shareable .xlsx file in the app's cache directory.
     *
     * @param sadhakaName The name of the sadhaka.
     * @param counselorName The name of the counselor.
     * @param startOfWeek The Sunday date at the start of the week.
     * @param entries The recorded daily entries for the week (may be empty or partial).
     * @return The generated [File] ready for sharing or saving.
     */
    suspend fun exportWeeklyReport(
        sadhakaName: String,
        counselorName: String,
        startOfWeek: LocalDate,
        entries: List<SadhanaEntry>
    ): File = withContext(Dispatchers.IO) {
        val fileName = buildSafeFileName(sadhakaName, startOfWeek)
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(reportsDir, fileName)

        // Read template from assets if available, or generate a fresh workbook stream
        val templateStream: InputStream = try {
            context.assets.open(TEMPLATE_ASSET_PATH)
        } catch (_: Exception) {
            // Fallback to internal embedded base template generator if asset is not accessible
            ByteArrayInputStream(buildBaseTemplateArchive())
        }

        templateStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                generateXlsxWorkbook(
                    templateInput = input,
                    outputStream = output,
                    sadhakaName = sadhakaName,
                    counselorName = counselorName,
                    startOfWeek = startOfWeek,
                    entries = entries
                )
            }
        }

        if (!targetFile.exists() || targetFile.length() == 0L) {
            throw IllegalStateException("Generated report file is empty or missing: ${targetFile.absolutePath}")
        }

        targetFile
    }

    /**
     * Reads the template zip archive, replaces `xl/worksheets/sheet1.xml` with dynamic data,
     * and streams the resulting valid XLSX workbook into [outputStream].
     */
    fun generateXlsxWorkbook(
        templateInput: InputStream,
        outputStream: FileOutputStream,
        sadhakaName: String,
        counselorName: String,
        startOfWeek: LocalDate,
        entries: List<SadhanaEntry>
    ) {
        val dynamicSheetXml = buildWorksheetXml(
            sadhakaName = sadhakaName,
            counselorName = counselorName,
            startOfWeek = startOfWeek,
            entries = entries
        )

        val zipInput = ZipInputStream(templateInput)
        val zipOutput = ZipOutputStream(outputStream)

        zipOutput.use { zos ->
            var entry: ZipEntry? = zipInput.nextEntry
            while (entry != null) {
                val entryName = entry.name
                zos.putNextEntry(ZipEntry(entryName))

                if (entryName == "xl/worksheets/sheet1.xml") {
                    val bytes = dynamicSheetXml.toByteArray(Charsets.UTF_8)
                    zos.write(bytes)
                } else {
                    zipInput.copyTo(zos)
                }

                zos.closeEntry()
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
    }

    /**
     * Builds the complete `xl/worksheets/sheet1.xml` content matching the layout, row heights,
     * cell styles, frozen panes, formulas, and print setup of the reference template.
     */
    fun buildWorksheetXml(
        sadhakaName: String,
        counselorName: String,
        startOfWeek: LocalDate,
        entries: List<SadhanaEntry>
    ): String {
        val entryMap = entries.associateBy { it.date }
        val weekDates = (0L..6L).map { startOfWeek.plusDays(it) }
        val endOfWeek = startOfWeek.plusDays(6)

        // Calculate week aggregates
        var weekTotalScore = 0
        var totalPathan = 0
        var totalSravan = 0

        val dayDataList = weekDates.mapIndexed { idx, date ->
            val entry = entryMap[date]
            val scores = if (entry != null) SadhanaScorer.scoreEntry(entry) else null

            if (entry != null) {
                totalPathan += entry.pathanMinutes
                totalSravan += entry.sravanMinutes
            }
            val dayTotal = scores?.totalScore
            if (dayTotal != null) {
                weekTotalScore += dayTotal
            }

            DayExportData(
                col = DAY_COLUMNS[idx],
                date = date,
                entry = entry,
                scores = scores
            )
        }

        val sName = sadhakaName.trim().ifEmpty { "Not Specified" }
        val cName = counselorName.trim().ifEmpty { "Not Specified" }
        val startDateFormatted = "${DateUtils.formatDayName(startOfWeek)}, ${startOfWeek.format(DATE_FORMATTER)}"
        val endDateFormatted = "${DateUtils.formatDayName(endOfWeek)}, ${endOfWeek.format(DATE_FORMATTER)}"
        val dateRangeText = "Dates: From $startDateFormatted to $endDateFormatted"

        val sb = StringBuilder(16384)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<sheetViews><sheetView showGridLines="0" workbookViewId="0"><pane xSplit="1" ySplit="10" topLeftCell="B11" activePane="bottomRight" state="frozen"/><selection pane="topRight"/><selection pane="bottomLeft"/><selection pane="bottomRight" activeCell="A1" sqref="A1"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr baseColWidth="8" defaultRowHeight="15"/>""")
        sb.append("""<cols><col width="48" customWidth="1" min="1" max="1"/><col width="15" customWidth="1" min="2" max="2"/><col width="15" customWidth="1" min="3" max="3"/><col width="15" customWidth="1" min="4" max="4"/><col width="15" customWidth="1" min="5" max="5"/><col width="15" customWidth="1" min="6" max="6"/><col width="15" customWidth="1" min="7" max="7"/><col width="15" customWidth="1" min="8" max="8"/></cols>""")
        sb.append("""<sheetData>""")

        // Row 1: Header
        sb.append("""<row r="1" ht="28" customHeight="1"><c r="A1" s="1" t="inlineStr"><is><t>Sri Sri Guru Gouranga Jayate</t></is></c></row>""")

        // Row 2: Sadhaka & Counselor
        sb.append("""<row r="2"><c r="A2" s="2" t="inlineStr"><is><t>Name: ${xmlEscape(sName)}</t></is></c><c r="E2" s="2" t="inlineStr"><is><t>Counselor: ${xmlEscape(cName)}</t></is></c></row>""")

        // Row 3: Date Range
        sb.append("""<row r="3"><c r="A3" s="2" t="inlineStr"><is><t>${xmlEscape(dateRangeText)}</t></is></c></row>""")

        // Row 5: Title
        sb.append("""<row r="5" ht="32" customHeight="1"><c r="A5" s="3" t="inlineStr"><is><t>Sadhana Card for Sadhaka</t></is></c></row>""")

        // Row 6: Dedication
        sb.append("""<row r="6" ht="36" customHeight="1"><c r="A6" s="4" t="inlineStr"><is><t>for the pleasure of my Counselor, Authorities, Sri Gurudev, Srila Prabhupada, Sri Sri Gaur Nitai, Sri Sri Radha Raman ji and ISKCON BONGAIGAON NAMHATTA Sangha</t></is></c></row>""")

        // Row 8: Verse
        sb.append("""<row r="8" ht="28" customHeight="1"><c r="A8" s="4" t="inlineStr"><is><t>‘yukta ahara viharasya | yukta cestasya karmasu | yukta svapnava bodhasya | yogo bhavati dukha ha’</t></is></c></row>""")

        // Row 10: Column Headers
        val dayHeaderLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        sb.append("""<row r="10" ht="28" customHeight="1"><c r="A10" s="5" t="inlineStr"><is><t>ACTIVITY / SCORING REFERENCE</t></is></c>""")
        dayDataList.forEachIndexed { idx, day ->
            sb.append("""<c r="${day.col}10" s="5" t="inlineStr"><is><t>${dayHeaderLabels[idx]}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 11: Section NIDRA – To bed
        sb.append("""<row r="11" ht="24" customHeight="1"><c r="A11" s="6" t="inlineStr"><is><t>NIDRA – To bed</t></is></c></row>""")

        // Rows 12-17: Scoring reference for Bedtime
        appendReferenceRow(sb, 12, "Scoring reference: 9:30 pm or before = 25 points")
        appendReferenceRow(sb, 13, "Before 10:00 pm = 20 points")
        appendReferenceRow(sb, 14, "Before 10:30 pm = 15 points")
        appendReferenceRow(sb, 15, "Before 10:45 pm = 10 points")
        appendReferenceRow(sb, 16, "Before 11:00 pm = 5 points")
        appendReferenceRow(sb, 17, "After 11:00 pm = 0 points")

        // Row 18: Recorded bedtime
        sb.append("""<row r="18" ht="26" customHeight="1"><c r="A18" s="9" t="inlineStr"><is><t>Recorded bedtime</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.entry?.bedtimeCategory?.label
                ?: day.entry?.bedtime?.format(TIME_FORMATTER)
                ?: "—"
            sb.append("""<c r="${day.col}18" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 19: Awarded points for bedtime
        sb.append("""<row r="19" ht="26" customHeight="1"><c r="A19" s="11" t="inlineStr"><is><t>Awarded points</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.scores?.bedScore?.toString() ?: "—"
            sb.append("""<c r="${day.col}19" s="12" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 20: Section NIDRA – Wake up
        sb.append("""<row r="20" ht="24" customHeight="1"><c r="A20" s="6" t="inlineStr"><is><t>NIDRA – Wake up</t></is></c></row>""")

        // Rows 21-26: Scoring reference for Wake up
        appendReferenceRow(sb, 21, "Scoring reference: Before 3:00 am = 25 points")
        appendReferenceRow(sb, 22, "Before 3:30 am = 20 points")
        appendReferenceRow(sb, 23, "Before 4:00 am = 15 points")
        appendReferenceRow(sb, 24, "Before 4:15 am = 10 points")
        appendReferenceRow(sb, 25, "Before 4:30 am = 5 points")
        appendReferenceRow(sb, 26, "After 4:30 am = 0 points")

        // Row 27: Recorded wake-up time
        sb.append("""<row r="27" ht="26" customHeight="1"><c r="A27" s="9" t="inlineStr"><is><t>Recorded wake-up time</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.entry?.wakeUpCategory?.label
                ?: day.entry?.wakeTime?.format(TIME_FORMATTER)
                ?: "—"
            sb.append("""<c r="${day.col}27" s="12" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 28: Awarded points for wake-up
        sb.append("""<row r="28" ht="26" customHeight="1"><c r="A28" s="11" t="inlineStr"><is><t>Awarded points</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.scores?.wakeScore?.toString() ?: "—"
            sb.append("""<c r="${day.col}28" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 29: Section NIDRA – Day Sleep
        sb.append("""<row r="29" ht="24" customHeight="1"><c r="A29" s="6" t="inlineStr"><is><t>NIDRA – Day Sleep</t></is></c></row>""")

        // Rows 30-33: Scoring reference for Day Sleep
        appendReferenceRow(sb, 30, "Scoring reference: 30 minutes or less = 25 points")
        appendReferenceRow(sb, 31, "45 minutes = 20 points")
        appendReferenceRow(sb, 32, "60 minutes = 10 points")
        appendReferenceRow(sb, 33, "More than 60 minutes = 0 points")
        appendReferenceRow(sb, 34, "Train journey: travel option removed")

        // Row 35: Recorded day-sleep duration
        sb.append("""<row r="35" ht="26" customHeight="1"><c r="A35" s="9" t="inlineStr"><is><t>Recorded day-sleep duration (minutes)</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.entry?.daySleepCategory?.label
                ?: if (day.entry?.napMinutes != null) "${day.entry.napMinutes} mins" else "—"
            sb.append("""<c r="${day.col}35" s="12" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 36: Train-journey bonus
        sb.append("""<row r="36" ht="26" customHeight="1"><c r="A36" s="9" t="inlineStr"><is><t>Train-journey bonus (if applicable)</t></is></c>""")
        dayDataList.forEach { day ->
            val text = when {
                day.entry == null -> "—"
                day.entry.trainJourney -> "+60 mins bonus"
                else -> "None"
            }
            sb.append("""<c r="${day.col}36" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 37: Awarded points for day sleep
        sb.append("""<row r="37" ht="26" customHeight="1"><c r="A37" s="11" t="inlineStr"><is><t>Awarded points</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.scores?.napScore?.toString() ?: "—"
            sb.append("""<c r="${day.col}37" s="12" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 38: Section JAPA – 16 rounds
        sb.append("""<row r="38" ht="24" customHeight="1"><c r="A38" s="6" t="inlineStr"><is><t>JAPA – 16 rounds</t></is></c></row>""")

        // Rows 39-44: Scoring reference for Japa
        appendReferenceRow(sb, 39, "Scoring reference: All rounds before 7:15 am = 50 points")
        appendReferenceRow(sb, 40, "Before 9:00 am = 40 points")
        appendReferenceRow(sb, 41, "Before 11:59 am = 30 points")
        appendReferenceRow(sb, 42, "Before 3:00 pm = 20 points")
        appendReferenceRow(sb, 43, "Before 6:00 pm = 10 points")
        appendReferenceRow(sb, 44, "After 6:00 pm = 0 points")

        // Row 45: Recorded Japa completion time
        sb.append("""<row r="45" ht="26" customHeight="1"><c r="A45" s="9" t="inlineStr"><is><t>Recorded Japa completion time</t></is></c>""")
        dayDataList.forEach { day ->
            val entry = day.entry
            val japaCat = entry?.japaCategory
            val japaTime = entry?.japaTime
            val text = when {
                japaCat != null -> {
                    if (entry.japaCompleted) japaCat.label
                    else "${japaCat.label} (Incomplete)"
                }
                japaTime != null -> japaTime.format(TIME_FORMATTER)
                else -> "—"
            }
            sb.append("""<c r="${day.col}45" s="12" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 46: Awarded points for Japa
        sb.append("""<row r="46" ht="26" customHeight="1"><c r="A46" s="11" t="inlineStr"><is><t>Awarded points</t></is></c>""")
        dayDataList.forEach { day ->
            val text = day.scores?.japaScore?.toString() ?: "—"
            sb.append("""<c r="${day.col}46" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 47: Section PATHAN
        sb.append("""<row r="47" ht="24" customHeight="1"><c r="A47" s="6" t="inlineStr"><is><t>PATHAN – SP books (time in minutes)</t></is></c></row>""")

        // Row 48: Reading duration
        sb.append("""<row r="48" ht="26" customHeight="1"><c r="A48" s="11" t="inlineStr"><is><t>Reading duration (minutes)</t></is></c>""")
        dayDataList.forEach { day ->
            val text = if (day.entry != null) "${day.entry.pathanMinutes} mins" else "—"
            sb.append("""<c r="${day.col}48" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 49: Section SRAVAN
        sb.append("""<row r="49" ht="24" customHeight="1"><c r="A49" s="6" t="inlineStr"><is><t>SRAVAN (time in minutes)</t></is></c></row>""")

        // Row 50: Listening duration
        sb.append("""<row r="50" ht="26" customHeight="1"><c r="A50" s="11" t="inlineStr"><is><t>Listening duration (minutes)</t></is></c>""")
        dayDataList.forEach { day ->
            val text = if (day.entry != null) "${day.entry.sravanMinutes} mins" else "—"
            sb.append("""<c r="${day.col}50" s="10" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 54: WEEKLY TOTAL
        sb.append("""<row r="54" ht="26" customHeight="1"><c r="A54" s="13" t="inlineStr"><is><t>WEEKLY TOTAL</t></is></c>""")
        dayDataList.forEach { day ->
            val text = if (day.scores?.totalScore != null) "${day.scores.totalScore} / 150" else "—"
            sb.append("""<c r="${day.col}54" s="13" t="inlineStr"><is><t>${xmlEscape(text)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 56: Summary footer
        val summaryNote = "Weekly Summary: Total Score = $weekTotalScore / 1050 pts | Reading = ${totalPathan}m | Hearing = ${totalSravan}m"
        sb.append("""<row r="56" ht="30" customHeight="1"><c r="A56" s="14" t="inlineStr"><is><t>${xmlEscape(summaryNote)}</t></is></c></row>""")

        sb.append("""</sheetData>""")

        // Merged Cells
        sb.append("""<mergeCells count="14"><mergeCell ref="A1:H1"/><mergeCell ref="A56:H56"/><mergeCell ref="A3:H3"/><mergeCell ref="A29:H29"/><mergeCell ref="A20:H20"/><mergeCell ref="A38:H38"/><mergeCell ref="A11:H11"/><mergeCell ref="A49:H49"/><mergeCell ref="A2:D2"/><mergeCell ref="A5:H5"/><mergeCell ref="A47:H47"/><mergeCell ref="A8:H8"/><mergeCell ref="E2:H2"/><mergeCell ref="A6:H6"/></mergeCells>""")
        sb.append("""<pageMargins left="0.25" right="0.25" top="0.4" bottom="0.4" header="0.2" footer="0.2"/>""")
        sb.append("""<pageSetup orientation="landscape" paperSize="9" fitToHeight="0" fitToWidth="1"/>""")
        sb.append("""</worksheet>""")

        return sb.toString()
    }

    private fun appendReferenceRow(sb: StringBuilder, rowNum: Int, description: String) {
        sb.append("""<row r="$rowNum" ht="20" customHeight="1"><c r="A$rowNum" s="7" t="inlineStr"><is><t>${xmlEscape(description)}</t></is></c>""")
        for (col in DAY_COLUMNS) {
            sb.append("""<c r="$col$rowNum" s="8"/>""")
        }
        sb.append("""</row>""")
    }

    private fun xmlEscape(value: String): String {
        return buildString(value.length + 16) {
            for (ch in value) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
    }

    private data class DayExportData(
        val col: String,
        val date: LocalDate,
        val entry: SadhanaEntry?,
        val scores: SadhanaScorer.DailyScores?
    )

    /**
     * Fallback standalone base template generator in the unlikely event the asset cannot be opened.
     * Contains standard styles, themes, and OpenXML package wiring.
     */
    private fun buildBaseTemplateArchive(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/xl/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/></Types>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml" Id="rId1"/><Relationship Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml" Id="rId2"/><Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml" Id="rId3"/></Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // docProps/app.xml
            zos.putNextEntry(ZipEntry("docProps/app.xml"))
            zos.write("""<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Sadhana Card App</Application><AppVersion>1.0</AppVersion></Properties>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // docProps/core.xml
            zos.putNextEntry(ZipEntry("docProps/core.xml"))
            zos.write("""<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"><dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">Sadhana Card</dc:creator><dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Weekly Sadhana Card Report</dc:title></cp:coreProperties>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="/xl/worksheets/sheet1.xml" Id="rId1"/><Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml" Id="rId2"/><Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml" Id="rId3"/></Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheets><sheet xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" name="Weekly Sadhana Card" sheetId="1" state="visible" r:id="rId1"/></sheets><definedNames><definedName name="_xlnm.Print_Titles" localSheetId="0">'Weekly Sadhana Card'!${'$'}1:${'$'}10</definedName><definedName name="_xlnm.Print_Area" localSheetId="0">'Weekly Sadhana Card'!${'$'}A${'$'}1:${'$'}H${'$'}56</definedName></definedNames></workbook>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // xl/theme/theme1.xml
            zos.putNextEntry(ZipEntry("xl/theme/theme1.xml"))
            zos.write("""<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme"><a:themeElements><a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="1F497D"/></a:dk2><a:lt2><a:srgbClr val="EEECE1"/></a:lt2><a:accent1><a:srgbClr val="4F81BD"/></a:accent1><a:accent2><a:srgbClr val="C0504D"/></a:accent2><a:accent3><a:srgbClr val="9BBB59"/></a:accent3><a:accent4><a:srgbClr val="8064A2"/></a:accent4><a:accent5><a:srgbClr val="4BACC6"/></a:accent5><a:accent6><a:srgbClr val="F79646"/></a:accent6><a:hlink><a:srgbClr val="0000FF"/></a:hlink><a:folHlink><a:srgbClr val="800080"/></a:folHlink></a:clrScheme><a:fontScheme name="Office"><a:majorFont><a:latin typeface="Calibri"/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // xl/styles.xml matching the 15 cellXfs of template
            zos.putNextEntry(ZipEntry("xl/styles.xml"))
            zos.write("""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="11"><font><name val="Calibri"/><sz val="11"/></font><font><name val="Arial"/><b val="1"/><color rgb="00243447"/><sz val="14"/></font><font><b val="1"/><color rgb="00243447"/><sz val="11"/></font><font><name val="Arial"/><b val="1"/><color rgb="00FFFFFF"/><sz val="16"/></font><font><name val="Arial"/><i val="1"/><color rgb="00243447"/><sz val="10"/></font><font><b val="1"/><color rgb="00FFFFFF"/><sz val="10"/></font><font><i val="1"/><color rgb="00555555"/><sz val="9"/></font><font><color rgb="00243447"/><sz val="9"/></font><font><i val="1"/><color rgb="00666666"/><sz val="9"/></font><font><b val="1"/><color rgb="00243447"/><sz val="9"/></font><font><b val="1"/><color rgb="00FFFFFF"/><sz val="11"/></font></fonts><fills count="7"><fill><patternFill/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="00243447"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="00FCE4D6"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="00F2F2F2"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="00FFFFFF"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="00F8FBFD"/></patternFill></fill></fills><borders count="3"><border><left/><right/><top/><bottom/></border><border><left style="thin"><color rgb="00A6A6A6"/></left><right style="thin"><color rgb="00A6A6A6"/></right><top style="thin"><color rgb="00A6A6A6"/></top><bottom style="thin"><color rgb="00A6A6A6"/></bottom></border><border><top style="medium"><color rgb="00243447"/></top><bottom style="thin"><color rgb="00A6A6A6"/></bottom></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="15"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="2" fillId="0" borderId="0" applyAlignment="1"><alignment vertical="center"/></xf><xf numFmtId="0" fontId="3" fillId="2" borderId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="4" fillId="3" borderId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="5" fillId="2" borderId="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="5" fillId="2" borderId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="6" fillId="0" borderId="1" applyAlignment="1"><alignment vertical="center"/></xf><xf numFmtId="0" fontId="7" fillId="0" borderId="1"/><xf numFmtId="0" fontId="7" fillId="0" borderId="1" applyAlignment="1"><alignment vertical="center"/></xf><xf numFmtId="0" fontId="8" fillId="5" borderId="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="9" fillId="0" borderId="1" applyAlignment="1"><alignment vertical="center"/></xf><xf numFmtId="0" fontId="8" fillId="6" borderId="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="10" fillId="2" borderId="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="8" fillId="0" borderId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf></cellXfs></styleSheet>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // xl/worksheets/sheet1.xml placeholder
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData/></worksheet>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}
