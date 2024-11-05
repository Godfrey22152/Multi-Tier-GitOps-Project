package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    private Account account;

    @BeforeEach
    public void setUp() {
        // Initialize an Account object before each test
        account = new Account();
        account.setId(1L);
        account.setUsername("testUser");
        account.setPassword("password123");
        account.setBalance(BigDecimal.valueOf(1000.00));
        account.setTransactions(new ArrayList<>());

        // Initialize authorities
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        account.setAuthorities(authorities);
    }

    @Test
    public void testGetId() {
        assertEquals(1L, account.getId());
    }

    @Test
    public void testGetUsername() {
        assertEquals("testUser", account.getUsername());
    }

    @Test
    public void testGetPassword() {
        assertEquals("password123", account.getPassword());
    }

    @Test
    public void testGetBalance() {
        assertEquals(BigDecimal.valueOf(1000.00), account.getBalance());
    }

    @Test
    public void testGetTransactions() {
        assertTrue(account.getTransactions().isEmpty());
    }

    @Test
    public void testSetAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        account.setAuthorities(authorities);
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    public void testUserDetailsInterface() {
        assertNotNull(account.getAuthorities(), "Authorities should not be null");
        assertFalse(account.getAuthorities().isEmpty(), "Authorities should not be empty");

        // Check for a specific role
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        assertTrue(account.getAuthorities().contains(authority), "Authorities should contain ROLE_USER");
    }
}
