package com.example.security

import com.example.model.CompanyTesterProfile
import com.example.model.FeaturePermission
import com.example.storage.CloudSyncManager
import com.example.storage.NilePayStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class PermissionCheckResult {
    object Granted : PermissionCheckResult()
    data class Denied(val message: String, val requiredPermission: FeaturePermission) : PermissionCheckResult()
}

object AccessControlManager {

    private val masterOwnerCompany = CompanyTesterProfile(
        companyId = "company-admin-master",
        companyName = "NilePay Core Operations (Admin)",
        contactPerson = "System Owner",
        contactEmail = "admin@nilepay.eg",
        isActive = true,
        grantedPermissions = FeaturePermission.values().toSet(),
        maxSingleTxLimitEGP = 100000.0,
        testEnvironment = "Production & Live Operations",
        notes = "Primary administrator account."
    )

    private val _companies = MutableStateFlow<List<CompanyTesterProfile>>(listOf(masterOwnerCompany))
    val companies: StateFlow<List<CompanyTesterProfile>> = _companies.asStateFlow()

    // Default active profile for the app's current user/device session
    private val _activeCompany = MutableStateFlow<CompanyTesterProfile>(masterOwnerCompany)
    val activeCompany: StateFlow<CompanyTesterProfile> = _activeCompany.asStateFlow()

    fun loadFromStorageOrSeed() {
        val loaded = NilePayStorageManager.loadCompanies()
        if (loaded != null && loaded.companies.isNotEmpty()) {
            _companies.value = loaded.companies
            val active = loaded.companies.find { it.companyId == loaded.activeCompanyId }
                ?: loaded.companies.firstOrNull() ?: masterOwnerCompany
            _activeCompany.value = active
        } else {
            val seedPartners = listOf(
                masterOwnerCompany,
                CompanyTesterProfile(
                    companyId = "company-vodafone-eg",
                    companyName = "Vodafone Egypt Partner Testing",
                    contactPerson = "Ahmed El-Sayed",
                    contactEmail = "ahmed.sayed@vodafone.com.eg",
                    isActive = true,
                    grantedPermissions = setOf(
                        FeaturePermission.NFC_CONTACTLESS_PAY,
                        FeaturePermission.EGYPTIAN_MOBILE_WALLETS,
                        FeaturePermission.VODAFONE_CASH_CASHOUT,
                        FeaturePermission.INSTAPAY_TRANSFERS
                    ),
                    maxSingleTxLimitEGP = 25000.0,
                    testEnvironment = "Vodafone Cash Sandbox",
                    notes = "Integration testing partner for MNO wallet & cashout."
                ),
                CompanyTesterProfile(
                    companyId = "company-cbe-meeza",
                    companyName = "CBE Meeza IPN QA Lab",
                    contactPerson = "Mona Radwan",
                    contactEmail = "mona.radwan@cbe.org.eg",
                    isActive = true,
                    grantedPermissions = FeaturePermission.values().toSet(),
                    maxSingleTxLimitEGP = 50000.0,
                    testEnvironment = "CBE National Switch IPN",
                    notes = "Central Bank regulatory compliance testing."
                )
            )
            _companies.value = seedPartners
            _activeCompany.value = masterOwnerCompany
            persistAndSync()
        }
    }

    fun applyRestoredCompanies(restoredCompanies: List<CompanyTesterProfile>, activeId: String?) {
        _companies.value = if (restoredCompanies.isNotEmpty()) restoredCompanies else listOf(masterOwnerCompany)
        val active = _companies.value.find { it.companyId == activeId } ?: _companies.value.first()
        _activeCompany.value = active
        NilePayStorageManager.saveCompanies(_companies.value, _activeCompany.value.companyId)
    }

    private fun persistAndSync() {
        NilePayStorageManager.saveCompanies(_companies.value, _activeCompany.value.companyId)
        CloudSyncManager.triggerAutoSyncIfEnabled()
    }

    fun deleteCompany(companyId: String) {
        if (companyId == masterOwnerCompany.companyId) return // Cannot delete master owner
        _companies.value = _companies.value.filter { it.companyId != companyId }
        if (_activeCompany.value.companyId == companyId) {
            _activeCompany.value = masterOwnerCompany
        }
        persistAndSync()
    }

