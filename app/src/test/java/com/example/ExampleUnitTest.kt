package com.example

import com.example.cards.CardRepository
import com.example.cards.NfcPaymentResult
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.model.WalletProvider
import com.example.nfc.NfcCardScanner
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.security.PermissionCheckResult
import com.example.wallets.EgyptianWalletEngine
import com.example.wallets.WalletOperationResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Before
    fun setUp() {
        val active = AccessControlManager.activeCompany.value
        if (!active.isActive) {
            AccessControlManager.setCompanyActiveStatus(active.companyId, true)
        }
    }

    @Test
    fun testCardTypeResolution() {
        assertEquals(CardType.VISA, NfcCardScanner.resolveCardType("4211890123456789", "A0000000031010"))
        assertEquals(CardType.MASTERCARD, NfcCardScanner.resolveCardType("5234901234561042", "A0000000041010"))
        assertEquals(CardType.MEEZA, NfcCardScanner.resolveCardType("5078129087654321", "A0000007271010"))
    }

    @Test
    fun testIssuingBankResolution() {
        assertTrue(NfcCardScanner.resolveIssuingBank("4211890123456789").contains("Commercial International Bank"))
        assertTrue(NfcCardScanner.resolveIssuingBank("5234901234561042").contains("Banque Misr"))
        assertTrue(NfcCardScanner.resolveIssuingBank("5078129087654321").contains("National Bank of Egypt"))
    }

    @Test
    fun testTokenizationAndCryptogram() {
        val token = CryptoTokenManager.tokenizeCard(
            rawCardNumber = "4211890123456789",
            holderName = "MAHMOUD EL-SHARKAWY",
            issuerBank = "CIB Egypt",
            cardType = CardType.VISA,
            expiryMonth = "10",
            expiryYear = "29"
        )

        assertEquals("6789", token.last4)
        assertTrue(token.fpanMasked.endsWith("6789"))
        assertTrue(token.dpan.contains("••••"))

        val cryptogram = CryptoTokenManager.generateEmvCryptogram(token.dpan, 250.0)
        assertNotNull(cryptogram.applicationCryptogramHex)
        assertEquals(16, cryptogram.applicationCryptogramHex.length) // 8 bytes in Hex
        assertTrue(cryptogram.atc > 0)
    }

    @Test
    fun testDynamicAccessControl() {
        val created = AccessControlManager.createCompany(
            name = "Alpha Testing Corp",
            contactPerson = "Lead QA",
            email = "qa@alpha.eg",
            permissions = setOf(FeaturePermission.NFC_CONTACTLESS_PAY),
            limitEGP = 500.0,
            notes = "Test environment"
        )

        assertNotNull(created)

        AccessControlManager.switchActiveCompany(created.companyId)
        assertTrue(AccessControlManager.hasPermission(FeaturePermission.NFC_CONTACTLESS_PAY))
        assertFalse(AccessControlManager.hasPermission(FeaturePermission.EGYPTIAN_MOBILE_WALLETS))

        // Check transaction limit
        val withinLimit = AccessControlManager.checkPermission(FeaturePermission.NFC_CONTACTLESS_PAY, 300.0)
        assertTrue(withinLimit is PermissionCheckResult.Granted)

        val exceedsLimit = AccessControlManager.checkPermission(FeaturePermission.NFC_CONTACTLESS_PAY, 800.0)
        assertTrue(exceedsLimit is PermissionCheckResult.Denied)

        // Toggle / Revoke access instantly
        AccessControlManager.togglePermission(created.companyId, FeaturePermission.NFC_CONTACTLESS_PAY)
        assertFalse(AccessControlManager.hasPermission(FeaturePermission.NFC_CONTACTLESS_PAY))

        // Reset to master owner
        AccessControlManager.switchActiveCompany("company-admin-master")
    }

    @Test
    fun testEgyptianWalletLinkingAndTransfer() {
        val initialWalletCount = EgyptianWalletEngine.wallets.value.size

        val wallet = EgyptianWalletEngine.linkWallet(
            provider = WalletProvider.VODAFONE_CASH,
            identifier = "01012345678",
            accountTitle = "Vodafone Cash Main",
            initialBalance = 1500.0
        )

        assertEquals(initialWalletCount + 1, EgyptianWalletEngine.wallets.value.size)
        assertEquals(1500.0, wallet.balanceEGP, 0.001)

        val transferResult = EgyptianWalletEngine.sendMobileWalletTransfer(
            walletId = wallet.id,
            targetPhone = "01098765432",
            amountEGP = 200.0,
            pin = "123456"
        )

        assertTrue(transferResult is WalletOperationResult.Success)
        val updatedWallet = EgyptianWalletEngine.wallets.value.find { it.id == wallet.id }
        assertEquals(1300.0, updatedWallet!!.balanceEGP, 0.001)
    }

    @Test
    fun testNfcPaymentExecution() {
        val card = CardRepository.registerCard(
            cardNumber = "4211890123456789",
            holderName = "AHMED HASSAN",
            issuerBank = "CIB",
            cardType = CardType.VISA,
            expiryMonth = "12",
            expiryYear = "28"
        )

        val paymentResult = CardRepository.processNfcTapPayment(
            cardId = card.id,
            merchantName = "Costa Coffee Zamalek",
            merchantCategory = "Dining",
            terminalId = "POS-CST-1099",
            amountEGP = 120.0
        )

        assertTrue(paymentResult is NfcPaymentResult.Success)
        val success = paymentResult as NfcPaymentResult.Success
        assertNotNull(success.cryptogramHex)
        assertEquals(120.0, success.transaction.amountEGP, 0.001)
        assertTrue(success.transaction.cbeReference.startsWith("CBE-NFC-"))
    }

    @Test
    fun testNfcServiceIntentFiltersAndTechLists() {
        val nfcService = com.example.nfc.NfcService(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val filters = nfcService.createContactlessPaymentIntentFilters()
        assertEquals(2, filters.size)
        assertTrue(filters.any { it.hasAction(android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED) })
        assertTrue(filters.any { it.hasAction(android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED) })

        val techLists = nfcService.createPaymentTechLists()
        assertTrue(techLists.isNotEmpty())
        assertTrue(techLists.any { list -> list.contains(android.nfc.tech.IsoDep::class.java.name) })
        assertTrue(techLists.any { list -> list.contains(android.nfc.tech.NfcA::class.java.name) })
    }
}
