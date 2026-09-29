package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class UserRole(val displayName: String, val dashboardLabel: String) {
    ADMIN("Admin", "Admin Dashboard"),
    CASHIER("Cashier", "Cashier Dashboard"),
    CUSTOMER("Customer", "Customer Dashboard");

    companion object {
        val PLAYER: UserRole get() = CUSTOMER
    }
}

enum class StaffRole(
    val displayName: String,
    val badgeLabel: String,
    val dutiesSummary: String,
    val emailPrefix: String
) {
    CASHIER(
        displayName = "Cashier",
        badgeLabel = "CASHIER",
        dutiesSummary = "POS Counter, Payment Verification & QR Pass Check-In",
        emailPrefix = "cashier"
    ),
    COURT_SIDE(
        displayName = "Court Side",
        badgeLabel = "COURT SIDE",
        dutiesSummary = "Court Side Attendant, Player Check-In & Match Flow",
        emailPrefix = "courtside"
    ),
    MAINTENANCE(
        displayName = "Maintenance",
        badgeLabel = "MAINTENANCE",
        dutiesSummary = "Court Surface Care, Net Inspection & Facility Upkeep",
        emailPrefix = "maintenance"
    );

    companion object {
        fun fromLabel(label: String?): StaffRole = when {
            label.equals("COURT_SIDE", ignoreCase = true) ||
                label.equals("Court Side", ignoreCase = true) ||
                label?.contains("court", ignoreCase = true) == true -> COURT_SIDE
            label.equals("MAINTENANCE", ignoreCase = true) ||
                label?.contains("maint", ignoreCase = true) == true -> MAINTENANCE
            else -> CASHIER
        }
    }
}

data class AuthUserSession(
    val fullName: String,
    val email: String,
    val role: UserRole,
    val staffRole: StaffRole? = null
)

data class RegisteredAccount(
    val fullName: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val staffRole: StaffRole = StaffRole.CASHIER
)

sealed class AuthResult {
    data class Success(
        val session: AuthUserSession,
        val targetSection: AppSection,
        val toastMessage: String
    ) : AuthResult()

    data class Error(
        val errorMessage: String
    ) : AuthResult()
}

val DefaultRegisteredAccounts = listOf(
    RegisteredAccount("Coach Anton (Owner)", "admin@pickleplay.ph", "123456", UserRole.ADMIN),
    RegisteredAccount("Maria S. (Cashier)", "cashier@pickleplay.ph", "123456", UserRole.CASHIER, StaffRole.CASHIER),
    RegisteredAccount("Marco D. (Court Side)", "courtside@pickleplay.ph", "123456", UserRole.CASHIER, StaffRole.COURT_SIDE),
    RegisteredAccount("Ramon T. (Maintenance)", "maintenance@pickleplay.ph", "123456", UserRole.CASHIER, StaffRole.MAINTENANCE),
    RegisteredAccount("Jonel P.", "customer@pickleplay.ph", "123456", UserRole.CUSTOMER),
    RegisteredAccount("Jonel P.", "player@pickleplay.ph", "123456", UserRole.CUSTOMER)
)

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val role: UserRole? = null,
    val session: AuthUserSession? = null,
    val showAuthModal: Boolean = false,
    val authModalInitialTab: Int = 0, // 0 = Log In Form, 1 = Customer Register Form
    val registeredAccounts: List<RegisteredAccount> = DefaultRegisteredAccounts,
    val lastErrorMessage: String? = null,
    val lastToastMessage: String? = null
) {
    val targetDashboardSection: AppSection?
        get() = role?.let { resolveDashboardSection(it) }

    val cashierAccounts: List<RegisteredAccount>
        get() = registeredAccounts.filter { it.role == UserRole.CASHIER }

    companion object {
        fun resolveDashboardSection(role: UserRole): AppSection = when (role) {
            UserRole.ADMIN -> AppSection.ADMIN
            UserRole.CASHIER -> AppSection.CASHIER
            UserRole.CUSTOMER -> AppSection.CUSTOMER_DASHBOARD
        }
    }
}

/**
 * Observable state holder that tracks user authentication status (`isAuthenticated`),
 * role (`Admin`, `Cashier`, `Customer` / `Player`), and Toast notification messages
 * to control navigation logic from the landing page.
 */
class AuthStateHolder {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()

    private val _authSession = MutableStateFlow<AuthUserSession?>(null)
    val authSession: StateFlow<AuthUserSession?> = _authSession.asStateFlow()

    private val _showAuthModal = MutableStateFlow(false)
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    private val _authModalInitialTab = MutableStateFlow(0)
    val authModalInitialTab: StateFlow<Int> = _authModalInitialTab.asStateFlow()

    private val _registeredAccounts = MutableStateFlow(DefaultRegisteredAccounts)
    val registeredAccounts: StateFlow<List<RegisteredAccount>> = _registeredAccounts.asStateFlow()

