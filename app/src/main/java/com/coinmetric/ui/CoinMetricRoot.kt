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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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

private sealed class Screen(val route: String, val label: String) {
    data object Dashboard : Screen("/", "Главная")
    data object Calendar : Screen("/calendar", "Календарь")
    data object Add : Screen("/add", "Добавить")
    data object Analytics : Screen("/analytics", "Аналитика")
    data object Settings : Screen("/settings", "Настройки")
}

@Composable
fun CoinMetricRoot(vm: CoinMetricViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Dashboard,
        Screen.Calendar,
        Screen.Add,
        Screen.Analytics,
        Screen.Settings,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            HeaderTitle(destination?.route ?: Screen.Dashboard.route)
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
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
            FloatingActionButton(onClick = { navController.navigate(Screen.Add.route) }) {
                Text("+")
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
private fun HeaderTitle(route: String) {
    val title = when (route) {
        Screen.Calendar.route -> "Календарь"
        Screen.Add.route -> "Добавление операции"
        Screen.Analytics.route -> "Аналитика"
        Screen.Settings.route -> "Настройки"
        else -> "CoinMetric"
    }
    CenterAlignedTopAppBar(title = { Text(title) })
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Распределение расходов", fontWeight = FontWeight.SemiBold)
                    Text("Еда — 39%")
                    Text("Транспорт — 22%")
                    Text("Развлечения — 15%")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Лимиты по категориям", fontWeight = FontWeight.SemiBold)
                    Text("Еда: 78%")
                    Text("Транспорт: 54%")
                    Text("Развлечения: 42%")
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun CalendarScreen(vm: CoinMetricViewModel) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Календарь", fontWeight = FontWeight.SemiBold)
                    Text("Сегодня")
                    val todayOps = state.latestTransactions.filter { it.contains("Сегодня") }
                    if (todayOps.isEmpty()) {
                        Text("Нет операций на выбранную дату")
                    } else {
                        todayOps.forEach { op -> Text(op) }
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
