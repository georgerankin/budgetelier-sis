package io.github.georgerankin.sis;

interface BankProfile {
    ParsedStatement parse(String csvText);
}
