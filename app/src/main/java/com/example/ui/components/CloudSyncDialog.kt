package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cards.CardRepository
import com.example.security.AccessControlManager
import com.example.storage.CloudSyncManager
import com.example.storage.CloudSyncStatus
import com.example.storage.NilePayStorageManager
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess
import com.example.wallets.EgyptianWalletEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats by NilePayStorageManager.stats.collectAsState()
    val isAutoSync by CloudSyncManager.isAutoSyncEnabled.collectAsState()
    val syncStatus by CloudSyncManager.syncStatus.collectAsState()
    val lastSyncTime by CloudSyncManager.lastCloudSyncTimestamp.collectAsState()
    val vaultId by CloudSyncManager.cloudVaultId.collectAsState()
    val statusMsg by CloudSyncManager.statusMessage.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("cloud_sync_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(NileCyan, Color(0xFF006874))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud & Local Sync",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الحفظ السحابي والمحلي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "حماية كامل بياناتك من الضياع عند أي تعديل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Local Persistence Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NileDarkSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NileSuccess.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Local Persistence",
                                    tint = NileSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الحفظ المحلي الدائم (Local Storage)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NileSuccess
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NileSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "نشط وآمن",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NileSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "يتم حفظ جميع البطاقات والمحافظ والمعاملات تلقائياً في ذاكرة الجهاز المشفرة. لا تُمحى البيانات عند التحديث أو إعادة تشغيل البرنامج.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageStatPill(
                                label = "البطاقات",
                                value = "${stats.cardsCount}"
                            )
                            StorageStatPill(
                                label = "المحافظ",
                                value = "${stats.walletsCount}"
                            )
                            StorageStatPill(
                                label = "المعاملات",
                                value = "${stats.transactionsCount}"
                            )
                            StorageStatPill(
                                label = "الحجم",
                                value = "${stats.totalSizeBytes / 1024 + 1} KB"
                            )
                        }
                    }
                }

                // 2. Cloud Backup & Sync Vault Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NileDarkSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NileCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloud Vault",
                                    tint = NileCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "السحابة المشفرة (Cloud Vault)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NileCyan
                                )
                            }

                            if (syncStatus == CloudSyncStatus.SYNCING || syncStatus == CloudSyncStatus.RESTORING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NileCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Synced",
                                    tint = NileCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "المزامنة السحابية التلقائية",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "حفظ فوري في السحابة عند أي تغيير",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAutoSync,
                                onCheckedChange = { CloudSyncManager.setAutoSyncEnabled(it) },
                                modifier = Modifier.testTag("auto_sync_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NileCyan,
                                    checkedTrackColor = NileCyan.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (lastSyncTime > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "آخر مزامنة: ${dateFormat.format(Date(lastSyncTime))}",
                                fontSize = 11.sp,
                                color = NileGold
                            )
                        }

                        if (statusMsg != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusMsg ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Cloud Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sync Now Button
                    Button(
                        onClick = {
                            CloudSyncManager.syncNowToCloud(silent = false) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NileCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_now_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مزامنة وحفظ سحابي فوري الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Restore from Cloud Button
                    OutlinedButton(
                        onClick = {
                            CloudSyncManager.restoreFromCloud { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NileCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_cloud_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استعادة البيانات من أحدث نسخة سحابية",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Export / Share Backup (Google Drive / WhatsApp / Files)
                    OutlinedButton(
                        onClick = {
                            val shareIntent = CloudSyncManager.exportBackupShareIntent(context)
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NileGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = NileGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير نسخة احتياطية (Google Drive / مشاركة)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = NileGold
                        )
                    }

                    // Import Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد نسخة", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NileError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_storage_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = NileError
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة ضبط", fontSize = 12.sp, color = NileError)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("إغلاق")
            }
        }
    )

    // Sub-dialog: Import Backup
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد نسخة احتياطية") },
            text = {
                Column {
                    Text(
                        text = "الصق نص النسخة الاحتياطية (JSON) لاستعادة كامل البطاقات والمحافظ والمعاملات:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text("كود النسخة الاحتياطية JSON") },
                        placeholder = { Text("{\"cards\": [...], \"wallets\": [...]}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            CloudSyncManager.importBackupJson(importText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showImportDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text("استيراد واستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Sub-dialog: Reset Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("تأكيد إعادة الضبط الافتراضي") },
            text = {
                Text(
                    text = "هل تريد استعادة البيانات الافتراضية التجريبية (بطاقات CIB، البنك الأهلي، بنك مصر ومحافظ فودافون كاش وإنستاباي)؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        NilePayStorageManager.clearAllStorage()
                        CardRepository.loadFromStorageOrSeed()
                        EgyptianWalletEngine.loadFromStorageOrSeed()
                        AccessControlManager.loadFromStorageOrSeed()
                        CloudSyncManager.syncNowToCloud(silent = false)
                        showResetConfirmDialog = false
                        Toast.makeText(context, "تمت إعادة ضبط البيانات الافتراضية بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileError)
                ) {
                    Text("إعادة الضبط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun StorageStatPill(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
