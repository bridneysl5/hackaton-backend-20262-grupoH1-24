package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private DecisionRepository decisionRepo;
    @Mock private PlaythroughRepository playthroughRepo;
    @Mock private StoryNodeRepository nodeRepo;
    @Mock private RealityLogRepository realityLogRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private ApplicationEventPublisher publisher;

    private DecisionService service;
    private User user;
    private Playthrough partida;
    private StoryNode nodoOrigen;

    @BeforeEach
    void setUp() {
        service = new DecisionService(decisionRepo, playthroughRepo, nodeRepo, realityLogRepo,
                new BranchClassifier(), currentUserService, publisher);

        user = new User();
        user.setId(1L);
        user.setEmail("ada@tuckersoft.test");
        user.setDisplayName("Ada Lovelace");
        user.setRole("ROLE_USER");

        nodoOrigen = new StoryNode();
        nodoOrigen.setId(10L);
        nodoOrigen.setNodeCode("NODE-CEREAL");
        nodoOrigen.setPrimaryBranchCode("NODE-BUS");
        nodoOrigen.setGlitchBranchCode("NODE-ESPEJO");

        partida = new Playthrough();
        partida.setId(100L);
        partida.setPlayerTag("STEFAN-01");
        partida.setUser(user);
        partida.setCurrentNode(nodoOrigen);
        partida.setStartNodeCode("NODE-CEREAL");
        partida.setLucidity(100);
        partida.setControlLevel(0);
        partida.setStatus("ACTIVA");
        partida.setCreatedAt(Instant.now());
        partida.setUpdatedAt(Instant.now());

        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(playthroughRepo.findById(100L)).thenReturn(Optional.of(partida));
        lenient().when(nodeRepo.findByNodeCode(anyString())).thenReturn(Optional.empty());
        lenient().when(decisionRepo.save(any(Decision.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(playthroughRepo.save(any(Playthrough.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("1 - 'Stefan destruye la camara' gana la regla 2: RUPTURA_CUARTA_PARED, no REBELDIA")
    void precedenciaDeReglas() {
        var req = new DecisionRequest(100L, "Stefan destruye la camara que lo estaba grabando.", "LEVE");

        var res = service.create(req, null);

        assertEquals("RUPTURA_CUARTA_PARED", res.branchType());
        assertEquals("Departamento Netflix", res.handlerUnit());
        assertEquals("BREAK_FOURTH_WALL", res.outcomeCode());
        // RUPTURA usa glitchBranchCode
        assertEquals("NODE-ESPEJO", res.resolvedNodeCode());
    }

    @Test
    @DisplayName("2 - Texto sin letras: ENTRADA_CORRUPTA, partida intacta y sin evento")
    void entradaCorruptaNoTocaLaPartida() {
        var req = new DecisionRequest(100L, "1234 5678 !!! ??? ***", "GRAVE");

        var res = service.create(req, null);

        assertEquals("ENTRADA_CORRUPTA", res.branchType());
        assertEquals("ERROR", res.status());
        assertNull(res.resolvedNodeCode());
        // La partida no se toca
        assertEquals(100, partida.getLucidity());
        assertEquals(0, partida.getControlLevel());
        assertEquals("ACTIVA", partida.getStatus());
        assertEquals(nodoOrigen, partida.getCurrentNode());
        verify(playthroughRepo, never()).save(any(Playthrough.class));
        verify(publisher, never()).publishEvent(any(DecisionCommittedEvent.class));
    }

    @Test
    @DisplayName("3 - CRITICO: lucidity -40 y controlLevel +45, dentro de los limites")
    void statsCritico() {
        var req = new DecisionRequest(100L, "Stefan acepta el trato y firma el contrato.", "CRITICO");

        service.create(req, null);

        assertEquals(60, partida.getLucidity());       // 100 - 40
        assertEquals(45, partida.getControlLevel());   // 0 + 45
        assertTrue(partida.getLucidity() >= 0 && partida.getLucidity() <= 100);
        assertTrue(partida.getControlLevel() >= 0 && partida.getControlLevel() <= 100);
    }

    @Test
    @DisplayName("4 - controlLevel >= 100 gana sobre lucidity <= 0: ENDING_PAC_SYMBOL")
    void ordenDeLosFinales() {
        partida.setLucidity(40);        // 40 - 40 = 0
        partida.setControlLevel(60);    // 60 + 45 = 105 -> 100

        var req = new DecisionRequest(100L, "Stefan acepta el trato y firma el contrato.", "CRITICO");
        service.create(req, null);

        assertEquals(0, partida.getLucidity());
        assertEquals(100, partida.getControlLevel());
        assertEquals("FINALIZADA", partida.getStatus());
        assertEquals("ENDING_PAC_SYMBOL", partida.getEndingCode());   // NO ENDING_WHITE_BEAR
    }

    @Test
    @DisplayName("5 - publishEvent: 1 vez en decision normal, 0 en ENTRADA_CORRUPTA")
    void publicacionDelEvento() {
        service.create(new DecisionRequest(100L, "Stefan acepta la oferta de Mohan.", "LEVE"), null);
        verify(publisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        reset(publisher);

        service.create(new DecisionRequest(100L, "((((( ))))) 00000 -----", "LEVE"), null);
        verify(publisher, never()).publishEvent(any(DecisionCommittedEvent.class));
    }
}
