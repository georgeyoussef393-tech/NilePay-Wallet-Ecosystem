package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.CompanyTesterProfile
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.security.BiometricAuthManager
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NileTopHeader(
    activeCompany: CompanyTesterProfile,
    onOpenAdmin: () -> Unit,
    onSwitchCompany: () -> Unit,
    onOpenCloudSync: () -> Unit = {},
    onOpenSecurityCenter: () -> Unit = {},
    onLockWallet: () -> Unit = { BiometricAuthManager.lockWallet() }
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NilePay Brand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NileCyan, Color(0xFF006874))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NP",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NilePay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NileGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "مصر",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NileGold
                                )
                            }
                        }
                        Text(
                            text = "معتمد من البنك المركزي وميزة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons: Quick Lock + Security Vault + Cloud Sync + Admin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Biometric Lock Button
                    Surface(
                        onClick = onLockWallet,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("quick_lock_wallet_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل بالبصمة",
                                tint = NileGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "قفل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NileGold
                            )
                        }
                    }

                    // Security Center Shield Button
                    Surface(
                        onClick = onOpenSecurityCenter,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("open_security_center_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "الأمان المشدد",
                                tint = NileCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الأمان",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NileCyan
                            )
                        }
                    }

                    // Cloud & Local Sync Button
                    Surface(
                        onClick = onOpenCloudSync,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("open_cloud_sync_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Cloud & Local Storage",
                                tint = NileSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "حفظ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NileSuccess
                            )
                        }
                    }

                    // Admin Button
                    Surface(
                        onClick = onOpenAdmin,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("open_admin_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Dashboard",
                                tint = NileCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الإدارة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NileCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Testing Company Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchCompany() }
                    .testTag("active_company_chip"),
                shape = RoundedCornerShape(10.dp),
                color = if (activeCompany.isActive) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    NileError.copy(alpha = 0.15f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activeCompany.isActive) NileCyan.copy(alpha = 0.4f) else NileError
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (activeCompany.isActive) Icons.Default.Business else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (activeCompany.isActive) NileCyan else NileError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeCompany.companyName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (activeCompany.isActive) NileSuccess else NileError)
                                )
                            }
                            Text(
                                text = if (activeCompany.isActive) {
                                    "${activeCompany.grantedPermissions.size} صلاحية نشطة • الحد الأقصى ${String.format(Locale.US, "%,.0f", activeCompany.maxSingleTxLimitEGP)} ج.م"
                                } else {
                                    "تم إلغاء الصلاحية من الإدارة"
                                },
                                fontSize = 11.sp,
                                color = if (activeCompany.isActive) MaterialTheme.colorScheme.onSurfaceVariant else NileError
                            )
                        }
                    }

                    Text(
                        text = "تبديل الصلاحية ▸",
                        fontSize = 11.sp,
                        color = NileCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    tx: TransactionRecord,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val formattedTime = dateFormatter.format(Date(tx.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("transaction_item_${tx.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status icon circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when (tx.status) {
                                TransactionStatus.SUCCESS -> NileSuccess.copy(alpha = 0.15f)
                                TransactionStatus.BLOCKED_ACCESS_CONTROL -> NileError.copy(alpha = 0.15f)
                                TransactionStatus.PENDING_CBE -> NileGold.copy(alpha = 0.15f)
                                TransactionStatus.FAILED -> NileError.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tx.status) {
                            TransactionStatus.SUCCESS -> Icons.Default.CheckCircle
                            TransactionStatus.BLOCKED_ACCESS_CONTROL -> Icons.Default.Lock
                            TransactionStatus.PENDING_CBE -> Icons.Default.Warning
                            TransactionStatus.FAILED -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when (tx.status) {
                            TransactionStatus.SUCCESS -> NileSuccess
                            TransactionStatus.BLOCKED_ACCESS_CONTROL -> NileError
                            TransactionStatus.PENDING_CBE -> NileGold
                            TransactionStatus.FAILED -> NileError
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${tx.type.label} • $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.status == TransactionStatus.BLOCKED_ACCESS_CONTROL) {
                        Text(
                            text = "محظور بسياسة الأمان",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileError
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.status == TransactionStatus.SUCCESS) "-" else ""}${String.format(Locale.US, "%,.2f", tx.amountEGP)} ج.م",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = when (tx.status) {
                        TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.onSurface
                        TransactionStatus.BLOCKED_ACCESS_CONTROL -> NileError
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = tx.cbeReference,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
