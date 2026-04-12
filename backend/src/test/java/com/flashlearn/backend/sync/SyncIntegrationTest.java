package com.flashlearn.backend.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashlearn.backend.auth.LoginRequest;
import com.flashlearn.backend.auth.LoginResponse;
import com.flashlearn.backend.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    // ── helpers ──────────────────────────────────────────────────────────────

    private String registerAndLogin(String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class).getAccessToken();
    }

    private SyncPushRequest buildPushRequest(List<SyncDeckDTO> decks, List<SyncFlashcardDTO> flashcards) {
        SyncPushRequest req = new SyncPushRequest();
        req.setClientTimestamp(LocalDateTime.now().minusMinutes(5));
        req.setDecks(decks);
        req.setFlashcards(flashcards);
        return req;
    }

    private SyncDeckDTO newDeck(String title) {
        SyncDeckDTO dto = new SyncDeckDTO();
        dto.setTitle(title);
        dto.setDescription("desc");
        dto.setPublic(false);
        dto.setUpdatedAt(LocalDateTime.now().minusMinutes(10));
        return dto;
    }

    @BeforeEach
    void setUp() throws Exception {
        accessToken = registerAndLogin("user@test.com", "password123");
    }

    // ── POST /sync/push ───────────────────────────────────────────────────────

    @Test
    void push_noAuth_returns401() throws Exception {
        SyncPushRequest req = buildPushRequest(List.of(), List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void push_emptyLists_returns200WithZeroCounts() throws Exception {
        SyncPushRequest req = buildPushRequest(List.of(), List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decksProcessed").value(0))
                .andExpect(jsonPath("$.flashcardsProcessed").value(0))
                .andExpect(jsonPath("$.conflicts").isArray())
                .andExpect(jsonPath("$.serverTimestamp").exists());
    }

    @Test
    void push_newDeck_returns200AndDeckIsCreated() throws Exception {
        SyncPushRequest req = buildPushRequest(List.of(newDeck("My Deck")), List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decksProcessed").value(1))
                .andExpect(jsonPath("$.conflicts").isEmpty());
    }

    @Test
    void push_missingClientTimestamp_returns400() throws Exception {
        SyncPushRequest req = new SyncPushRequest();
        // clientTimestamp intentionally null

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push_deckWithBlankTitle_returns400() throws Exception {
        SyncDeckDTO bad = new SyncDeckDTO();
        bad.setTitle("");
        bad.setUpdatedAt(LocalDateTime.now());

        SyncPushRequest req = buildPushRequest(List.of(bad), List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push_updateOwnDeck_noConflict() throws Exception {
        // create deck
        SyncPushRequest createReq = buildPushRequest(List.of(newDeck("Original")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // pull to get the assigned server id
        MvcResult pullResult = mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        SyncPullResponse pulled = objectMapper.readValue(
                pullResult.getResponse().getContentAsString(), SyncPullResponse.class);
        Long deckId = pulled.getDecks().get(0).getId();

        // update that deck with its id — clientTimestamp after server save → no conflict
        SyncDeckDTO update = newDeck("Updated");
        update.setId(deckId);
        update.setUpdatedAt(LocalDateTime.now());

        SyncPushRequest updateReq = new SyncPushRequest();
        updateReq.setClientTimestamp(LocalDateTime.now().plusSeconds(1));
        updateReq.setDecks(List.of(update));
        updateReq.setFlashcards(List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicts").isEmpty());
    }

    @Test
    void push_updateAnotherUsersDeck_returns403() throws Exception {
        // userA creates a deck
        String userA = registerAndLogin("userA@test.com", "password123");
        SyncPushRequest createReq = buildPushRequest(List.of(newDeck("A's deck")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userA)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // get deck id
        MvcResult pullResult = mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + userA))
                .andReturn();
        SyncPullResponse pulled = objectMapper.readValue(
                pullResult.getResponse().getContentAsString(), SyncPullResponse.class);
        Long deckId = pulled.getDecks().get(0).getId();

        // userB tries to update it
        String userB = registerAndLogin("userB@test.com", "password123");
        SyncDeckDTO stolen = newDeck("B stole it");
        stolen.setId(deckId);
        stolen.setUpdatedAt(LocalDateTime.now());

        SyncPushRequest stealReq = buildPushRequest(List.of(stolen), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userB)
                        .content(objectMapper.writeValueAsString(stealReq)))
                .andExpect(status().isForbidden());
    }

    // ── GET /sync/pull ────────────────────────────────────────────────────────

    @Test
    void pull_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pull_noData_returnsEmptyLists() throws Exception {
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks").isArray())
                .andExpect(jsonPath("$.flashcards").isArray())
                .andExpect(jsonPath("$.decks").isEmpty())
                .andExpect(jsonPath("$.serverTimestamp").exists());
    }

    @Test
    void pull_afterPush_returnsDeck() throws Exception {
        SyncPushRequest req = buildPushRequest(List.of(newDeck("Pull Test")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks[0].title").value("Pull Test"))
                .andExpect(jsonPath("$.totalDecks").value(1));
    }

    @Test
    void pull_isolatesDataBetweenUsers() throws Exception {
        // userA pushes a deck
        String userA = registerAndLogin("isolA@test.com", "password123");
        SyncPushRequest req = buildPushRequest(List.of(newDeck("A private")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userA)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // userB should see nothing
        String userB = registerAndLogin("isolB@test.com", "password123");
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + userB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDecks").value(0));
    }

    @Test
    void pull_pagination_defaultPageIsZero() throws Exception {
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(50));
    }

    @Test
    void pull_futureSince_returnsEmpty() throws Exception {
        SyncPushRequest req = buildPushRequest(List.of(newDeck("Old Deck")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // ask for changes after far future — should return nothing
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2099-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDecks").value(0));
    }

    // ── last-write-wins ───────────────────────────────────────────────────────

    @Test
    void push_conflict_clientNewerThanServer_clientWins() throws Exception {
        SyncPushRequest createReq = buildPushRequest(List.of(newDeck("Original")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        MvcResult pullResult = mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        Long deckId = objectMapper.readValue(
                pullResult.getResponse().getContentAsString(), SyncPullResponse.class)
                .getDecks().get(0).getId();

        // klient edytował później niż serwer → client-wins
        SyncDeckDTO update = newDeck("Client Version");
        update.setId(deckId);
        update.setUpdatedAt(LocalDateTime.now().plusSeconds(5));

        SyncPushRequest conflictReq = new SyncPushRequest();
        conflictReq.setClientTimestamp(LocalDateTime.now().minusMinutes(10));
        conflictReq.setDecks(List.of(update));
        conflictReq.setFlashcards(List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicts[0]").value(
                        org.hamcrest.Matchers.containsString("client-wins")));

        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks[0].title").value("Client Version"));
    }

    @Test
    void push_conflict_serverNewerThanClient_serverWins() throws Exception {
        SyncPushRequest createReq = buildPushRequest(List.of(newDeck("Server Version")), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        MvcResult pullResult = mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        Long deckId = objectMapper.readValue(
                pullResult.getResponse().getContentAsString(), SyncPullResponse.class)
                .getDecks().get(0).getId();

        // klient edytował wcześniej niż serwer → server-wins
        SyncDeckDTO update = newDeck("Client Old Version");
        update.setId(deckId);
        update.setUpdatedAt(LocalDateTime.now().minusMinutes(20));

        SyncPushRequest conflictReq = new SyncPushRequest();
        conflictReq.setClientTimestamp(LocalDateTime.now().minusMinutes(10));
        conflictReq.setDecks(List.of(update));
        conflictReq.setFlashcards(List.of());

        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicts[0]").value(
                        org.hamcrest.Matchers.containsString("server-wins")));

        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks[0].title").value("Server Version"));
    }

    // ── walidacja rozmiarów ───────────────────────────────────────────────────

    @Test
    void push_deckTitleTooLong_returns400() throws Exception {
        SyncDeckDTO deck = new SyncDeckDTO();
        deck.setTitle("A".repeat(101));
        deck.setUpdatedAt(LocalDateTime.now());

        SyncPushRequest req = buildPushRequest(List.of(deck), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push_deckDescriptionTooLong_returns400() throws Exception {
        SyncDeckDTO deck = newDeck("Valid Title");
        deck.setDescription("D".repeat(501));

        SyncPushRequest req = buildPushRequest(List.of(deck), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push_flashcardQuestionTooLong_returns400() throws Exception {
        SyncFlashcardDTO flashcard = new SyncFlashcardDTO();
        flashcard.setQuestion("Q".repeat(501));
        flashcard.setAnswer("A");
        flashcard.setUpdatedAt(LocalDateTime.now());

        SyncPushRequest req = buildPushRequest(List.of(), List.of(flashcard));
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── sanityzacja HTML ──────────────────────────────────────────────────────

    @Test
    void push_htmlInDeckTitle_isSanitized() throws Exception {
        SyncDeckDTO deck = newDeck("<b>Bold</b>");

        SyncPushRequest req = buildPushRequest(List.of(deck), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // pull and verify HTML was stripped
        mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks[0].title").value("Bold"));
    }

    @Test
    void push_scriptTagInFlashcard_isSanitized() throws Exception {
        SyncDeckDTO deck = newDeck("Deck");
        SyncPushRequest deckReq = buildPushRequest(List.of(deck), List.of());
        mockMvc.perform(post("/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(deckReq)))
                .andExpect(status().isOk());

        // get deck id
        MvcResult pullResult = mockMvc.perform(get("/sync/pull")
                        .param("since", "2000-01-01T00:00:00")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        Long deckId = objectMapper.readValue(
                pullResult.getResponse().getContentAsString(), SyncPullResponse.class)
                .getDecks().get(0).getId();

        // push flashcard via /decks endpoint to get an id
        String flashcardBody = """
                {"question":"<script>alert('xss')</script>What?","answer":"<i>Answer</i>"}
                """;
        MvcResult fcResult = mockMvc.perform(post("/decks/{id}/flashcards", deckId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(flashcardBody))
                .andExpect(status().isCreated())
                .andReturn();

        String question = objectMapper.readTree(fcResult.getResponse().getContentAsString())
                .get("question").asText();
        String answer = objectMapper.readTree(fcResult.getResponse().getContentAsString())
                .get("answer").asText();

        assert !question.contains("<script>") : "script tag should be stripped";
        assert !answer.contains("<i>") : "HTML tags should be stripped";
    }
}
