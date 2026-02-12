package com.coinmetric.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coinmetric.ui.theme.CoinMetricTheme

private sealed class Screen(val route: String, val label: String) {
    data object Dashboard : Screen("/", "Главная")
    data object Calendar : Screen("/calendar", "Календарь")
    data object Add : Screen("/add", "Добавить")
    data object Analytics : Screen("/analytics", "Аналитика")
    data object Settings : Screen("/settings", "Настройки")
}

private data class HeaderConfig(
    val title: String,
    val subtitle: String? = null,
)

@Composable
fun CoinMetricRoot(vm: CoinMetricViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination
    val currentRoute = destination?.route ?: Screen.Dashboard.route

    val navScreens = listOf(
        Screen.Dashboard,
        Screen.Calendar,
        Screen.Add,
        Screen.Analytics,
        Screen.Settings,
    )

    CoinMetricTheme(darkTheme = settings.darkThemeEnabled) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            topBar = {
                HeaderTitle(
                    route = currentRoute,
                    onCancelAdd = { navController.navigateUp() },
                )
            },
            bottomBar = {
                NavigationBar {
                    navScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(screen.label.take(1)) },
                            label = { Text(screen.label) },
                        )
                    }
                }
            },
        ) { padding ->
            MobileLayout(padding) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(vm)
                    }
                    composable(Screen.Calendar.route) {
                        CalendarScreen(vm) { navController.navigate(Screen.Add.route) }
                    }
                    composable(Screen.Add.route) {
                        AddScreen(vm) { navController.navigate(Screen.Dashboard.route) }
                    }
                    composable(Screen.Analytics.route) {
                        AnalyticsScreen(vm)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileLayout(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 12.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderTitle(route: String, onCancelAdd: () -> Unit) {
    val config = when (route) {
        Screen.Calendar.route -> HeaderConfig("Календарь", "Операции по датам")
        Screen.Add.route -> HeaderConfig("Добавление операции")
        Screen.Analytics.route -> HeaderConfig("Аналитика", "Структура расходов и лимиты")
        Screen.Settings.route -> HeaderConfig("Настройки", "Тема, синхронизация и доступ")
        else -> HeaderConfig("CoinMetric", "Семейный финансовый обзор")
    }

    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(config.title)
                config.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (route == Screen.Add.route) {
                IconButton(
                    onClick = onCancelAdd,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        "❌",
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        actions = {
            if (route == Screen.Add.route) {
                Spacer(Modifier.widthIn(min = 64.dp))
            }
        },
    )
}

@Composable
private fun DashboardScreen(vm: CoinMetricViewModel) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (settings.showOnboarding) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Быстрый старт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("1) Добавьте первую операцию через кнопку «Добавить».\n2) Укажите лимиты и следите за прогрессом.\n3) Включите семейный доступ в настройках для совместного бюджета.")
                        Button(
                            onClick = vm::dismissOnboarding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Понятно")
                        }
                    }
                }
            }
        }
        if (state.isLoading) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Общий баланс", style = MaterialTheme.typography.titleMedium)
                    Text(state.balance.toRubCurrency(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Доход: ${state.income.toRubCurrency()}")
                        Text("Расход: ${state.expense.toRubCurrency()}")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Лимиты", "${state.limitsUsedPercent}%", Modifier.weight(1f))
                MetricCard("Ср. расход/день", state.avgDailyExpense.toRubCurrency(), Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Тренд расходов", fontWeight = FontWeight.SemiBold)
                    Text(state.expenseTrendText)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Последние операции", fontWeight = FontWeight.SemiBold)
                    state.latestTransactions.forEach { Text(it) }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddScreen(vm: CoinMetricViewModel, goToDashboard: () -> Unit) {
    val state by vm.addState.collectAsStateWithLifecycle()
    var calculatorExpanded by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf(state.amount) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Введите сумму, выберите категорию и сохраните операцию",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.amount,
                onValueChange = vm::updateAmount,
                label = { Text("Сумма") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.amountError != null,
                singleLine = true,
                supportingText = {
                    state.amountError?.let { Text(it) }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        calculatorExpanded = !calculatorExpanded
                        if (calculatorExpanded) expression = state.amount
                    }) {
                        Text("🧮", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    }
                }
            )

            if (calculatorExpanded) {
                CalculatorPad(
                    expression = expression,
                    onExpressionChange = { expression = it },
                    onApply = {
                        val result = runCatching { evalMathExpression(expression) }.getOrNull()
                        if (result != null && result >= 0) {
                            vm.updateAmount(result.toInt().toString())
                            expression = result.toInt().toString()
                            calculatorExpanded = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.category,
                onValueChange = vm::updateCategory,
                label = { Text("Категория") },
                isError = state.categoryError != null,
                singleLine = true,
                supportingText = {
                    state.categoryError?.let { Text(it) }
                },
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.note,
                onValueChange = vm::updateNote,
                label = { Text("Заметка") },
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Доход")
                Switch(checked = state.isIncome, onCheckedChange = vm::updateIncomeFlag)
            }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        state.successMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.saveTransaction(goToDashboard) },
            ) {
                Text(if (state.id == null) "Сохранить" else "Сохранить изменения")
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun CalculatorPad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", "(", ")", "+"),
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = expression,
                onValueChange = onExpressionChange,
                label = { Text("Калькулятор") },
                singleLine = true,
            )
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Button(
                            onClick = { onExpressionChange(expression + key) },
                            modifier = Modifier.weight(1f),
                        ) { Text(key) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onExpressionChange("") }, modifier = Modifier.weight(1f)) { Text("C") }
                Button(
                    onClick = { onExpressionChange(expression.dropLast(1)) },
                    modifier = Modifier.weight(1f),
                ) { Text("⌫") }
                Button(onClick = onApply, modifier = Modifier.weight(2f)) { Text("=") }
            }
        }
    }
}

