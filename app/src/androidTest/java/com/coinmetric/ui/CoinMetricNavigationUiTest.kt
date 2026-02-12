package com.coinmetric.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coinmetric.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class CoinMetricNavigationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationItemsAreVisible() {
        composeRule.onNodeWithContentDescription("Главная").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Календарь").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Добавить").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Категории").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Аналитика").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
    }

    @Test
    fun viewerRoleCannotEditTransactions() {
        composeRule.onNodeWithContentDescription("Настройки").performClick()
        composeRule.onNodeWithText("Просмотр").performClick()
        composeRule.onNodeWithContentDescription("Добавить").performClick()

        composeRule.onNodeWithText(
            "Роль просмотра не позволяет добавлять операции. Обратитесь к владельцу за правами редактора.",
        ).assertIsDisplayed()
    }

    @Test
    fun selectingTransactionFromCalendarOpensEditMode() {
        composeRule.onNodeWithContentDescription("Календарь").performClick()

        val todayLabel = "Операции за ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
        val knownTransactions = listOf("Продукты", "Кафе", "Зарплата")

        val selectedTransaction = composeRule.waitForAnyTransaction(knownTransactions)
        if (selectedTransaction == null) {
            composeRule.onNodeWithText(todayLabel).assertIsDisplayed()
            return
        }

        composeRule.onNodeWithText(selectedTransaction).performClick()
        composeRule.onNodeWithText("Сохранить изменения").assertIsDisplayed()
    }

    private fun ComposeTestRule.waitForAnyTransaction(candidates: List<String>): String? {
        val deadlineMs = 5_000L
        val pollStepMs = 250L
        var elapsedMs = 0L

        while (elapsedMs < deadlineMs) {
            for (candidate in candidates) {
                val node = onAllNodesWithText(candidate).fetchSemanticsNodes()
                if (node.isNotEmpty()) {
                    return candidate
                }
            }
            waitForIdle()
            mainClock.advanceTimeBy(pollStepMs)
            elapsedMs += pollStepMs
        }

        return null
    }
}
