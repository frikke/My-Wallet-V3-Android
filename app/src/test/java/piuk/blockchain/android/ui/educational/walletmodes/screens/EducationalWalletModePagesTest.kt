package piuk.blockchain.android.ui.educational.walletmodes.screens

import kotlin.test.assertEquals
import org.junit.Test

class EducationalWalletModePagesTest {

    @Test
    fun `GIVEN ordinal = 0, WHEN get values of ordinal, THEN INTRO should be returned`() {
        assertEquals(EducationalWalletModePages.INTRO, EducationalWalletModePages.values()[0])
    }

    @Test
    fun `GIVEN ordinal = 1, WHEN get values of ordinal, THEN MENU should be returned`() {
        assertEquals(EducationalWalletModePages.MENU, EducationalWalletModePages.values()[1])
    }

    @Test
    fun `GIVEN ordinal = 2, WHEN get values of ordinal, THEN DEFI should be returned`() {
        assertEquals(EducationalWalletModePages.DEFI, EducationalWalletModePages.values()[2])
    }

    @Test
    fun `GIVEN ordinal = 3, WHEN get values of ordinal, THEN TRADING should be returned`() {
        assertEquals(EducationalWalletModePages.TRADING, EducationalWalletModePages.values()[3])
    }
}
