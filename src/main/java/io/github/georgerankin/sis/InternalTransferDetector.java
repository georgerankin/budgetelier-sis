package io.github.georgerankin.sis;

import java.util.List;

class InternalTransferDetector {

    private static final String MATCHING_IBAN = "Partner IBAN matches one of your accounts";
    private static final String MATCHING_NAME = "Partner name matches one of your accounts";
    private static final TransferCheck NOT_INTERNAL = new TransferCheck(false, null, null);

    TransferCheck check(StatementRow row, List<BankAccount> accounts) {
        if (row.partnerIban() != null) {
            for (BankAccount account : accounts) {
                if (account.iban() != null && normalizeIban(account.iban()).equals(normalizeIban(row.partnerIban()))) {
                    return new TransferCheck(true, MATCHING_IBAN, account);
                }
            }
            return NOT_INTERNAL;
        }

        for (BankAccount account : accounts) {
            if (normalizeName(account.accountName()).equals(normalizeName(row.partnerName()))) {
                return new TransferCheck(true, MATCHING_NAME, account);
            }
        }

        return NOT_INTERNAL;
    }

    private String normalizeIban(String iban) {
        return iban.replace(" ", "").toUpperCase();
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase();
    }
}
