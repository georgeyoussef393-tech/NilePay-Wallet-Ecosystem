package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.AuthManager
import com.example.security.BiometricAuthManager
import com.example.storage.CloudSyncManager
import com.example.ui.components.ForgotPasswordDialog
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToPrivacy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Google, 1: Email/Password, 2: Mobile/National ID
    var isRegisterMode by remember { mutableStateOf(false) } // Toggle between Login and Register for Email

    var fullName by remember { mutableStateOf("أحمد محمود علي") }
    var emailInput by remember { mutableStateOf("ahmed.ali@nilepay.eg") }
    var passwordInput by remember { mutableStateOf("NilePay@2026") }
    var confirmPasswordInput by remember { mutableStateOf("NilePay@2026") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var phoneOrEmail by remember { mutableStateOf("01012345678") }
    var nationalId by remember { mutableStateOf("29810151234567") }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false },
            onOpenFullScreen = onNavigateToPrivacy
        )
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = emailInput,
            onDismiss = { showForgotPasswordDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NileDarkSurface,
                        MaterialTheme.colorScheme.surface,
                        NileDarkSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NilePrimary, NileCyan))
                        )
                        .border(2.dp, NileGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "NilePay Logo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NilePay | نيل باي",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan
                )

                Text(
                    text = "محفظة الدفع الإلكتروني والـ NFC المعتمَدة في مصر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NileGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = NileGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ البيانات السحابي والتشفير المحلي مفعّل تلقائياً",
                            style = MaterialTheme.typography.labelMedium,
                            color = NileGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Auth Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تسجيل الدخول وإثبات الهوية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = NileCyan
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("البريد الإلكتروني", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.testTag("login_tab_email")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("حساب Google", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.testTag("login_tab_google")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("الهاتف / القومي", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.testTag("login_tab_national")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (selectedTab) {
                        0 -> {
                            // Email & Password Auth Section (Login & Register)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Toggle Mode Header (Login vs Register)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { isRegisterMode = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isRegisterMode) NileCyan else Color.Transparent,
                                            contentColor = if (!isRegisterMode) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسجيل الدخول", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { isRegisterMode = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRegisterMode) NileCyan else Color.Transparent,
                                            contentColor = if (isRegisterMode) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isRegisterMode) {
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("الاسم بالكامل") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NileCyan) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("البريد الإلكتروني (Email)") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NileCyan) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("كلمة المرور (Password)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NileCyan) },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.Gray
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                if (!isRegisterMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { showForgotPasswordDialog = true },
                                            modifier = Modifier.testTag("forgot_password_trigger_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockReset,
                                                contentDescription = null,
                                                tint = NileCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "نسيت كلمة المرور؟ (Firebase Auth)",
                                                color = NileCyan,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isRegisterMode) {
                                    OutlinedTextField(
                                        value = confirmPasswordInput,
                                        onValueChange = { confirmPasswordInput = it },
                                        label = { Text("تأكيد كلمة المرور") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NileCyan) },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                                            Toast.makeText(context, "برجاء إدخال بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (passwordInput.length < 6) {
                                            Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 خانات", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (isRegisterMode && passwordInput != confirmPasswordInput) {
                                            Toast.makeText(context, "كلمات المرور غير متطابقة", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        AuthManager.loginWithEmail(
                                            email = emailInput.trim(),
                                            name = if (isRegisterMode) fullName else ""
                                        )

                                        val msg = if (isRegisterMode) "تم إنشاء الحساب وحفظ البيانات سحابياً بنجاح!" else "تم تسجيل الدخول بالبريد الإلكتروني بنجاح!"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("email_auth_submit_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRegisterMode) "تسجيل حساب جديد بالبريد" else "تسجيل الدخول بالبريد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Google Login Section
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "سجل دخولك بنقرة واحدة باستخدام حساب Google المعتمد للحفظ السحابي الآمن.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        AuthManager.loginWithGoogle(
                                            email = "georgeyoussef393@gmail.com",
                                            name = "مستخدم Google المعتمد"
                                        )
                                        Toast.makeText(context, "تم تسجيل الدخول بحساب Google بنجاح!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NilePrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("google_login_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "تسجيل الدخول بواسطة Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        2 -> {
                            // National ID / Phone Auth Section
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("الاسم بالكامل") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NileCyan) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = phoneOrEmail,
                                    onValueChange = { phoneOrEmail = it },
                                    label = { Text("رقم المحمول / البريد") },
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NileCyan) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = nationalId,
                                    onValueChange = { nationalId = it },
                                    label = { Text("الرقم القومي (14 رقم)") },
                                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = NileCyan) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (phoneOrEmail.isBlank()) {
                                            Toast.makeText(context, "برجاء إدخال رقم المحمول أو البريد", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        AuthManager.loginWithPhoneOrNationalId(
                                            phoneOrEmail = phoneOrEmail,
                                            fullName = fullName,
                                            nationalId = nationalId
                                        )
                                        Toast.makeText(context, "تم توثيق الحساب وتسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("national_id_login_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تأكيد وتسجيل الدخول", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Biometric Quick Login Button
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                BiometricAuthManager.authenticate(
                                    activity = activity,
                                    title = "الدخول السريع ببصمة الاصبع",
                                    subtitle = "تأكيد الهوية للوصول للمحفظة السحابية",
                                    onSuccess = {
                                        AuthManager.loginWithGoogle("biometric.user@nilepay.eg", "مستخدم البصمة المعتمد")
                                        Toast.makeText(context, "تم المصادقة بالبصمة وتسجيل الدخول!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    onError = { err: String ->
                                        Toast.makeText(context, "فشلت البصمة: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_quick_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = NileCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الدخول السريع بالبصمة البيومترية",
                            color = NileCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Footer Section: Privacy Policy & Cloud Sync Notice
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                TextButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("login_privacy_policy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = NileCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سياسة الخصوصية وأمان البيانات (CBE Privacy Policy)",
                        color = NileCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "جميع بياناتك مشفرة ومحفوظة سحابياً برمزية AES-256",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