    fun switchActiveCompany(companyId: String) {
        val target = _companies.value.find { it.companyId == companyId }
        if (target != null) {
            _activeCompany.value = target
            persistAndSync()
        }
    }

    /**
     * Instantly check if a feature permission is currently granted to the active testing tenant
     */
    fun hasPermission(permission: FeaturePermission): Boolean {
        val profile = _activeCompany.value
        if (!profile.isActive) return false
        return profile.grantedPermissions.contains(permission)
    }

    /**
     * Evaluates permission and transaction limit constraints
     */
    fun checkPermission(
        permission: FeaturePermission,
        amountEGP: Double = 0.0
    ): PermissionCheckResult {
        val profile = _activeCompany.value

        if (!profile.isActive) {
            return PermissionCheckResult.Denied(
                message = "Access for '${profile.companyName}' has been revoked or suspended by the NilePay administrator.",
                requiredPermission = permission
            )
        }

        if (!profile.grantedPermissions.contains(permission)) {
            return PermissionCheckResult.Denied(
                message = "Feature '${permission.title}' is not authorized for testing group '${profile.companyName}'. Please request access from the NilePay dashboard.",
                requiredPermission = permission
            )
        }

        if (amountEGP > profile.maxSingleTxLimitEGP) {
            return PermissionCheckResult.Denied(
                message = "Transaction amount (${amountEGP} EGP) exceeds testing policy limit (${profile.maxSingleTxLimitEGP} EGP) for '${profile.companyName}'.",
                requiredPermission = FeaturePermission.HIGH_VALUE_TRANSACTIONS
            )
        }

        return PermissionCheckResult.Granted
    }

    /**
     * Instantly grant or revoke permissions for a company from the Admin Dashboard
     */
    fun updateCompanyPermissions(
        companyId: String,
        updatedPermissions: Set<FeaturePermission>,
        isActive: Boolean,
        maxLimitEGP: Double? = null
    ) {
        _companies.value = _companies.value.map { company ->
            if (company.companyId == companyId) {
                company.copy(
                    grantedPermissions = updatedPermissions,
                    isActive = isActive,
                    maxSingleTxLimitEGP = maxLimitEGP ?: company.maxSingleTxLimitEGP
                )
            } else {
                company
            }
        }

        // Keep active company in sync immediately if it was modified
        if (_activeCompany.value.companyId == companyId) {
            val updated = _companies.value.find { it.companyId == companyId }
            if (updated != null) {
                _activeCompany.value = updated
            }
        }
        persistAndSync()
    }

    /**
     * Create a new testing company tenant from the dashboard
     */
    fun createCompany(
        name: String,
        contactPerson: String,
        email: String,
        permissions: Set<FeaturePermission>,
        limitEGP: Double,
        notes: String
    ): CompanyTesterProfile {
        val newProfile = CompanyTesterProfile(
            companyId = "company-${UUID.randomUUID().toString().take(8)}",
            companyName = name,
            contactPerson = contactPerson,
            contactEmail = email,
            isActive = true,
            grantedPermissions = permissions,
            maxSingleTxLimitEGP = limitEGP,
            testEnvironment = "CBE Partner Sandbox",
            notes = notes
        )
        _companies.value = _companies.value + newProfile
        persistAndSync()
        return newProfile
    }

    /**
     * Instantly toggle a single permission for a company
     */
    fun togglePermission(companyId: String, permission: FeaturePermission) {
        val target = _companies.value.find { it.companyId == companyId } ?: return
        val newSet = if (target.grantedPermissions.contains(permission)) {
            target.grantedPermissions - permission
        } else {
            target.grantedPermissions + permission
        }
        updateCompanyPermissions(companyId, newSet, target.isActive)
    }

    /**
     * Instant one-tap kill-switch: revoke company access completely
     */
    fun setCompanyActiveStatus(companyId: String, isActive: Boolean) {
        val target = _companies.value.find { it.companyId == companyId } ?: return
        updateCompanyPermissions(companyId, target.grantedPermissions, isActive)
    }
}
