package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FeaturePermission
import com.example.model.WalletProvider
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.ui.theme.EtisalatGreen
import com.example.ui.theme.InstaPayPurple
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NileSuccess
import com.example.ui.theme.OrangeCash
import com.example.ui.theme.VodafoneRed
import com.example.ui.theme.WePayPurple
import com.example.wallets.EgyptianWalletEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ApiIntegrationPattern(
    val title: String,
    val description: String,
    val endpoint: String,
    val securityLevel: String
) {
    CBE_MEEZA_REST_HMAC(
        title = "CBE Meeza IPN REST API (HMAC-SHA256)",
        description = "Central Bank of Egypt Open Banking Gateway with JWE Encrypted Payload & Mutual TLS 1.3",
        endpoint = "https://api.cbe.gov.eg/v2/ipn/wallets/link-intent",
        securityLevel = "BANKING_GRADE_A_PLUS"
    ),
    MNO_OAUTH2_MUTUAL_TLS(
        title = "MNO Direct OAuth2.0 Tokenization",
        description = "Direct OAuth2 Token Exchange with Vodafone / Orange / Etisalat / WE Core Switch",
        endpoint = "https://mno-gateway.egypt.gov.eg/oauth/v2/token",
        securityLevel = "MNO_ENCRYPTED_SDF"
    ),
    DELEGATED_IPA_TOKENIZER(
        title = "InstaPay IPN Alias Delegated Tokenizer",
        description = "Instant Peer-to-Peer IPA Name Server Lookup and CBE Instant Clearing Validation",
        endpoint = "https://ipn.instapay.eg/v1/alias/tokenize",
        securityLevel = "CBE_IPN_CERTIFIED"
    )
}

