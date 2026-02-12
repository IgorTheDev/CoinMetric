package com.coinmetric.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinMetricViewModelCategoryLimitTest {

    @Test
    fun addNewCategory_addsCategoryAndClearsInput() {
        val vm = CoinMetricViewModel()
        vm.updateCategoriesNewCategoryName("Питомцы")

        vm.addNewCategory()

        val state = vm.categoriesState.value
        assertTrue(state.categories.contains("Питомцы"))
        assertEquals("", state.newCategoryName)
        assertEquals("Категория добавлена", state.successMessage)
    }

    @Test
    fun addNewCategory_duplicateShowsError() {
        val vm = CoinMetricViewModel()
        vm.updateCategoriesNewCategoryName("Еда")

        vm.addNewCategory()

        assertEquals("Такая категория уже существует", vm.categoriesState.value.error)
    }

    @Test
    fun saveMonthlyLimit_withInvalidValueReturnsValidationError() {
        val vm = CoinMetricViewModel()
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("0")

        vm.saveMonthlyLimit()

        assertEquals("Введите корректный лимит", vm.categoriesState.value.error)
    }

    @Test
    fun saveMonthlyLimit_persistsLimitForSelectedCategory() {
        val vm = CoinMetricViewModel()
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("17500")

        vm.saveMonthlyLimit()

        val state = vm.categoriesState.value
        assertEquals(17_500, state.monthlyLimits["Еда"])
        assertEquals("Лимит на месяц сохранён", state.successMessage)
    }
}
