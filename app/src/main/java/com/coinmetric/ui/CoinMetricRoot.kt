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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coinmetric.ui.theme.CoinMetricTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.coinmetric.auth.GoogleAuthConfig


private sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : Screen("dashboard", "Главная", Icons.Filled.Home)
    data object Calendar : Screen("calendar", "Календарь", Icons.Filled.CalendarMonth)
    data object Add : Screen("add", "Добавить", Icons.Filled.AddCircle)
    data object Categories : Screen("categories", "Категории", Icons.Filled.Category)
    data object Analytics : Screen("analytics", "Аналитика", Icons.Filled.Analytics)
    data object Settings : Screen("settings", "Настройки", Icons.Filled.Settings)
    data object Subscription : Screen("subscription", "Подписка", Icons.Filled.Settings)
}

private data class HeaderConfig(
    val title: String,
    val subtitle: String? = null,
)

@Composable
fun CoinMetricRoot(startRoute: String? = null, vm: CoinMetricViewModel = viewModel()) {
    val context = LocalContext.current
    val onboardingPrefs = remember(context) { context.getSharedPreferences("coinmetric_prefs", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(vm) {
        val onboardingCompleted = onboardingPrefs.getBoolean("onboarding_completed", false)
        vm.setOnboardingVisible(!onboardingCompleted)
    }

    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(settings.recurringRemindersEnabled) {
        if (settings.recurringRemindersEnabled) {
            RecurringReminderScheduler.schedule(context)
        } else {
            RecurringReminderScheduler.cancel(context)
        }
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: (startRoute ?: Screen.Dashboard.route)

    val limitAlert by vm.limitAlertEvent.collectAsStateWithLifecycle()
    LaunchedEffect(limitAlert) {
        val alert = limitAlert ?: return@LaunchedEffect
        val helper = LimitNotificationHelper(context)
        if (alert.isExceeded) {
            helper.notifyLimitExceeded(alert.category, alert.spent, alert.limit)
        } else {
            helper.notifyLimitAlmostReached(alert.category, alert.spent, alert.limit)
        }
        vm.consumeLimitAlertEvent()
    }


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
                CoinMetricBottomNavigation(
                    currentDestinationRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { padding ->
            MobileLayout(padding) {
                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    startDestination = startRoute ?: Screen.Dashboard.route,
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(vm = vm, onOnboardingDismissed = {
                            onboardingPrefs.edit().putBoolean("onboarding_completed", true).apply()
                        })
                    }
                    composable(Screen.Calendar.route) {
                        CalendarScreen(vm) { navController.navigate(Screen.Add.route) }
                    }
                    composable(Screen.Add.route) {
                        AddScreen(vm) { navController.navigate(Screen.Dashboard.route) }
                    }
                    composable(Screen.Categories.route) {
                        CategoriesScreen(vm = vm)
                    }
                    composable(Screen.Analytics.route) {
                        AnalyticsScreen(
                            vm = vm,
                            openAddScreen = { navController.navigate(Screen.Add.route) },
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            vm = vm,
                            onOpenSubscription = { navController.navigate(Screen.Subscription.route) },
                            onOnboardingVisibilityChanged = { isVisible ->
                                onboardingPrefs.edit().putBoolean("onboarding_completed", !isVisible).apply()
                            },
                        )
                    }
                    composable(Screen.Subscription.route) {
                        SubscriptionScreen(vm = vm)
                    }
                }
            }
        }
    }
}


@Composable
private fun CoinMetricBottomNavigation(
    currentDestinationRoute: String,
    onNavigate: (String) -> Unit,
) {
    val screens = listOf(
        Screen.Dashboard,
        Screen.Calendar,
        Screen.Add,
        Screen.Categories,
        Screen.Analytics,
        Screen.Settings,
    )

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                selected = currentDestinationRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                icon = {
                    if (screen == Screen.Add) {
                        FloatingActionButton(
                            onClick = { onNavigate(Screen.Add.route) },
                            modifier = Modifier.size(56.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                        )
                    }
                },
                label = null,
                alwaysShowLabel = false,
            )
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
                .fillMaxSize()
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderTitle(route: String, onCancelAdd: () -> Unit) {
    val config = when (route) {
        Screen.Calendar.route -> HeaderConfig("Календарь", "Операции по датам")
        Screen.Add.route -> HeaderConfig("Добавление операции")
        Screen.Categories.route -> HeaderConfig("Категории", "Лимиты и управление категориями")
        Screen.Analytics.route -> HeaderConfig("Аналитика", "Структура расходов и лимиты")
        Screen.Settings.route -> HeaderConfig("Настройки", "Тема, синхронизация и доступ")
        Screen.Subscription.route -> HeaderConfig("Подписка и безопасность", "Тарифы и защита аккаунта")
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
private fun DashboardScreen(vm: CoinMetricViewModel, onOnboardingDismissed: () -> Unit) {
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
                            onClick = {
                                vm.dismissOnboarding()
                                onOnboardingDismissed()
                            },
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
                    ExpenseTrendChart(state.expenseTrend)
                    Text(
                        state.expenseTrendText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ExpenseTrendChart(points: List<Int>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setNoDataText("Недостаточно данных")
                setTouchEnabled(false)
                setPinchZoom(false)
                axisRight.isEnabled = false
                xAxis.isEnabled = false
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = surfaceVariant.toArgb()
                    textColor = onSurface.toArgb()
                }
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = points.mapIndexed { index, value ->
                Entry(index.toFloat(), kotlin.math.abs(value).toFloat())
            }
            val dataSet = LineDataSet(entries, "Расходы").apply {
                color = primaryColor.toArgb()
                setCircleColor(primaryColor.toArgb())
                lineWidth = 2.5f
                circleRadius = 3.5f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = primaryColor.copy(alpha = 0.2f).toArgb()
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScreen(vm: CoinMetricViewModel, goToDashboard: () -> Unit) {
    val state by vm.addState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var calculatorExpanded by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf(state.amount) }
    val canEditTransactions = settings.currentUserRole == "owner" || settings.currentUserRole == "editor"

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
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
                    enabled = canEditTransactions,
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
                var categoriesExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoriesExpanded,
                    onExpandedChange = { if (canEditTransactions) categoriesExpanded = !categoriesExpanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = state.category,
                        onValueChange = {},
                        readOnly = true,
                        enabled = canEditTransactions,
                        label = { Text("Категория") },
                        isError = state.categoryError != null,
                        singleLine = true,
                        supportingText = {
                            state.categoryError?.let { Text(it) }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded)
                        },
                    )
                    DropdownMenu(
                        expanded = categoriesExpanded,
                        onDismissRequest = { categoriesExpanded = false },
                    ) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    vm.updateCategory(category)
                                    categoriesExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.note,
                    onValueChange = vm::updateNote,
                    enabled = canEditTransactions,
                    label = { Text("Заметка") },
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Доход")
                    Switch(checked = state.isIncome, onCheckedChange = vm::updateIncomeFlag, enabled = canEditTransactions)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Постоянный платёж")
                    Switch(checked = state.isRecurring, onCheckedChange = vm::updateRecurringFlag, enabled = canEditTransactions)
                }
            }
            if (!canEditTransactions) {
                item {
                    Text(
                        "Роль просмотра не позволяет добавлять операции. Обратитесь к владельцу за правами редактора.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            state.successMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.primary) }
            }
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            onClick = { vm.saveTransaction(goToDashboard) },
            enabled = canEditTransactions,
        ) {
            Text(if (state.id == null) "Сохранить" else "Сохранить изменения")
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreen(vm: CoinMetricViewModel) {
    val state by vm.categoriesState.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(
                "Добавляйте категории и задавайте лимит расходов на месяц.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Новая категория", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = state.newCategoryName,
                            onValueChange = vm::updateCategoriesNewCategoryName,
                            label = { Text("Название категории") },
                            singleLine = true,
                        )
                        Button(onClick = vm::addNewCategory) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Лимит на месяц", fontWeight = FontWeight.SemiBold)
                    var categoriesExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoriesExpanded,
                        onExpandedChange = { categoriesExpanded = !categoriesExpanded },
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            value = state.selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Категория") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded)
                            },
                        )
                        DropdownMenu(
                            expanded = categoriesExpanded,
                            onDismissRequest = { categoriesExpanded = false },
                        ) {
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        vm.updateSelectedLimitCategory(category)
                                        categoriesExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.monthlyLimitInput,
                        onValueChange = vm::updateMonthlyLimitInput,
                        label = { Text("Лимит в месяц, ₽") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = vm::saveMonthlyLimit,
                    ) {
                        Text("Сохранить лимит")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Текущие лимиты", fontWeight = FontWeight.SemiBold)
                    if (state.monthlyLimits.isEmpty()) {
                        Text("Лимиты пока не заданы")
                    } else {
                        state.monthlyLimits.toSortedMap().forEach { (category, limit) ->
                            Text("$category — ${limit.toRubCurrency()}")
                        }
                    }
                }
            }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        state.successMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun AnalyticsScreen(vm: CoinMetricViewModel, openAddScreen: () -> Unit) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val categoriesState by vm.categoriesState.collectAsStateWithLifecycle()
    val expensesByCategory = state.allTransactions
        .filterNot { it.income }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { kotlin.math.abs(it.amount) } }
    val totalExpenses = expensesByCategory.values.sum().coerceAtLeast(1)

    val categoryDistribution = expensesByCategory.entries
        .sortedByDescending { it.value }
        .map { (title, amount) ->
            title to amount.toFloat() / totalExpenses
        }

    val limitsByCategory = categoriesState.monthlyLimits.map { (title, limit) ->
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
                "Краткая сводка распределения трат, лимитов и всех операций",
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
                        ExpensePieChart(categoryDistribution)
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
                    if (limitsByCategory.isEmpty()) {
                        Text("Лимиты по категориям пока не заданы")
                    } else {
                        limitsByCategory.forEach { (title, progress) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("$title: ${(progress * 100).toInt()}%")
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = if (progress >= 0.85f) MaterialTheme.colorScheme.error else Color.Unspecified,
                                )
                                if (progress >= 1f) {
                                    Text(
                                        "Лимит превышен",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Все транзакции", fontWeight = FontWeight.SemiBold)
                    if (state.allTransactions.isEmpty()) {
                        Text("Пока нет операций")
                    } else {
                        state.allTransactions.forEach { tx ->
                            val sign = if (tx.amount >= 0) "+" else "-"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .clickable {
                                        vm.startEditingTransaction(tx)
                                        openAddScreen()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(tx.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${tx.category} · ${tx.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "$sign${kotlin.math.abs(tx.amount).toRubCurrency()}",
                                    color = if (tx.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ExpensePieChart(categoryDistribution: List<Pair<String, Float>>) {
    val colors = listOf(
        CoinMetricThemeColors.Expense,
        CoinMetricThemeColors.Income,
        CoinMetricThemeColors.Violet,
        CoinMetricThemeColors.Orange,
        CoinMetricThemeColors.Yellow,
    )

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setUsePercentValues(true)
                setEntryLabelColor(Color.White.toArgb())
                setEntryLabelTextSize(12f)
                legend.apply {
                    isEnabled = true
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    textSize = 12f
                }
            }
        },
        update = { chart ->
            val entries = categoryDistribution.map { (title, percent) ->
                PieEntry(percent, title)
            }

            val colorInts = categoryDistribution.indices.map { index ->
                colors[index % colors.size].toArgb()
            }

            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colorInts
                valueTextSize = 12f
                valueTextColor = Color.White.toArgb()
                sliceSpace = 2f
            }

            chart.data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(chart))
            }
            chart.invalidate()
        },
    )
}

private object CoinMetricThemeColors {
    val Income = Color(0xFF10B981)
    val Expense = Color(0xFFEF4444)
    val Violet = Color(0xFFA855F7)
    val Yellow = Color(0xFFEAB308)
    val Orange = Color(0xFFF97316)
}

@Composable
private fun CalendarScreen(vm: CoinMetricViewModel, openAddScreen: () -> Unit) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val canEditTransactions = settings.currentUserRole != "viewer"
    val transactions = state.allTransactions
    val datesWithTransactions = remember(transactions) { transactions.map { LocalDate.parse(it.date) }.toSet() }
    var selectedDate by remember(transactions) { mutableStateOf(datesWithTransactions.maxOrNull() ?: LocalDate.now()) }

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
                    AnimatedContent(
                        targetState = selectedDate,
                        label = "calendar_selected_date",
                    ) { date ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Операции за ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", fontWeight = FontWeight.SemiBold)
                            val selectedItems = transactions.filter { it.date == date.toString() }
                            if (selectedItems.isEmpty()) {
                                Text("На выбранную дату операций нет")
                            } else {
                                selectedItems.forEach { tx ->
                                    val sign = if (tx.amount >= 0) "+" else "-"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    vm.startEditingTransaction(tx)
                                                    openAddScreen()
                                                },
                                        ) {
                                            Text(tx.title, fontWeight = FontWeight.Medium)
                                            Text(tx.category, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("$sign${kotlin.math.abs(tx.amount).toRubCurrency()}")
                                            IconButton(
                                                onClick = { vm.deleteTransaction(tx) },
                                                enabled = canEditTransactions,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Удалить транзакцию",
                                                    tint = if (canEditTransactions) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!canEditTransactions) {
            item {
                Text(
                    "Для роли просмотра удаление операций недоступно.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    datesWithTransactions: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
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

        Crossfade(targetState = currentMonth, label = "calendar_month_crossfade") { month ->
            val monthFirstDay = month.atDay(1)
            val monthLeadingEmpty = (monthFirstDay.dayOfWeek.value + 6) % 7
            val monthLength = month.lengthOfMonth()
            val totalCells = ((monthLeadingEmpty + monthLength + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekStart in 0 until totalCells step 7) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (offset in 0..6) {
                            val index = weekStart + offset
                            val dayNumber = index - monthLeadingEmpty + 1
                            if (dayNumber !in 1..monthLength) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val date = month.atDay(dayNumber)
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
    }
}

@Composable
private fun SettingsScreen(vm: CoinMetricViewModel, onOpenSubscription: () -> Unit, onOnboardingVisibilityChanged: (Boolean) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val canManageMembers = settings.currentUserRole == "owner"

    // Function to get current user email
    fun getCurrentUserEmail(): String {
        return settings.currentUserEmail.takeIf { it.isNotEmpty() } ?: "Аккаунт не указан"
    }



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
                    SettingRow(\"Тёмная тема\", settings.darkThemeEnabled) { vm.setDarkTheme(it) }
                    SettingRow(\"Синхронизация Google\", settings.googleSyncEnabled) { 
                        vm.setGoogleSync(it) 
                    }
                    if (settings.googleSyncEnabled) {
                        // Show current user email when Google sync is enabled
                        Text(
                            text = "Аккаунт: ${getCurrentUserEmail()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        // Add Google Sign-In button when sync is enabled but no account is specified
                        if (settings.currentUserEmail.isEmpty()) {
                            Button(
                                onClick = { 
                                    val signInIntent = GoogleAuthConfig.getGoogleSignInClient(context).signInIntent
                                    (context as android.app.Activity).startActivityForResult(signInIntent, 9001)
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4285F4), // Google blue
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Войти через Google")
                            }
                        }
                    }
                    SettingRow(\"Офлайн-режим\", settings.isOfflineMode) { vm.setOfflineMode(it) }
                    SettingRow("Показывать подсказки", settings.showOnboarding) {
                        vm.setOnboardingVisible(it)
                        onOnboardingVisibilityChanged(it)
                    }

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
                    Text("Текущая роль", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Для проверки сценариев доступа можно переключить активную роль.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.currentUserRole == "owner",
                            onClick = { vm.setCurrentUserRole("owner") },
                            label = { Text("Владелец") },
                        )
                        FilterChip(
                            selected = settings.currentUserRole == "editor",
                            onClick = { vm.setCurrentUserRole("editor") },
                            label = { Text("Редактор") },
                        )
                        FilterChip(
                            selected = settings.currentUserRole == "viewer",
                            onClick = { vm.setCurrentUserRole("viewer") },
                            label = { Text("Просмотр") },
                        )
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
                        enabled = canManageMembers,
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
                            enabled = canManageMembers,
                            onClick = { vm.updateInviteRole("viewer") },
                            label = { Text("Просмотр") },
                        )
                        FilterChip(
                            selected = settings.inviteRole == "editor",
                            enabled = canManageMembers,
                            onClick = { vm.updateInviteRole("editor") },
                            label = { Text("Редактор") },
                        )
                    }
                    if (!canManageMembers) {
                        Text(
                            "Управление приглашениями доступно только владельцу бюджета.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = vm::sendFamilyInvite,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canManageMembers,
                    ) {
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
                                        Button(
                                            onClick = { vm.updateInviteStatus(invite.email, "Принято") },
                                            enabled = canManageMembers,
                                        ) {
                                            Text("Принять")
                                        }
                                        Button(
                                            onClick = { vm.updateInviteStatus(invite.email, "Отклонено") },
                                            enabled = canManageMembers,
                                        ) {
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Подписка и безопасность", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Текущий план: ${if (settings.subscriptionPlan == "pro") "CoinMetric Pro" else "CoinMetric Free"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onOpenSubscription, modifier = Modifier.fillMaxWidth()) {
                        Text("Открыть детали тарифов и защиты")
                    }
                }
            }
        }
        if (!settings.securitySetupCompleted) {
            item {
                SecuritySetupCard(vm = vm, settings = settings)
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Общие настройки", fontWeight = FontWeight.SemiBold)
                    Text("Валюта по умолчанию: RUB")
                    SettingRow("Уведомления о лимитах", settings.recurringRemindersEnabled) { vm.setRecurringReminders(it) }
                    
                    // Google Sign In section
                    Text("Синхронизация с Google", fontWeight = FontWeight.SemiBold)
                    if (settings.googleSyncEnabled) {
                        Text("Вход выполнен: ${getCurrentUserEmail()}")
                        Button(
                            onClick = {
                                vm.setGoogleSync(false)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отключить синхронизацию")
                        }
                    } else {
                        Text(
                            text = "Войдите в аккаунт для использования синхронизации",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Журнал действий", fontWeight = FontWeight.SemiBold)
                    if (settings.activityLog.isEmpty()) {
                        Text(
                            "Пока нет действий участников. После операций здесь появится история изменений.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        settings.activityLog.take(8).forEach { log ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${log.actor} · ${log.action}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    log.target,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    log.createdAtLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SubscriptionScreen(vm: CoinMetricViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Выберите тариф", fontWeight = FontWeight.SemiBold)
                    Text("Free: базовый учёт бюджета и семейный доступ. Pro: расширенная аналитика, приоритетная синхронизация и больше автоматизаций.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.subscriptionPlan == "free",
                            onClick = { vm.setSubscriptionPlan("free") },
                            label = { Text("Free") },
                        )
                        FilterChip(
                            selected = settings.subscriptionPlan == "pro",
                            onClick = { vm.setSubscriptionPlan("pro") },
                            label = { Text("Pro") },
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Безопасность", fontWeight = FontWeight.SemiBold)
                    SettingRow("PIN-защита", settings.pinProtectionEnabled) { vm.setPinProtectionEnabled(it) }
                    SettingRow("Вход по биометрии", settings.biometricProtectionEnabled) { vm.setBiometricProtectionEnabled(it) }
                    Text(
                        if (settings.securitySetupCompleted) "Первичная настройка безопасности завершена" else "Запустите мастер в настройках для первичной конфигурации.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecuritySetupCard(vm: CoinMetricViewModel, settings: SettingsState) {
    var wizardOpened by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Мастер первичной настройки безопасности", fontWeight = FontWeight.SemiBold)
            Text("Выполните два шага, чтобы защитить вход в приложение.")
            Button(onClick = { wizardOpened = !wizardOpened }, modifier = Modifier.fillMaxWidth()) {
                Text(if (wizardOpened) "Скрыть мастер" else "Запустить мастер")
            }
            if (wizardOpened) {
                SettingRow("Шаг 1: включить PIN", settings.pinProtectionEnabled) { vm.setPinProtectionEnabled(it) }
                SettingRow("Шаг 2: включить биометрию", settings.biometricProtectionEnabled) { vm.setBiometricProtectionEnabled(it) }
                Button(
                    onClick = {
                        vm.completeSecuritySetup()
                        wizardOpened = false
                    },
                    enabled = settings.pinProtectionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Завершить мастер")
                }
            }
        }
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

@Preview(showBackground = true)
@Composable
private fun CoinMetricRootPreview() {
    CoinMetricRoot(vm = CoinMetricViewModel())
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    CoinMetricTheme {
        DashboardScreen(vm = CoinMetricViewModel(), onOnboardingDismissed = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AddScreenPreview() {
    CoinMetricTheme {
        AddScreen(vm = CoinMetricViewModel(), goToDashboard = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenPreview() {
    CoinMetricTheme {
        CategoriesScreen(vm = CoinMetricViewModel())
    }
}

@Composable
private fun AnalyticsScreenPreview() {
    CoinMetricTheme {
        AnalyticsScreen(vm = CoinMetricViewModel(), openAddScreen = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    CoinMetricTheme {
        CalendarScreen(vm = CoinMetricViewModel(), openAddScreen = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    CoinMetricTheme {
        SettingsScreen(vm = CoinMetricViewModel(), onOpenSubscription = {}, onOnboardingVisibilityChanged = {})
    }
}
