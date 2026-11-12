package io.github.georgerankin.sis;

public record BankAccount(
    long id,
    String accountName,
    String accountType,
    String iban) {
}
