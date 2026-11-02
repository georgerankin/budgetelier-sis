package io.github.georgerankin.sis;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController 
public class StatementController {
    
    @PostMapping("/v1/statements/parse") 
    public ParseResponse parse(@RequestPart("file") MultipartFile file, @RequestPart("context") MultipartFile context) {
        // Implementation for parsing the statement
        return new ParseResponse(
            List.of(new Transaction(
                2, "2026-03-01", "2026-03-01", "Example Landlord",
                "DE00000000000000000099", "March rent landlord", "-800.00",
                4L, "rent", "landlord", "expenses", 
                false, null, "placeholder-fingerprint-1")),
            List.of(new Unparsed(
                4, "Invalid amount")));
    }
}
