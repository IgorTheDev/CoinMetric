package com.coinmetric.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min

private enum class Screen {
    Dashboard,
    Calendar,
    Add,
    Analytics,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinMetricRoot(
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
) {
    val vm: CoinMetricViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screen) {
                            Screen.Dashboard -> "CoinMetric"
                            Screen.Calendar -> "Календарь"
                            Screen.Add -> "Добавить операцию"
                            Screen.Analytics -> "Аналитика"
                            Screen.Settings -> "Настройки"
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { screen = Screen.Add },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text("+")
            }
        },
        bottomBar = {
            NavigationBar {
                NavItem("Главная", screen == Screen.Dashboard) { screen = Screen.Dashboard }
                NavItem("Календарь", screen == Screen.Calendar) { screen = Screen.Calendar }
                NavItem("Аналитика", screen == Screen.Analytics) { screen = Screen.Analytics }
                NavItem("Настройки", screen == Screen.Settings) { screen = Screen.Settings }
            }
        },
    ) { padding ->
        MobileLayout(modifier = Modifier.padding(padding)) { containerModifier ->
            AnimatedContent(targetState = screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { target ->
                when (target) {
                    Screen.Dashboard -> DashboardScreen(state = state, vm = vm, modifier = containerModifier)
                    Screen.Calendar -> CalendarScreen(state = state, vm = vm, modifier = containerModifier)
                    Screen.Add -> AddTransactionScreen(
                        state = state,
                        vm = vm,
                        modifier = containerModifier,
                        onDone = { screen = Screen.Dashboard },
                    )
                    Screen.Analytics -> AnalyticsScreen(state = state, modifier = containerModifier)
                    Screen.Settings -> SettingsScreen(
                        state = state,
                        vm = vm,
                        darkTheme = darkTheme,
                        onDarkThemeChanged = onDarkThemeChanged,
                        modifier = containerModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Box(modifier = Modifier.width(1.dp)) },
        label = { Text(label) },
    )
}

@Composable
private fun MobileLayout(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        content(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .widthIn(max = 460.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun DashboardScreen(state: UiState, vm: CoinMetricViewModel, modifier: Modifier = Modifier) {
    val monthlyExpense = state.monthlyReport.expense
    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val avgPerDay = monthlyExpense / todayDay

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard(title = "Общий баланс") {
                val balance = state.totalIncome - state.totalExpense
                Text(
                    text = "${"%.2f".format(balance)} ₽",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                SummaryRow("Доход", "${"%.2f".format(state.totalIncome)} ₽")
                SummaryRow("Расход", "${"%.2f".format(state.totalExpense)} ₽")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Активных лимитов", state.limitProgress.size.toString())
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Средний расход / день", "${"%.2f".format(avgPerDay)} ₽")
                }
            }
        }

        item {
            SectionCard(title = "Тренд расходов") {
                Text("Неделя: ${"%.2f".format(state.weeklyReport.expense)} ₽")
                Text("Месяц: ${"%.2f".format(state.monthlyReport.expense)} ₽")
                Text("Топ категория: ${state.monthlyReport.topExpenseCategory}")
            }
        }

        item { Text("Последние операции", style = MaterialTheme.typography.titleMedium) }
        if (state.transactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Пока нет операций",
                    message = "Добавьте первую транзакцию на экране «Добавить».",
                )
            }
        } else {
            items(state.transactions.take(8)) { tx ->
                SectionCard(compact = true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(tx.note.ifBlank { "Без заметки" })
                            Text(vm.formatDate(tx.dateEpochMillis), style = MaterialTheme.typography.bodySmall)
                        }
                        Text((if (tx.isIncome) "+" else "-") + "${"%.2f".format(tx.amount)} ₽")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .height(90.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AddTransactionScreen(
    state: UiState,
    vm: CoinMetricViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text("Отмена") }
            Text("Новая операция", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(68.dp))
        }

        SectionCard {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Сумма или выражение") },
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Доход")
                Switch(checked = isIncome, onCheckedChange = { isIncome = it })
            }
            val categoryName = state.categories.firstOrNull()?.name ?: "Категория не создана"
            Text("Категория: $categoryName")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val hasAmount = parseAmount(expr) != null
                    val categoryId = state.categories.firstOrNull()?.id
                    if (!hasAmount || categoryId == null) {
                        message = "Заполните сумму и создайте хотя бы одну категорию"
                        return@Button
                    }
                    vm.addTransaction(expr, note, categoryId, state.members.firstOrNull()?.id, isIncome)
                    message = "Операция сохранена"
                    onDone()
                },
            ) {
                Text("Сохранить")
            }
        }

        message?.let {
            Text(it, color = if (it.contains("сохранена")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AnalyticsScreen(state: UiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard(title = "Распределение расходов") {
                val total = state.categorySpend.sumOf { it.spent }
                Text("Всего расходов: ${"%.2f".format(total)} ₽", style = MaterialTheme.typography.titleMedium)
                if (state.categorySpend.isEmpty()) {
                    Text("Пока нет расходов для аналитики")
                } else {
                    state.categorySpend.forEach { row ->
                        val progress = if (total > 0) (row.spent / total).toFloat() else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SummaryRow(row.category, "${"%.2f".format(row.spent)} ₽")
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        item { Text("Лимиты по категориям", style = MaterialTheme.typography.titleMedium) }
        if (state.limitProgress.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Лимитов пока нет",
                    message = "Добавьте лимиты, чтобы отслеживать прогресс расходов.",
                )
            }
        } else {
            items(state.limitProgress) { progress ->
                SectionCard(compact = true) {
                    SummaryRow(progress.categoryName, "${"%.2f".format(progress.spent)} / ${"%.2f".format(progress.limit)} ₽")
                    LinearProgressIndicator(
                        progress = { min(1f, progress.progress.coerceAtLeast(0f)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(progress.status, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(state: UiState, vm: CoinMetricViewModel, modifier: Modifier = Modifier) {
    val groupedTransactions = state.transactions.groupBy { vm.formatDate(it.dateEpochMillis) }.toList()
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionCard(title = "Транзакции по дням") {
                Text("Отмечены даты, в которые были операции")
            }
        }

        if (groupedTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Нет операций",
                    message = "Добавьте транзакцию и она появится в календаре.",
                )
            }
        } else {
            items(groupedTransactions) { (date, items) ->
                SectionCard(title = date, compact = true) {
                    items.forEach {
                        SummaryRow(it.note.ifBlank { "Операция" }, "${"%.2f".format(it.amount)} ₽")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: UiState,
    vm: CoinMetricViewModel,
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var syncEnabled by rememberSaveable { mutableStateOf(true) }
    var inviteEmail by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard(title = "Тема") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (darkTheme) "Тёмная" else "Светлая")
                    Switch(checked = darkTheme, onCheckedChange = onDarkThemeChanged)
                }
            }
        }

        item {
            SectionCard(title = "Синхронизация Google") {
                Text("Включите синхронизацию для бэкапа данных")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (syncEnabled) "Активна" else "Отключена")
                    Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
                }
            }
        }

        item {
            SectionCard(title = "Семейный доступ") {
                if (state.members.isEmpty()) {
                    Text("Участников пока нет")
                } else {
                    state.members.forEach {
                        SummaryRow(it.name, it.role)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text("Email для приглашения") },
                )
                OutlinedButton(onClick = {
                    if (inviteEmail.isNotBlank()) {
                        vm.sendInvite(inviteEmail, "Владелец бюджета")
                        inviteEmail = ""
                    }
                }) {
                    Text("Пригласить")
                }
            }
        }

        item {
            SectionCard(title = "Общие") {
                Text("Подписка")
                Text("Безопасность")
                Text("Выход", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionCard(title: String? = null, compact: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value)
    }
}

private fun parseAmount(expression: String): Double? =
    runCatching { ExpressionCalculator.eval(expression) }.getOrNull()?.takeIf { it > 0 }
