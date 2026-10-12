package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.NilePaySecurityEngine
import com.example.security.SecurityAlertLevel
import com.example.security.SecurityAuditEvent
import com.example.security.SecurityCategory
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityVaultDialog(
    onDismiss: () -> Unit
) {
    val isBiometrics by NilePaySecurityEngine.isBiometricEnforced.collectAsState()
    val isAntiRelay by NilePaySecurityEngine.isNfcAntiRelayActive.collectAsState()
    val isEncryption by NilePaySecurityEngine.isDataEncryptionEnforced.collectAsState()
    val isScreenProtect by NilePaySecurityEngine.isScreenCaptureProtected.collectAsState()
    val isFraudDetect by NilePaySecurityEngine.isFraudDetectionActive.collectAsState()
    val contactlessLimit by NilePaySecurityEngine.contactlessNoPinLimitEGP.collectAsState()
    val auditLogs by NilePaySecurityEngine.auditLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Controls, 1: Live Audit Logs, 2: Protocols

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_vault_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NileCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Vault",
                            tint = NileCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مركز أمان البطاقات و NFC",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "حماية مشددة مطابقة لمعايير البنك المركزي و Apple Pay",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NileDarkSurface,
                    contentColor = NileCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("دروع الأمان", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("سجل التدقيق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (auditLogs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(NileCyan)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${auditLogs.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("معايير CBE", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Shield Controls Tab
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                SecurityOverviewCard()
                            }

                            item {
                                SecurityToggleCard(
                                    title = "المصادقة بالبصمة البيومترية (Biometric / Face)",
                                    subtitle = "طلب بصمة الإصبع أو الوجه قبل أي عملية دفع تلامسي أو استعراض للبطاقة",
                                    icon = Icons.Default.Fingerprint,
                                    isChecked = isBiometrics,
                                    accentColor = NileCyan,
                                    onToggle = { NilePaySecurityEngine.toggleBiometrics(it) }
                                )
                            }

                            item {
                                SecurityToggleCard(
                                    title = "درع حماية NFC ضد الترحيل (Anti-Relay Guard)",
                                    subtitle = "رصد زمن الاستجابة ومنع محاولات اعتراض أو تمديد إشارة NFC عن بعد",
                                    icon = Icons.Default.Nfc,
                                    isChecked = isAntiRelay,
                                    accentColor = NileSuccess,
                                    onToggle = { NilePaySecurityEngine.toggleAntiRelay(it) }
                                )
                            }

                            item {
                                SecurityToggleCard(
                                    title = "التشفير التام للبيانات (AES-256-GCM Hardware Vault)",
                                    subtitle = "تشفير جميع بيانات البطاقات والرموز محلياً وسحابياً بمفاتيح أجهزة مشفرة",
                                    icon = Icons.Default.VpnKey,
                                    isChecked = isEncryption,
                                    accentColor = NileGold,
                                    onToggle = { NilePaySecurityEngine.toggleDataEncryption(it) }
                                )
                            }

                            item {
                                SecurityToggleCard(
                                    title = "منع لقطات الشاشة (Anti-Screen Capture / Spyware)",
                                    subtitle = "حظر تصوير الشاشة أو تسجيلها لحماية أرقام DPAN والرموز السرية",
                                    icon = Icons.Default.Lock,
                                    isChecked = isScreenProtect,
                                    accentColor = NilePrimary,
                                    onToggle = { NilePaySecurityEngine.toggleScreenCaptureProtection(it) }
                                )
                            }

                            item {
                                SecurityToggleCard(
                                    title = "الرصد الذكي للاحتيال (AI Anti-Fraud & Spending Guard)",
                                    subtitle = "مراقبة الحدود القصوى والعمليات غير الاعتيادية وتجميد البطاقة فوراً",
                                    icon = Icons.Default.Security,
                                    isChecked = isFraudDetect,
                                    accentColor = Color(0xFFE040FB),
                                    onToggle = { NilePaySecurityEngine.toggleFraudDetection(it) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Live Security Audit Logs Tab
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل الأمان الحي المباشر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { NilePaySecurityEngine.clearAuditLogs() }) {
                                    Text("مسح السجل", fontSize = 11.sp, color = NileError)
                                }
                            }

                            if (auditLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد أحداث مسجلة حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(auditLogs) { log ->
                                        SecurityLogItem(log)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // CBE & Apple Pay Standards Info Tab
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                ProtocolCard(
                                    title = "1. معيار الترميز الرقمي (Tokenization - VTS / MDES)",
                                    desc = "لا يتم حفظ أو إرسال رقم البطاقة الحقيقي (FPAN) مطلقاً. يتم إنشاء رقم افتراضي مشفر (DPAN) مخصص فقط لهذا الهاتف ولا يمكن استخدامه على أي جهاز آخر."
                                )
                            }
                            item {
                                ProtocolCard(
                                    title = "2. التشفير الديناميكي للعمليات (EMV Contactless ARQC)",
                                    desc = "كل عملية دفع عبر NFC تولد شفرة رقمية فريدة (Cryptogram) لمرة واحدة فقط باستخدام Application Transaction Counter (ATC) لا يمكن تكرارها أو نسخها."
                                )
                            }
                            item {
                                ProtocolCard(
                                    title = "3. درع البنك المركزي المصري (CBE Meeza IPN Compliance)",
                                    desc = "يتوافق نظام المحافظ المصرية والبطاقات الوطنية (ميزة) مع التشفير السيادي للبنك المركزي وشبكة المدفوعات اللحظية IPN."
                                )
                            }
                            item {
                                ProtocolCard(
                                    title = "4. الحفظ المشفر التلقائي (Zero Data Loss Protection)",
                                    desc = "يتم مزامنة وتشفير كافة التغييرات محلياً وسحابياً لمنع أي فقدان للبيانات عند تحديث البرنامج أو إعادة تشغيله."
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "تم وحفظ الإعدادات", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SecurityOverviewCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NileDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00384D),
                            NileDarkSurface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NileSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حالة الأمان العامة: محمي ومحصن",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NileCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Level 4 Shield",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NileCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "جميع العمليات التلامسية والبيانات المخزنة محمية بتشفير عسكري وضوابط أمان تمنع النسخ أو الاحتيال بشكل كامل.",
                    fontSize = 11.sp,
                    color = Color(0xFFDDDDDD),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SecurityToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) accentColor.copy(alpha = 0.4f) else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun SecurityLogItem(log: SecurityAuditEvent) {
    val formatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH) }
    val timeFormatted = formatter.format(Date(log.timestamp))

    val (badgeColor, badgeText) = when (log.level) {
        SecurityAlertLevel.SHIELDED -> Pair(NileSuccess, "محمي")
        SecurityAlertLevel.INFO -> Pair(NileCyan, "معلومات")
        SecurityAlertLevel.WARNING -> Pair(NileGold, "تنبيه")
        SecurityAlertLevel.CRITICAL -> Pair(NileError, "حظر")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = NileDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = log.details,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeFormatted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProtocolCard(title: String, desc: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NileCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