@Composable
fun LinkWalletScreen(
    onNavigateBack: () -> Unit,
    onWalletLinked: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val activeCompany by AccessControlManager.activeCompany.collectAsState()

    var currentStep by remember { mutableStateOf(1) } // 1: Select & Identity, 2: API Pattern & Handshake, 3: OTP Challenge, 4: Connected

    var selectedProvider by remember { mutableStateOf(WalletProvider.VODAFONE_CASH) }
    var selectedApiPattern by remember { mutableStateOf(ApiIntegrationPattern.CBE_MEEZA_REST_HMAC) }

    var accountIdentifier by remember { mutableStateOf("010 9876 5432") }
    var accountTitle by remember { mutableStateOf("Primary Vodafone Wallet") }
    var nationalIdInput by remember { mutableStateOf("29410151203918") }
    var initialBalanceInput by remember { mutableStateOf("1500.00") }

    // API Handshake States
    var isExecutingHandshake by remember { mutableStateOf(false) }
    var apiLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var generatedApiToken by remember { mutableStateOf<String?>(null) }
    var apiHandshakeSuccess by remember { mutableStateOf(false) }

    // OTP Challenge States
    var otpInput by remember { mutableStateOf("") }
    var sentOtpCode by remember { mutableStateOf<String?>(null) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successBannerMessage by remember { mutableStateOf<String?>(null) }

    val hasPermission = activeCompany.isActive && activeCompany.grantedPermissions.contains(FeaturePermission.EGYPTIAN_MOBILE_WALLETS)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("link_wallet_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ربط محفظة إلكترونية خارجية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "CBE Meeza Gateway & Secure Open API Integration",
                        style = MaterialTheme.typography.bodySmall,
                        color = NileCyan
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stepper Progress Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepItem(stepNumber = 1, title = "المزود والبيانات", isActive = currentStep == 1, isCompleted = currentStep > 1)
                    StepDivider(isCompleted = currentStep > 1)
                    StepItem(stepNumber = 2, title = "مصادقة الـ API", isActive = currentStep == 2, isCompleted = currentStep > 2)
                    StepDivider(isCompleted = currentStep > 2)
                    StepItem(stepNumber = 3, title = "تأكيد OTP", isActive = currentStep == 3, isCompleted = currentStep > 3)
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = NileError.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileError)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = NileError)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = errorMessage!!, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // STEP 1: Select Provider & Account Details
        if (currentStep == 1) {
            item {
                Text(
                    text = "1. اختر مزود المحفظة المحمولة أو InstaPay",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                val providers = listOf(
                    WalletProvider.VODAFONE_CASH,
                    WalletProvider.INSTAPAY_MEEZA,
                    WalletProvider.ORANGE_MONEY,
                    WalletProvider.ETISALAT_CASH,
                    WalletProvider.WE_PAY
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providers.forEach { provider ->
                        val isSelected = selectedProvider == provider
                        val brandColor = Color(provider.colorHex)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProvider = provider
                                    if (provider == WalletProvider.INSTAPAY_MEEZA && !accountIdentifier.contains("@")) {
                                        accountIdentifier = "george@instapay"
                                        accountTitle = "George Youssef (InstaPay IPN)"
                                    } else if (provider != WalletProvider.INSTAPAY_MEEZA && accountIdentifier.contains("@")) {
                                        accountIdentifier = "010 9876 5432"
                                        accountTitle = "George ${provider.title}"
                                    }
                                }
                                .testTag("provider_selection_${provider.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) brandColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) brandColor else Color(0x22FFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(brandColor.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider.title.take(2).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = brandColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = provider.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "رمز الخدمة: ${provider.serviceCode} • البوابة الوطنية ميزة",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = brandColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Text(
                    text = "بيانات الحساب والهوية الوطنية (CBE Compliance)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedProvider == WalletProvider.INSTAPAY_MEEZA) "عنوان إنستا باي (IPA)" else "رقم الموبايل المسجل بالشريحة (010/011/012/015)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountIdentifier,
                            onValueChange = { accountIdentifier = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mobile_identifier_input"),
                            singleLine = true,
                            placeholder = { Text(if (selectedProvider == WalletProvider.INSTAPAY_MEEZA) "username@instapay" else "010xxxxxxxx") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(selectedProvider.colorHex),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "اسم الحساب المستعار / Label",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountTitle,
                            onValueChange = { accountTitle = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_title_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(selectedProvider.colorHex),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "الرقم القومي المصري (14 رقم)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = nationalIdInput,
                            onValueChange = { if (it.length <= 14) nationalIdInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("national_id_input"),
                            singleLine = true,
                            placeholder = { Text("29401011234567") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(selectedProvider.colorHex),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "الرصيد المبدئي الافتراضي (EGP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = initialBalanceInput,
                            onValueChange = { initialBalanceInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = { Text("EGP", color = NileCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(selectedProvider.colorHex),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val cleanId = accountIdentifier.trim()
                        if (cleanId.isBlank()) {
                            errorMessage = "يرجى إدخال رقم الموبايل أو عنوان InstaPay صحيح."
                            return@Button
                        }
                        if (nationalIdInput.length < 14) {
                            errorMessage = "الرقم القومي يجب أن يتكون من 14 رقمًا حسب تعليمات البنك المركزي."
                            return@Button
                        }
                        errorMessage = null
                        currentStep = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("next_step_to_api_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(selectedProvider.colorHex),
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "المتابعة لإعداد بروتوكول ה-API", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // STEP 2: Select API Integration Pattern & Run Handshake
        if (currentStep == 2) {
            item {
                Text(
                    text = "2. اختيار نمط الربط البرمجي (Secure API Integration Pattern)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ApiIntegrationPattern.values().forEach { pattern ->
                        val isChosen = selectedApiPattern == pattern
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApiPattern = pattern },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) NileCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isChosen) 1.5.dp else 0.5.dp,
                                color = if (isChosen) NileCyan else Color(0x22FFFFFF)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pattern.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isChosen) NileCyan else Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NileGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = pattern.securityLevel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NileGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pattern.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ENDPOINT: ${pattern.endpoint}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NileCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Live API Handshake Simulation Console
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF09121C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E354D))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = NileCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "سجل الاتصال المباشر (API Live Terminal Log)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            if (apiHandshakeSuccess) {
                                Surface(shape = RoundedCornerShape(4.dp), color = NileSuccess.copy(alpha = 0.2f)) {
                                    Text("HTTP 200 OK", fontSize = 10.sp, color = NileSuccess, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (apiLogs.isEmpty()) {
                            Text(
                                text = "اضغط على زر 'تنفيذ المصادقة والـ Handshake' للاتصال ببوابة البنك المركزي المصري.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                apiLogs.forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (logLine.contains("200 OK") || logLine.contains("SUCCESS")) NileSuccess
                                        else if (logLine.contains("POST") || logLine.contains("HEADER")) NileCyan
                                        else Color(0xFFCFD8DC)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isExecutingHandshake) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = NileCyan,
                                trackColor = Color(0xFF1E354D)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                isExecutingHandshake = true
                                apiHandshakeSuccess = false
                                apiLogs = emptyList()

                                coroutineScope.launch {
                                    apiLogs = apiLogs + "--> POST ${selectedApiPattern.endpoint}"
                                    delay(400)
                                    apiLogs = apiLogs + "--> Headers: { Host: api.cbe.gov.eg, X-CBE-Signature: HMAC_SHA256(secret) }"
                                    delay(400)
                                    apiLogs = apiLogs + "--> Payload JWE: Encrypted_Account_Identifier_NID (${nationalIdInput.take(4)}••••••••)"
                                    delay(500)
                                    apiLogs = apiLogs + "<-- Response: HTTP 200 OK (TLS 1.3 Cipher AES-256-GCM)"
                                    val token = "ipn_tok_" + System.currentTimeMillis().toString().takeLast(8)
                                    generatedApiToken = token
                                    delay(300)
                                    apiLogs = apiLogs + "<-- Issued Binding Token: $token (Status: VERIFIED_CBE_MEEZA)"
                                    isExecutingHandshake = false
                                    apiHandshakeSuccess = true
                                }
                            },
                            enabled = !isExecutingHandshake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("initiate_api_link_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isExecutingHandshake) "جاري إجراء المصادقة والـ Handshake..." else "تنفيذ المصادقة والـ Handshake عبر الـ API",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "رجوع", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (!apiHandshakeSuccess) {
                                errorMessage = "يرجى تنفيذ اتصال الـ API أولاً قبل المتابعة."
                                return@Button
                            }
                            errorMessage = null
                            // Generate OTP Code
                            val code = CryptoTokenManager.generateAtmCashoutOtp()
                            sentOtpCode = code
                            currentStep = 3
                        },
                        enabled = apiHandshakeSuccess,
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("next_step_to_otp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "المتابعة للتحقق برقم OTP", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // STEP 3: OTP Challenge & Biometric Authorization
        if (currentStep == 3) {
            item {
                Text(
                    text = "3. التحقق بخطوتين (2FA OTP & Biometric Unlock)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SimCard, contentDescription = null, tint = NileGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم إرسال كود التأكيد (SMS OTP)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تم إرسال كود من 6 أرقام إلى الشريحة المسجلة بالمحفظة (${accountIdentifier}).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (sentOtpCode != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NileGold.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NileGold)
                            ) {
                                Text(
                                    text = "كود الاختبار السريع (Simulated OTP): ${sentOtpCode}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NileGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "أدخل كود ה-OTP (6 أرقام):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 6) otpInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input"),
                            singleLine = true,
                            placeholder = { Text("e.g. 123456") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NileCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (otpInput.isBlank()) {
                                    errorMessage = "يرجى إدخال كود ה-OTP المطلوب."
                                    return@Button
                                }
                                isVerifyingOtp = true
                                coroutineScope.launch {
                                    delay(600)
                                    isVerifyingOtp = false
                                    otpVerified = true
                                    errorMessage = null

                                    // Create and Save Wallet to Engine
                                    val balance = initialBalanceInput.toDoubleOrNull() ?: 0.0
                                    val newAcc = EgyptianWalletEngine.linkWallet(
                                        provider = selectedProvider,
                                        identifier = accountIdentifier.trim(),
                                        accountTitle = accountTitle.ifBlank { "${selectedProvider.title} Account" },
                                        nationalId = nationalIdInput,
                                        initialBalance = balance
                                    )

                                    successBannerMessage = "تم ربط محفظة ${selectedProvider.title} بنجاح عبر API البنك المركزي!"
                                    currentStep = 4
                                    onWalletLinked()
                                }
                            },
                            enabled = !isVerifyingOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isVerifyingOtp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "تأكيد كود OTP وإتمام الربط", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // STEP 4: Linked & Confirmed Summary Screen
        if (currentStep == 4) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .testTag("wallet_linked_success_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF072118)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileSuccess)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NileSuccess.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = NileSuccess,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تم ربط المحفظة الخارجية بنجاح!",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الحساب جاهز الآن للتحويل السريع واستلام الأموال والدفع عبر NilePay.",
                            fontSize = 12.sp,
                            color = Color(0xFFB2DFDB),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailRow(label = "المزود", value = selectedProvider.title)
                            DetailRow(label = "المعرف المسجل", value = accountIdentifier)
                            DetailRow(label = "رمز التوكن بالبنك", value = generatedApiToken ?: "ipn_tok_8f912a")
                            DetailRow(label = "بروتوكول الـ API", value = selectedApiPattern.title)
                            DetailRow(label = "حالة الاعتماد", value = "VERIFIED_CBE_MEEZA")
                            DetailRow(label = "الرصيد المتاح", value = "${String.format(Locale.US, "%,.2f", initialBalanceInput.toDoubleOrNull() ?: 0.0)} EGP")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("return_to_wallets_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "العودة لقائمة المحافظ الحالية", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) NileSuccess
                    else if (isActive) NileCyan
                    else Color.DarkGray
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            } else {
                Text(text = stepNumber.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) NileCyan else if (isCompleted) Color.White else Color.Gray
        )
    }
}

@Composable
private fun StepDivider(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(2.dp)
            .background(if (isCompleted) NileSuccess else Color.DarkGray)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
