package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.viewmodel.DefaultRegisteredAccounts
import com.example.viewmodel.RegisteredAccount

/**
 * Centralized Toast notification helper for authentication (Sign In & Register)
 * to show success or error states upon form submission.
 */
object AuthToastNotifier {
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Reusable LoginScreen component with fields for email and password, a 'Sign In' button,
 * and an integrated Toast notification system to show success or error states upon form submission
 * (as well as supporting Customer registration when [isRegisterMode] is enabled).
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    initialEmail: String = "admin@pickleplay.ph",
    initialPassword: String = "123456",
    isRegisterMode: Boolean = false,
    registeredAccounts: List<RegisteredAccount> = DefaultRegisteredAccounts,
    onSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onRegister: (fullName: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onToastShown: (message: String, isError: Boolean) -> Unit = { _, _ -> },
    onCancel: (() -> Unit)? = null
) {
    if (isRegisterMode) {
        RegisterScreen(
            modifier = modifier,
            registeredAccounts = registeredAccounts,
            onRegister = onRegister,
            onToastShown = onToastShown,
            onCancel = onCancel
        )
        return
    }

    val context = LocalContext.current
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("login_screen_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Sign In to PicklePlay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter your email and password to open your role-based dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    feedbackMessage = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("admin@pickleplay.ph") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_email")
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    feedbackMessage = null
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_password")
            )

            // Inline Toast / Submission Status Feedback Banner
            feedbackMessage?.let { message ->
                AuthFeedbackBanner(
                    message = message,
                    isError = isFeedbackError
                )
            }

            Surface(
                color = EmeraldPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Account Credentials Guide:",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "• Admin: admin@pickleplay.ph\n• Cashier: cashier@pickleplay.ph (or added in Admin Dashboard)\n• Customer: customer@pickleplay.ph",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCancel != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dismiss_auth_dialog_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()
                        val matchedAccount = registeredAccounts.find {
                            it.email.equals(cleanEmail, ignoreCase = true)
                        }

                        when {
                            cleanEmail.isBlank() || cleanPassword.isBlank() -> {
                                val err = "Error: Please enter both email and password to sign in."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            !cleanEmail.contains("@") || !cleanEmail.contains(".") -> {
                                val err = "Error: Please enter a valid email address."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            cleanPassword.length < 4 -> {
                                val err = "Error: Password must be at least 4 characters."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            matchedAccount != null && matchedAccount.password != cleanPassword -> {
                                val err = "Error: Invalid password for $cleanEmail."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            else -> {
                                val roleLabel = matchedAccount?.role?.displayName ?: when {
                                    cleanEmail.contains("admin", ignoreCase = true) -> "Admin"
                                    cleanEmail.contains("cashier", ignoreCase = true) -> "Cashier"
                                    else -> "Customer"
                                }
                                val successMsg = "Sign in successful! Welcome (${roleLabel})."
                                isFeedbackError = false
                                feedbackMessage = successMsg
                                AuthToastNotifier.showToast(context, successMsg)
                                onToastShown(successMsg, false)
                                onSignIn(cleanEmail, cleanPassword)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("submit_login_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Reusable Customer Registration Screen component with fields for full name, email, and password,
 * a 'Register' button, and an integrated Toast notification system to show success or error states.
 * All registered users are created as Customer accounts.
 */
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    initialFullName: String = "",
    initialEmail: String = "",
    initialPassword: String = "123456",
    registeredAccounts: List<RegisteredAccount> = DefaultRegisteredAccounts,
    onRegister: (fullName: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onToastShown: (message: String, isError: Boolean) -> Unit = { _, _ -> },
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var fullName by remember(initialFullName) { mutableStateOf(initialFullName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("register_screen_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Customer Registration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All new registrations are created as Customer accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    feedbackMessage = null
                },
                label = { Text("Customer Full Name") },
                placeholder = { Text("e.g., Alex Rivera") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_fullname")
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    feedbackMessage = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("customer@pickleplay.ph") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_email")
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    feedbackMessage = null
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_password")
            )

            feedbackMessage?.let { message ->
                AuthFeedbackBanner(
                    message = message,
                    isError = isFeedbackError
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCancel != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dismiss_auth_dialog_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val cleanName = fullName.trim()
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()

                        when {
                            cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank() -> {
                                val err = "Error: Please complete full name, email, and password to register."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            !cleanEmail.contains("@") || !cleanEmail.contains(".") -> {
                                val err = "Error: Please enter a valid email address."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            cleanPassword.length < 4 -> {
                                val err = "Error: Password must be at least 4 characters."
                                isFeedbackError = true
                                feedbackMessage = err
                                AuthToastNotifier.showToast(context, err)
                                onToastShown(err, true)
                            }
                            else -> {
                                val successMsg = "Registration successful! Welcome $cleanName (Customer)."
                                isFeedbackError = false
                                feedbackMessage = successMsg
                                AuthToastNotifier.showToast(context, successMsg)
                                onToastShown(successMsg, false)
                                onRegister(cleanName, cleanEmail, cleanPassword)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("submit_register_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToReg,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeedbackBanner(
    message: String,
    isError: Boolean
) {
    Surface(
        color = if (isError) MaintenanceRedBg else AvailableGreenBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isError) MaintenanceRed else AvailableGreen),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_status_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaintenanceRed else AvailableGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaintenanceRed else AvailableGreen
            )
        }
    }
}
