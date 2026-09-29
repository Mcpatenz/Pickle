package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.LoginScreen as ScreenLoginScreen
import com.example.ui.screens.RegisterScreen as ScreenRegisterScreen
import com.example.viewmodel.DefaultRegisteredAccounts
import com.example.viewmodel.RegisteredAccount

/**
 * Reusable LoginScreen component re-exported in `com.example.ui.components`
 * for convenient access across screens and dialogs.
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
    ScreenLoginScreen(
        modifier = modifier,
        initialEmail = initialEmail,
        initialPassword = initialPassword,
        isRegisterMode = isRegisterMode,
        registeredAccounts = registeredAccounts,
        onSignIn = onSignIn,
        onRegister = onRegister,
        onToastShown = onToastShown,
        onCancel = onCancel
    )
}

/**
 * Reusable RegisterScreen component re-exported in `com.example.ui.components`.
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
    ScreenRegisterScreen(
        modifier = modifier,
        initialFullName = initialFullName,
        initialEmail = initialEmail,
        initialPassword = initialPassword,
        registeredAccounts = registeredAccounts,
        onRegister = onRegister,
        onToastShown = onToastShown,
        onCancel = onCancel
    )
}

/**
 * Dialog wrapper hosting the reusable [LoginScreen] (when [initialTab] == 0)
 * or [RegisterScreen] (when [initialTab] == 1), with integrated Toast notifications
 * for success and error states upon form submission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthLoginRegisterDialog(
    initialTab: Int = 0, // 0 = Sign In (LoginScreen), 1 = Customer Registration (RegisterScreen)
    registeredAccounts: List<RegisteredAccount> = DefaultRegisteredAccounts,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (fullName: String, email: String, password: String) -> Unit,
    onToastShown: (message: String, isError: Boolean) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_login_register_dialog")
    ) {
        ScreenLoginScreen(
            isRegisterMode = initialTab == 1,
            registeredAccounts = registeredAccounts,
            onSignIn = onLogin,
            onRegister = onRegister,
            onToastShown = onToastShown,
            onCancel = onDismiss
        )
    }
}
