package com.recred.backend.web;

import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockCommand;
import com.recred.backend.infrastructure.web.BulkStockController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BulkStockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ParseBulkStock parseBulkStock;

    @InjectMocks
    private BulkStockController bulkStockController;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(bulkStockController).build();
    }

    @Test
    void uploadFile_ShouldReturnOk_WhenFileIsProvided() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        when(parseBulkStock.execute(any(ParseBulkStockCommand.class)))
                .thenReturn(new BulkStockParseResponse(List.of()));

        mockMvc.perform(multipart("/api/products/bulk-upload")
                        .file(file))
                .andExpect(status().isOk());
    }
}
