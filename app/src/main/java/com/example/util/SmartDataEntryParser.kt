package com.example.util

import java.text.SimpleDateFormat
import java.util.*

data class ParsedInspectionItem(
    val id: String = UUID.randomUUID().toString(),
    val index: Int = 1,
    var module: String = "wagon", // "wagon", "coach", "locomotive"
    val number: String,
    val fault: String,
    val actionTaken: String,
    val spareParts: String,
    val status: String = "Completed",
    val date: String,
    val maintenanceType: String = "Acceptance Inspection",
    val signOffTag: String = "#Qualified",
    val subsystem: String = "Bogie & Running Gear",
    val couplerSide1: String = "",
    val couplerSide2: String = "",
    val flowDetection: String = "",
    val brakeStatus: String = "",
    val inspectionNotes: List<String> = emptyList()
)

data class ParseInspectionResult(
    val success: Boolean,
    val detectedDate: String,
    val title: String,
    val items: List<ParsedInspectionItem>,
    val summaryMessage: String
)

object SmartDataEntryParser {

    val SAMPLE_INSPECTION_TEXT: String = """Acceptance and quality control 
Sep 21/2026

1. PW2ER 0122
Fault Record:
Door refused to close due to bent door track.
Action Taken:
Coupler flow detection was not checked due to flow machine malfunction.
Both-side coupler knuckle pins changed.
Both-side bent slide door tracks adjusted.
Coupler height side one 882 mm and side two 876 mm.
Brake system checked; result normal.
#Qualified 
2. PW2ER 0095
Fault Record:
Door refused to close due to bent door track.
Coupler flow detection and coupler height checked.
Action Taken:
Coupler flow detection was not checked due to flow machine malfunction.
Both-side coupler knuckle pins changed.
Both-side bent slide door tracks adjusted.
Coupler height side one 883 mm and side two 884 mm.
Brake systeimiting valve changed.
#Qualified 
3. NW5ER 0518
Fault Record:
Wheel No. 3 flange thickness below the limit.
Action Taken:
Coupler flow detection was not checked due to flow machine malfunction.
Wheel axles No. 2, 3, and 4 changed.
Coupler height side one 882 mm and side two 887 mm.
#Qualified 
4. NW5ER 0517
Fault Record:
Wheel cause.
Action Taken:
Wheel axles No. 2 and 3 changed.
Coupler height side 885 mm and side two 885mm
#Qualified 
5. NW5ER 0822
Fault Record:
Wheel cause.
Action Taken:
Coupler flow detection was not checked due to flow machine malfunction.
All wheelsets changed.
Coupler height side one 886 mm and side two 887mm.
Brake system checked; result normal.
#Qualified 
6. NW5ER 0701
Fault Record:
Wheel No. 2 flange thickness below the limit (23 mm).
Coupler height and flow detection checked.
Action Taken:
Coupler flow detection was not checked due to flow machine malfunction.
Wheel axle No. 1 changed.
Side 2 elastic side bearing replaced.
Coupler height side one 874mm and side two 875 mm. Emergency changed 
#Qualified""".trimIndent()

    private val monthMap = mapOf(
        "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
        "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
        "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
    )

