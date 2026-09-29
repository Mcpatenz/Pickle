package com.example

import com.example.viewmodel.AppSection
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun authViewModel_tracksAuthStatusAndRoleAndControlsLandingNavigation() {
        val authViewModel = AuthViewModel()

        // 1. Unauthenticated initial state on landing page
        assertFalse(authViewModel.isAuthenticated.value)
        assertNull(authViewModel.userRole.value)
        assertEquals(AppSection.HOME, authViewModel.currentSection.value)

        // Attempting to navigate from landing page while unauthenticated opens Log In form
        val unauthenticatedTarget = authViewModel.navigateFromLandingPage()
        assertEquals(AppSection.HOME, unauthenticatedTarget)
        assertTrue(authViewModel.showAuthModal.value)

        // 2. All user registrations are Customer -> opens Customer Dashboard
        val customerTarget = authViewModel.registerCustomer("Alex Rivera", "alex@pickleplay.ph", "123456")
        assertTrue(authViewModel.isAuthenticated.value)
        assertEquals(UserRole.CUSTOMER, authViewModel.userRole.value)
        assertEquals(AppSection.CUSTOMER_DASHBOARD, customerTarget)

        // 3. Log in as Admin -> opens Admin Dashboard
        val adminTarget = authViewModel.login("admin@pickleplay.ph", "123456")
        assertTrue(authViewModel.isAuthenticated.value)
        assertEquals(UserRole.ADMIN, authViewModel.userRole.value)
        assertEquals(AppSection.ADMIN, adminTarget)

        // 4. Add Cashier from Admin Dashboard, then log in as that Cashier -> opens Cashier Dashboard
        val addedCashier = authViewModel.addCashierFromAdmin("Carlo Cashier", "carlo@pickleplay.ph", "123456")
        assertEquals(UserRole.CASHIER, addedCashier.role)
        val cashierTarget = authViewModel.login("carlo@pickleplay.ph", "123456")
        assertTrue(authViewModel.isAuthenticated.value)
        assertEquals(UserRole.CASHIER, authViewModel.userRole.value)
        assertEquals(AppSection.CASHIER, cashierTarget)

        // 5. Log out -> resets authentication status and returns to landing page (HOME)
        authViewModel.logout()
        assertFalse(authViewModel.isAuthenticated.value)
        assertNull(authViewModel.userRole.value)
        assertEquals(AppSection.HOME, authViewModel.currentSection.value)
    }

    @Test
    fun authViewModel_validatesSignInAndRegisterWithToastStates() {
        val authViewModel = AuthViewModel()

        // Error state on empty Sign In submission
        val emptyLoginResult = authViewModel.submitSignIn("", "")
        assertTrue(emptyLoginResult is com.example.viewmodel.AuthResult.Error)
        assertTrue(authViewModel.toastNotification.value?.contains("Error") == true)

        // Error state on wrong password for registered account
        val wrongPassResult = authViewModel.submitSignIn("admin@pickleplay.ph", "wrongpass")
        assertTrue(wrongPassResult is com.example.viewmodel.AuthResult.Error)
        assertTrue(authViewModel.toastNotification.value?.contains("Invalid password") == true)

        // Success state on valid Sign In submission
        val validLoginResult = authViewModel.submitSignIn("admin@pickleplay.ph", "123456")
        assertTrue(validLoginResult is com.example.viewmodel.AuthResult.Success)
        assertTrue(authViewModel.toastNotification.value?.contains("Sign in successful") == true)
        assertEquals(AppSection.ADMIN, authViewModel.currentSection.value)

        // Error state on incomplete Customer Registration submission
        val emptyRegisterResult = authViewModel.submitCustomerRegistration("", "bad-email", "12")
        assertTrue(emptyRegisterResult is com.example.viewmodel.AuthResult.Error)
        assertTrue(authViewModel.toastNotification.value?.contains("Error") == true)

        // Success state on valid Customer Registration submission
        val validRegisterResult = authViewModel.submitCustomerRegistration("Bea Santos", "bea@pickleplay.ph", "123456")
        assertTrue(validRegisterResult is com.example.viewmodel.AuthResult.Success)
        assertTrue(authViewModel.toastNotification.value?.contains("Registration successful") == true)
        assertEquals(UserRole.CUSTOMER, authViewModel.userRole.value)
        assertEquals(AppSection.CUSTOMER_DASHBOARD, authViewModel.currentSection.value)

        // Add staff accounts with Cashier, Court Side, and Maintenance roles
        val courtSideStaff = authViewModel.addCashierFromAdmin(
            "Paolo Reyes",
            "paolo.courtside@pickleplay.ph",
            "123456",
            com.example.viewmodel.StaffRole.COURT_SIDE
        )
        assertEquals(com.example.viewmodel.StaffRole.COURT_SIDE, courtSideStaff.staffRole)

        val maintenanceStaff = authViewModel.addCashierFromAdmin(
            "Dante Cruz",
            "dante.maintenance@pickleplay.ph",
            "123456",
            com.example.viewmodel.StaffRole.MAINTENANCE
        )
        assertEquals(com.example.viewmodel.StaffRole.MAINTENANCE, maintenanceStaff.staffRole)
    }
}
