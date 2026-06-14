package com.example.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ProcessingSession
import com.example.data.parser.LogParser
import com.example.data.parser.ParsedMember
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: SessionRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    // Persistent events list
    val allSessions: StateFlow<List<ProcessingSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _rosterInput = MutableStateFlow(DEFAULT_SAMPLE_ROSTER)
    val rosterInput = _rosterInput.asStateFlow()

    private val _logInput = MutableStateFlow(DEFAULT_SAMPLE_LOGS)
    val logInput = _logInput.asStateFlow()

    private val _sessionTitleInput = MutableStateFlow("Sự kiện Check-in Đoàn")
    val sessionTitleInput = _sessionTitleInput.asStateFlow()

    private val _apiKeyInput = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val apiKeyInput = _apiKeyInput.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _parsedResults = MutableStateFlow<List<ParsedMember>>(emptyList())
    val parsedResults = _parsedResults.asStateFlow()

    private val _markdownResult = MutableStateFlow("")
    val markdownResult = _markdownResult.asStateFlow()

    private val _usingAiResult = MutableStateFlow(false)
    val usingAiResult = _usingAiResult.asStateFlow()

    private val _aiOutputText = MutableStateFlow("")
    val aiOutputText = _aiOutputText.asStateFlow()

    private val _selectedSession = MutableStateFlow<ProcessingSession?>(null)
    val selectedSession = _selectedSession.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // State for user session and login role
    private val _currentUserRole = MutableStateFlow<String?>(null) // null = not logged in, "member" = Đoàn viên, "admin" = Quản trị viên/Bí thư
    val currentUserRole = _currentUserRole.asStateFlow()

    private val _loggedInMemberName = MutableStateFlow("")
    val loggedInMemberName = _loggedInMemberName.asStateFlow()

    init {
        // Run a default offline analysis of sample data on startup so the app is populated beautifully
        runOfflineAnalysis()
    }

    fun updateRoster(text: String) {
        _rosterInput.value = text
    }

    fun updateLogs(text: String) {
        _logInput.value = text
    }

    fun updateTitle(text: String) {
        _sessionTitleInput.value = text
    }

    fun updateApiKey(key: String) {
        _apiKeyInput.value = key
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Conducts a deterministic offline parsing flow instantly, updating the state.
     */
    fun runOfflineAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _usingAiResult.value = false
            _errorMessage.value = null
            try {
                val results = LogParser.parseLogs(_rosterInput.value, _logInput.value)
                _parsedResults.value = results
                _markdownResult.value = LogParser.exportToMarkdownTable(results)
                _aiOutputText.value = ""
            } catch (e: Exception) {
                _errorMessage.value = "Xảy ra lỗi khi phân tích: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Conducts an AI analysis via Gemini API, updating the state with rich summaries.
     */
    fun runAiAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _usingAiResult.value = true
            _errorMessage.value = null
            try {
                // First do a backup offline parse to populate the tabular results in parallel
                val backupResults = LogParser.parseLogs(_rosterInput.value, _logInput.value)
                _parsedResults.value = backupResults

                // Query Gemini
                val aiResponse = GeminiServiceClient.analyzeCheckinsWithAI(
                    _apiKeyInput.value,
                    _rosterInput.value,
                    _logInput.value
                )
                
                if (aiResponse.startsWith("LỖI") || aiResponse.startsWith("Lỗi")) {
                    _errorMessage.value = aiResponse
                    _usingAiResult.value = false
                    _markdownResult.value = LogParser.exportToMarkdownTable(backupResults)
                } else {
                    _aiOutputText.value = aiResponse
                    _markdownResult.value = aiResponse
                }
            } catch (e: Exception) {
                _errorMessage.value = "Xảy ra lỗi khi gọi AI: ${e.message}"
                _usingAiResult.value = false
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Persists the current session to the Room database.
     */
    fun saveCurrentSession() {
        viewModelScope.launch {
            try {
                val resultsJson = LogParser.parsedListToJson(_parsedResults.value)
                val newSession = ProcessingSession(
                    title = _sessionTitleInput.value.ifBlank { "Sự kiện " + System.currentTimeMillis() },
                    rosterText = _rosterInput.value,
                    logText = _logInput.value,
                    parsedResultsJson = resultsJson,
                    aiSummary = if (_usingAiResult.value) _aiOutputText.value else null
                )
                repository.saveSession(newSession)
            } catch (e: Exception) {
                _errorMessage.value = "Không thể lưu vào Lịch Sử: ${e.message}"
            }
        }
    }

    /**
     * Loads a session from database.
     */
    fun loadSession(session: ProcessingSession) {
        _selectedSession.value = session
        _sessionTitleInput.value = session.title
        _rosterInput.value = session.rosterText
        _logInput.value = session.logText
        
        if (session.aiSummary != null && session.aiSummary.isNotBlank()) {
            _usingAiResult.value = true
            _aiOutputText.value = session.aiSummary
            _markdownResult.value = session.aiSummary
        } else {
            _usingAiResult.value = false
            _aiOutputText.value = ""
        }

        val restoredList = LogParser.jsonToParsedList(session.parsedResultsJson)
        _parsedResults.value = restoredList
        
        if (!_usingAiResult.value) {
            _markdownResult.value = LogParser.exportToMarkdownTable(restoredList)
        }
    }

    /**
     * Deletes a session from database.
     */
    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteSessionById(sessionId)
                if (_selectedSession.value?.id == sessionId) {
                    _selectedSession.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Không thể xóa sự kiện: ${e.message}"
            }
        }
    }

    fun fillSampleData() {
        _rosterInput.value = DEFAULT_SAMPLE_ROSTER
        _logInput.value = DEFAULT_SAMPLE_LOGS
        _sessionTitleInput.value = "Sự kiện Check-in Đoàn"
        runOfflineAnalysis()
    }

    fun clearInputs() {
        _rosterInput.value = ""
        _logInput.value = ""
        _sessionTitleInput.value = ""
        _parsedResults.value = emptyList()
        _markdownResult.value = ""
        _aiOutputText.value = ""
        _usingAiResult.value = false
        _selectedSession.value = null
    }

    fun loginAsMember(name: String) {
        _currentUserRole.value = "member"
        _loggedInMemberName.value = name.trim()
    }

    fun loginAsAdmin() {
        _currentUserRole.value = "admin"
        _loggedInMemberName.value = ""
    }

    fun logout() {
        _currentUserRole.value = null
        _loggedInMemberName.value = ""
    }

    /**
     * Resolves a barcode scan string (e.g. ID "DV003" or name "Lê Hoàng Quốc Duy") to the corresponding ParsedMember.
     */
    fun findMemberByBarcode(barcode: String): ParsedMember? {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return null
        
        return _parsedResults.value.find { member ->
            val expectedId = String.format("DV%03d", member.stt)
            expectedId.equals(cleanBarcode, ignoreCase = true) || 
            member.name.trim().equals(cleanBarcode, ignoreCase = true)
        }
    }

    /**
     * Attempts to check in/out a volunteer by their ID or name via scanner, returning the result.
     * Appends a compatible log line to logs to maintain consistency and re-analyzes.
     */
    fun checkInMemberByBarcode(barcode: String, isCheckIn: Boolean, customTimeStr: String? = null): Pair<Boolean, String> {
        val member = findMemberByBarcode(barcode) ?: return Pair(false, "Không tìm thấy Đoàn viên có mã số hoặc tên '$barcode' trong danh sách.")
        
        val timeStr = if (customTimeStr.isNullOrBlank()) {
            val now = java.util.Calendar.getInstance()
            String.format("%02d:%02d:%02d", now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), now.get(java.util.Calendar.SECOND))
        } else {
            if (customTimeStr.length == 5) "$customTimeStr:00" else customTimeStr
        }
        
        val dateToday = "14/06/2026"
        val checkType = if (isCheckIn) "Vào cổng" else "Ra cổng"
        
        val newLogLine = "[$dateToday $timeStr] FaceID quét thành công: ${member.name} - $checkType"
        
        val currentLogs = _logInput.value.trim()
        val updatedLogs = if (currentLogs.isEmpty()) {
            newLogLine
        } else {
            currentLogs + "\n" + newLogLine
        }
        
        _logInput.value = updatedLogs
        runOfflineAnalysis()
        
        val actionVerb = if (isCheckIn) "Vào cổng" else "Ra cổng"
        return Pair(true, "Điểm danh $actionVerb thành công cho đồng chí ${member.name} lúc $timeStr!")
    }

    companion object {
        const val DEFAULT_SAMPLE_ROSTER = """Nguyễn Văn An
Trần Thị Bình
Lê Hoàng Quốc Duy
Phạm Minh Hải
Nguyễn Hoàng Lan
Đỗ Đức Mạnh
Vũ Thị Ngọc
Trịnh Văn Hải
Bùi Thanh Thảo"""

        const val DEFAULT_SAMPLE_LOGS = """[14/06/2026 07:55:12] FaceID quét thành công: Nguyễn Văn An - Vào cổng
[14/06/2026 08:00:45] FaceID quét thành công: Trần Thị Bình - Vào cổng
[14/06/2026 08:02:11] FaceID quét thành công: Lê Hoàng Quốc Duy - Vào cổng
[14/06/2026 08:15:30] FaceID quét thành công: Nguyễn Hoàng Lan - Vào cổng
[14/06/2026 08:20:00] FaceID quét thành công: Đỗ Đức Mạnh - Vào cổng
[14/06/2026 08:22:15] FaceID quét thành công: Vũ Thị Ngọc - Vào cổng
[14/06/2026 08:30:00] FaceID quét thành công: Trịnh Văn Hải - Vào cổng
[14/06/2026 11:30:22] FaceID quét thành công: Trần Thị Bình - Ra cổng (Về sớm)
[14/06/2026 12:00:15] FaceID quét thành công: Nguyễn Văn An - Vào cổng (Ca chiều)
[14/06/2026 16:45:10] FaceID quét thành công: Lê Hoàng Quốc Duy - Ra cổng (Hoàn thành)
[14/06/2026 17:00:05] FaceID quét thành công: Nguyễn Hoàng Lan - Ra cổng (Hoàn thành)
[14/06/2026 17:05:40] FaceID quét thành công: Đỗ Đức Mạnh - Ra cổng (Hoàn thành)
[14/06/2026 17:15:22] FaceID quét thành công: Vũ Thị Ngọc - Ra cổng (Hoàn thành)
[14/06/2026 17:30:00] FaceID quét thành công: Nguyễn Văn An - Ra cổng (Hoàn thành)"""
    }
}

class MainViewModelFactory(
    private val repository: SessionRepository,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
