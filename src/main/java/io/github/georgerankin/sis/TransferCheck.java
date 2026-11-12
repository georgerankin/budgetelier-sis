package io.github.georgerankin.sis;

public record TransferCheck(
    boolean isInternalTransfer,
    String reason,
    BankAccount matchedAccount) {
    
}