    private val _toastNotification = MutableStateFlow<String?>(null)
    val toastNotification: StateFlow<String?> = _toastNotification.asStateFlow()

    fun clearToastNotification() {
        _toastNotification.value = null
    }

    fun openAuthModal(isRegister: Boolean = false) {
        val tab = if (isRegister) 1 else 0
        _authModalInitialTab.value = tab
        _showAuthModal.value = true
        _uiState.update {
            it.copy(
                showAuthModal = true,
                authModalInitialTab = tab,
                lastErrorMessage = null
            )
        }
    }

    fun closeAuthModal() {
        _showAuthModal.value = false
        _uiState.update { it.copy(showAuthModal = false, lastErrorMessage = null) }
    }

    fun resolveDashboardSection(role: UserRole): AppSection {
        return AuthUiState.resolveDashboardSection(role)
    }

    /**
     * Evaluates authentication status and user role from the landing page:
     * - If unauthenticated, opens the Log In form and returns null.
     * - If authenticated, returns the target [AppSection] for the active [UserRole].
     */
    fun resolveLandingNavigationTarget(): AppSection? {
        val session = _authSession.value
        return if (!_isAuthenticated.value || session == null) {
            openAuthModal(isRegister = false)
            null
        } else {
            resolveDashboardSection(session.role)
        }
    }

