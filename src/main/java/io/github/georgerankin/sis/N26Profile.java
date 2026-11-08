package io.github.georgerankin.sis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.time.format.DateTimeParseException;

class N26Profile implements BankProfile {

    private static final String BOOKING_DATE = "Booking Date";
    private static final String VALUE_DATE = "Value Date";
    private static final String PARTNER_NAME = "Partner Name";
    private static final String PARTNER_IBAN = "Partner Iban";
    private static final String PAYMENT_REFERENCE = "Payment Reference";
    private static final String AMOUNT_EUR = "Amount (EUR)";
    
    void checkHeader(List<String> header) {
        List<String> required = List.of(
            BOOKING_DATE,
            VALUE_DATE,
            PARTNER_NAME,
            PARTNER_IBAN,
            PAYMENT_REFERENCE,
            AMOUNT_EUR);
        List<String> missing = new ArrayList<>();

        for (String column : required) {
            if (!header.contains(column)) {
                missing.add(column);
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidStatementException(
                "The CSV is missing required columns: " + String.join(", ", missing)
            );
        }
    }


    BigDecimal parseAmount(String text) {
        if (!text.matches("-?\\d+(\\.\\d{1,2})?")) {
            throw new NumberFormatException("Invalid amount");
        }
        return new BigDecimal(text).setScale(2);
    }

    LocalDate parseDate(String text) {
        return LocalDate.parse(text);
    }

    StatementRow parseRow(int sourceRow, Map<String, String> fields) {
        return new StatementRow(
            sourceRow,
            parseDate(fields.get(BOOKING_DATE)),
            parseDate(fields.get(VALUE_DATE)),
            fields.get(PARTNER_NAME),
            turnBlankTextToNull(fields.get(PARTNER_IBAN)),
            fields.get(PAYMENT_REFERENCE),
            parseAmount(fields.get(AMOUNT_EUR))
        );
    }

    @Override
    public ParsedStatement parse(String csvText) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader() // treat the first line as the header
            .setSkipHeaderRecord(true) //...and don't return it as a data row
            .get();

        List<StatementRow> rows = new ArrayList<>();
        List<Unparsed> unparsed = new ArrayList<>();

        if (csvText.startsWith("\uFEFF"))
            {
                csvText = csvText.substring(1);
            }

        try (CSVParser parser = CSVParser.parse(csvText, format)) {
            
            checkHeader(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                int sourceRow = (int) record.getRecordNumber() + 1;
                try {
                    rows.add(parseRow(sourceRow, record.toMap()));
                } catch (NumberFormatException e) {
                    unparsed.add(new Unparsed(sourceRow, "Invalid amount"));
                } catch (DateTimeParseException e) {
                    unparsed.add(new Unparsed(sourceRow, "Invalid date"));
                }
            }
        } catch (IOException e) {
            throw new InvalidStatementException("The CSV could not be read");
        }

        return new ParsedStatement(rows, unparsed);
    }

    private String turnBlankTextToNull(String text) {
        return text.isBlank() ? null : text;
    }

}
