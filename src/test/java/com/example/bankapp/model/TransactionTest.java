package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    private Transaction transaction;
    private Account account;

    @BeforeEach
    public void setUp() {
        // Initialize an Account object to associate with the Transaction
        account = new Account();
        account.setId(1L);
        account.setUsername("testUser");
        account.setPassword("password123");
        account.setBalance(BigDecimal.valueOf(1000.00));

        // Initialize a Transaction object before each test
        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(BigDecimal.valueOf(150.00));
        transaction.setType("DEPOSIT");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setAccount(account);
    }

    @Test
    public void testGetId() {
        assertEquals(1L, transaction.getId());
    }

    @Test
    public void testGetAmount() {
        assertEquals(BigDecimal.valueOf(150.00), transaction.getAmount());
    }

    @Test
    public void testGetType() {
        assertEquals("DEPOSIT", transaction.getType());
    }

    @Test
    public void testGetTimestamp() {
        assertNotNull(transaction.getTimestamp());
    }

    @Test
    public void testGetAccount() {
        assertEquals(account, transaction.getAccount());
    }

    @Test
    public void testSetAmount() {
        BigDecimal newAmount = BigDecimal.valueOf(200.00);
        transaction.setAmount(newAmount);
        assertEquals(newAmount, transaction.getAmount());
    }

    @Test
    public void testSetType() {
        String newType = "WITHDRAWAL";
        transaction.setType(newType);
        assertEquals(newType, transaction.getType());
    }

    @Test
    public void testSetTimestamp() {
        LocalDateTime newTimestamp = LocalDateTime.now().plusDays(1);
        transaction.setTimestamp(newTimestamp);
        assertEquals(newTimestamp, transaction.getTimestamp());
    }

    @Test
    public void testSetAccount() {
        Account newAccount = new Account();
        newAccount.setId(2L);
        transaction.setAccount(newAccount);
        assertEquals(newAccount, transaction.getAccount());
    }
}
