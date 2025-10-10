package ph.edu.comteq.bankingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.edu.comteq.bankingapp.ui.theme.BankingAppTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

// Data Models
data class Account(
    val id: String,
    val name: String,
    val type: String,
    val number: String,
    var balance: Double
)

data class Transaction(
    val id: String,
    val sourceAccount: Account,
    val destinationAccount: DestinationAccount,
    val amount: Double,
    val timestamp: Date,
    val type: String,
    val status: String
)

data class DestinationAccount(
    val name: String = "",
    val number: String,
    val type: String
)

data class EmailNotification(
    val id: Long,
    val to: String,
    val subject: String,
    val timestamp: Date,
    val transactionId: String,
    val amount: Double,
    val sourceAccount: Account,
    val destinationAccount: DestinationAccount,
    val status: String,
    val transferType: String
)

// ViewModel State
class BankingViewModel {
    var accounts = mutableStateListOf(
        Account("A", "Account A", "Savings", "1111222233", 10000.0),
        Account("B", "Account B", "Checking", "4444555566", 5000.0),
        Account("C", "Account C", "Savings", "7777999988", 2000.0)
    )

    var transactions = mutableStateListOf<Transaction>()
    var emails = mutableStateListOf<EmailNotification>()

    val validExternalAccounts = listOf(
        "1234567890", "9876543210", "5678901234", "7777888899"
    )

    val userEmail = "testuser@example.com"
    val minAmount = 100.0
    val maxAmount = 50000.0

    fun generateTransactionId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val timeStamp = System.currentTimeMillis().toString().takeLast(6)
        return "TXN${dateFormat.format(Date())}$timeStamp"
    }

    fun validateTransfer(
        sourceId: String,
        destType: String,
        destId: String,
        externalAccount: String,
        amount: String
    ): String? {
        if (sourceId.isEmpty()) return "Please select a source account"

        if (destType == "own" && destId.isEmpty()) {
            return "Please select a destination account"
        }

        if (destType == "external" && externalAccount.isEmpty()) {
            return "Please enter external account number"
        }

        if (destType == "own" && sourceId == destId) {
            return "Source and destination accounts must be different"
        }

        if (destType == "external" && !validExternalAccounts.contains(externalAccount)) {
            return "Invalid destination account number"
        }

        val amountNum = amount.toDoubleOrNull()
        if (amountNum == null) return "Please enter a valid numeric amount"
        if (amountNum <= 0) return "Amount must be greater than P0"
        if (amountNum < minAmount) return "Minimum transfer amount is P$minAmount"
        if (amountNum > maxAmount) return "Maximum transfer amount is P${maxAmount.toInt()}"

        val srcAccount = accounts.find { it.id == sourceId }
        if (srcAccount != null && srcAccount.balance < amountNum) {
            return "Insufficient balance in source account"
        }

        return null
    }

    fun processTransfer(
        sourceId: String,
        destType: String,
        destId: String,
        externalAccount: String,
        amount: String
    ): Transaction {
        val amountNum = amount.toDouble()
        val srcAccount = accounts.find { it.id == sourceId }!!
        val transactionId = generateTransactionId()
        val timestamp = Date()

        val deductionAmount = if (amountNum == 1000.0) amountNum + 100 else amountNum

        // Update source balance
        srcAccount.balance -= deductionAmount

        // Update destination balance if own account
        val destAccount = if (destType == "own") {
            val dest = accounts.find { it.id == destId }!!
            dest.balance += amountNum
            DestinationAccount(dest.name, dest.number, dest.type)
        } else {
            DestinationAccount("", externalAccount, "External Account")
        }

        // Create transaction
        val transaction = Transaction(
            id = transactionId,
            sourceAccount = srcAccount.copy(),
            destinationAccount = destAccount,
            amount = amountNum,
            timestamp = timestamp,
            type = destType,
            status = "Successful"
        )

        transactions.add(0, transaction)

        // Send email
        val email = EmailNotification(
            id = System.currentTimeMillis(),
            to = userEmail,
            subject = "Fund Transfer Confirmation",
            timestamp = timestamp,
            transactionId = transactionId,
            amount = amountNum,
            sourceAccount = srcAccount.copy(),
            destinationAccount = destAccount,
            status = "Successful",
            transferType = destType
        )

        emails.add(0, email)

        return transaction
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BankingAppTheme {
                BankingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingApp() {
    val viewModel = remember { BankingViewModel() }
    var currentScreen by remember { mutableStateOf("home") }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                emailCount = viewModel.emails.size
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransfer = { currentScreen = "transfer" }
                )
                "transfer" -> TransferScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "home" },
                    onTransferComplete = { transaction ->
                        currentTransaction = transaction
                        currentScreen = "confirmation"
                    }
                )
                "confirmation" -> ConfirmationScreen(
                    transaction = currentTransaction,
                    onHome = {
                        currentTransaction = null
                        currentScreen = "home"
                    },
                    onNewTransfer = {
                        currentTransaction = null
                        currentScreen = "transfer"
                    }
                )
                "history" -> HistoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "home" }
                )
                "email" -> EmailScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BankingViewModel, onNavigateToTransfer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Online Banking",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Welcome,", fontSize = 12.sp, color = Color.Gray)
                Text(
                    viewModel.userEmail,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Your Accounts",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Accounts list
        viewModel.accounts.forEach { account ->
            AccountCard(account)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transfer button
        Button(
            onClick = onNavigateToTransfer,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fund Transfer")
        }
    }
}