    /**
     * Validates sign-in credentials and produces an [AuthResult] with a Toast message
     * for either success or error states.
     */
    fun submitSignIn(
        email: String,
        password: String,
        fallbackCustomerName: String = "Jonel P."
    ): AuthResult {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            val error = "Error: Please enter both email and password to sign in."
            recordError(error)
            return AuthResult.Error(error)
        }

        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            val error = "Error: Please enter a valid email address."
            recordError(error)
            return AuthResult.Error(error)
        }

        if (cleanPassword.length < 4) {
            val error = "Error: Password must be at least 4 characters."
            recordError(error)
            return AuthResult.Error(error)
        }

        val matchedAccount = _registeredAccounts.value.find {
            it.email.equals(cleanEmail, ignoreCase = true)
        }

        if (matchedAccount != null && matchedAccount.password != cleanPassword) {
            val error = "Error: Invalid password for $cleanEmail."
            recordError(error)
            return AuthResult.Error(error)
        }

        val session = loginWithCredentials(cleanEmail, cleanPassword, fallbackCustomerName)
        val destination = resolveDashboardSection(session.role)
        val successMsg = "Sign in successful! Welcome ${session.fullName} (${session.role.displayName})."
        _toastNotification.value = successMsg
        _uiState.update { it.copy(lastToastMessage = successMsg, lastErrorMessage = null) }
        return AuthResult.Success(session, destination, successMsg)
    }

    /**
     * Validates Customer registration fields and produces an [AuthResult] with a Toast message
     * for either success or error states. All registered accounts are assigned [UserRole.CUSTOMER].
     */
    fun submitCustomerRegistration(
        fullName: String,
        email: String,
        password: String
    ): AuthResult {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            val error = "Error: Please complete full name, email, and password to register."
            recordError(error)
            return AuthResult.Error(error)
        }

        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            val error = "Error: Please enter a valid email address."
            recordError(error)
            return AuthResult.Error(error)
        }

        if (cleanPassword.length < 4) {
            val error = "Error: Password must be at least 4 characters."
            recordError(error)
            return AuthResult.Error(error)
        }

        val session = registerCustomer(cleanName, cleanEmail, cleanPassword)
        val destination = resolveDashboardSection(session.role)
        val successMsg = "Registration successful! Welcome ${session.fullName} (Customer)."
        _toastNotification.value = successMsg
        _uiState.update { it.copy(lastToastMessage = successMsg, lastErrorMessage = null) }
        return AuthResult.Success(session, destination, successMsg)
    }

    private fun recordError(error: String) {
        _toastNotification.value = error
        _uiState.update { it.copy(lastErrorMessage = error, lastToastMessage = error) }
    }

    /**
     * Logs in a user using their email and password and determines their role
     * automatically from registered accounts (Admin, Cashier added from Admin Dashboard, or Customer).
     */
    fun loginWithCredentials(
        email: String,
        password: String,
        fallbackCustomerName: String = "Jonel P."
    ): AuthUserSession {
        val cleanEmail = email.trim()
        val matchedAccount = _registeredAccounts.value.find {
            it.email.equals(cleanEmail, ignoreCase = true)
        }

        val resolvedRole = matchedAccount?.role ?: when {
            cleanEmail.contains("admin", ignoreCase = true) -> UserRole.ADMIN
            cleanEmail.contains("cashier", ignoreCase = true) -> UserRole.CASHIER
            else -> UserRole.CUSTOMER
        }

        val displayName = matchedAccount?.fullName ?: when (resolvedRole) {
            UserRole.ADMIN -> "Coach Anton (Owner)"
            UserRole.CASHIER -> "Cashier Staff"
            UserRole.CUSTOMER -> fallbackCustomerName
        }

        val effectiveEmail = cleanEmail.ifBlank {
            when (resolvedRole) {
                UserRole.ADMIN -> "admin@pickleplay.ph"
                UserRole.CASHIER -> "cashier@pickleplay.ph"
                UserRole.CUSTOMER -> "customer@pickleplay.ph"
            }
        }

        val session = AuthUserSession(
            fullName = displayName,
            email = effectiveEmail,
            role = resolvedRole,
            staffRole = if (resolvedRole == UserRole.CASHIER) (matchedAccount?.staffRole ?: StaffRole.CASHIER) else null
        )
        applyAuthenticatedSession(session)
        return session
    }

    /**
     * Overload supporting explicit role parameter for unit tests or role switching.
     */
    fun login(
        email: String,
        password: String,
        selectedRole: UserRole? = null,
        fallbackPlayerName: String = "Jonel P."
    ): AuthUserSession {
        if (selectedRole == null) {
            return loginWithCredentials(email, password, fallbackPlayerName)
        }
        val cleanEmail = email.trim()
        val matchedAccount = _registeredAccounts.value.find {
            it.email.equals(cleanEmail, ignoreCase = true)
        }
        val effectiveRole = matchedAccount?.role ?: selectedRole
        val displayName = matchedAccount?.fullName ?: when (effectiveRole) {
            UserRole.ADMIN -> "Coach Anton (Owner)"
            UserRole.CASHIER -> "Maria S. (Cashier)"
            UserRole.CUSTOMER -> fallbackPlayerName
        }
        val effectiveEmail = cleanEmail.ifBlank {
            when (effectiveRole) {
                UserRole.ADMIN -> "admin@pickleplay.ph"
                UserRole.CASHIER -> "cashier@pickleplay.ph"
                UserRole.CUSTOMER -> "customer@pickleplay.ph"
            }
        }
        val session = AuthUserSession(
            fullName = displayName,
            email = effectiveEmail,
            role = effectiveRole
        )
        applyAuthenticatedSession(session)
        return session
    }

    /**
     * All users registering via the public registration form are registered as CUSTOMER.
     */
    fun registerCustomer(
        fullName: String,
        email: String,
        password: String
    ): AuthUserSession {
        return register(fullName, email, password, UserRole.CUSTOMER)
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole = UserRole.CUSTOMER
    ): AuthUserSession {
        val cleanName = fullName.trim().ifBlank { "PicklePlay Customer" }
        val cleanEmail = email.trim().ifBlank { "customer@pickleplay.ph" }
        val newAccount = RegisteredAccount(
            fullName = cleanName,
            email = cleanEmail,
            password = password.ifBlank { "123456" },
            role = UserRole.CUSTOMER // All user registrations are Customer
        )
        val updatedAccounts = listOf(newAccount) + _registeredAccounts.value.filterNot {
            it.email.equals(cleanEmail, ignoreCase = true)
        }
        _registeredAccounts.value = updatedAccounts

        val session = AuthUserSession(
            fullName = cleanName,
            email = cleanEmail,
            role = UserRole.CUSTOMER
        )
        applyAuthenticatedSession(session, updatedAccounts)
        return session
    }

    /**
     * Adds a Staff / Cashier account (Cashier, Court Side, or Maintenance) from the Admin Dashboard.
     */
    fun addCashierAccount(
        fullName: String,
        email: String,
        password: String,
        staffRole: StaffRole = StaffRole.CASHIER
    ): RegisteredAccount {
        val cleanName = fullName.trim().ifBlank { "${staffRole.displayName} Staff" }
        val cleanEmail = email.trim().ifBlank {
            "${staffRole.emailPrefix}${_registeredAccounts.value.count { it.role == UserRole.CASHIER } + 1}@pickleplay.ph"
        }
        val cleanPassword = password.trim().ifBlank { "123456" }
        val cashierAccount = RegisteredAccount(
            fullName = cleanName,
            email = cleanEmail,
            password = cleanPassword,
            role = UserRole.CASHIER,
            staffRole = staffRole
        )
        val updatedAccounts = listOf(cashierAccount) + _registeredAccounts.value.filterNot {
            it.email.equals(cleanEmail, ignoreCase = true)
        }
        _registeredAccounts.value = updatedAccounts
        _uiState.update { it.copy(registeredAccounts = updatedAccounts) }
        return cashierAccount
    }

    fun removeCashierAccount(email: String) {
        val updatedAccounts = _registeredAccounts.value.filterNot {
            it.role == UserRole.CASHIER && it.email.equals(email.trim(), ignoreCase = true)
        }
        _registeredAccounts.value = updatedAccounts
        _uiState.update { it.copy(registeredAccounts = updatedAccounts) }
    }

    fun switchRole(role: UserRole, fallbackPlayerName: String = "Jonel P."): AuthUserSession {
        val defaultEmail = when (role) {
            UserRole.ADMIN -> "admin@pickleplay.ph"
            UserRole.CASHIER -> "cashier@pickleplay.ph"
            UserRole.CUSTOMER -> "customer@pickleplay.ph"
        }
        return login(defaultEmail, "123456", role, fallbackPlayerName)
    }

    fun logout() {
        _authSession.value = null
        _isAuthenticated.value = false
        _userRole.value = null
        _showAuthModal.value = false
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                role = null,
                session = null,
                showAuthModal = false,
                lastErrorMessage = null
            )
        }
    }

    private fun applyAuthenticatedSession(
        session: AuthUserSession,
        accounts: List<RegisteredAccount> = _registeredAccounts.value
    ) {
        _authSession.value = session
        _isAuthenticated.value = true
        _userRole.value = session.role
        _showAuthModal.value = false
        _uiState.value = AuthUiState(
            isAuthenticated = true,
            role = session.role,
            session = session,
            showAuthModal = false,
            authModalInitialTab = _authModalInitialTab.value,
            registeredAccounts = accounts,
            lastErrorMessage = null
        )
    }
}

