package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(BigDecimal.ZERO);

        // Mock password encoding
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    }

    @Test
    void testFindAccountByUsername() {
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account));
        Account foundAccount = accountService.findAccountByUsername("testuser");
        assertEquals("testuser", foundAccount.getUsername());
    }

    @Test
    void testRegisterAccount_Success() {
        Account newAccount = new Account();
        newAccount.setUsername("newuser");
        newAccount.setPassword("password");
        newAccount.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenReturn(newAccount);

        Account registeredAccount = accountService.registerAccount("newuser", "password");
        assertEquals("newuser", registeredAccount.getUsername());
    }

    @Test
    void testRegisterAccount_UsernameExists() {
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.registerAccount("testuser", "password");
        });
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testDeposit() {
        BigDecimal initialBalance = BigDecimal.valueOf(100);
        account.setBalance(initialBalance);
        when(accountRepository.save(any())).thenReturn(account);
        accountService.deposit(account, BigDecimal.valueOf(50));
        assertEquals(initialBalance.add(BigDecimal.valueOf(50)), account.getBalance());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_Success() {
        account.setBalance(BigDecimal.valueOf(100));
        when(accountRepository.save(any())).thenReturn(account);
        accountService.withdraw(account, BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(50), account.getBalance());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        account.setBalance(BigDecimal.valueOf(30));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.withdraw(account, BigDecimal.valueOf(50));
        });
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void testGetTransactionHistory() {
        when(transactionRepository.findByAccountId(any())).thenReturn(List.of(new Transaction()));
        List<Transaction> transactions = accountService.getTransactionHistory(account);
        assertEquals(1, transactions.size());
    }

    @Test
    void testTransferAmount_Success() {
        Account toAccount = new Account();
        toAccount.setBalance(BigDecimal.valueOf(100));
    
        when(accountRepository.findByUsername("recipientUser")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any())).thenReturn(account).thenReturn(toAccount);

        account.setBalance(BigDecimal.valueOf(150));
        accountService.transferAmount(account, "recipientUser", BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(100), account.getBalance());
        assertEquals(BigDecimal.valueOf(150), toAccount.getBalance());

        // Allow both sender and receiver transaction saves
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void testTransferAmount_InsufficientFunds() {
        account.setBalance(BigDecimal.valueOf(30));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(account, "recipientUser", BigDecimal.valueOf(50));
        });
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void testTransferAmount_RecipientNotFound() {
        Account sender = new Account();
        sender.setUsername("senderUser");
        sender.setBalance(BigDecimal.valueOf(100));

        // Mocking the behavior of accountRepository to return an empty Optional
        when(accountRepository.findByUsername("recipientUser")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(sender, "recipientUser", BigDecimal.valueOf(50));
        });
    
        assertEquals("Recipient account not found", exception.getMessage());
    }
}
