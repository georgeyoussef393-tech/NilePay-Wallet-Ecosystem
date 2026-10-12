package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.AdminDashboardManager
import com.example.admin.AdminFirestoreRepository
import com.example.admin.FirestoreUserAccountStatus
import com.example.model.CompanyTesterProfile
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.security.AccessControlManager
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NileSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit
) {
    val companies by AccessControlManager.companies.collectAsState()
    val activeCompany by AccessControlManager.activeCompany.collectAsState()
    val transactions by AdminDashboardManager.mergedTransactionsFlow.collectAsState(initial = emptyList())
    val userAccounts by AdminFirestoreRepository.realtimeUserAccounts.collectAsState()
    val isFirestoreConnected by AdminFirestoreRepository.isFirestoreConnected.collectAsState()
    val metrics by AdminDashboardManager.metricsFlow.collectAsState(
        initial = com.example.admin.SystemDashboardMetrics(
            totalVolumeEGP = 0.0,
            completedTransactionsCount = 0,
            blockedByPolicyCount = 0,
            activeTestingCompaniesCount = 0,
            revokedCompaniesCount = 0,
            activeUserAccountsCount = 0,
            suspendedUserAccountsCount = 0,
            flaggedRiskAccountsCount = 0,
            cbeMeezaStatus = "ONLINE (CBE IPN 14ms)",
            vodafoneSdfStatus = "OPERATIONAL",
            hceEmvVaultStatus = "PCI-DSS Level 1 Encrypted",
            isFirestoreLive = false
        )
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var editingCompany by remember { mutableStateOf<CompanyTesterProfile?>(null) }
    var editingUserAccount by remember { mutableStateOf<FirestoreUserAccountStatus?>(null) }
    var showCreateCompanyDialog by remember { mutableStateOf(false) }
    var showCreateUserAccountDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showMintTxDialog by remember { mutableStateOf(false) }
    var selectedTxDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Admin Header with Firestore Realtime Status Badge
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = NileCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Control Console",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Central Real-Time Monitoring & Governance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cloud Sync Vault Quick Trigger
                Surface(
                    onClick = { showCloudSyncDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("admin_open_cloud_sync_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Sync",
                            tint = NileSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حفظ ومزامنة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileSuccess
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Firestore Live Streaming Connection Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isFirestoreConnected) Color(0xFF07271E) else Color(0xFF2E2000),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFirestoreConnected) NileSuccess else NileGold
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isFirestoreConnected) NileSuccess else NileGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFirestoreConnected) "FIRESTORE LIVE STREAM: CONNECTED & SYNCING" else "FIRESTORE STANDBY (Local Snapshot Fallback Active)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirestoreConnected) NileSuccess else NileGold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "CBE IPN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Real-Time System Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Volume
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "TOTAL VOLUME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NileCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", metrics.totalVolumeEGP)} EGP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(text = "CBE Cleared", fontSize = 9.sp, color = NileSuccess)
                    }
                }

                // Metric 2: Settled TX
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "SETTLED TX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NileSuccess)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${metrics.completedTransactionsCount}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(text = "Live Streamed", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Metric 3: Active Users
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "USER ACCOUNTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NileGold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userAccounts.size}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${metrics.activeUserAccountsCount} Active • ${metrics.suspendedUserAccountsCount} Susp",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NileCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("admin_tab_row")
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Tenants & Rules", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("User Accounts", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Live Feed", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Architecture", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // TAB 0: Dynamic Access Control & Testing Matrix
        if (selectedTabIndex == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TESTING TENANTS & PERMISSIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Modify or revoke access instantly per company",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateCompanyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_company_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Company", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(companies) { company ->
                val isCurrentActive = company.companyId == activeCompany.companyId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("company_card_${company.companyId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentActive) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCurrentActive) 1.5.dp else 1.dp,
                        color = if (!company.isActive) NileError else if (isCurrentActive) NileCyan else Color(0x33FFFFFF)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (company.isActive) NileCyan.copy(alpha = 0.2f) else NileError.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (company.isActive) Icons.Default.Business else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (company.isActive) NileCyan else NileError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = company.companyName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrentActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NileCyan)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = "ACTIVE NOW", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${company.contactPerson} (${company.contactEmail})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingCompany = company },
                                    modifier = Modifier.testTag("edit_company_${company.companyId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Permissions",
                                        tint = NileCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (company.companyId != "company-admin-master") {
                                    IconButton(
                                        onClick = { AccessControlManager.deleteCompany(company.companyId) },
                                        modifier = Modifier.testTag("delete_company_${company.companyId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = "Remove Company",
                                            tint = NileError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (company.isActive) {
                                    "Status: Authorized (${company.grantedPermissions.size} features) • Max: ${String.format(Locale.US, "%,.0f", company.maxSingleTxLimitEGP)} EGP"
                                } else {
                                    "STATUS: ACCESS FULLY REVOKED"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (company.isActive) NileSuccess else NileError
                            )

                            if (!isCurrentActive) {
                                TextButton(
                                    onClick = { AccessControlManager.switchActiveCompany(company.companyId) },
                                    modifier = Modifier.testTag("switch_to_${company.companyId}")
                                ) {
                                    Text(text = "Test as this company", fontSize = 11.sp, color = NileCyan)
                                }
                            }
                        }

                        if (company.isActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                company.grantedPermissions.take(3).forEach { perm ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x3300C2CB))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = perm.code, fontSize = 9.sp, color = NileCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (company.grantedPermissions.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x22FFFFFF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "+${company.grantedPermissions.size - 3} more", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: User Accounts Statuses (Firestore Realtime Governance)
        if (selectedTabIndex == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FIRESTORE USER ACCOUNTS MONITOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real-time user account status, limits & risk flags",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateUserAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_add_user_account_button")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(userAccounts) { acc ->
                val isSuspended = acc.status == "SUSPENDED"
                val isFlagged = acc.status == "FLAGGED_RISK"
                val isPending = acc.status == "PENDING_VERIFICATION"

                val statusBg = when (acc.status) {
                    "ACTIVE" -> NileSuccess.copy(alpha = 0.15f)
                    "SUSPENDED" -> NileError.copy(alpha = 0.15f)
                    "FLAGGED_RISK" -> NileGold.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val statusBorder = when (acc.status) {
                    "ACTIVE" -> NileSuccess
                    "SUSPENDED" -> NileError
                    "FLAGGED_RISK" -> NileGold
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("user_account_card_${acc.userId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(statusBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (acc.status) {
                                            "ACTIVE" -> Icons.Default.VerifiedUser
                                            "SUSPENDED" -> Icons.Default.Block
                                            "FLAGGED_RISK" -> Icons.Default.Warning
                                            else -> Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = statusBorder,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${acc.email} • ID: ${acc.userId}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Quick status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = acc.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = statusBorder
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "WALLET BALANCE", fontSize = 9.sp, color = NileCyan, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", acc.walletBalanceEGP)} EGP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(text = "DAILY EGP LIMIT", fontSize = 9.sp, color = NileGold, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", acc.dailyLimitEGP)} EGP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(text = "LINKED WALLETS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${acc.linkedWalletsCount} Wallets",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Admin Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSuspended) {
                                Button(
                                    onClick = {
                                        AdminFirestoreRepository.updateUserAccountStatusInFirestore(acc.userId, "ACTIVE")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileSuccess, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Restore Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        AdminFirestoreRepository.updateUserAccountStatusInFirestore(acc.userId, "SUSPENDED")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileError, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Freeze / Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isFlagged) {
                                Button(
                                    onClick = {
                                        AdminFirestoreRepository.updateUserAccountStatusInFirestore(acc.userId, "ACTIVE")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = NileCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Clear Risk Flag", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (!isSuspended) {
                                Button(
                                    onClick = {
                                        AdminFirestoreRepository.updateUserAccountStatusInFirestore(acc.userId, "FLAGGED_RISK")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Flag Risk Anomaly", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { editingUserAccount = acc },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B38), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: Live Real-Time Transaction Monitor
        if (selectedTabIndex == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE TRANSACTION FEED (FIRESTORE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real-time CBE Ledger Ingestion & POS Taps",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showMintTxDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_mint_tx_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Mint Test TX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_empty_transactions_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Transactions Recorded Yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Real-time records from contactless NFC POS taps and mobile wallet transactions will appear here as they occur in Firestore.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(transactions) { tx ->
                    TransactionItemRow(
                        tx = tx,
                        onClick = { selectedTxDetail = tx }
                    )
                }
            }
        }

        // TAB 3: Serverless Microservices Architecture Blueprint
        if (selectedTabIndex == 3) {
            item {
                Text(
                    text = "SERVERLESS MICROSERVICES ARCHITECTURE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "High-availability Egyptian fintech platform topology with zero-trust tokenization and Firestore real-time policy sync.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            items(AdminDashboardManager.serverlessMicroservicesBlueprint) { svc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = svc.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NileCyan
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00382B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HEALTHY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NileSuccess
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stack: ${svc.technology}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NileGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = svc.responsibility,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                // Egyptian CBE Regulatory Compliance Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0C2B1C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00843D))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Central Bank of Egypt (CBE) Compliance Certified",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Meeza IPN Circular 2021/41 compliant\n• Host Card Emulation ISO 7816-4 APDU\n• National ID 14-digit validation & SDF MNO binding\n• Firestore Real-Time Audit & Security Rules Engine",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal Sheet: Edit User Account Limits / Status
    if (editingUserAccount != null) {
        val user = editingUserAccount!!
        var limitInput by remember { mutableStateOf(user.dailyLimitEGP.toString()) }
        var selectedStatus by remember { mutableStateOf(user.status) }

        AlertDialog(
            onDismissRequest = { editingUserAccount = null },
            title = { Text(text = "Manage User: ${user.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "EMAIL: ${user.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "USER ID: ${user.userId}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NileCyan)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "DAILY EGP SPENDING LIMIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NileGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "ACCOUNT STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NileCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf("ACTIVE", "SUSPENDED", "FLAGGED_RISK", "PENDING_VERIFICATION").forEach { st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatus = st }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = (selectedStatus == st),
                                onCheckedChange = { if (it) selectedStatus = st },
                                colors = CheckboxDefaults.colors(checkedColor = NileCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = st, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull() ?: user.dailyLimitEGP
                        AdminFirestoreRepository.updateUserAccountStatusInFirestore(
                            userId = user.userId,
                            newStatus = selectedStatus,
                            dailyLimit = limit
                        )
                        editingUserAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Save in Firestore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUserAccount = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Sheet: Mint Test Transaction
    if (showMintTxDialog) {
        var txTitle by remember { mutableStateOf("POS Tap @ Starbucks Cairo") }
        var txAmount by remember { mutableStateOf("250.00") }

        AlertDialog(
            onDismissRequest = { showMintTxDialog = false },
            title = { Text(text = "Mint Real-Time Test Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = txTitle,
                        onValueChange = { txTitle = it },
                        label = { Text("Transaction Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = txAmount,
                        onValueChange = { txAmount = it },
                        label = { Text("Amount in EGP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = txAmount.toDoubleOrNull() ?: 150.0
                        AdminDashboardManager.mintTestTransactionToFirestore(
                            title = txTitle,
                            amountEGP = amt,
                            type = TransactionType.NFC_TAP_PAY,
                            status = TransactionStatus.SUCCESS
                        )
                        showMintTxDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Publish to Firestore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMintTxDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Sheet: Add New User Account in Firestore
    if (showCreateUserAccountDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var balance by remember { mutableStateOf("15000") }
        var limit by remember { mutableStateOf("30000") }

        AlertDialog(
            onDismissRequest = { showCreateUserAccountDialog = false },
            title = { Text(text = "Register New User Account (Firestore)", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (Arabic/English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email / Egyptian ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Initial Balance (EGP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Daily Limit (EGP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            AdminFirestoreRepository.createNewUserAccountInFirestore(
                                name = name,
                                email = email.ifBlank { "user@nilepay.eg" },
                                initialBalance = balance.toDoubleOrNull() ?: 10000.0,
                                dailyLimit = limit.toDoubleOrNull() ?: 25000.0
                            )
                            showCreateUserAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Create in Firestore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserAccountDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Sheet: Edit Company Permissions & Kill Switch
    if (editingCompany != null) {
        val company = editingCompany!!
        var currentPerms by remember { mutableStateOf(company.grantedPermissions) }
        var isCompanyActive by remember { mutableStateOf(company.isActive) }
        var maxLimitInput by remember { mutableStateOf(company.maxSingleTxLimitEGP.toString()) }

        AlertDialog(
            onDismissRequest = { editingCompany = null },
            title = {
                Text(
                    text = "Edit Access: ${company.companyName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = "DYNAMIC ACCESS KILL SWITCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompanyActive) NileSuccess else NileError
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { isCompanyActive = !isCompanyActive },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kill_switch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompanyActive) NileError else NileSuccess
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompanyActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCompanyActive) "INSTANTLY REVOKE ALL ACCESS" else "RESTORE ACCESS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "MAX SINGLE TRANSACTION LIMIT (EGP)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = maxLimitInput,
                            onValueChange = { maxLimitInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "GRANULAR FEATURE PERMISSIONS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(FeaturePermission.values().toList()) { perm ->
                        val isChecked = currentPerms.contains(perm)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentPerms = if (isChecked) currentPerms - perm else currentPerms + perm
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    currentPerms = if (checked) currentPerms + perm else currentPerms - perm
                                },
                                colors = CheckboxDefaults.colors(checkedColor = NileCyan),
                                modifier = Modifier.testTag("toggle_perm_${perm.code}")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = perm.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = perm.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = maxLimitInput.toDoubleOrNull() ?: company.maxSingleTxLimitEGP
                        AccessControlManager.updateCompanyPermissions(
                            companyId = company.companyId,
                            updatedPermissions = currentPerms,
                            isActive = isCompanyActive,
                            maxLimitEGP = limit
                        )
                        // Sync to Firestore
                        AdminFirestoreRepository.pushCompanyProfileToFirestore(
                            company.copy(
                                grantedPermissions = currentPerms,
                                isActive = isCompanyActive,
                                maxSingleTxLimitEGP = limit
                            )
                        )
                        editingCompany = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_permissions_button")
                ) {
                    Text(text = "Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCompany = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Sheet: Register New Testing Company
    if (showCreateCompanyDialog) {
        var newName by remember { mutableStateOf("") }
        var newContact by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newLimit by remember { mutableStateOf("") }
        var newPerms by remember {
            mutableStateOf(
                setOf(
                    FeaturePermission.NFC_CONTACTLESS_PAY,
                    FeaturePermission.EGYPTIAN_MOBILE_WALLETS
                )
            )
        }

        AlertDialog(
            onDismissRequest = { showCreateCompanyDialog = false },
            title = { Text(text = "Register Testing Partner Company", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Company Name (e.g. Fawry QA)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContact,
                        onValueChange = { newContact = it },
                        label = { Text("Lead Contact Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Contact Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newLimit,
                        onValueChange = { newLimit = it },
                        label = { Text("Testing Tx Limit (EGP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            AccessControlManager.createCompany(
                                name = newName,
                                contactPerson = newContact.ifBlank { "Tester" },
                                email = newEmail.ifBlank { "test@company.eg" },
                                permissions = newPerms,
                                limitEGP = newLimit.toDoubleOrNull() ?: 10000.0,
                                notes = "Registered via Admin Dashboard"
                            )
                            showCreateCompanyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Create Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCompanyDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Sheet: Transaction Audit Details
    if (selectedTxDetail != null) {
        val tx = selectedTxDetail!!
        AlertDialog(
            onDismissRequest = { selectedTxDetail = null },
            title = {
                Text(
                    text = "Transaction Audit Detail",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "TITLE: ${tx.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "AMOUNT: ${String.format(Locale.US, "%,.2f", tx.amountEGP)} EGP", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NileCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "CBE REFERENCE: ${tx.cbeReference}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NileGold)
                    Text(text = "TYPE: ${tx.type.label}", fontSize = 12.sp)
                    Text(text = "STATUS: ${tx.status.name}", fontWeight = FontWeight.Bold, color = if (tx.status == TransactionStatus.SUCCESS) NileSuccess else NileError)
                    Text(text = "SOURCE: ${tx.source}", fontSize = 12.sp)
                    Text(text = "RECIPIENT: ${tx.recipient}", fontSize = 12.sp)
                    Text(text = "AUTH METHOD: ${tx.authMethod}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    if (tx.terminalLocation != null) {
                        Text(text = "LOCATION: ${tx.terminalLocation}", fontSize = 12.sp)
                    }
                    if (tx.apduCode != null) {
                        Text(text = "APDU STATUS: ${tx.apduCode}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NileCyan)
                    }
                    if (tx.notes != null) {
                        Text(text = "NOTES: ${tx.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxDetail = null }) {
                    Text(text = "Close", fontWeight = FontWeight.Bold, color = NileCyan)
                }
            }
        )
    }

    // Modal Sheet: Cloud Sync & Backup Vault
    if (showCloudSyncDialog) {
        CloudSyncDialog(
            onDismiss = { showCloudSyncDialog = false }
        )
    }
}
