package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.SecurityVaultDialog
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSecurityVaultDialog by remember { mutableStateOf(false) }

    if (showSecurityVaultDialog) {
        SecurityVaultDialog(
            onDismiss = { showSecurityVaultDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = NileDarkSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("privacy_screen_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NilePrimary, NileCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "سياسة الخصوصية وأمان البيانات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "شروط الحماية والتشفير المصرفي - NilePay Wallet",
                                style = MaterialTheme.typography.bodySmall,
                                color = NileCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "وثيقة سياسة الخصوصية وأمان البيانات المالية - محفظة NilePay NFC مصر.\nhttps://nilepay.eg/privacy"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة وثيقة الخصوصية"))
                        },
                        modifier = Modifier.testTag("privacy_share_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة الوثيقة",
                            tint = NileCyan
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // CBE Regulatory Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NileDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NileGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cbe_compliance_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NileGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "معتمد",
                                tint = NileGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مطابقة لتعليمات البنك المركزي المصري (CBE)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NileGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تخضع كافة البيانات والعمليات المالية للتشفير المتقدم وشروط معيار السلامة المصرفية PCI-DSS واللوائح الوطنية المصرية لوسائل الدفع الإلكترونية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Security Quick Stats Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecurityStatChip(
                        icon = Icons.Default.Lock,
                        title = "256-bit AES",
                        subtitle = "تشفير محلي",
                        modifier = Modifier.weight(1f)
                    )
                    SecurityStatChip(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Guard",
                        subtitle = "بصمة إلزامية",
                        modifier = Modifier.weight(1f)
                    )
                    SecurityStatChip(
                        icon = Icons.Default.CloudDone,
                        title = "Zero-Knowledge",
                        subtitle = "سحابة مشفرة",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Main Scrollable Policy Text Document Container
                SelectionContainer {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_text_document_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "وثيقة الشروط والخصوصية وأمان البيانات المالية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NilePrimary
                            )

                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Section 1
                            PrivacyPolicySection(
                                sectionNumber = "١",
                                title = "مقدمة ونطاق تغطية المحفظة المالية",
                                icon = Icons.Default.Shield,
                                bodyText = """
تلتزم محفظة NilePay NFC بحماية خصوصية وأمان أصولك المالية وبيانات بطاقاتك المصرفية (فيزا، ماستركارد، ميزة) وحسابات المحافظ الرقمية (فودافون كاش، إنستا باي، أورانج كاش).
تسري هذه السياسة على كافة المعاملات المالية المحلية التي تتم داخل جمهورية مصر العربية عبر تقنية الاتصال قريب المدى (NFC)، أو رموز الاستجابة السريعة (QR Code)، أو المزامنة السحابية.
                                """.trimIndent()
                            )

                            // Section 2
                            PrivacyPolicySection(
                                sectionNumber = "٢",
                                title = "تشفير البطاقات وتحويلها لرموز (Tokenization)",
                                icon = Icons.Default.Lock,
                                bodyText = """
لا تتكيف المحفظة مطلقاً مع تخزين أرقام البطاقات الحقيقية (PAN) في الذاكرة المفتوحة. يتم استخدام خوارزميات التشفير المعتمدة من البنك المركزي المصري وقواعد بيانات Room المشفرة محلياً (Encrypted SQLite):
• تشفير مفاتيح AES-256 بالاعتماد على مخزن المفاتيح الآمن في نظام أندرويد (Android KeyStore System).
• توليد رموز دفع بديلة (Payment Tokens) صالحة للاستخدام المؤقت أو المقيد دون كشف أرقام البطاقة الأصلية للمتاجر أو ماكينات الـ POS.
• حماية بيانات المشتريات والمعاملات السابقة وحذف الرموز الحساسة تلقائياً عند طلب المستخدم.
                                """.trimIndent()
                            )

                            // Section 3
                            PrivacyPolicySection(
                                sectionNumber = "٣",
                                title = "اشتراطات المصادقة البيومترية قبل الدفع (Biometric Gate)",
                                icon = Icons.Default.Fingerprint,
                                bodyText = """
تطرد المحفظة أي محاولة لإجراء دفع غير مصرح به عبر تقنية الـ NFC من خلال فرض بوابة أمان حيوية إلزامية:
• يلزم تأكيد هوية المستخدم عن طريق بصمة الاصبع (Fingerprint) أو البصمة البيومترية للوجه (Face ID) عبر مكتبة androidx.biometric الرسمية.
• عدم إرسال أية حزمة بيانات NFC إلى نقاط البيع (POS Terminals) إلا بعد نجاح المصادقة البيومترية.
• إغلاق المحفظة وقفل البطاقات فورياً في حال اكتشاف محاولات فتح متكررة خاطئة.
                                """.trimIndent()
                            )

                            // Section 4
                            PrivacyPolicySection(
                                sectionNumber = "٤",
                                title = "الحفظ السحابي الآمن وبنية Zero-Knowledge",
                                icon = Icons.Default.CloudDone,
                                bodyText = """
تعتمد المحفظة خدمة المزامنة السحابية الهجينة (Firebase Firestore Cloud Storage) لدعم استعادة الحساب وتنسيق البيانات:
• يتم تشفير النسخ الاحتياطية سحابياً قبل مغادرتها الجهاز بحيث لا تتمكن السيرفرات السحابية من فك تشفير المعاملات.
• إتاحة المزامنة بضغط زر واحدة أو تعطيل المزامنة السحابية نهائياً والاعتماد على التخزين المحلي الآمن فقط.
• حماية تسجيل الدخول ببروتوكولات Firebase Auth مع دعم استعادة كلمة المرور عبر رابط إلكتروني مؤمن.
                                """.trimIndent()
                            )

                            // Section 5
                            PrivacyPolicySection(
                                sectionNumber = "٥",
                                title = "حماية الجهاز ومنع لقطات الشاشة (Anti-Spyware & FLAG_SECURE)",
                                icon = Icons.Default.Security,
                                bodyText = """
لحماية بياناتك من برامج التجسس والتطبيقات الضارة وتطبيقات التسجيل الخفي:
• حظر لقطات الشاشة وتصوير الفيديو (Window FLAG_SECURE) داخل شاشات تفاصيل البطاقات والمعاملات المالية.
• التحقق من سلامة بيئة التشغيل ومنع البرمجيات الضارة أو التلاعب بذاكرة النظام أثناء تنشيط تقنية NFC.
                                """.trimIndent()
                            )

                            // Section 6
                            PrivacyPolicySection(
                                sectionNumber = "٦",
                                title = "حقوق المستخدم والتحكم والمسح الشامل للبيانات",
                                icon = Icons.Default.DeleteSweep,
                                bodyText = """
لك كمستخدم كامل السيطرة المستقلة على بياناتك الشخصية والمالية:
• حق الاطلاع والمراجعة الشاملة لجميع السجلات والمعاملات المسجلة.
• حق مسح كافة البطاقات والرموز والبيانات المخزنة محلياً وسحابياً بضغطة زر واحدة بدون ترك أي أثر على الجهاز أو السحابة.
• التقديم لحظر البطاقة المفقودة فوراً بنقرة واحدة داخل المحفظة.
                                """.trimIndent()
                            )

                            // Section 7
                            PrivacyPolicySection(
                                sectionNumber = "٧",
                                title = "التواصل والدعم الفني والأمني في مصر",
                                icon = Icons.Default.Info,
                                bodyText = """
لأي استفسارات بخصوص حماية البيانات أو الإبلاغ عن ثغرات أمنية:
البريد الإلكتروني للسلامة المالية: privacy@nilepay.eg
خدمة العملاء: ١٩٩٩٩ (جمهورية مصر العربية)
تاريخ التحديث الأخير: أكتوبر ٢٠٢٦
                                """.trimIndent()
                            )
                        }
                    }
                }

                // Action Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = NilePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("privacy_accept_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "موافق وفهمت شروط الأمان والخصوصية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showSecurityVaultDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_security_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NileCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فتح خزانة الأمان والتحكم بالبيانات",
                            color = NileCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SecurityStatChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NileCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PrivacyPolicySection(
    sectionNumber: String,
    title: String,
    icon: ImageVector,
    bodyText: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NilePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sectionNumber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NilePrimary
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NileCyan,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}
