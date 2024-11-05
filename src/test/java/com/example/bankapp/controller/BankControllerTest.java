package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BankControllerTest {

    @InjectMocks
    private BankController bankController;

    @Mock
    private AccountService accountService;

    @Mock
    private Model model;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize account
        account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.ZERO);

        // Set up SecurityContext and Authentication mocks
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testDashboard() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        String viewName = bankController.dashboard(model);
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("account", account);
    }

    @Test
    void testShowRegistrationForm() {
        String viewName = bankController.showRegistrationForm();
        assertEquals("register", viewName);
    }

    @Test
    void testRegisterAccount_Success() {
        when(accountService.registerAccount(anyString(), anyString())).thenReturn(account);
        String viewName = bankController.registerAccount("newuser", "password", model);
        assertEquals("redirect:/login", viewName);
    }

    @Test
    void testRegisterAccount_Failure() {
        when(accountService.registerAccount(anyString(), anyString())).thenThrow(new RuntimeException("Username already exists"));
        String viewName = bankController.registerAccount("existinguser", "password", model);
        assertEquals("register", viewName);
        verify(model).addAttribute("error", "Username already exists");
    }

    @Test
    void testDeposit() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        String viewName = bankController.deposit(BigDecimal.valueOf(100));
        assertEquals("redirect:/dashboard", viewName);
        verify(accountService).deposit(account, BigDecimal.valueOf(100));
    }

    @Test
    void testWithdraw_Success() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        String viewName = bankController.withdraw(BigDecimal.valueOf(50), model);
        assertEquals("redirect:/dashboard", viewName);
        verify(accountService).withdraw(account, BigDecimal.valueOf(50));
    } 

    @Test
    void testWithdraw_InsufficientFunds() {
    account.setBalance(BigDecimal.valueOf(20)); // Set balance to 20
    when(accountService.findAccountByUsername(anyString())).thenReturn(account);
    
    // Correcting the way to simulate an exception for a void method
    doThrow(new RuntimeException("Insufficient funds")).when(accountService).withdraw(any(), any());
    
    String viewName = bankController.withdraw(BigDecimal.valueOf(50), model);
    assertEquals("dashboard", viewName); // Expect to return to dashboard
    verify(model).addAttribute("error", "Insufficient funds"); // Verify error message
    verify(accountService).withdraw(account, BigDecimal.valueOf(50)); // Verify withdrawal attempt
    }

    @Test
    void testTransactionHistory() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        String viewName = bankController.transactionHistory(model);
        assertEquals("transactions", viewName);
        verify(accountService).getTransactionHistory(account);
    }

    @Test
    void testTransferAmount_Success() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        String viewName = bankController.transferAmount("recipientUser", BigDecimal.valueOf(50), model);
        assertEquals("redirect:/dashboard", viewName);
        verify(accountService).transferAmount(account, "recipientUser", BigDecimal.valueOf(50));
    }

    @Test
    void testTransferAmount_Failure() {
        when(accountService.findAccountByUsername(anyString())).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds")).when(accountService).transferAmount(any(), anyString(), any());
        String viewName = bankController.transferAmount("recipientUser", BigDecimal.valueOf(50), model);
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("error", "Insufficient funds");
    }
}
