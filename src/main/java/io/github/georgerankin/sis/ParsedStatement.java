package io.github.georgerankin.sis;

import java.util.List;

public record ParsedStatement(List<StatementRow> statementRows, List<Unparsed> unparsed) {
}
