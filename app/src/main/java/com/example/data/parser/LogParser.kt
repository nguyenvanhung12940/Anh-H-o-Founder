package com.example.data.parser

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.Locale

data class ParsedMember(
    val stt: Int,
    val name: String,
    val checkIn: String,
    val checkOut: String,
    val totalHours: Double,
    val status: String,
    val isPresent: Boolean,
    val note: String = ""
) : Serializable

object LogParser {

    /**
     * Parses the original roster and raw FaceID check-in log text.
     * Evaluates attendance times, checks against the 4-hour threshold,
     * and maps appropriate statuses.
     */
    fun parseLogs(rosterText: String, logText: String): List<ParsedMember> {
        val members = rosterText.split(Regex("[\n\r]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        
        val logLines = logText.split(Regex("[\n\r]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsedList = mutableListOf<ParsedMember>()

        // Process alphabetically sorted members
        val sortedMembers = members.sortedWith(java.text.Collator.getInstance(Locale("vi", "VN")))

        sortedMembers.forEachIndexed { index, name ->
            val collectedMinutes = mutableListOf<Int>()
            
            // Search name in raw log lines
            logLines.forEach { line ->
                if (findNameMatches(line, name)) {
                    val times = extractTimesFromLine(line)
                    collectedMinutes.addAll(times)
                }
            }

            val stt = index + 1
            val distinctMinutes = collectedMinutes.distinct().sorted()

            if (distinctMinutes.isEmpty()) {
                parsedList.add(
                    ParsedMember(
                        stt = stt,
                        name = name,
                        checkIn = "--:--",
                        checkOut = "--:--",
                        totalHours = 0.0,
                        status = "Không tham gia",
                        isPresent = false,
                        note = "Vắng mặt"
                    )
                )
            } else if (distinctMinutes.size == 1) {
                // Only one timestamp detected
                val singleTimeStr = formatMinutesToTime(distinctMinutes.first())
                parsedList.add(
                    ParsedMember(
                        stt = stt,
                        name = name,
                        checkIn = singleTimeStr,
                        checkOut = "--:--",
                        totalHours = 0.0,
                        status = "Thiếu dữ liệu check-out",
                        isPresent = true,
                        note = "Chỉ quét FaceID 1 lần"
                    )
                )
            } else {
                // Standard case: check-in (earliest) and check-out (latest)
                val checkInMin = distinctMinutes.first()
                val checkOutMin = distinctMinutes.last()
                val checkInStr = formatMinutesToTime(checkInMin)
                val checkOutStr = formatMinutesToTime(checkOutMin)
                
                val durationMin = checkOutMin - checkInMin
                val totalHours = durationMin / 60.0
                
                val status = if (totalHours >= 4.0) {
                    "Đạt chỉ tiêu tình nguyện"
                } else {
                    "Chưa đạt chỉ tiêu (Dưới 4h)"
                }

                parsedList.add(
                    ParsedMember(
                        stt = stt,
                        name = name,
                        checkIn = checkInStr,
                        checkOut = checkOutStr,
                        totalHours = totalHours,
                        status = status,
                        isPresent = true,
                        note = "Tổng: ${formatHours(totalHours)}"
                    )
                )
            }
        }
        return parsedList
    }

    /**
     * Checks if a name matches inside a log line exactly, preventing substring false-matches.
     * For instance, "Nguyễn Văn A" should not match "Nguyễn Văn An".
     */
    private fun findNameMatches(line: String, name: String): Boolean {
        val cleanLine = line.trim().lowercase()
        val cleanName = name.trim().lowercase()
        if (cleanName.isEmpty()) return false

        var index = cleanLine.indexOf(cleanName)
        while (index != -1) {
            // Check boundary to avoid word parts matching
            val leftOk = index == 0 || !cleanLine[index - 1].isLetterOrDigit()
            val rightIndex = index + cleanName.length
            val rightOk = rightIndex == cleanLine.length || !cleanLine[rightIndex].isLetterOrDigit()

            if (leftOk && rightOk) {
                return true
            }
            index = cleanLine.indexOf(cleanName, index + 1)
        }
        return false
    }

    /**
     * Extracts numerical time expressions (HH:MM / HHhMM / HHh) from a text line.
     */
    private fun extractTimesFromLine(line: String): List<Int> {
        val times = mutableListOf<Int>()

        // 1. Regex for HH:MM:SS or HH:MM
        val colonRegex = Regex("\\b([0-1]?\\d|2[0-3])[:.]([0-5]\\d)(?:[:.]([0-5]\\d))?\\s*([aApP][mM])?\\b")
        val matches1 = colonRegex.findAll(line)
        for (m in matches1) {
            var hour = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            val ampm = m.groupValues[4]
            if (ampm.isNotEmpty()) {
                if (ampm.lowercase() == "pm" && hour < 12) hour += 12
                if (ampm.lowercase() == "am" && hour == 12) hour = 0
            }
            times.add(hour * 60 + min)
        }

        // 2. Regex for HHhMM or HHh (Vietnamese timeline, e.g. "8h30" or "17h")
        val hRegex = Regex("\\b([0-1]?\\d|2[0-3])[hH]([0-5]\\d)?\\b")
        val matches2 = hRegex.findAll(line)
        for (m in matches2) {
            val hour = m.groupValues[1].toInt()
            val minStr = m.groupValues[2]
            val min = if (minStr.isNotEmpty()) minStr.toInt() else 0
            val minsValue = hour * 60 + min
            if (minsValue !in times) {
                times.add(minsValue)
            }
        }

        return times
    }

    private fun formatMinutesToTime(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hrs, mins)
    }

    fun formatHours(hours: Double): String {
        val totalMins = (hours * 60).toInt()
        val h = totalMins / 60
        val m = totalMins % 60
        return if (h > 0) {
            if (m > 0) "${h}h ${m}p" else "${h}h"
        } else {
            "${m}p"
        }
    }

    /**
     * Helper to export data list as Markdown Table.
     */
    fun exportToMarkdownTable(list: List<ParsedMember>): String {
        val sb = StringBuilder()
        sb.append("| STT | Họ và Tên | Check-in | Check-out | Tổng thời gian | Trạng thái | Ghi chú |\n")
        sb.append("|---|---|---|---|---|---|---|\n")
        list.forEach { item ->
            val hoursStr = if (item.totalHours > 0) "${String.format("%.1f", item.totalHours)} giờ (${formatHours(item.totalHours)})" else "--"
            sb.append("| ${item.stt} | ${item.name} | ${item.checkIn} | ${item.checkOut} | $hoursStr | ${item.status} | ${item.note} |\n")
        }
        return sb.toString()
    }

    /**
     * Serializes a list of ParsedMember directly into JSON.
     */
    fun parsedListToJson(list: List<ParsedMember>): String {
        val jsonArray = JSONArray()
        list.forEach { member ->
            val obj = JSONObject().apply {
                put("stt", member.stt)
                put("name", member.name)
                put("checkIn", member.checkIn)
                put("checkOut", member.checkOut)
                put("totalHours", member.totalHours)
                put("status", member.status)
                put("isPresent", member.isPresent)
                put("note", member.note)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * Deserializes a list of ParsedMember from JSON.
     */
    fun jsonToParsedList(jsonStr: String): List<ParsedMember> {
        val list = mutableListOf<ParsedMember>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ParsedMember(
                        stt = obj.getInt("stt"),
                        name = obj.getString("name"),
                        checkIn = obj.getString("checkIn"),
                        checkOut = obj.getString("checkOut"),
                        totalHours = obj.getDouble("totalHours"),
                        status = obj.getString("status"),
                        isPresent = obj.getBoolean("isPresent"),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
