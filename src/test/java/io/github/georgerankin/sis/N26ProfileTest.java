package io.github.georgerankin.sis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class N26ProfileTest {

    private final N26Profile profile = new N26Profile();

    @ParameterizedTest
    @CsvSource({
        "-800.00, -800.00",
        "-12, -12.00",
        "-9.9, -9.90",
        "2500.00, 2500.00",
        "9.99, 9.99"
    })
    void parseAmountReadsN26Formats(String text, BigDecimal expected) {
        assertEquals(expected, profile.parseAmount(text));
    }

    @ParameterizedTest 
    @ValueSource(strings = {
        "abc", 
        "", 
        " ", 
        "€800.00", 
        "--5", 
        "1E3", 
        "76,543.21", 
        "76.543,21", 
        "4.321",
        "+5", 
        "76 543"
    })
    void parseAmountRejectsInvalidText(String text) {
        assertThrows(NumberFormatException.class, () -> profile.parseAmount(text));
    }

    @Test
    void parseDateReadsIsoDate() {
        assertEquals(LocalDate.of(2026, 3, 1), profile.parseDate("2026-03-01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "03.01.2026",
        "03-01-2026",
        "3.1.26",
        "2026-02-31",
        ""
    })
    void parseDateRejectsInvalidText(String text) {
        assertThrows(DateTimeParseException.class, () -> profile.parseDate(text));
    }

    @Test
    void parseRowReadsN26Columns() {
        Map<String, String> fields = Map.of(
                "Booking Date", "2026-03-01",
                "Value Date", "2026-03-01",
                "Partner Name", "Example Landlord",
                "Partner Iban", "DE00000000000000000099",
                "Payment Reference", "March rent landlord",
                "Amount (EUR)", "-800.00");

        StatementRow expected = new StatementRow(
                2,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                "Example Landlord",
                "DE00000000000000000099",
                "March rent landlord",
                new BigDecimal("-800.00"));

        assertEquals(expected, profile.parseRow(2, fields));
    }

    @Test 
    void parseRowTreatsBlankIbanAsNull() {
        Map<String, String> fields = Map.of(
                "Booking Date", "2026-03-01",
                "Value Date", "2026-03-01",
                "Partner Name", "Example Landlord",
                "Partner Iban", "",
                "Payment Reference", "March rent landlord",
                "Amount (EUR)", "-800.00");
        StatementRow actual = profile.parseRow(2, fields);
        assertNull(actual.partnerIban());
    }

    @Test 
    void checkHeaderRejectsMissingColumns() {
        List<String> header = List.of(
            "Value Date",
            "Partner Name",
            "Partner Iban", 
            "Type",
            "Payment Reference",
            "Account Name");
        InvalidStatementException exception = assertThrows(
            InvalidStatementException.class, () -> profile.checkHeader(header));
        
            assertEquals("The CSV is missing required columns: Booking Date, Amount (EUR)", exception.getMessage());            
    }

    @Test 
    void checkHeaderAcceptsN26Header() {
        List<String> header = List.of(
            "Booking Date",
            "Value Date",
            "Partner Name",
            "Partner Iban",
            "Type",
            "Payment Reference",
            "Account Name",
            "Amount (EUR)",
            "Original Amount",
            "Original Currency",
            "Exchange Rate"
        );
        assertDoesNotThrow( () -> profile.checkHeader(header));
    }

    @Test 
    void parseReadsGoodRowWithSourceRow() {
        String csv = """
        "Booking Date","Value Date","Partner Name","Partner Iban",Type,"Payment Reference","Account Name","Amount (EUR)","Original Amount","Original Currency","Exchange Rate"
        2026-03-01,2026-03-01,"Example Landlord",DE00000000000000000099,"Example Type","March rent landlord","Main Account",-800.00,,,
        """;

        ParsedStatement result = profile.parse(csv);

        assertEquals(1, result.statementRows().size());
        assertEquals(2, result.statementRows().get(0).sourceRow());
        assertEquals(0, result.unparsed().size());
    }

    @Test 
    void parsePutsRowWithInvalidAmountInUnparsed() {
        String csv = """
        "Booking Date","Value Date","Partner Name","Partner Iban",Type,"Payment Reference","Account Name","Amount (EUR)","Original Amount","Original Currency","Exchange Rate"
        2026-03-03,2026-03-03,"Example Shop",DE00000000000000000097,"Example Type","Broken row","Main Account",abc,,,
        """;

        ParsedStatement result = profile.parse(csv);

        assertEquals(1, result.unparsed().size());
        assertEquals(2, result.unparsed().get(0).sourceRow());
        assertEquals("Invalid amount", result.unparsed().get(0).reason());
    }

    @Test 
    void parsePutsRowWithInvalidDateInUnparsed() {
        String csv = """
        "Booking Date","Value Date","Partner Name","Partner Iban",Type,"Payment Reference","Account Name","Amount (EUR)","Original Amount","Original Currency","Exchange Rate"
        2026.03.04,2026-03-04,"Example Employer",DE00000000000000000098,"Example Type","Salary, March employer","Main Account",2500.00,,,
        """;

        ParsedStatement result = profile.parse(csv);

        assertEquals(1, result.unparsed().size());
        assertEquals(2, result.unparsed().get(0).sourceRow());
        assertEquals("Invalid date", result.unparsed().get(0).reason());
    }

    @Test 
    void parseRejectsCSVWithMissingRequiredColumn() {
        String csv = """
        "Booking Date","Value Date","Partner Name","Partner Iban",Type,"Payment Reference","Account Name","Original Amount","Original Currency","Exchange Rate"
        2026-03-04,2026-03-04,"Example Employer",DE00000000000000000098,"Example Type","Salary, March employer","Main Account"
        """;

        InvalidStatementException exception = assertThrows(
            InvalidStatementException.class, () -> profile.parse(csv));
        
            assertEquals("The CSV is missing required columns: Amount (EUR)", exception.getMessage());            
    }

    @Test 
    void parseReadsSampleFile() throws IOException {
        String csv = new String(
            getClass().getResourceAsStream("/csv/n26-sample.csv").readAllBytes(),
            StandardCharsets.UTF_8);

        ParsedStatement result = profile.parse(csv);

        assertEquals(4, result.statementRows().size());
        assertEquals(2, result.statementRows().get(0).sourceRow());
        assertEquals(3, result.statementRows().get(1).sourceRow());
        assertEquals(5, result.statementRows().get(2).sourceRow());
        assertEquals(6, result.statementRows().get(3).sourceRow());

        assertEquals(1, result.unparsed().size());
        assertEquals(4, result.unparsed().get(0).sourceRow());
        assertEquals("Invalid amount", result.unparsed().get(0).reason());
    }

    @Test 
    void parseIgnoresByteOrderMark() {
        String csv = "\uFEFF" + """
        "Booking Date","Value Date","Partner Name","Partner Iban",Type,"Payment Reference","Account Name","Amount (EUR)","Original Amount","Original Currency","Exchange Rate"
        2026-03-01,2026-03-01,"Example Landlord",DE00000000000000000099,"Example Type","March rent landlord","Main Account",-800.00,,,
        """;

        ParsedStatement result = profile.parse(csv);

        assertEquals(1, result.statementRows().size());
        assertEquals(2, result.statementRows().get(0).sourceRow());
        assertEquals(0, result.unparsed().size());
    }

}