package com.example

import com.example.util.SmartDataEntryParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSmartDataEntryParser_parsesUserSampleInspectionData() {
    val sampleText = """Acceptance and quality control 
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

    val result = SmartDataEntryParser.parseInspectionReport(sampleText)

    assertTrue("Parsing should succeed", result.success)
    assertEquals("2026-09-21", result.detectedDate)
    assertEquals(6, result.items.size)

    // Verify Item 1: PW2ER 0122
    val item1 = result.items[0]
    assertEquals("PW2ER 0122", item1.number)
    assertEquals("882 mm", item1.couplerSide1)
    assertEquals("876 mm", item1.couplerSide2)
    assertTrue(item1.flowDetection.contains("malfunction", ignoreCase = true))
    assertTrue(item1.brakeStatus.contains("normal", ignoreCase = true))
    assertEquals("#Qualified", item1.signOffTag)
    assertEquals("Completed", item1.status)

    // Verify Item 2: PW2ER 0095
    val item2 = result.items[1]
    assertEquals("PW2ER 0095", item2.number)
    assertEquals("883 mm", item2.couplerSide1)
    assertEquals("884 mm", item2.couplerSide2)
    assertTrue(item2.brakeStatus.contains("valve", ignoreCase = true))

    // Verify Item 3: NW5ER 0518
    val item3 = result.items[2]
    assertEquals("NW5ER 0518", item3.number)
    assertEquals("882 mm", item3.couplerSide1)
    assertEquals("887 mm", item3.couplerSide2)
    assertTrue(item3.spareParts.contains("Wheel axles", ignoreCase = true))

    // Verify Item 4: NW5ER 0517
    val item4 = result.items[3]
    assertEquals("NW5ER 0517", item4.number)
    assertEquals("885 mm", item4.couplerSide1)
    assertEquals("885 mm", item4.couplerSide2)

    // Verify Item 5: NW5ER 0822
    val item5 = result.items[4]
    assertEquals("NW5ER 0822", item5.number)
    assertEquals("886 mm", item5.couplerSide1)
    assertEquals("887 mm", item5.couplerSide2)

    // Verify Item 6: NW5ER 0701
    val item6 = result.items[5]
    assertEquals("NW5ER 0701", item6.number)
    assertEquals("874 mm", item6.couplerSide1)
    assertEquals("875 mm", item6.couplerSide2)
    assertTrue(item6.spareParts.contains("elastic side bearing", ignoreCase = true))
  }

  @Test
  fun testIsBatchInspectionNotes_detectsBatchNotes() {
    assertTrue(SmartDataEntryParser.isBatchInspectionNotes(SmartDataEntryParser.SAMPLE_INSPECTION_TEXT))
    assertFalse(SmartDataEntryParser.isBatchInspectionNotes("Short note"))
  }

  @Test
  fun testSmartReport_onlyIncludesEnteredData_excludesEmptyLocomotive() {
    val report = com.example.data.model.DailyReportEntity(
      id = 1L,
      reportDate = "2026-09-21",
      status = "Published",
      title = "EDR Acceptance Report",
      executiveSummary = "Acceptance QC conducted for wagons.",
      qcWagonCount = 6,
      qcLocoCount = 0,
      qcCoachCount = 0
    )
    val wagonEntity = com.example.data.model.QCEntity(
      id = 101L,
      module = "wagon",
      number = "PW2ER 0122",
      maintenanceType = "Coupler and Door Track",
      description = "Coupler adjusted",
      fault = "Door refused to close",
      status = "Completed"
    )
    val bundle = com.example.data.repository.ReportDataBundle(
      date = "2026-09-21",
      qcWagon = listOf(wagonEntity),
      qcLoco = emptyList(),
      qcCoach = emptyList(),
      taskAqms = emptyList(),
      taskWagon = emptyList(),
      taskLoco = emptyList(),
      taskCoach = emptyList(),
      taskEquipment = emptyList()
    )

    val smartText = com.example.util.PdfReportExporter.buildSmartReportText(
      report = report,
      data = bundle,
      publicUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/2026-09-21.json"
    )

    assertTrue("Must mention Wagon", smartText.contains("Wagon Acceptance Inspections: 1 units"))
    assertFalse("Must NOT mention Locomotive when locomotive has no data", smartText.contains("Locomotive Sign-offs"))
    assertFalse("Must NOT mention Coach when coach has no data", smartText.contains("Coach Quality Checks"))
  }

  @Test
  fun testSmartReport_containsPublicVerificationUrl() {
    val report = com.example.data.model.DailyReportEntity(
      id = 1L,
      reportDate = "2026-09-21",
      status = "Published",
      title = "EDR Acceptance Report",
      executiveSummary = "Test summary",
      publicUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/2026-09-21.json"
    )
    val bundle = com.example.data.repository.ReportDataBundle(
      date = "2026-09-21",
      qcWagon = emptyList(),
      qcLoco = emptyList(),
      qcCoach = emptyList(),
      taskAqms = emptyList(),
      taskWagon = emptyList(),
      taskLoco = emptyList(),
      taskCoach = emptyList(),
      taskEquipment = emptyList()
    )
    val text = com.example.util.PdfReportExporter.buildSmartReportText(
      report = report,
      data = bundle,
      publicUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/2026-09-21.json"
    )
    assertTrue("Must contain live URL in report text", text.contains("https://acceptance-8781f-default-rtdb.firebaseio.com/reports/2026-09-21.json"))
  }
}
