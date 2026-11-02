package io.github.georgerankin.sis;

import java.util.List;

public record ParseResponse(List<Transaction> transactions, List<Unparsed> unparsed) {
}