    /**
     * Extracts date from headers like "Sep 21/2026", "September 21, 2026", "2026-09-21"
     */
    fun extractDate(rawText: String, fallbackDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())): String {
        // Pattern 1: "Sep 21/2026" or "Sep 21, 2026" or "September 21 2026"
        val wordDateRegex = Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+(\d{1,2})[/\s,]+(\d{4})\b""")
        wordDateRegex.find(rawText)?.let { match ->
            val monthName = match.groupValues[1].lowercase()
            val month = monthMap[monthName] ?: "01"
            val day = match.groupValues[2].padStart(2, '0')
            val year = match.groupValues[3]
            return "$year-$month-$day"
        }

        // Pattern 2: "2026-09-21" or "2026/09/21"
        val isoDateRegex = Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""")
        isoDateRegex.find(rawText)?.let { match ->
            val year = match.groupValues[1]
            val month = match.groupValues[2].padStart(2, '0')
            val day = match.groupValues[3].padStart(2, '0')
            return "$year-$month-$day"
        }

        // Pattern 3: "21/09/2026" or "21-09-2026"
        val dmyDateRegex = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})\b""")
        dmyDateRegex.find(rawText)?.let { match ->
            val day = match.groupValues[1].padStart(2, '0')
            val month = match.groupValues[2].padStart(2, '0')
            val year = match.groupValues[3]
            return "$year-$month-$day"
        }

        return fallbackDate
    }

    /**
     * Checks if a raw string looks like multi-record inspection notes
     */
    fun isBatchInspectionNotes(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 25) return false
        val lower = trimmed.lowercase()
        val hasKeywords = lower.contains("fault record") || lower.contains("action taken") ||
                lower.contains("#qualified") || lower.contains("coupler height") || lower.contains("pw2er") || lower.contains("nw5er")
        val linesCount = trimmed.lines().count { it.isNotBlank() }
        return (hasKeywords && linesCount >= 3) || linesCount >= 6
    }

    /**
     * Extracts coupler height measurements for side 1 and side 2
     */
    fun extractCouplerHeights(text: String): Pair<String, String> {
        val side1Regex = Regex("""(?i)(?:side\s*one|side\s*1|side)\s*(\d{3})\s*mm""")
        val side2Regex = Regex("""(?i)(?:side\s*two|side\s*2)\s*(\d{3})\s*mm""")

        val s1 = side1Regex.find(text)?.groupValues?.get(1)?.let { "$it mm" } ?: ""
        val s2 = side2Regex.find(text)?.groupValues?.get(1)?.let { "$it mm" } ?: ""
        return Pair(s1, s2)
    }

    /**
     * Extracts Coupler Flow Detection status
     */
    fun extractFlowDetection(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("flow machine malfunction") || lower.contains("flow detection was not checked") || lower.contains("not checked due to flow") ->
                "Not checked (Flow machine malfunction)"
            lower.contains("flow detection") && lower.contains("checked") ->
                "Checked & Verified"
            else -> ""
        }
    }

    /**
     * Extracts Brake System Check status
     */
    fun extractBrakeStatus(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("limiting valve changed") || lower.contains("brake systeimiting valve") || lower.contains("limiting valve") ->
                "Limiting valve changed"
            lower.contains("brake system checked; result normal") || lower.contains("result normal") ->
                "Normal (Pass)"
            lower.contains("emergency changed") ->
                "Emergency valve replaced"
            lower.contains("brake") -> "Checked"
            else -> ""
        }
    }

    /**
     * Auto-detects railway subsystem from fault and action text
     */
    fun detectSubsystem(fault: String, action: String): String {
        val combined = "$fault $action".lowercase()
        return when {
            combined.contains("slide door") || combined.contains("door") -> "Slide Door & Coupler System"
            combined.contains("flange") && combined.contains("bearing") -> "Wheelset & Side Bearing"
            combined.contains("wheelset") || combined.contains("wheel axle") || combined.contains("wheel") -> "Wheelset & Bogie Assembly"
            combined.contains("coupler") -> "Coupler & Draft Gear"
            combined.contains("brake") -> "Air Brake System"
            else -> "Bogie & Running Gear"
        }
    }

    /**
     * Intelligently parses unstructured raw text pasted from clipboard/chat/inspectors
     * and arranges it into structured acceptance records.
     */
    fun parseInspectionReport(
        rawText: String,
        defaultModule: String = "wagon",
        fallbackDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ): ParseInspectionResult {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return ParseInspectionResult(
                success = false,
                detectedDate = fallbackDate,
                title = "Empty Text",
                items = emptyList(),
                summaryMessage = "No text provided to parse."
            )
        }

        val detectedDate = extractDate(trimmed, fallbackDate)

        // Split text into lines
        val lines = trimmed.lines().map { it.trim() }

        // Find record boundaries:
        // A record line usually looks like:
        // "1. PW2ER 0122" or "PW2ER 0122" or "2. PW2ER 0095" or "3. NW5ER 0518"
        // Also supports "1) PW2ER 0122", "1- PW2ER 0122", "[1] PW2ER 0122", "Item 1: PW2ER 0122"
        val itemStartRegex = Regex(
            """^(?:(?:(?:item|no\.?)\s*)?(\d+)[\.\)\]\:\-]?\s*|\[(\d+)\]\s*)?([A-Za-z0-9][\-A-Za-z0-9]*(?:\s+[\-A-Za-z0-9]+)?)\s*$""",
            RegexOption.IGNORE_CASE
        )

        data class RawBlock(val number: String, val index: Int, val lines: MutableList<String>)
        val blocks = mutableListOf<RawBlock>()
        var currentBlock: RawBlock? = null

        for (line in lines) {
            if (line.isBlank()) continue

            // Skip top title header lines like "Acceptance and quality control" or "Sep 21/2026"
            val isHeaderLine = (line.contains("acceptance", ignoreCase = true) && line.contains("quality", ignoreCase = true)) ||
                    line.contains("quality control", ignoreCase = true) ||
                    line.matches(Regex("""(?i).*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+\d{1,2}[/\s,]+\d{4}.*""")) ||
                    line.matches(Regex("""\b\d{4}[-/.]\d{1,2}[-/.]\d{1,2}\b"""))
            if (isHeaderLine && blocks.isEmpty()) {
                continue
            }

            val startMatch = itemStartRegex.find(line)
            val candidateId = startMatch?.groupValues?.get(3)?.trim()
            val candidateIndex = startMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: startMatch?.groupValues?.get(2)?.toIntOrNull()
                ?: (blocks.size + 1)

            val isNewItemHeader = if (candidateId != null && candidateId.length in 4..16) {
                val hasDigit = candidateId.any { it.isDigit() }
                val hasLetter = candidateId.any { it.isLetter() }
                val notAKeyword = !candidateId.startsWith("fault", ignoreCase = true) &&
                        !candidateId.startsWith("action", ignoreCase = true) &&
                        !candidateId.startsWith("coupler", ignoreCase = true) &&
                        !candidateId.startsWith("brake", ignoreCase = true) &&
                        !candidateId.startsWith("wheel", ignoreCase = true) &&
                        !candidateId.startsWith("qualified", ignoreCase = true) &&
                        !candidateId.startsWith("unqualified", ignoreCase = true)
                hasDigit && hasLetter && notAKeyword
            } else false

            if (isNewItemHeader && candidateId != null) {
                val block = RawBlock(candidateId, candidateIndex, mutableListOf())
                blocks.add(block)
                currentBlock = block
            } else {
                currentBlock?.lines?.add(line)
            }
        }

        // If no items were detected by number regex, try block splitting by #Qualified or Fault Record
        if (blocks.isEmpty()) {
            return ParseInspectionResult(
                success = false,
                detectedDate = detectedDate,
                title = "Could not identify vehicle numbers",
                items = emptyList(),
                summaryMessage = "No rolling stock IDs (e.g. '1. PW2ER 0122') could be detected in the pasted text."
            )
        }

        val parsedItems = blocks.mapIndexed { index, block ->
            var state = "NONE" // "FAULT", "ACTION"
            val faultLines = mutableListOf<String>()
            val actionLines = mutableListOf<String>()
            var signOff = "#Qualified"
            var status = "Completed"

            for (l in block.lines) {
                val lower = l.lowercase()
                when {
                    lower.startsWith("fault record:") || lower.startsWith("fault:") || lower.startsWith("defect:") -> {
                        state = "FAULT"
                        val remaining = l.substringAfter(':').trim()
                        if (remaining.isNotBlank()) faultLines.add(remaining)
                    }
                    lower.startsWith("action taken:") || lower.startsWith("action:") || lower.startsWith("corrective action:") -> {
                        state = "ACTION"
                        val remaining = l.substringAfter(':').trim()
                        if (remaining.isNotBlank()) actionLines.add(remaining)
                    }
                    lower.contains("#qualified") || lower == "qualified" -> {
                        signOff = "#Qualified"
                        status = "Completed"
                        val cleanLine = l.replace(Regex("""(?i)#qualified|qualified"""), "").trim()
                        if (cleanLine.isNotBlank()) {
                            if (state == "ACTION") actionLines.add(cleanLine)
                            else if (state == "FAULT") faultLines.add(cleanLine)
                        }
                    }
                    lower.contains("#unqualified") || lower == "unqualified" || lower.contains("rejected") -> {
                        signOff = "#Unqualified"
                        status = "Rejected"
                        val cleanLine = l.replace(Regex("""(?i)#unqualified|unqualified|rejected"""), "").trim()
                        if (cleanLine.isNotBlank()) {
                            if (state == "ACTION") actionLines.add(cleanLine)
                            else if (state == "FAULT") faultLines.add(cleanLine)
                        }
                    }
                    else -> {
                        if (state == "FAULT") faultLines.add(l)
                        else if (state == "ACTION") actionLines.add(l)
                    }
                }
            }

            val faultText = if (faultLines.isNotEmpty()) faultLines.joinToString("\n") else "Conforming to standard inspection"
            val actionText = if (actionLines.isNotEmpty()) actionLines.joinToString("\n") else "Standard inspection and functional checks performed."

            // Extract spare parts changed from action text
            val spareParts = extractSpareParts(actionText)

            // Extract coupler height side 1 and side 2
            val (coupler1, coupler2) = extractCouplerHeights(actionText)

            // Extract flow detection & brake status
            val flowStatus = extractFlowDetection(actionText)
            val brakeStatus = extractBrakeStatus(actionText)

            // Auto-detect subsystem
            val subsystem = detectSubsystem(faultText, actionText)

            // Auto-detect module:
            val numUpper = block.number.uppercase()
            val detectedModule = when {
                numUpper.startsWith("PW") -> "wagon" // PW2ER is standard gauge passenger/wagon stock
                numUpper.startsWith("NW") -> "wagon" // NW5ER is standard gauge freight wagon
                numUpper.startsWith("LC") || numUpper.startsWith("HXD") || numUpper.startsWith("DF") -> "locomotive"
                numUpper.startsWith("CH") || numUpper.startsWith("YZ") || numUpper.startsWith("RW") -> "coach"
                else -> defaultModule
            }

            ParsedInspectionItem(
                index = index + 1,
                module = detectedModule,
                number = block.number,
                fault = faultText,
                actionTaken = actionText,
                spareParts = spareParts,
                status = status,
                date = detectedDate,
                maintenanceType = if (faultLines.isNotEmpty()) "Corrective / Acceptance" else "Preventive Acceptance",
                signOffTag = signOff,
                subsystem = subsystem,
                couplerSide1 = coupler1,
                couplerSide2 = coupler2,
                flowDetection = flowStatus,
                brakeStatus = brakeStatus,
                inspectionNotes = actionLines
            )
        }

        return ParseInspectionResult(
            success = true,
            detectedDate = detectedDate,
            title = "Acceptance and Quality Control",
            items = parsedItems,
            summaryMessage = "Arranged ${parsedItems.size} inspection records for $detectedDate."
        )
    }

    /**
     * Scans action lines for parts that were changed, replaced, or adjusted
     */
    private fun extractSpareParts(actionText: String): String {
        val parts = mutableListOf<String>()
        val lines = actionText.lines()

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("changed") || lower.contains("replaced") || lower.contains("adjusted") || lower.contains("renewed")) {
                // Protect "No." abbreviation (e.g., "Wheel axles No. 2, 3, and 4 changed")
                val sanitized = line.replace(Regex("""(?i)\bno\.\s*(\d+)"""), "No_$1")
                val clauses = sanitized.split(Regex("""\.\s*""")).map { it.trim() }.filter { it.isNotBlank() }
                for (clause in clauses) {
                    val cLower = clause.lowercase()
                    if (cLower.contains("changed") || cLower.contains("replaced") || cLower.contains("adjusted") || cLower.contains("renewed")) {
                        var clean = clause
                            .replace(Regex("""(?i)\b(changed|replaced|adjusted|renewed|was|were)\b"""), "")
                            .replace(Regex("""^[•\-\*]\s*"""), "")
                            .replace("No_", "No. ")
                            .trim()
                        if (clean.isNotBlank() && clean.length in 3..60 && !clean.contains("flow machine", ignoreCase = true)) {
                            parts.add(clean)
                        }
                    }
                }
            }
        }

        return if (parts.isNotEmpty()) parts.distinct().joinToString(", ") else "None (Adjustment & Verification)"
    }
}
