package cz.humblej.squares.server.metadata;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetadataControllerTest {
    private static final Instant SERVER_TIME = Instant.parse("2026-07-21T12:34:56Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MetadataProperties properties = new MetadataProperties("v1", 1, "4.3.0", "1.0.0");
        Clock clock = Clock.fixed(SERVER_TIME, ZoneOffset.UTC);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MetadataController(properties, clock))
                .build();
    }

    @Test
    void returnsApiMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.currentRulesVersion").value(1))
                .andExpect(jsonPath("$.minimumWindowsVersion").value("4.3.0"))
                .andExpect(jsonPath("$.minimumAndroidVersion").value("1.0.0"))
                .andExpect(jsonPath("$.serverTime").value("2026-07-21T12:34:56Z"));
    }
}
