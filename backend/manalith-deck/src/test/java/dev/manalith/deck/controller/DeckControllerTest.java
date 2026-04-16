package dev.manalith.deck.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.deck.dto.*;
import dev.manalith.deck.exception.DeckNotFoundException;
import dev.manalith.deck.service.DeckExportService;
import dev.manalith.deck.service.DeckImportService;
import dev.manalith.deck.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC slice tests for {@link DeckController}.
 *
 * <p>All service dependencies are mocked via {@code @MockBean}.
 * Authentication is set up either via {@code @WithMockUser} (for endpoints that
 * only need a valid principal marker) or by manually injecting a UUID into the
 * {@link SecurityContextHolder} for endpoints that extract the principal as UUID.
 */
@WebMvcTest(DeckController.class)
class DeckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeckService deckService;

    @MockBean
    private DeckImportService deckImportService;

    @MockBean
    private DeckExportService deckExportService;

    private UUID userId;
    private UUID deckId;
    private DeckSummaryDTO sampleSummary;
    private DeckDetailDTO sampleDetail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();

        sampleSummary = new DeckSummaryDTO(
                deckId.toString(), userId.toString(), "TestUser",
                "My Deck", "standard", "A test deck",
                true, 60, 15,
                "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"
        );

        sampleDetail = new DeckDetailDTO(
                deckId.toString(), userId.toString(), "TestUser",
                "My Deck", "standard", "A test deck",
                true,
                "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z",
                List.of(), List.of(), List.of(),
                null
        );
    }

    /** Helper: set UUID as Security principal directly in the SecurityContext. */
    private void authenticateAs(UUID id) {
        var auth = new UsernamePasswordAuthenticationToken(id, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------------------------------------------------------------------------
    // GET /api/decks/public
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/decks/public — returns 200 with page content")
    void listPublicDecks_returnsOkWithPage() throws Exception {
        Page<DeckSummaryDTO> page = new PageImpl<>(List.of(sampleSummary));
        when(deckService.getPublicDecks(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/decks/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("My Deck"));
    }

    // -------------------------------------------------------------------------
    // GET /api/decks/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/decks/{id} — existing public deck returns 200")
    void getDeck_existingDeck_returns200() throws Exception {
        when(deckService.getDeckDetail(eq(deckId), any())).thenReturn(sampleDetail);

        mockMvc.perform(get("/api/decks/{id}", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Deck"));
    }

    @Test
    @DisplayName("GET /api/decks/{id} — non-existent deck returns 404")
    void getDeck_notFound_returns404() throws Exception {
        when(deckService.getDeckDetail(any(), any()))
                .thenThrow(new DeckNotFoundException(deckId));

        mockMvc.perform(get("/api/decks/{id}", deckId))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/decks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/decks — valid body returns 201 with Location header")
    @WithMockUser
    void createDeck_validBody_returns201WithLocation() throws Exception {
        authenticateAs(userId);

        when(deckService.createDeck(eq(userId), any(CreateDeckRequest.class))).thenReturn(sampleSummary);

        String body = objectMapper.writeValueAsString(
                new CreateDeckRequest("My Deck", "standard", "A test deck", true));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/decks/" + deckId))
                .andExpect(jsonPath("$.name").value("My Deck"));
    }

    @Test
    @DisplayName("POST /api/decks — blank name returns 400")
    @WithMockUser
    void createDeck_blankName_returns400() throws Exception {
        authenticateAs(userId);

        // name is blank — violates @NotBlank
        String body = """
                {"name": "", "format": "standard", "description": null, "isPublic": false}
                """;

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/decks/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/decks/{id} — returns 204")
    @WithMockUser
    void deleteDeck_returns204() throws Exception {
        authenticateAs(userId);

        doNothing().when(deckService).deleteDeck(any(), any());

        mockMvc.perform(delete("/api/decks/{id}", deckId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // POST /api/decks/{id}/export
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/decks/{id}/export?format=text — returns 200 with text/plain")
    void exportDeck_textFormat_returnsPlainText() throws Exception {
        when(deckExportService.export(eq(deckId), any(), eq("text")))
                .thenReturn("// My Deck\n4 Lightning Bolt\n");

        mockMvc.perform(post("/api/decks/{id}/export", deckId)
                        .param("format", "text")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lightning Bolt")));
    }

    // -------------------------------------------------------------------------
    // POST /api/decks/import
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/decks/import — returns 201 with deck detail")
    @WithMockUser
    void importDeck_returns201() throws Exception {
        authenticateAs(userId);

        when(deckImportService.importDeck(eq(userId), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(sampleDetail);

        String body = objectMapper.writeValueAsString(
                new DeckController.ImportDeckRequest(
                        "My Deck", "standard", "4 Lightning Bolt\n", "text"));

        mockMvc.perform(post("/api/decks/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Deck"));
    }
}
