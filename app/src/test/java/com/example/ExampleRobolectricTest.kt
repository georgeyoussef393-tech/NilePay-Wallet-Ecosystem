package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cards.CardRepository
import com.example.cards.NfcPaymentResult
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.model.WalletProvider
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.security.PermissionCheckResult
import com.example.wallets.EgyptianWalletEngine
import com.example.wallets.WalletOperationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches NilePay`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NilePay", appName)
    }

    @Test
    fun `card tokenization generates DPAN and EMV cryptogram`() {
        val card = CardRepository.registerCard(
            cardNumber = "4123456789012345",
            holderName = "TAREK SAYED",
            issuerBank = "CIB Egypt",
            cardType = CardType.VISA,
            expiryMonth = "12",
            expiryYear = "28"
        )
        assertTrue(card.dpan.startsWith("4916"))
        assertEquals("2345", card.last4)

        val cryptogram = CryptoTokenManager.generateEmvCryptogram(
            dpan = card.dpan,
            amountEGP = 250.0
        )
        assertNotNull(cryptogram.applicationCryptogramHex)
        assertEquals(16, cryptogram.applicationCryptogramHex.length)
        assertEquals("818", cryptogram.currencyCode) // Egyptian Pound ISO 4217
    }

    @Test
    fun `dynamic access control enforces permissions and instant revocation`() {
        // Admin Master has all permissions
        AccessControlManager.switchActiveCompany("company-admin-master")
        val permResult = AccessControlManager.checkPermission(FeaturePermission.NFC_CONTACTLESS_PAY, 500.0)
        assertTrue(permResult is PermissionCheckResult.Granted)

        // Dynamically create a restricted partner company without NFC permission
        val partner = AccessControlManager.createCompany(
            name = "FinTech Partner QA",
            contactPerson = "Lead QA",
            email = "qa@partner.eg",
            permissions = setOf(FeaturePermission.EGYPTIAN_MOBILE_WALLETS),
            limitEGP = 5000.0,
            notes = "Partner testing wallet only"
        )

        // Switch to the partner
        AccessControlManager.switchActiveCompany(partner.companyId)
        val blockedNfc = AccessControlManager.checkPermission(FeaturePermission.NFC_CONTACTLESS_PAY, 500.0)
        assertTrue(blockedNfc is PermissionCheckResult.Denied)

        // Admin grants NFC permission dynamically
        AccessControlManager.togglePermission(partner.companyId, FeaturePermission.NFC_CONTACTLESS_PAY)
        val grantedNfc = AccessControlManager.checkPermission(FeaturePermission.NFC_CONTACTLESS_PAY, 500.0)
        assertTrue(grantedNfc is PermissionCheckResult.Granted)

        // Test instant kill switch (revoke all access)
        AccessControlManager.setCompanyActiveStatus(partner.companyId, false)
        val killedAccess = AccessControlManager.checkPermission(FeaturePermission.EGYPTIAN_MOBILE_WALLETS, 100.0)
        assertTrue(killedAccess is PermissionCheckResult.Denied)
    }

    @Test
    fun `vodafone cash and egyptian wallet transfer flow`() {
        AccessControlManager.switchActiveCompany("company-admin-master")

        // Dynamically link a real Vodafone Cash wallet with funds
        val wallet = EgyptianWalletEngine.linkWallet(
            provider = WalletProvider.VODAFONE_CASH,
            identifier = "01091234567",
            accountTitle = "Primary Vodafone Cash",
            nationalId = "29408151234567",
            initialBalance = 2500.0
        )

        val transferRes = EgyptianWalletEngine.sendMobileWalletTransfer(
            walletId = wallet.id,
            targetPhone = "01091234567",
            amountEGP = 100.0,
            pin = "123456"
        )
        assertTrue(transferRes is WalletOperationResult.Success)

        val otpRes = EgyptianWalletEngine.requestAtmCashout(
            walletId = wallet.id,
            amountEGP = 200.0
        )
        assertTrue(otpRes is WalletOperationResult.Success)
        val otp = (otpRes as WalletOperationResult.Success).extraInfo
        assertNotNull(otp)
        assertEquals(6, otp?.length)
    }

    @Test
    fun `nfc contactless payment pos execution`() {
        AccessControlManager.switchActiveCompany("company-admin-master")

        // Register a card dynamically
        val card = CardRepository.registerCard(
            cardNumber = "5412345678901234",
            holderName = "TAREK SAYED",
            issuerBank = "Banque Misr",
            cardType = CardType.MASTERCARD,
            expiryMonth = "11",
            expiryYear = "29"
        )

        val paymentResult = CardRepository.processNfcTapPayment(
            cardId = card.id,
            merchantName = "Carrefour Egypt",
            merchantCategory = "Supermarket",
            terminalId = "POS-CRF-01",
            amountEGP = 350.0
        )
        assertTrue(paymentResult is NfcPaymentResult.Success)
    }
}
