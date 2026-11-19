package com.stasybell.os

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.stasybell.os.data.local.StasyBellDatabase
import com.stasybell.os.data.repository.LocalDemoRepository
import com.stasybell.os.security.SecureTokenStore
import com.stasybell.os.ui.navigation.StasyBellApp
import com.stasybell.os.ui.theme.StasyBellTheme
import com.stasybell.os.ui.viewmodel.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI Critical User Journey (CUJ) tests under Robolectric:
 * Login -> Today Dashboard -> 5 Bottom Tabs -> Finance manual income -> Reports & Archive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xxhdpi")
class StasyBellUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: StasyBellDatabase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StasyBellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val tokenStore = SecureTokenStore(context)
        tokenStore.clearSession()
        val repo = LocalDemoRepository(db, tokenStore)
        runBlocking { repo.initialize() }
        viewModel = MainViewModel(repo, tokenStore, initiallyInitialized = true)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `owner login navigates to Today dashboard, all 5 tabs render, and manual cash income works`() {
        composeRule.setContent {
            StasyBellTheme {
                StasyBellApp(viewModel = viewModel, onRequestBiometricAuth = {})
            }
        }

        // Screen 01: Login with prefilled Demo credentials
        waitForTag("login_submit_button")
        composeRule.onNodeWithTag("login_submit_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Screen 02: Today Dashboard with KPI tiles
        waitForTag("today_screen")
        composeRule.onNodeWithTag("today_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("kpi_today_cash").assertIsDisplayed()

        // Screen 03/04: Calendar tab
        composeRule.onNodeWithTag("tab_calendar").performClick()
        waitForTag("calendar_screen")
        composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()

        // Screen 07: Clients tab
        composeRule.onNodeWithTag("tab_clients").performClick()
        waitForTag("clients_screen")
        composeRule.onNodeWithTag("clients_screen").assertIsDisplayed()

        // Screen 10/11: Finance tab + add manual income
        composeRule.onNodeWithTag("tab_finance").performClick()
        waitForTag("finance_screen")
        composeRule.onNodeWithTag("finance_kpi_balance").assertIsDisplayed()
        composeRule.onNodeWithTag("finance_add_income_button").performClick()
        waitForTag("tx_amount_input")
        composeRule.onNodeWithTag("tx_amount_input").performTextReplacement("3500")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_save_button").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        val sheetClosed = composeRule.onAllNodesWithTag("tx_amount_input").fetchSemanticsNodes().isEmpty()
        check(sheetClosed) { "AddTransactionOverlaySheet did not close after clicking tx_save_button" }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.uiState.value.financeTransactions.any { it.amountKopecks == 350_000L }
        }
        composeRule.waitForIdle()

        // Screen 16: More & Settings tab -> Open Reports & Archive
        composeRule.onNodeWithTag("tab_more").performClick()
        waitForTag("more_screen")
        composeRule.onNodeWithTag("more_reports_tile").performClick()
        waitForTag("reports_screen")
        composeRule.onNodeWithTag("report_tab_archive").performClick()
        waitForTag("archive_orphan_header")
        composeRule.onNodeWithTag("archive_orphan_header").assertIsDisplayed()

        // Navigate back to More and open live Website Preview
        composeRule.onNodeWithTag("reports_back_button").performClick()
        waitForTag("more_screen")
        composeRule.onNodeWithTag("more_website_tile").performClick()
        waitForTag("website_preview_screen")
        composeRule.onNodeWithTag("website_preview_test_placeholder").assertIsDisplayed()
        composeRule.onNodeWithTag("website_preview_back_button").performClick()
        waitForTag("more_screen")
    }
}
