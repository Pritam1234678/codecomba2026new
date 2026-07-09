package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.*;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.repository.*;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.*;
import com.example.codecombat2026.security.services.*;
import com.example.codecombat2026.service.InviteTokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PrivateContestInviteController
 * 
 * Requirements: 5.2, 5.5, 5.6, 6.1, 6.2, 7.1, 7.3, 7.4
 */
@WebMvcTest(controllers = PrivateContestInviteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
class PrivateContestInviteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InviteTokenService inviteTokenService;

    @MockBean
    private PrivateContestRepository privateContestRepository;

    @MockBean
    private PrivateContestParticipantRepository participantRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt entryPoint;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private UserDetailsImpl testUser;
    private UserDetailsImpl hostUser;
    private User testUserEntity;
    private User hostUserEntity;
    private Contest testContest;
    private PrivateContest privateContest;
    private PrivateContestInvitation invitation;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new UserDetailsImpl(55L, "testuser", "test@example.com", "password", Collections.emptyList());
        hostUser = new UserDetailsImpl(42L, "hostuser", "host@example.com", "password", Collections.emptyList());

        testUserEntity = new User();
        testUserEntity.setId(55L);
        testUserEntity.setUsername("testuser");
        testUserEntity.setEmail("test@example.com");
        testUserEntity.setFullName("Test User");

        hostUserEntity = new User();
        hostUserEntity.setId(42L);
        hostUserEntity.setUsername("hostuser");
        hostUserEntity.setEmail("host@example.com");
        hostUserEntity.setFullName("Host User");

        testContest = new Contest();
        testContest.setId(501L);
        testContest.setName("CS101 Midterm Exam");
        testContest.setDescription("Data structures assessment");
        testContest.setStartTime(LocalDateTime.of(2026, 2, 1, 14, 0));
        testContest.setEndTime(LocalDateTime.of(2026, 2, 1, 17, 0));
        testContest.setStatus(Contest.ContestStatus.UPCOMING);

        privateContest = new PrivateContest();
        privateContest.setId(101L);
        privateContest.setContest(testContest);
        privateContest.setHostUser(hostUserEntity);
        privateContest.setEnableProctoring(false);
        privateContest.setCancelled(false);

        invitation = new PrivateContestInvitation();
        invitation.setId(1L);
        invitation.setContest(testContest);
        invitation.setToken("validToken123");
        invitation.setExpiresAt(LocalDateTime.of(2026, 3, 3, 14, 0));
        invitation.setInvalidated(false);

        doAnswer(inv -> {
            inv.getArgument(2, jakarta.servlet.FilterChain.class).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(authTokenFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Preview contest with valid token")
    void previewContest_Success() throws Exception {
        when(inviteTokenService.validateToken("validToken123")).thenReturn(Optional.of(invitation));
        when(privateContestRepository.findByContestId(501L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.countByContestId(501L)).thenReturn(35L);

        mvc.perform(get("/api/contests/private/join").param("token", "validToken123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contestName", is("CS101 Midterm Exam")))
            .andExpect(jsonPath("$.hostUsername", is("hostuser")))
            .andExpect(jsonPath("$.participantCount", is(35)))
            .andExpect(jsonPath("$.tokenValid", is(true)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Join contest with valid token")
    void joinContest_Success() throws Exception {
        when(inviteTokenService.validateToken("validToken123")).thenReturn(Optional.of(invitation));
        when(privateContestRepository.findByContestId(501L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.existsByContestIdAndUserId(501L, 55L)).thenReturn(false);
        when(participantRepository.countByContestId(501L)).thenReturn(35L);
        when(userRepository.findById(55L)).thenReturn(Optional.of(testUserEntity));
        when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/contests/private/join")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"validToken123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contestId", is(501)));
    }

    @Test
    @WithMockUser(username = "hostuser", roles = "USER")
    @DisplayName("Regenerate token as host")
    void regenerateToken_Success() throws Exception {
        PrivateContestInvitation newInv = new PrivateContestInvitation();
        newInv.setToken("newToken456");
        newInv.setExpiresAt(LocalDateTime.of(2026, 3, 15, 14, 0));

        when(privateContestRepository.findByContestId(501L)).thenReturn(Optional.of(privateContest));
        when(inviteTokenService.createInvitation(any(), any())).thenReturn(newInv);

        mvc.perform(post("/api/contests/private/501/invite/regenerate").with(user(hostUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", is("newToken456")));
    }

    @Test
    @WithMockUser(username = "hostuser", roles = "USER")
    @DisplayName("List participants as host")
    void listParticipants_Success() throws Exception {
        PrivateContestParticipant p = new PrivateContestParticipant();
        p.setContest(testContest);
        p.setUser(testUserEntity);

        when(privateContestRepository.findByContestId(501L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestId(501L)).thenReturn(Arrays.asList(p));

        mvc.perform(get("/api/contests/private/501/participants").with(user(hostUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contestId", is(501)));
    }

    @Test
    @WithMockUser(username = "hostuser", roles = "USER")
    @DisplayName("Remove participant as host")
    void removeParticipant_Success() throws Exception {
        PrivateContestParticipant p = new PrivateContestParticipant();
        p.setContest(testContest);
        p.setUser(testUserEntity);

        when(privateContestRepository.findByContestId(501L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(501L, 55L)).thenReturn(Optional.of(p));

        mvc.perform(delete("/api/contests/private/501/participants/55").with(user(hostUser)))
            .andExpect(status().isNoContent());

        verify(participantRepository).delete(p);
    }
}
