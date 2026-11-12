package io.github.georgerankin.sis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InternalTransferDetectorTest {

    private final InternalTransferDetector internalTransferDetector = new InternalTransferDetector();

    @Test
    void checkReturnsNotInternalWhenNoAccountMatches() {

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Example Landlord",
                "DE00000000000000000099",
                "March rent landlord",
                new BigDecimal("-800.00"));

        List<BankAccount> accounts = List.of(
            new BankAccount(
            0,
            "Main Account",
            "checking",
            "GB00000000000000000099" ));

        TransferCheck result = internalTransferDetector.check(row, accounts);

        assertFalse(result.isInternalTransfer());
        assertNull(result.reason());
        assertNull(result.matchedAccount());

    }

    @Test
    void checkFlagsRowWhosePartnerIbanMatchesAnAccount() {

        BankAccount savings = new BankAccount(
            1,
            "Holiday Savings",
            "savings",
            "DE00000000000000000002" );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Holiday Savings",
                "DE00000000000000000002",
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(savings));

        assertTrue(result.isInternalTransfer());
        assertEquals("Partner IBAN matches one of your accounts", result.reason());
        assertEquals(savings, result.matchedAccount());

    }

    @ParameterizedTest
    @CsvSource({
        "de00000000000000000002 , DE00000000000000000002,",
        "DE00 0000 0000 0000 0000 02 , DE00000000000000000002,",
        "de00 0000 0000 0000 0000 02 , DE00000000000000000002,",
    })
    void checkMatchesIbanRegardlessOfSpacesAndCase(String accountIban, String partnerIban) {
        BankAccount savings = new BankAccount(
            1, 
            "Holiday Savings", 
            "savings", 
            accountIban
        );

        StatementRow row = new StatementRow(
            2,
            LocalDate.of(2026,3,2),
            LocalDate.of(2026,3,2),
            "Holiday Savings",
            partnerIban,
            "Monthly transfer",
            new BigDecimal("-100.00"));
        
            TransferCheck result = internalTransferDetector.check(row, List.of(savings));
        
        assertTrue(result.isInternalTransfer());
    }

    @Test
    void checkSkipsAccountWithNullIban() {

        BankAccount savings = new BankAccount(
            1,
            "Holiday Savings",
            "savings",
            null );

        BankAccount checking = new BankAccount(
            2,
            "Monthly Expenses",
            "checking",
            "DE00000000000000000002" );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Monthly Expenses",
                "DE00000000000000000002",
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(savings,checking));

        assertTrue(result.isInternalTransfer());
        assertEquals("Partner IBAN matches one of your accounts", result.reason());
        assertEquals(checking, result.matchedAccount());

    }

    @Test
    void checkFlagsRowWhosePartnerNameMatchesAnAccount() {

        BankAccount savings = new BankAccount(
            1,
            "Holiday Savings",
            "savings",
            null );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Holiday Savings",
                null,
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(savings));

        assertTrue(result.isInternalTransfer());
        assertEquals("Partner name matches one of your accounts", result.reason());
        assertEquals(savings, result.matchedAccount());

    }
    
    @Test 
    void checkIgnoresNameMatchWhenPartnerIbanDiffers() {
        BankAccount savings = new BankAccount(
            1,
            "Holiday Savings",
            "savings",
            "DE00000000000000000002" );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Holiday Savings",
                "DE00000000000000000003",
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(savings));

        assertFalse(result.isInternalTransfer());
        assertNull(result.reason());
        assertNull(result.matchedAccount());
    }

    @Test
    void checkMatchesByNameWhenRowHasNoIban() {

        BankAccount checking = new BankAccount(
            1,
            "Monthly Expenses",
            "checking",
            "DE00000000000000000001" );
        
        BankAccount savings = new BankAccount(
            2,
            "Holiday Savings",
            "savings",
           "DE00000000000000000002" );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Holiday Savings",
                null,
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(checking,savings));

        assertTrue(result.isInternalTransfer());
        assertEquals("Partner name matches one of your accounts", result.reason());
        assertEquals(savings, result.matchedAccount());
    }

    @Test 
    void checkDoesNotMatchWhenPartnerNameOnlyContainsAccountName() {
        BankAccount savings = new BankAccount(
            1,
            "Savings",
            "savings",
            "DE00000000000000000002" );

        StatementRow row = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Savings & Loans Ltd",
                null,
                "Monthly Transfer",
                new BigDecimal("-100.00"));

        TransferCheck result = internalTransferDetector.check(row, List.of(savings));

        assertFalse(result.isInternalTransfer());
        assertNull(result.reason());
        assertNull(result.matchedAccount());
    }
}
