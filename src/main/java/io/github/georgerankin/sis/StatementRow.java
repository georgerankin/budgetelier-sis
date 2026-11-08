package io.github.georgerankin.sis;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatementRow(
    int sourceRow,
    LocalDate bookingDate,
    LocalDate valueDate,
    String partnerName,
    String partnerIban,
    String paymentReference,
    BigDecimal amount) {
}