/**
 * Simple ViewModel wrapper around [AuthStateHolder] to track user authentication status
 * and role (`Admin`, `Cashier`, `Customer` / `Player`) and control navigation logic from the landing page.
 */
class AuthViewModel : ViewModel() {
    val stateHolder = AuthStateHolder()

    val uiState: StateFlow<AuthUiState> = stateHolder.uiState
    val isAuthenticated: StateFlow<Boolean> = stateHolder.isAuthenticated
    val userRole: StateFlow<UserRole?> = stateHolder.userRole
    val authSession: StateFlow<AuthUserSession?> = stateHolder.authSession
    val showAuthModal: StateFlow<Boolean> = stateHolder.showAuthModal
    val authModalInitialTab: StateFlow<Int> = stateHolder.authModalInitialTab
    val registeredAccounts: StateFlow<List<RegisteredAccount>> = stateHolder.registeredAccounts
    val toastNotification: StateFlow<String?> = stateHolder.toastNotification

    private val _currentSection = MutableStateFlow(AppSection.HOME)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    fun openAuthModal(isRegister: Boolean = false) {
        stateHolder.openAuthModal(isRegister)
    }

    fun closeAuthModal() {
        stateHolder.closeAuthModal()
    }

    fun submitSignIn(email: String, password: String): AuthResult {
        val result = stateHolder.submitSignIn(email, password)
        if (result is AuthResult.Success) {
            _currentSection.value = result.targetSection
        }
        return result
    }

    fun submitCustomerRegistration(fullName: String, email: String, password: String): AuthResult {
        val result = stateHolder.submitCustomerRegistration(fullName, email, password)
        if (result is AuthResult.Success) {
            _currentSection.value = result.targetSection
        }
        return result
    }

    fun login(email: String, password: String): AppSection {
        val session = stateHolder.loginWithCredentials(email, password)
        val destination = stateHolder.resolveDashboardSection(session.role)
        _currentSection.value = destination
        return destination
    }

    fun loginWithRole(email: String, password: String, role: UserRole): AppSection {
        val session = stateHolder.login(email, password, role)
        val destination = stateHolder.resolveDashboardSection(session.role)
        _currentSection.value = destination
        return destination
    }

    fun registerCustomer(fullName: String, email: String, password: String): AppSection {
        val session = stateHolder.registerCustomer(fullName, email, password)
        val destination = stateHolder.resolveDashboardSection(session.role)
        _currentSection.value = destination
        return destination
    }

    fun addCashierFromAdmin(
        fullName: String,
        email: String,
        password: String,
        staffRole: StaffRole = StaffRole.CASHIER
    ): RegisteredAccount {
        return stateHolder.addCashierAccount(fullName, email, password, staffRole)
    }

    fun switchRole(role: UserRole): AppSection {
        val session = stateHolder.switchRole(role)
        val destination = stateHolder.resolveDashboardSection(session.role)
        _currentSection.value = destination
        return destination
    }

    fun navigateFromLandingPage(): AppSection {
        val target = stateHolder.resolveLandingNavigationTarget()
        if (target != null) {
            _currentSection.value = target
        }
        return _currentSection.value
    }

    fun navigateTo(section: AppSection) {
        _currentSection.value = section
    }

    fun logout() {
        stateHolder.logout()
        _currentSection.value = AppSection.HOME
    }
}
