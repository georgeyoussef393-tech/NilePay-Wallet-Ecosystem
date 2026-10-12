package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    onOpenFullScreen: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onOpenFullScreen != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenFullScreen()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("open_full_privacy_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الوثيقة الكاملة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NilePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("close_privacy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("موافق ومفهوم", fontWeight = FontWeight.Bold)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NilePrimary, NileCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = "الخصوصية والأمان",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "سياسة الخصوصية وأمان البيانات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "حقوقك وحماية بياناتك الماليّة في مصر",
                        style = MaterialTheme.typography.bodySmall,
                        color = NileCyan
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CBE Compliance Header Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NileDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = NileGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "معتمدة ومطابقة لتعليمات البنك المركزي المصري (CBE)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NileGold
                            )
                            Text(
                                text = "تخضع كافة البيانات المالية والتشفير لأعلى المعايير المصرفية الوطنية والدولية PCI-DSS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Section 1: Local Encrypted Storage & Tokenization
                PrivacySectionCard(
                    icon = Icons.Default.Lock,
                    title = "1. التشفير المحلي والرمزية (Tokenization)",
                    description = "لا يتم حفظ رقم بطاقتك الائتمانية (PAN) الحقيقي في الجهاز إطلاقاً. يتم تحويل البيانات إلى رمزيّة مشفرة (Payment Token) مخزنة داخل قاعدة بيانات روم المشفرة (Encrypted Room Database) ومعتمدة بمفاتيح Android KeyStore التابعة للجهاز."
                )

                // Section 2: Biometric Protection
                PrivacySectionCard(
                    icon = Icons.Default.Fingerprint,
                    title = "2. الحماية البيومترية الإلزامية",
                    description = "تتطلب كل عملية دفع عبر الـ NFC أو التحويل المالي مصادقة بيومترية بصرية أو بالبصمة قبل إطلاق إشارة الدفع إلى ماكينة الـ POS، لمنع أي معالجة غير مصرح بها."
                )

                // Section 3: Encrypted Cloud Backup Policy
                PrivacySectionCard(
                    icon = Icons.Default.CloudDone,
                    title = "3. سياسة الحفظ والمزامنة السحابية",
                    description = "يتم مزامنة البيانات والنسخ الاحتياطي في سحابة مشفرة Zero-Knowledge Vault. يتم تشفير البيانات على جهازك أولاً قبل إرسالها إلى السحابة، مما يضمن ألا يستطيع أحد غيرك الوصول إلى بياناتك."
                )

                // Section 4: Data Erasure & Privacy Control
                PrivacySectionCard(
                    icon = Icons.Default.Gavel,
                    title = "4. التحكم الكامل وحق مسح البيانات",
                    description = "لديك الحق الكامل في مسح كافة بطاقاتك ومعاملاتك المخزنة محلياً وسحابياً بضغطة زر واحدة عبر لوحة التحكم الأمنية، بدون ترك أي أثر."
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "تاريخ آخر تحديث للسياسة: أكتوبر 2026 - جمهورية مصر العربية",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun PrivacySectionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NileCyan,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
