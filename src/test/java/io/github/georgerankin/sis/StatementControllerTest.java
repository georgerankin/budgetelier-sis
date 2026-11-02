package io.github.georgerankin.sis;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest 
class StatementControllerTest {
    
    @Autowired 
    private MockMvc mockMvc;

    @Test
    void parseReturnsOkForValidRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "statement.csv", "text/csv", "fake csv".getBytes());
        MockMultipartFile context = new MockMultipartFile(
            "context", "", "application/json", "{}".getBytes());

        mockMvc.perform(multipart("/v1/statements/parse").file(file).file(context))
            .andExpect(status().isOk());
    }

    @Test
    void parseReturnsBadRequestWhenFileIsMissing() throws Exception {
        MockMultipartFile context = new MockMultipartFile(
            "context", "", "application/json", "{}".getBytes());
        mockMvc.perform(multipart("/v1/statements/parse").file(context))
            .andExpect(status().isBadRequest());
    }

    @Test 
    void parseReturnsBadRequestWhenContextIsMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "statement.csv", "text/csv", "fake csv".getBytes());
        mockMvc.perform(multipart("/v1/statements/parse").file(file))
            .andExpect(status().isBadRequest());
    }

    @Test 
    void parseReturnsTransactionsAndUnparsedLists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "statement.csv", "text/csv", "fake csv".getBytes());
        MockMultipartFile context = new MockMultipartFile(
            "context", "", "application/json", "{}".getBytes());

        mockMvc.perform(multipart("/v1/statements/parse").file(file).file(context))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions").isArray())
            .andExpect(jsonPath("$.unparsed").isArray());
    }

    @Test
    void parseReturnsTransactionWithContractFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", "fake csv".getBytes());
        MockMultipartFile context = new MockMultipartFile(
                "context", "", "application/json", "{}".getBytes());

        mockMvc.perform(multipart("/v1/statements/parse").file(file).file(context))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].source_row").value(2))
                .andExpect(jsonPath("$.transactions[0].amount").value("-800.00"))
                .andExpect(jsonPath("$.transactions[0].category_name").value("rent"))
                .andExpect(jsonPath("$.transactions[0].is_internal_transfer").value(false));
                
    }

    @Test
    void parseReturnsUnparsedWithContractFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", "fake csv".getBytes());
        MockMultipartFile context = new MockMultipartFile(
                "context", "", "application/json", "{}".getBytes());

        mockMvc.perform(multipart("/v1/statements/parse").file(file).file(context))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unparsed[0].source_row").value(4))
                .andExpect(jsonPath("$.unparsed[0].reason").value("Invalid amount"));
        }

}
