package com.coinmetric.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coinmetric.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
}
