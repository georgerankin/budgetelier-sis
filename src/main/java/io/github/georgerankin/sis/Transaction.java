package io.github.georgerankin.sis;

public record Transaction(
    int sourceRow,
    String bookingDate,
    String valueDate,
    String partnerName,
    String partnerIban,
    String paymentReference,
    String amount,
    Long suggestedCategoryId,
    String categoryName,
    String matchedKeyword,
    String suggestedType,
    boolean isInternalTransfer,
    String internalTransferReason,
    String fingerprint) {
}

