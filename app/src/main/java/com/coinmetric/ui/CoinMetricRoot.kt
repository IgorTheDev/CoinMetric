package com.coinmetric.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
                Text(
                    "CoinMetric • семейный бюджет",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, t ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(t) })
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
    LazyColumn(modifier = modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Доходы: ${"%.2f".format(state.totalIncome)} ₽")
                    Text("Расходы: ${"%.2f".format(state.totalExpense)} ₽")
                    Text("Баланс: ${"%.2f".format(state.totalIncome - state.totalExpense)} ₽")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Лимит категории на текущий месяц", style = MaterialTheme.typography.titleMedium)
                    TextField(
                        value = limitValue,
                        onValueChange = { limitValue = it },
                        label = { Text("Лимит для категории ${state.categories.firstOrNull()?.name ?: "—"}") },
                    )
                    Button(onClick = {
                        val categoryId = state.categories.firstOrNull()?.id ?: return@Button
                        val limit = limitValue.toDoubleOrNull() ?: return@Button
                        vm.addCategoryLimit(categoryId, limit)
                        limitValue = ""
                    }) { Text("Сохранить лимит") }
                }
            }
        }

        if (state.limitProgress.isNotEmpty()) {
            item {
                Text("Траты по лимитам в текущем месяце", style = MaterialTheme.typography.titleMedium)
            }
            items(state.limitProgress) { progress ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(progress.categoryName)
                            Text(progress.status)
                        }
                        LinearProgressIndicator(
                            progress = { min(1f, progress.progress.coerceAtLeast(0f)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${"%.2f".format(progress.spent)} ₽ из ${"%.2f".format(progress.limit)} ₽")
                    }
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
            Text("Регулярные отчёты", style = MaterialTheme.typography.titleMedium)
        }
        item {
            PeriodReportCard(report = state.weeklyReport)
        }
        item {
            PeriodReportCard(report = state.monthlyReport)
        }

        item { Text("Аналитика по категориям", style = MaterialTheme.typography.titleMedium) }
        if (state.categorySpend.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Нет данных для аналитики",
                    message = "Добавьте несколько операций, и здесь появится распределение расходов по категориям.",
                )
            }
        } else {
            items(state.categorySpend) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.category)
                    Text("${"%.2f".format(it.spent)} ₽")
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PeriodReportCard(report: BudgetPeriodReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(report.title, style = MaterialTheme.typography.titleSmall)
            Text("Доходы: ${"%.2f".format(report.income)} ₽")
            Text("Расходы: ${"%.2f".format(report.expense)} ₽")
            Text("Топ категория: ${report.topExpenseCategory}")
            val trend = report.expenseTrendPercent
            Text(
                if (trend == null) {
                    "Изменение к прошлому периоду: недостаточно данных"
                } else {
                    "Изменение к прошлому периоду: ${if (trend >= 0) "+" else ""}${"%.1f".format(trend)}%"
                },
            )
        }
    }
}

@Composable
private fun TransactionsScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var expr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Добавить транзакцию (калькулятор в сумме)")
        TextField(value = expr, onValueChange = { expr = it }, label = { Text("Сумма/выражение, например 1200+350/2") })
        TextField(value = note, onValueChange = { note = it }, label = { Text("Комментарий") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                vm.addTransaction(expr, note, state.categories.firstOrNull()?.id, state.members.firstOrNull()?.id, false)
                expr = ""
                note = ""
            }) { Text("Расход") }
            Button(onClick = {
                vm.addTransaction(expr, note, state.categories.firstOrNull()?.id, state.members.firstOrNull()?.id, true)
                expr = ""
                note = ""
            }) { Text("Доход") }
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
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.note.ifBlank { "Без названия" })
                                Text(vm.formatDate(tx.dateEpochMillis))
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
    LazyColumn(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Календарь трат") }
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
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(date, style = MaterialTheme.typography.titleSmall)
                        items.forEach {
                            Text("• ${it.note.ifBlank { "Операция" }}: ${"%.2f".format(it.amount)} ₽")
                        }
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
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Семейный доступ")
        TextField(value = name, onValueChange = { name = it }, label = { Text("Имя") })
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email (Google)") })
        Button(onClick = {
            if (name.isNotBlank() && email.isNotBlank()) {
                vm.addFamilyMember(name, email)
                name = ""
                email = ""
            }
        }) { Text("Добавить участника") }

        TextField(value = inviteEmail, onValueChange = { inviteEmail = it }, label = { Text("Email для приглашения") })
        Button(onClick = {
            if (inviteEmail.isNotBlank()) {
                vm.sendInvite(inviteEmail, name.ifBlank { "Владелец бюджета" })
                inviteEmail = ""
            }
        }) { Text("Отправить приглашение на совместное редактирование") }

        Text("Участники")
        if (state.members.isEmpty() && state.invites.isEmpty()) {
            EmptyStateCard(
                title = "Семья ещё не добавлена",
                message = "Добавьте участников вручную или отправьте приглашение по email для совместного бюджета.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.members) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(it.name)
                            Text(it.email)
                            Text("Роль: ${it.role}")
                        }
                    }
                }
                items(state.invites) { invite ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Приглашение: ${invite.email}")
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
}

@Composable
private fun RecurringScreen(vm: CoinMetricViewModel, state: UiState, modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Постоянные платежи")
        TextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
        TextField(value = amount, onValueChange = { amount = it }, label = { Text("Сумма") })
        TextField(value = day, onValueChange = { day = it }, label = { Text("День месяца") })
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

        Text("Список")
        if (state.recurringPayments.isEmpty()) {
            EmptyStateCard(
                title = "Нет постоянных платежей",
                message = "Добавьте регулярные расходы, чтобы не забывать о ежемесячных списаниях.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.recurringPayments) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.title} (${it.dayOfMonth} число)")
                        Text("${"%.2f".format(it.amount)} ₽")
                    }
                }
            }
        }
    }
}
