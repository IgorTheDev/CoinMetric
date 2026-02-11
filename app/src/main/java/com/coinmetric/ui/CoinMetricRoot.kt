package com.coinmetric.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            floatingActionButton = {
                if (currentRoute != Screen.Add.route) {
                    FloatingActionButton(onClick = { navController.navigate(Screen.Add.route) }) {
                        Text("Добавить")
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
                        CalendarScreen(vm)
                    }
                    composable(Screen.Add.route) {
                        AddScreen(vm) { navController.navigate(Screen.Dashboard.route) }
                    }
                    composable(Screen.Analytics.route) {
                        AnalyticsScreen()
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
                IconButton(onClick = onCancelAdd) {
                    Text("Отмена")
                }
            }
        },
    )
}

@Composable
private fun DashboardScreen(vm: CoinMetricViewModel) {
    val state by vm.dashboard.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Общий баланс", style = MaterialTheme.typography.titleMedium)
                    Text("${state.balance} ₽", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Доход: ${state.income} ₽")
                        Text("Расход: ${state.expense} ₽")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Лимиты", "${state.limitsUsedPercent}%", Modifier.weight(1f))
                MetricCard("Ср. расход/день", "${state.avgDailyExpense} ₽", Modifier.weight(1f))
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
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.category,
                onValueChange = vm::updateCategory,
                label = { Text("Категория") },
                singleLine = true,
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
                Text("Сохранить")
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun AnalyticsScreen() {
    val categoryDistribution = listOf(
        "Еда" to 0.39f,
        "Транспорт" to 0.22f,
        "Развлечения" to 0.15f,
    )
    val limitsByCategory = listOf(
        "Еда" to 0.78f,
        "Транспорт" to 0.54f,
        "Развлечения" to 0.42f,
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    categoryDistribution.forEach { (title, percent) ->
                        Text("$title — ${(percent * 100).toInt()}%")
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
private fun CalendarScreen(vm: CoinMetricViewModel) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val groupedTransactions = state.latestTransactions.groupBy {
        it.substringAfterLast("· ").trim()
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "История транзакций с группировкой по дате",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Календарь", fontWeight = FontWeight.SemiBold)
                    if (groupedTransactions.isEmpty()) {
                        Text("Нет операций на выбранную дату")
                    } else {
                        groupedTransactions.forEach { (date, items) ->
                            Text(date, fontWeight = FontWeight.Medium)
                            items.forEach { op -> Text(op) }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
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
                    SettingRow("Тёмная тема", settings.darkThemeEnabled) { vm.setDarkTheme(it) }
                    SettingRow("Синхронизация Google", settings.googleSyncEnabled) { vm.setGoogleSync(it) }
                    Text("Семейный доступ")
                    Text("Добавьте участников семьи в следующих версиях")
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Выйти")
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
