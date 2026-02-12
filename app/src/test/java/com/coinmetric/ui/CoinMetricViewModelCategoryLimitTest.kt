package com.coinmetric.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinMetricViewModelCategoryLimitTest {

    @Test
    fun setCategoryMonthlyLimit_updatesSelectedCategoryLimit() {
        val vm = CoinMetricViewModel()
        vm.updateCategory("Еда")
        vm.updateCategoryLimitAmount("22000")

        vm.setCategoryMonthlyLimit()

        assertEquals(22_000, vm.dashboard.value.categoryMonthlyLimits["Еда"])
        assertTrue(vm.addState.value.categoryLimitMessage?.contains("Еда") == true)
    }

    @Test
    fun setCategoryMonthlyLimit_withoutCategory_setsError() {
        val vm = CoinMetricViewModel()
        vm.updateCategoryLimitAmount("10000")

        vm.setCategoryMonthlyLimit()

        assertEquals("Сначала выберите категорию", vm.addState.value.error)
    }
}
