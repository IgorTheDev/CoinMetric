package com.coinmetric.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinMetricRoot() {
    val vm: CoinMetricViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Дашборд", "Транзакции", "Календарь", "Семья", "Платежи")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "CoinMetric",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                )
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                    }
                }
            }
        },
    ) { padding ->
        when (tab) {
            0 -> DashboardScreen(vm, state, Modifier.padding(padding))
            1 -> TransactionsScreen(vm, state, Modifier.padding(padding))
            2 -> CalendarScreen(vm, state, Modifier.padding(padding))
            3 -> FamilyScreen(vm, state, Modifier.padding(padding))
            else -> RecurringScreen(vm, state, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DashboardScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var limitValue by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(title = "Обзор бюджета") {
                SummaryRow("Доходы", "${"%.2f".format(state.totalIncome)} ₽")
                SummaryRow("Расходы", "${"%.2f".format(state.totalExpense)} ₽")
                SummaryRow("Баланс", "${"%.2f".format(state.totalIncome - state.totalExpense)} ₽")
            }
        }

        item {
            SectionCard(title = "Лимит категории на текущий месяц") {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = limitValue,
                    onValueChange = { limitValue = it },
                    label = { Text("Лимит для категории ${state.categories.firstOrNull()?.name ?: "—"}") },
                )
                Button(
                    onClick = {
                        val categoryId = state.categories.firstOrNull()?.id ?: return@Button
                        val limit = limitValue.toDoubleOrNull() ?: return@Button
                        vm.addCategoryLimit(categoryId, limit)
                        limitValue = ""
                    },
                ) {
                    Text("Сохранить лимит")
                }
            }
        }

        item {
            Text("Лимиты и прогресс", style = MaterialTheme.typography.titleMedium)
        }

        if (state.limitProgress.isNotEmpty()) {
            items(state.limitProgress) { progress ->
                SectionCard(title = progress.categoryName, compact = true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(progress.status)
                        Text("${"%.2f".format(progress.spent)} / ${"%.2f".format(progress.limit)} ₽")
                    }
                    LinearProgressIndicator(
                        progress = { min(1f, progress.progress.coerceAtLeast(0f)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            item {
                EmptyStateCard(
                    title = "Лимиты пока не настроены",
                    message = "Добавьте лимит для категории выше, чтобы видеть прогресс трат и предупреждения о превышении.",
                )
            }
        }

        item {
            Text("Отчёты", style = MaterialTheme.typography.titleMedium)
        }
        item {
            PeriodReportCard(report = state.weeklyReport)
        }
        item {
            PeriodReportCard(report = state.monthlyReport)
        }

        item {
            Text("Аналитика по категориям", style = MaterialTheme.typography.titleMedium)
        }
        if (state.categorySpend.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Нет данных для аналитики",
                    message = "Добавьте несколько операций, и здесь появится распределение расходов по категориям.",
                )
            }
        } else {
            items(state.categorySpend) {
                SectionCard(compact = true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.category)
                        Text("${"%.2f".format(it.spent)} ₽")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
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
private fun PeriodReportCard(report: BudgetPeriodReport) {
    SectionCard(title = report.title) {
        SummaryRow("Доходы", "${"%.2f".format(report.income)} ₽")
        SummaryRow("Расходы", "${"%.2f".format(report.expense)} ₽")
        SummaryRow("Топ категория", report.topExpenseCategory)
        val trend = report.expenseTrendPercent
        SummaryRow(
            "Изменение к прошлому периоду",
            if (trend == null) {
                "недостаточно данных"
            } else {
                "${if (trend >= 0) "+" else ""}${"%.1f".format(trend)}%"
            },
        )
    }
}

@Composable
private fun TransactionsScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var expr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(
        modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionCard(title = "Добавить транзакцию") {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Сумма/выражение, например 1200+350/2") },
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = { note = it },
                label = { Text("Комментарий") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        vm.addTransaction(expr, note, state.categories.firstOrNull()?.id, state.members.firstOrNull()?.id, false)
                        expr = ""
                        note = ""
                    },
                ) { Text("Расход") }
                Button(
                    onClick = {
                        vm.addTransaction(expr, note, state.categories.firstOrNull()?.id, state.members.firstOrNull()?.id, true)
                        expr = ""
                        note = ""
                    },
                ) { Text("Доход") }
            }
        }

        Text("Последние операции", style = MaterialTheme.typography.titleMedium)
        if (state.transactions.isEmpty()) {
            EmptyStateCard(
                title = "Пока нет операций",
                message = "Добавьте первую транзакцию, чтобы начать отслеживать бюджет семьи.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.transactions.take(20)) { tx ->
                    SectionCard(compact = true) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.note.ifBlank { "Без названия" })
                                Text(vm.formatDate(tx.dateEpochMillis), style = MaterialTheme.typography.bodySmall)
                            }
                            Text((if (tx.isIncome) "+" else "-") + "${"%.2f".format(tx.amount)} ₽")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Календарь трат", style = MaterialTheme.typography.titleMedium) }
        val groupedTransactions = state.transactions.groupBy { vm.formatDate(it.dateEpochMillis) }.toList()
        if (groupedTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Календарь пока пуст",
                    message = "После добавления транзакций они автоматически распределятся по датам.",
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
private fun FamilyScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    Column(
        modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(title = "Семейный доступ") {
            TextField(modifier = Modifier.fillMaxWidth(), value = name, onValueChange = { name = it }, label = { Text("Имя") })
            TextField(modifier = Modifier.fillMaxWidth(), value = email, onValueChange = { email = it }, label = { Text("Email (Google)") })
            Button(onClick = {
                if (name.isNotBlank() && email.isNotBlank()) {
                    vm.addFamilyMember(name, email)
                    name = ""
                    email = ""
                }
            }) { Text("Добавить участника") }

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = inviteEmail,
                onValueChange = { inviteEmail = it },
                label = { Text("Email для приглашения") },
            )
            Button(onClick = {
                if (inviteEmail.isNotBlank()) {
                    vm.sendInvite(inviteEmail, name.ifBlank { "Владелец бюджета" })
                    inviteEmail = ""
                }
            }) { Text("Отправить приглашение") }
        }

        Text("Участники", style = MaterialTheme.typography.titleMedium)
        if (state.members.isEmpty() && state.invites.isEmpty()) {
            EmptyStateCard(
                title = "Семья ещё не добавлена",
                message = "Добавьте участников вручную или отправьте приглашение по email для совместного бюджета.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.members) {
                    SectionCard(compact = true) {
                        Text(it.name)
                        Text(it.email)
                        Text("Роль: ${it.role}")
                    }
                }
                items(state.invites) { invite ->
                    SectionCard(title = "Приглашение: ${invite.email}", compact = true) {
                        Text("Статус: ${invite.status}")
                        if (invite.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.acceptInvite(invite) }) { Text("Принять") }
                                Button(onClick = { vm.declineInvite(invite) }) { Text("Отклонить") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    Column(
        modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(title = "Постоянные платежи") {
            TextField(modifier = Modifier.fillMaxWidth(), value = title, onValueChange = { title = it }, label = { Text("Название") })
            TextField(modifier = Modifier.fillMaxWidth(), value = amount, onValueChange = { amount = it }, label = { Text("Сумма") })
            TextField(modifier = Modifier.fillMaxWidth(), value = day, onValueChange = { day = it }, label = { Text("День месяца") })
            Button(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                val d = day.toIntOrNull() ?: 1
                if (title.isNotBlank() && a > 0) {
                    vm.addRecurring(title, a, d)
                    title = ""
                    amount = ""
                    day = "1"
                }
            }) { Text("Добавить") }
        }

        Text("Список", style = MaterialTheme.typography.titleMedium)
        if (state.recurringPayments.isEmpty()) {
            EmptyStateCard(
                title = "Нет постоянных платежей",
                message = "Добавьте регулярные расходы, чтобы не забывать о ежемесячных списаниях.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.recurringPayments) {
                    SectionCard(compact = true) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${it.title} (${it.dayOfMonth} число)")
                            Text("${"%.2f".format(it.amount)} ₽")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String? = null, compact: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 1.dp else 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 12.dp),
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
        Text(label, modifier = Modifier.width(170.dp))
        Text(value)
    }
}