@Composable
fun AccountCard(account: Account) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(account.type, fontSize = 14.sp, color = Color.Gray)
                Text(
                    "Acc# ${account.number}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                formatCurrency(account.balance),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: BankingViewModel,
    onBack: () -> Unit,
    onTransferComplete: (Transaction) -> Unit
) {
    var sourceAccountId by remember { mutableStateOf("") }
    var destinationType by remember { mutableStateOf("own") }
    var destinationAccountId by remember { mutableStateOf("") }
    var externalAccount by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Fund Transfer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it, color = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Source account dropdown
        Text("Source Account *", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        var sourceExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = sourceExpanded,
            onExpandedChange = { sourceExpanded = it }
        ) {
            OutlinedTextField(
                value = viewModel.accounts.find { it.id == sourceAccountId }?.let {
                    "${it.name} (${it.type}) - ${formatCurrency(it.balance)}"
                } ?: "Select source account",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sourceExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = sourceExpanded,
                onDismissRequest = { sourceExpanded = false }
            ) {
                viewModel.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name} (${account.type}) - ${formatCurrency(account.balance)}") },
                        onClick = {
                            sourceAccountId = account.id
                            sourceExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transfer to type
        Text("Transfer To *", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = destinationType == "own",
                onClick = {
                    destinationType = "own"
                    externalAccount = ""
                },
                label = { Text("Own Account") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = destinationType == "external",
                onClick = {
                    destinationType = "external"
                    destinationAccountId = ""
                },
                label = { Text("External Account") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Destination selection
        if (destinationType == "own") {
            var destExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = destExpanded,
                onExpandedChange = { destExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.accounts.find { it.id == destinationAccountId }?.let {
                        "${it.name} (${it.type}) - Acc# ${it.number}"
                    } ?: "Select destination account",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(destExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = destExpanded,
                    onDismissRequest = { destExpanded = false }
                ) {
                    viewModel.accounts.filter { it.id != sourceAccountId }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (${account.type}) - Acc# ${account.number}") },
                            onClick = {
                                destinationAccountId = account.id
                                destExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = externalAccount,
                onValueChange = { externalAccount = it },
                label = { Text("External Account Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                "Valid test accounts: ${viewModel.validExternalAccounts.joinToString(", ")}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount input
        Text("Amount (PHP) *", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            placeholder = { Text("0.00") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Text(
            "Min: P${viewModel.minAmount.toInt()} | Max: P${viewModel.maxAmount.toInt()}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = {
                val validationError = viewModel.validateTransfer(
                    sourceAccountId, destinationType, destinationAccountId,
                    externalAccount, amount
                )

                if (validationError != null) {
                    error = validationError
                } else {
                    val transaction = viewModel.processTransfer(
                        sourceAccountId, destinationType, destinationAccountId,
                        externalAccount, amount
                    )
                    onTransferComplete(transaction)

                    // Reset form
                    sourceAccountId = ""
                    destinationAccountId = ""
                    externalAccount = ""
                    amount = ""
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm Transfer")
        }
    }
}

@Composable
fun ConfirmationScreen(
    transaction: Transaction?,
    onHome: () -> Unit,
    onNewTransfer: () -> Unit
) {
    transaction?.let { txn ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Success icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Transfer Successful!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your transaction has been completed",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("Transaction ID", txn.id, isMonospace = true)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Status", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                txn.status,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Amount", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                formatCurrency(txn.amount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("From", fontSize = 14.sp, color = Color.Gray)
                    Text(txn.sourceAccount.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${txn.sourceAccount.type} - Acc# ${txn.sourceAccount.number}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("To", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        if (txn.type == "own") txn.destinationAccount.name else "External Account",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (txn.type == "own")
                            "${txn.destinationAccount.type} - Acc# ${txn.destinationAccount.number}"
                        else
                            "Acc# ${txn.destinationAccount.number}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    if (txn.type == "external") {
                        Text(
                            "Transfer may take 1-2 business days",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Date & Time", fontSize = 14.sp, color = Color.Gray)
                    Text(formatDateTime(txn.timestamp), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Home")
                }

                Button(
                    onClick = onNewTransfer,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Transfer")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: BankingViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text(
                "Transaction History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (viewModel.transactions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No transactions yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.transactions) { txn ->
                    TransactionCard(txn)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        transaction.id,
                        fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.Gray
                    )
                    Text(
                        formatDateTime(transaction.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    formatCurrency(transaction.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "From: ${transaction.sourceAccount.name} (${transaction.sourceAccount.number})",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                "To: ${if (transaction.type == "own") transaction.destinationAccount.name else "External Account"} (${transaction.destinationAccount.number})",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                transaction.status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF10B981)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailScreen(viewModel: BankingViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text(
                "Email Inbox",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(viewModel.userEmail, fontSize = 14.sp, color = Color.Gray)
                Text(
                    "${viewModel.emails.size} notification(s)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.emails.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No emails yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.emails) { email ->
                    EmailCard(email)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun EmailCard(email: EmailNotification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        email.subject,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        formatDateTime(email.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Dear Customer,", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your fund transfer has been completed successfully.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    EmailDetailRow("Transaction ID:", email.transactionId)
                    EmailDetailRow("Amount:", formatCurrency(email.amount))
                    EmailDetailRow(
                        "From:",
                        "${email.sourceAccount.name} (Acc# ${email.sourceAccount.number})"
                    )
                    EmailDetailRow(
                        "To:",
                        "${if (email.transferType == "own") email.destinationAccount.name else "External Account"} (Acc# ${email.destinationAccount.number})"
                    )
                    EmailDetailRow("Status:", email.status, isStatus = true)

                    if (email.transferType == "external") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Note: Transfer to external account may take 1-2 business days",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmailDetailRow(label: String, value: String, isStatus: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            fontSize = 14.sp,
            color = if (isStatus) Color(0xFF10B981) else Color.Black,
            fontWeight = if (isStatus) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    emailCount: Int
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = { onScreenChange("home") },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentScreen == "transfer",
            onClick = { onScreenChange("transfer") },
            icon = { Icon(Icons.Default.Send, "Transfer") },
            label = { Text("Transfer") }
        )
        NavigationBarItem(
            selected = currentScreen == "history",
            onClick = { onScreenChange("history") },
            icon = { Icon(Icons.Default.List, "History") },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = currentScreen == "email",
            onClick = { onScreenChange("email") },
            icon = {
                BadgedBox(
                    badge = {
                        if (emailCount > 0) {
                            Badge { Text(emailCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Email, "Email")
                }
            },
            label = { Text("Email") }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Column {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else null
        )
    }
}

// Helper functions
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    format.currency = Currency.getInstance("PHP")
    return format.format(amount).replace("PHP", "P")
}

fun formatDateTime(date: Date): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BankingAppTheme {
        BankingApp()
    }
}