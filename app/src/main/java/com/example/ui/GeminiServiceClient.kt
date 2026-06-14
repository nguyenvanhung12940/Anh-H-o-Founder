package com.example.ui

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiServiceClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls the Gemini 3.5 Flash API to analyze the roster and FaceID logs.
     * Returns the finalized Markdown table string.
     */
    suspend fun analyzeCheckinsWithAI(
        userApiKey: String?,
        rosterText: String,
        logText: String
    ): String = withContext(Dispatchers.IO) {
        val resolvedKey = when {
            !userApiKey.isNullOrBlank() -> userApiKey
            BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (resolvedKey.isEmpty()) {
            return@withContext "LỖI: Chưa cấu hình Gemini API Key. Vui lòng nhập API Key ở phần Quản lý API để sử dụng tính năng phân tích AI."
        }

        val systemInstruction = """
            Bạn là một Chuyên gia Xử lý Dữ liệu và Điều phối nhân sự Đoàn xuất sắc. Nhiệm vụ của bạn là:
            1. Tiếp nhận (1) Danh sách Đoàn viên gốc và (2) Nhật ký FaceID quét thô (gồm Tên và mốc thời gian Check-in/Check-out ngẫu nhiên).
            2. Đối chiếu, lọc trùng lặp và liệt kê chính xác những người thực tế có tham gia.
            3. Tính toán tổng số giờ tham gia của từng người (Thời gian Check-out trừ Check-in). Nếu chỉ có 1 mốc, ghi nhận check-in và báo vắng check-out.
            4. Nếu tổng thời gian lớn hơn hoặc bằng 4 tiếng, hãy tự động thêm cột 'Trạng thái' ghi 'Đạt chỉ tiêu tình nguyện'. Nếu ít hơn, ghi 'Chưa đạt chỉ tiêu'. Nếu vắng hoàn toàn, ghi 'Không tham gia'.
            5. Sắp xếp kết quả cuối cùng theo thứ tự chữ cái Tiếng Việt của Tên.
            6. Xuất kết quả dưới dạng bảng Markdown sạch sẽ, hoàn hảo cho việc đồng bộ vào Microsoft Excel/Google Sheets. Hãy viết hoàn toàn bằng Tiếng Việt.
        """.trimIndent()

        val promptText = """
            Dưới đây là dữ liệu đầu vào cần xử lý:

            --- (1) DANH SÁCH ĐOÀN VIÊN GỐC ---
            $rosterText

            --- (2) NHẬT KÝ FACEID THÔ ---
            $logText

            Hãy phân tích thông minh, chính xác, tính toán số giờ cẩn thận và trả về bảng Markdown hoàn chỉnh cùng một chương trình thống kê tóm tắt ngắn gọn (Số người tham gia, số người đạt chỉ tiêu, số người vắng).
        """.trimIndent()

        try {
            val jsonRequestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                // Make it deterministic and clean
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$resolvedKey")
                .post(jsonRequestBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Lỗi API (${response.code}): ${response.body?.string() ?: "Không rõ nguyên nhân"}"
                }

                val responseBodyStr = response.body?.string()
                    ?: return@withContext "Lỗi: Phản hồi từ Gemini API rỗng."

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext "Lỗi: Không tìm thấy nội dung phản hồi từ AI."
                }

                val textResult = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .optString("text")

                if (textResult.isNullOrBlank()) {
                    "Lỗi: Nội dung phản hồi từ AI trống."
                } else {
                    textResult
                }
            }
        } catch (e: Exception) {
            "Lỗi kết nối hoặc xử lý API: ${e.message}"
        }
    }
}