@Composable
private fun AnalyticsScreen(vm: CoinMetricViewModel) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val expensesByCategory = state.recentTransactions
        .filterNot { it.income }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { kotlin.math.abs(it.amount) } }
    val totalExpenses = expensesByCategory.values.sum().coerceAtLeast(1)

    val categoryDistribution = expensesByCategory.entries
        .sortedByDescending { it.value }
        .map { (title, amount) ->
            title to amount.toFloat() / totalExpenses
        }

    val limitsByCategory = mapOf(
        "Еда" to 15_000,
        "Транспорт" to 6_000,
        "Развлечения" to 7_500,
        "Досуг" to 5_000,
    ).map { (title, limit) ->
        val spent = expensesByCategory[title] ?: 0
        title to (spent.toFloat() / limit).coerceIn(0f, 1f)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.isLoading) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        item {
            Text(
                "Краткая сводка распределения трат и статуса лимитов",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Распределение расходов", fontWeight = FontWeight.SemiBold)
                    if (categoryDistribution.isEmpty()) {
                        Text("Пока нет расходных операций для анализа")
                    } else {
                        categoryDistribution.forEach { (title, percent) ->
                            Text("$title — ${(percent * 100).toInt()}%")
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Лимиты по категориям", fontWeight = FontWeight.SemiBold)
                    limitsByCategory.forEach { (title, progress) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$title: ${(progress * 100).toInt()}%")
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = if (progress >= 0.85f) MaterialTheme.colorScheme.error else Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun CalendarScreen(vm: CoinMetricViewModel, openAddScreen: () -> Unit) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val transactions = state.recentTransactions
    val datesWithTransactions = remember(transactions) { transactions.map { LocalDate.parse(it.date) }.toSet() }
    var selectedDate by remember { mutableStateOf(datesWithTransactions.maxOrNull() ?: LocalDate.now()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "История транзакций по датам. Нажмите на операцию, чтобы отредактировать.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Календарь", fontWeight = FontWeight.SemiBold)
                    CalendarView(
                        selectedDate = selectedDate,
                        datesWithTransactions = datesWithTransactions,
                        onDateSelected = { selectedDate = it },
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Операции за ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", fontWeight = FontWeight.SemiBold)
                    val selectedItems = transactions.filter { it.date == selectedDate.toString() }
                    if (selectedItems.isEmpty()) {
                        Text("На выбранную дату операций нет")
                    } else {
                        selectedItems.forEach { tx ->
                            val sign = if (tx.amount >= 0) "+" else "-"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.startEditingTransaction(tx)
                                        openAddScreen()
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(tx.title, fontWeight = FontWeight.Medium)
                                    Text(tx.category, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("$sign${kotlin.math.abs(tx.amount).toRubCurrency()}")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    datesWithTransactions: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val firstDay = currentMonth.atDay(1)
    val leadingEmpty = (firstDay.dayOfWeek.value + 6) % 7
    val monthDays = currentMonth.lengthOfMonth()
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Text("◀") }
            Text(
                currentMonth.atDay(1).format(monthFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Text("▶") }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        val totalCells = ((leadingEmpty + monthDays + 6) / 7) * 7
        for (weekStart in 0 until totalCells step 7) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (offset in 0..6) {
                    val index = weekStart + offset
                    val dayNumber = index - leadingEmpty + 1
                    if (dayNumber !in 1..monthDays) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = currentMonth.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val hasTransactions = date in datesWithTransactions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                )
                                .background(
                                    if (hasTransactions) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent,
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(dayNumber.toString(), fontWeight = if (hasTransactions) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: CoinMetricViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Персонализация приложения и параметры семейного доступа",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Тема и синхронизация", fontWeight = FontWeight.SemiBold)
                    SettingRow("Тёмная тема", settings.darkThemeEnabled) { vm.setDarkTheme(it) }
                    SettingRow("Синхронизация Google", settings.googleSyncEnabled) { vm.setGoogleSync(it) }
                    SettingRow("Офлайн-режим", settings.isOfflineMode) { vm.setOfflineMode(it) }
                    SettingRow("Показывать подсказки", settings.showOnboarding) { vm.setOnboardingVisible(it) }

                    val syncStatus = when {
                        settings.isSyncInProgress -> "Синхронизация выполняется..."
                        settings.syncError != null -> settings.syncError ?: "Ошибка синхронизации"
                        settings.pendingSyncItems > 0 -> "Ожидают отправки: ${settings.pendingSyncItems}"
                        settings.lastSyncTimeLabel != null -> "Последняя синхронизация: ${settings.lastSyncTimeLabel}"
                        else -> "Локальная база готова к работе офлайн"
                    }

                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.syncError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )

                    if (settings.syncError != null) {
                        Button(onClick = vm::retrySync, modifier = Modifier.fillMaxWidth()) {
                            Text("Повторить синхронизацию")
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Семейный доступ", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Пригласите участника семьи, чтобы совместно вести бюджет и видеть общие лимиты.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = settings.inviteEmail,
                        onValueChange = vm::updateInviteEmail,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email участника") },
                        isError = settings.inviteError != null,
                        singleLine = true,
                        supportingText = {
                            settings.inviteError?.let { Text(it) }
                        },
                    )
                    Text("Роль доступа", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.inviteRole == "viewer",
                            onClick = { vm.updateInviteRole("viewer") },
                            label = { Text("Просмотр") },
                        )
                        FilterChip(
                            selected = settings.inviteRole == "editor",
                            onClick = { vm.updateInviteRole("editor") },
                            label = { Text("Редактор") },
                        )
                    }
                    Button(onClick = vm::sendFamilyInvite, modifier = Modifier.fillMaxWidth()) {
                        Text("Отправить приглашение")
                    }
                    settings.inviteSuccessMessage?.let { successText ->
                        Text(
                            successText,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (settings.pendingInvites.isNotEmpty()) {
                        Text("Отправленные приглашения", style = MaterialTheme.typography.labelLarge)
                        settings.pendingInvites.forEach { invite ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(invite.email)
                                        Text(
                                            "Роль: ${invite.role} · ${invite.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (invite.status == "Ожидает принятия") {
                                                    MaterialTheme.colorScheme.tertiary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                            ),
                                    )
                                }
                                if (invite.status == "Ожидает принятия") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.updateInviteStatus(invite.email, "Принято") }) {
                                            Text("Принять")
                                        }
                                        Button(onClick = { vm.updateInviteStatus(invite.email, "Отклонено") }) {
                                            Text("Отклонить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Общие настройки", fontWeight = FontWeight.SemiBold)
                    Text("Валюта по умолчанию: RUB")
                    Text("Уведомления о лимитах: включены")
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Выйти из аккаунта")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Простой парсер математических выражений
 * Поддерживает основные операции: +, -, *, /
 */
fun evalMathExpression(expr: String): Double {
    val tokens = expr.replace(" ", "")
    if (tokens.isBlank()) return 0.0

    fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        else -> 0
    }

    val values = ArrayDeque<Double>()
    val operators = ArrayDeque<Char>()
    var i = 0

    fun applyTopOperator() {
        val op = operators.removeLast()
        val right = values.removeLast()
        val left = values.removeLast()
        values.addLast(
            when (op) {
                '+' -> left + right
                '-' -> left - right
                '*' -> left * right
                '/' -> left / right
                else -> 0.0
            },
        )
    }

    while (i < tokens.length) {
        val ch = tokens[i]
        when {
            ch.isDigit() || ch == '.' -> {
                var j = i
                while (j < tokens.length && (tokens[j].isDigit() || tokens[j] == '.')) j++
                values.addLast(tokens.substring(i, j).toDouble())
                i = j
            }
            ch == '(' -> {
                operators.addLast(ch)
                i++
            }
            ch == ')' -> {
                while (operators.isNotEmpty() && operators.last() != '(') applyTopOperator()
                if (operators.isNotEmpty() && operators.last() == '(') operators.removeLast()
                i++
            }
            ch in charArrayOf('+', '-', '*', '/') -> {
                while (operators.isNotEmpty() && precedence(operators.last()) >= precedence(ch)) {
                    if (operators.last() == '(') break
                    applyTopOperator()
                }
                operators.addLast(ch)
                i++
            }
            else -> i++
        }
    }

    while (operators.isNotEmpty()) {
        if (operators.last() == '(') {
            operators.removeLast()
        } else {
            applyTopOperator()
        }
    }

    return values.lastOrNull() ?: 0.0
}
