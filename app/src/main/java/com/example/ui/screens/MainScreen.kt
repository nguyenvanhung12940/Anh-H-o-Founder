package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.example.data.database.ProcessingSession
import com.example.data.parser.LogParser
import com.example.data.parser.ParsedMember
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val rosterInput by viewModel.rosterInput.collectAsStateWithLifecycle()
    val logInput by viewModel.logInput.collectAsStateWithLifecycle()
    val sessionTitleInput by viewModel.sessionTitleInput.collectAsStateWithLifecycle()
    val apiKeyInput by viewModel.apiKeyInput.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val parsedResults by viewModel.parsedResults.collectAsStateWithLifecycle()
    val markdownResult by viewModel.markdownResult.collectAsStateWithLifecycle()
    val usingAiResult by viewModel.usingAiResult.collectAsStateWithLifecycle()
    val aiOutputText by viewModel.aiOutputText.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val loggedInMemberName by viewModel.loggedInMemberName.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Xử lý Check-in, 1 = Lịch sử & Cấu hình
    var showApiKey by remember { mutableStateOf(false) }
    var isInputsExpanded by remember { mutableStateOf(false) }

    // Display error toasts cleanly
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    if (currentUserRole == null) {
        LoginScreen(viewModel = viewModel)
    } else if (currentUserRole == "member") {
        MemberDashboardScreen(viewModel = viewModel, memberName = loggedInMemberName)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            YouthUnionLogo(modifier = Modifier.size(40.dp))
                            Column {
                                Text(
                                    text = "Điều phối Sự kiện",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Chế độ: Chuyên gia Xử lý",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = TextStyle(letterSpacing = 0.5.sp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(18.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Đăng xuất",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.fillSampleData()
                                Toast.makeText(context, "Đã nạp dữ liệu mẫu!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(18.dp))
                                .testTag("load_sample_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Nạp dữ liệu mẫu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // Sliding tab selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Quét & Xử Lý", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ", modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Lịch Sử & Cấu Hình", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cấu hình", modifier = Modifier.size(18.dp)) }
                )
            }

            if (activeTab == 0) {
                // TAB 1: Home Processing Area
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        QrScannerAdminCard(viewModel = viewModel)
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(20.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .animateContentSize()
                            ) {
                                // Collapsible Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isInputsExpanded = !isInputsExpanded },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isInputsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Cấu Hình & Nhập Liệu",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Danh sách, nhật ký & phân tích sự kiện",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    IconButton(onClick = { isInputsExpanded = !isInputsExpanded }) {
                                        Icon(
                                            imageVector = if (isInputsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isInputsExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFFBEAE5))

                                        OutlinedTextField(
                                            value = sessionTitleInput,
                                            onValueChange = { viewModel.updateTitle(it) },
                                            label = { Text("Tên Hoạt Động / Sự Kiện", fontSize = 12.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("session_title_input"),
                                            singleLine = true,
                                            placeholder = { Text("Ví dụ: Hè Tình Nguyện Chi Đoàn 2026", fontSize = 12.sp) }
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Danh sách gốc",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "${rosterInput.split('\n').filter { it.isNotBlank() }.size} người",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    OutlinedTextField(
                                                        value = rosterInput,
                                                        onValueChange = { viewModel.updateRoster(it) },
                                                        placeholder = { Text("Mỗi người một dòng...", fontSize = 11.sp) },
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .testTag("roster_input"),
                                                        textStyle = TextStyle(fontSize = 11.sp)
                                                    )
                                                }
                                            }

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Nhật ký FaceID",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "${logInput.split('\n').filter { it.isNotBlank() }.size} dòng",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    OutlinedTextField(
                                                        value = logInput,
                                                        onValueChange = { viewModel.updateLogs(it) },
                                                        placeholder = { Text("Nhật ký check-in...", fontSize = 11.sp) },
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .testTag("logs_input"),
                                                        textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.runOfflineAnalysis() },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier
                                                    .weight(1.1f)
                                                    .height(42.dp)
                                                    .testTag("offline_parse_button"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = "Parse offline", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Phân Tích Thường", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            Button(
                                                onClick = { viewModel.runAiAnalysis() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFFB300),
                                                    contentColor = Color.Black
                                                ),
                                                modifier = Modifier
                                                    .weight(1.1f)
                                                    .height(42.dp)
                                                    .testTag("ai_parse_button"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = "Parse Gemini AI", tint = Color.Black, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Phân Tích Bằng AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.saveCurrentSession()
                                                    Toast.makeText(context, "Đã lưu vào lịch sử sự kiện!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                                                    .size(42.dp)
                                                    .testTag("save_session_button"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Lưu hoạt động",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.clearInputs() },
                                                modifier = Modifier
                                                    .background(Colors_RedContainer, RoundedCornerShape(10.dp))
                                                    .size(42.dp)
                                                    .testTag("clear_inputs_button"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa trắng",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isAnalyzing) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = if (usingAiResult) "Đang liên hệ Chuyên gia AI..." else "Đang phân tích và đối sánh...",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    if (parsedResults.isNotEmpty() && !isAnalyzing) {
                        val presentCount = parsedResults.count { it.isPresent }
                        val meetCriteriaCount = parsedResults.count { it.totalHours >= 4.0 }
                        val absentCount = parsedResults.count { !it.isPresent }

                        item {
                            // STATISTICS COMPONENT
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Kết Quả Thống Kê Sơ Bộ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatisticBox(
                                            title = "Thực Tế Tham Gia",
                                            value = "$presentCount / ${parsedResults.size}",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            borderColor = Color(0xFFEACFC8),
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatisticBox(
                                            title = "Đạt Chỉ Tiêu (>=4h)",
                                            value = "$meetCriteriaCount",
                                            containerColor = Color(0xFFE8DEF8),
                                            borderColor = Color(0xFFD0BCFF),
                                            contentColor = Color(0xFF4F378B),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatisticBox(
                                            title = "Vắng Mặt",
                                            value = "$absentCount",
                                            containerColor = Color(0xFFFEE8E6),
                                            borderColor = Color(0xFFF9C3C0),
                                            contentColor = Color(0xFFB3261E),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // RESULT EXPORT AND DISPLAY BUTTONS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Báo Cáo Thành Tích (Markdown / Excel)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(markdownResult))
                                                Toast.makeText(context, "Đã sao chép bảng Markdown!", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sao chép Bảng", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display raw markdown string inside box for copying
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                            .heightIn(max = 180.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = markdownResult,
                                            onValueChange = {},
                                            readOnly = true,
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxSize(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Bảng Thống Kê Chi Tiết Nhân Sự",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Detailed list of parsed members with colorful state indicators
                        items(parsedResults) { member ->
                            MemberStatusCard(member = member)
                        }
                    } else if (!isAnalyzing) {
                        item {
                            EmptyStateComponent(
                                title = "Chưa có dữ liệu phân tích",
                                subtitle = "Hãy nhập Danh sách và Nhật ký FaceID bên trên, sau đó ấn 'Phân Tích Thường' hoặc 'Phân Tích bằng AI' để xem kết quả thống kê hoạt động."
                            )
                        }
                    }
                }
            } else {
                // TAB 2: HISTORY & CREDENTIALS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // API Configuration Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Quản lý Chìa Khóa API (Gemini)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "Chìa khóa này được dùng để gửi yêu cầu phân tích thông minh tới mô hình Gemini 3.5 Flash nhằm đọc hiểu tiếng Việt ngẫu nhiên hiệu quả tối đa.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { viewModel.updateApiKey(it) },
                                    label = { Text("Gemini API Key") },
                                    placeholder = { Text("Nhập AI_STUDIO_API_KEY...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("api_key_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                imageVector = if (showApiKey) Icons.Default.Lock else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Show Key"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "An toàn bảo mật",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Khóa được lưu cục bộ trên thiết bị của bạn an toàn.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Lịch Sử Xử Lý Sự Kiện (${allSessions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (allSessions.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "Trống",
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Chưa có sự kiện nào được lưu",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Nhấn nút Lưu (biểu tượng chia sẻ/lưu) ở Tab quét chính để ghi lại phiên làm việc này.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(allSessions) { session ->
                            HistorySessionCard(
                                session = session,
                                selected = selectedSession?.id == session.id,
                                onLoad = {
                                    viewModel.loadSession(session)
                                    activeTab = 0
                                    Toast.makeText(context, "Đã nạp lại ${session.title}!", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.deleteSession(session.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun StatisticBox(
    title: String,
    value: String,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .padding(4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MemberStatusCard(member: ParsedMember) {
    val isMeet = member.totalHours >= 4.0
    
    val cardColor = when {
        !member.isPresent -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        isMeet -> Color(0xFFFBFEEB)  // Soft light green tint
        else -> Color(0xFFFFF8F6)      // Warm light rose/peach tint
    }

    val cardBorderColor = when {
        !member.isPresent -> Color(0xFFEFEFEF)
        isMeet -> Color(0xFFE1F5FE)
        else -> Color(0xFFFCEAE5)
    }

    val tagBgColor = when {
        !member.isPresent -> Color(0xFFE2E2E2)
        isMeet -> Color(0xFFD1E7DD)  // Green background
        else -> Color(0xFFF8D7DA)    // Red background
    }

    val tagTextColor = when {
        !member.isPresent -> Color(0xFF555555)
        isMeet -> Color(0xFF0F5132)  // Green text
        else -> Color(0xFF842029)    // Red text
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${member.stt}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (member.isPresent) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = "Vào", tint = Color(0xFF8F4C38).copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Đến: ${member.checkIn}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Clear, contentDescription = "Ra", tint = Color(0xFF4F378B).copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Về: ${member.checkOut}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = tagBgColor,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = member.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                
                if (member.isPresent && member.totalHours > 0) {
                    Text(
                        text = "${String.format("%.1f", member.totalHours)}h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySessionCard(
    session: ProcessingSession,
    selected: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(session.timestamp))
    } catch (e: Exception) {
        "--"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    val aiTag = if (session.aiSummary != null) "Mô hình AI" else "Phần mềm"
                    Surface(
                        color = if (session.aiSummary != null) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = aiTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (session.aiSummary != null) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLoad) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Tải lại",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Colors_Red_Text
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateComponent(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Empty Logo",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// Brand theme color values
val Colors_Green_Accent = Color(0xFF2E7D32)
val Colors_Red_Text = Color(0xFFC62828)
val Colors_RedContainer = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isMemberTab by remember { mutableStateOf(true) }

    var memberNameInput by remember { mutableStateOf("") }
    var memberIdInput by remember { mutableStateOf("") }

    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }

    val rosterInput by viewModel.rosterInput.collectAsStateWithLifecycle()
    val availableNames = remember(rosterInput) {
        rosterInput.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            YouthUnionLogo(modifier = Modifier.size(68.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Điều phối Sự kiện",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "CỔNG ĐIỂM DANH ĐOÀN VIÊN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = TextStyle(letterSpacing = 1.5.sp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(6.dp)
                    .border(1.dp, Color(0xFFFBEAE5), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isMemberTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isMemberTab = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ĐOÀN VIÊN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isMemberTab) Color.White else MaterialTheme.colorScheme.secondary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isMemberTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isMemberTab = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BÍ THƯ XÃ / BAN CÁN TRỊ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (!isMemberTab) Color.White else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isMemberTab) {
                        Text(
                            text = "Đăng Nhập Cổng Đoàn Viên",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = memberNameInput,
                            onValueChange = { memberNameInput = it },
                            label = { Text("Họ và Tên Đoàn Viên") },
                            placeholder = { Text("Ví dụ: Nguyễn Văn An") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("member_name_input")
                        )

                        OutlinedTextField(
                            value = memberIdInput,
                            onValueChange = { memberIdInput = it },
                            label = { Text("Mã Số Đoàn Viên (Không bắt buộc)") },
                            placeholder = { Text("Nhập MSĐV (nếu có)") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (availableNames.isNotEmpty()) {
                            Text(
                                text = "Chọn nhanh tên đồng chí có trong danh bạ:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableNames.take(8).forEach { name ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { memberNameInput = name }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (memberNameInput.trim().isBlank()) {
                                    Toast.makeText(context, "Vui lòng chọn hoặc điền Họ tên đồng chí!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.loginAsMember(memberNameInput)
                                    Toast.makeText(context, "Xin chào đồng chí $memberNameInput!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_member_button")
                        ) {
                            Text("ĐĂNG NHẬP HOẠT ĐỘNG", fontWeight = FontWeight.Bold)
                        }

                    } else {
                        Text(
                            text = "Đăng Nhập Cho Ban Cán Bộ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = adminUsernameInput,
                            onValueChange = { adminUsernameInput = it },
                            label = { Text("Tên Đăng Nhập Quản Trị") },
                            placeholder = { Text("admin") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_username_input")
                        )

                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = { adminPasswordInput = it },
                            label = { Text("Mật Khẩu") },
                            placeholder = { Text("Mặc định: 123456") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    adminUsernameInput = "admin"
                                    adminPasswordInput = "123456"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("MẪU BÍ THƯ XÃ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                if (adminUsernameInput.trim() == "admin" && adminPasswordInput == "123456") {
                                    viewModel.loginAsAdmin()
                                    Toast.makeText(context, "Đăng nhập vai trò Quản lý thành công!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sai thông tin! Gợi ý: admin | 123456", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_admin_button")
                        ) {
                            Text("ĐĂNG NHẬP HỆ THỐNG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCEAE5).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "“Đâu cần thanh niên có, việc gì khó có thanh niên” — Khẩu hiệu hành động của Đoàn TNCS Hồ Chí Minh.",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    viewModel: MainViewModel,
    memberName: String
) {
    val context = LocalContext.current
    val parsedResults by viewModel.parsedResults.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val sessionTitleInput by viewModel.sessionTitleInput.collectAsStateWithLifecycle()

    val currentMemberData = remember(parsedResults, memberName) {
        parsedResults.find { it.name.trim().equals(memberName.trim(), ignoreCase = true) }
    }

    val matchingHistory = remember(allSessions, memberName) {
        allSessions.mapNotNull { session ->
            try {
                val list = LogParser.jsonToParsedList(session.parsedResultsJson)
                val match = list.find { it.name.trim().equals(memberName.trim(), ignoreCase = true) }
                if (match != null) {
                    Pair(session, match)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        YouthUnionLogo(modifier = Modifier.size(36.dp))
                        Column {
                            Text(
                                text = "Đồng chí: $memberName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cổng thông tin Đoàn Viên",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "Đã đăng xuất cổng Đoàn Viên!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEBCFC8), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "XIN CHÀO ĐỒNG CHÍ!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(letterSpacing = 1.sp)
                        )
                        Text(
                            text = memberName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Đây là cổng tra cứu học tập, tham gia tình nguyện và tích lũy giờ Đoàn viên của riêng đồng chí.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Dữ liệu sự kiện đang diễn ra".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    style = TextStyle(letterSpacing = 1.sp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(24.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = sessionTitleInput.ifBlank { "Sự kiện hiện tại" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentMemberData != null) {
                            val isMeet = currentMemberData.totalHours >= 4.0
                            val statusBg = if (isMeet) Color(0xFFD1E7DD) else Color(0xFFF8D7DA)
                            val statusText = if (isMeet) Color(0xFF0F5132) else Color(0xFF842029)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Thời gian quét FaceID", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đến: ${currentMemberData.checkIn}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF4F378B), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Về: ${currentMemberData.checkOut}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("Tổng giờ", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = "${String.format("%.1f", currentMemberData.totalHours)}h",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFF0E0DC))
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Đánh giá tiêu chuẩn", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = if (isMeet) "ĐẠT TIÊU CHUẨN ĐOÀN VIÊN" else "CHƯA ĐỦ ĐIỀU KIỆN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isMeet) Colors_Green_Accent else Color(0xFFB3261E)
                                    )
                                }

                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = currentMemberData.status,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEBCFC8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Đồng chí chưa có dữ liệu điểm danh",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tên đồng chí không có trong danh sách Roster hoặc FaceID Log chưa ghi nhận lượt quét.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sổ tay tham gia lịch sử".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    style = TextStyle(letterSpacing = 1.sp)
                )
            }

            if (matchingHistory.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(20.dp)),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Lịch sử sự kiện trống",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Toàn bộ lịch sử hoạt động quá khứ chưa lưu trữ dữ liệu của đồng chí.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                items(matchingHistory) { (session, match) ->
                    val isMeet = match.totalHours >= 4.0
                    val statusBg = if (isMeet) Color(0xFFE8F5E9) else Color(0xFFFEE8E6)
                    val statusText = if (isMeet) Color(0xFF2E7D32) else Color(0xFFB3261E)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${String.format("%.1f", match.totalHours)}h",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = match.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusText
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = "STT: ${match.stt}", fontSize = 11.sp, color = Color.Gray)
                                Text(text = "Vào: ${match.checkIn}", fontSize = 11.sp, color = Color.Gray)
                                Text(text = "Ra: ${match.checkOut}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCEAE5).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "YÊU CẦU ĐÍNH CHÍNH / BÁO CÁO SAI LỆCH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Nếu đồng chí phát hiện số giờ quét FaceID hoặc thông tin check-in bị thiếu, vui lòng liên hệ trực tiếp với Thường trực Đoàn Xã / Quản trị viên để kịp thời cập nhật lại trong file Excel gốc.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Analyzer passing camera images to Google Play Services ML Kit Barcode Detector.
 */
@androidx.camera.core.ExperimentalGetImage
class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onQrCodeScanned(value)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

/**
 * CameraX PreviewView with integrated image analyzer for instant QR scanning.
 */
@Composable
fun CameraScannerView(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasError by remember { mutableStateOf(false) }

    if (hasError) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lỗi thiết bị máy ảnh.\nVui lòng dử dụng chế độ mô phỏng quét.",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    } else {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor, @androidx.camera.core.ExperimentalGetImage { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                BarcodeScanning.getClient().process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                onBarcodeScanned(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        })

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        hasError = true
                    }
                }, executor)
                previewView
            },
            modifier = modifier
        )
    }
}

/**
 * Admin component for quick local QR registration.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScannerAdminCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isLiveCameraMode by remember { mutableStateOf(false) }
    var isCheckInAction by remember { mutableStateOf(true) }
    var cameraActive by remember { mutableStateOf(false) }
    
    var lastScannedValue by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableLongStateOf(0L) }
    var activeScanSuccessMember by remember { mutableStateOf<ParsedMember?>(null) }
    var activeScanMessage by remember { mutableStateOf("") }
    
    var selectedSimHourOption by remember { mutableStateOf("08:00") }
    val simHoursList = listOf("08:00", "08:15", "11:30", "12:00", "13:30", "17:00")

    val parsedResults by viewModel.parsedResults.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qr_scanner_admin_card")
            .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "QR Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quét Mã QR Đoàn Viên Tại Chỗ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hệ thống đăng ký Check-in/Check-out nhanh",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFFBEAE5))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                    .padding(4.dp)
                    .border(1.dp, Color(0xFFFBEAE5), RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCheckInAction) Color(0xFFD1E7DD) else Color.Transparent)
                        .clickable { isCheckInAction = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isCheckInAction) Color(0xFF0F5132) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ĐIỂM DANH VÀO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isCheckInAction) Color(0xFF0F5132) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isCheckInAction) Color(0xFFFEE8E6) else Color.Transparent)
                        .clickable { isCheckInAction = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (!isCheckInAction) Color(0xFFB3261E) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ĐIỂM DANH RA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (!isCheckInAction) Color(0xFFB3261E) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chế độ:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                FilterChip(
                    selected = !isLiveCameraMode,
                    onClick = {
                        isLiveCameraMode = false
                        cameraActive = false
                    },
                    label = { Text("Mô phỏng Thẻ QR", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = isLiveCameraMode,
                    onClick = {
                        isLiveCameraMode = true
                    },
                    label = { Text("Quét Camera Thật", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = activeScanSuccessMember != null) {
                activeScanSuccessMember?.let { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isCheckInAction) Color(0xFFD1E7DD).copy(alpha = 0.5f) else Color(0xFFFEE8E6).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isCheckInAction) Color(0xFFBADBCC) else Color(0xFFF5C2C1), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(if (isCheckInAction) Color(0xFF0F5132) else Color(0xFFB3261E), RoundedCornerShape(21.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCheckInAction) Icons.Default.CheckCircle else Icons.Default.Clear,
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCheckInAction) Color(0xFF0F5132) else Color(0xFF842029)
                                )
                                Text(
                                    text = activeScanMessage,
                                    fontSize = 11.sp,
                                    color = if (isCheckInAction) Color(0xFF146c43) else Color(0xFF842029)
                                )
                            }
                            IconButton(onClick = { activeScanSuccessMember = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    tint = Color.Gray,
                                    contentDescription = "Close Feedback",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLiveCameraMode) {
                if (cameraActive) {
                    if (cameraPermissionState.status.isGranted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        ) {
                            CameraScannerView(
                                onBarcodeScanned = { rawBarcode ->
                                    val now = System.currentTimeMillis()
                                    if (rawBarcode != lastScannedValue || now - lastScanTime > 2000L) {
                                        lastScannedValue = rawBarcode
                                        lastScanTime = now
                                        
                                        val result = viewModel.checkInMemberByBarcode(
                                            barcode = rawBarcode,
                                            isCheckIn = isCheckInAction
                                        )
                                        
                                        if (result.first) {
                                            val found = viewModel.findMemberByBarcode(rawBarcode)
                                            activeScanSuccessMember = found
                                            activeScanMessage = result.second
                                            Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            val gridColor = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                
                                val boxW = w * 0.65f
                                val boxH = h * 0.65f
                                val startX = (w - boxW) / 2
                                val startY = (h - boxH) / 2
                                
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(w, startY)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, startY),
                                    size = Size(startX, boxH)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(startX + boxW, startY),
                                    size = Size(startX, boxH)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, startY + boxH),
                                    size = Size(w, startY)
                                )

                                val lineLength = 24.dp.toPx()
                                val strokeWidth = 3.dp.toPx()
                                
                                drawLine(gridColor, Offset(startX, startY), Offset(startX + lineLength, startY), strokeWidth)
                                drawLine(gridColor, Offset(startX, startY), Offset(startX, startY + lineLength), strokeWidth)

                                drawLine(gridColor, Offset(startX + boxW, startY), Offset(startX + boxW - lineLength, startY), strokeWidth)
                                drawLine(gridColor, Offset(startX + boxW, startY), Offset(startX + boxW, startY + lineLength), strokeWidth)

                                drawLine(gridColor, Offset(startX, startY + boxH), Offset(startX + lineLength, startY + boxH), strokeWidth)
                                drawLine(gridColor, Offset(startX, startY + boxH), Offset(startX, startY + boxH - lineLength), strokeWidth)

                                drawLine(gridColor, Offset(startX + boxW, startY + boxH), Offset(startX + boxW - lineLength, startY + boxH), strokeWidth)
                                drawLine(gridColor, Offset(startX + boxW, startY + boxH), Offset(startX + boxW, startY + boxH - lineLength), strokeWidth)
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.Green, RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = "ỐNG KÍNH HOẠT ĐỘNG",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { cameraActive = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dừng Máy Ảnh", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dừng máy ảnh quét", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Yêu cầu quyền", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Yêu cầu quyền máy ảnh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Quyền máy ảnh cần thiết để khởi chạy camera quét ID.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("CẤP QUYỀN CAMERA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFF0E0DC), RoundedCornerShape(16.dp))
                            .clickable { cameraActive = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Nhấn để khởi động Máy ảnh Quét QR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Quét thẻ tình nguyện viên trực tiếp để ghi nhận điểm danh",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CHỌN THỜI GIAN MÔ PHỎNG QUÉT SỰ KIỆN:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        simHoursList.forEach { simHour ->
                            val isSelected = selectedSimHourOption == simHour
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSimHourOption = simHour },
                                label = { Text(simHour, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    Text(
                        text = "DANH BẠ THẺ ĐOÀN VIÊN ĐỂ QUÉT NHANH:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (parsedResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chưa nạp Roster Đoàn viên. Hãy bấm biểu tượng Xoay vòng tròn hoặc điền danh sách trước.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(parsedResults) { member ->
                                val volId = String.format("DV%03d", member.stt)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .border(1.dp, Color(0xFFFBEAE5), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = volId,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val result = viewModel.checkInMemberByBarcode(
                                                    barcode = volId,
                                                    isCheckIn = isCheckInAction,
                                                    customTimeStr = selectedSimHourOption
                                                )
                                                if (result.first) {
                                                    val found = viewModel.findMemberByBarcode(volId)
                                                    activeScanSuccessMember = found
                                                    activeScanMessage = result.second
                                                    Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCheckInAction) Color(0xFF198754) else Color(0xFFDC3545)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isCheckInAction) "QUÉT VÀO" else "QUÉT RA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern vector emblem representing youth volunteerism
 */
@Composable
fun YouthUnionLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE53935), Color(0xFFC62828))
                ),
                shape = RoundedCornerShape(26) // Squircle modern emblem shape
            )
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw a circular thin elegant gold halo
            drawCircle(
                color = Color(0xFFFFD700),
                radius = w * 0.44f,
                style = Stroke(width = w * 0.04f)
            )

            // 2. Beautiful inner deep blue background representing youth
            drawCircle(
                color = Color(0xFF0D47A1),
                radius = w * 0.40f
            )

            // 3. Draw deep green path for checking / volunteerism
            val leafPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.20f, h * 0.65f)
                quadraticTo(w * 0.35f, h * 0.45f, w * 0.50f, h * 0.50f)
                quadraticTo(w * 0.65f, h * 0.55f, w * 0.80f, h * 0.35f)
                quadraticTo(w * 0.70f, h * 0.70f, w * 0.50f, h * 0.75f)
                quadraticTo(w * 0.30f, h * 0.80f, w * 0.20f, h * 0.65f)
                close()
            }
            drawPath(
                path = leafPath,
                color = Color(0xFF2E7D32)
            )

            // 4. Glowing gold 5-pointed star in the center
            val cx = w * 0.5f
            val cy = h * 0.5f
            val outerRadius = w * 0.18f
            val innerRadius = w * 0.07f
            val starPath = androidx.compose.ui.graphics.Path().apply {
                var angle = -Math.PI / 2
                val dAngle = Math.PI / 5
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerRadius else innerRadius
                    val x = cx + r * Math.cos(angle)
                    val y = cy + r * Math.sin(angle)
                    if (i == 0) {
                        moveTo(x.toFloat(), y.toFloat())
                    } else {
                        lineTo(x.toFloat(), y.toFloat())
                    }
                    angle += dAngle
                }
                close()
            }
            drawPath(
                path = starPath,
                color = Color(0xFFFFD700)
            )
        }
    }
}


