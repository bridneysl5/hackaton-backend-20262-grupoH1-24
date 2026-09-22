package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.*;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionService {

    private final DecisionRepository decisionRepo;
    private final PlaythroughRepository playthroughRepo;
    private final StoryNodeRepository nodeRepo;
    private final RealityLogRepository realityLogRepo;
    private final BranchClassifier classifier;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher publisher;

    public DecisionService(DecisionRepository decisionRepo, PlaythroughRepository playthroughRepo,
                           StoryNodeRepository nodeRepo, RealityLogRepository realityLogRepo,
                           BranchClassifier classifier, CurrentUserService currentUserService,
                           ApplicationEventPublisher publisher) {
        this.decisionRepo = decisionRepo;
        this.playthroughRepo = playthroughRepo;
        this.nodeRepo = nodeRepo;
        this.realityLogRepo = realityLogRepo;
        this.classifier = classifier;
        this.currentUserService = currentUserService;
        this.publisher = publisher;
    }

    @Transactional
    public DecisionResponse create(DecisionRequest req, String simulateHeader) {
        // 1. Usuario del token + la partida tiene que ser SUYA (el admin ajeno tambien recibe 403)
        User me = currentUserService.getCurrentUser();
        Playthrough p = playthroughRepo.findById(req.playthroughId())
                .orElseThrow(() -> ApiException.notFound("Partida no encontrada"));
        if (!p.getUser().getId().equals(me.getId())) {
            throw ApiException.forbidden("No puedes decidir sobre una partida ajena");
        }

        // 2. La partida tiene que estar ACTIVA
        if (!"ACTIVA".equals(p.getStatus())) {
            throw ApiException.conflict("La partida ya esta FINALIZADA");
        }

        Instant now = Instant.now();
        StoryNode sourceNode = p.getCurrentNode();

        // 3. Clasificar y derivar
        String branchType = classifier.clasificar(req.rawInput());
        String handlerUnit = classifier.handlerUnit(branchType);
        String outcomeCode = classifier.outcomeCode(branchType);

        Decision d = new Decision();
        d.setPlaythrough(p);
        d.setNode(sourceNode);
        d.setRawInput(req.rawInput());
        d.setBranchType(branchType);
        d.setImpactLevel(req.impactLevel());
        d.setHandlerUnit(handlerUnit);
        d.setOutcomeCode(outcomeCode);
        d.setCreatedAt(now);
        d.setUpdatedAt(now);

        // 4. ENTRADA_CORRUPTA: se guarda y NADA MAS. Sin tocar la partida, sin evento. 201.
        if ("ENTRADA_CORRUPTA".equals(branchType)) {
            d.setResolvedNodeCode(null);
            d.setStatus("ERROR");
            return DecisionResponse.from(decisionRepo.save(d));
        }

        // 5a. Stats segun el impacto, con limites 0..100
        int lucidity = p.getLucidity();
        int control = p.getControlLevel();
        switch (req.impactLevel()) {
            case "LEVE"      -> { lucidity -= 5;  control += 5;  }
            case "MODERADO"  -> { lucidity -= 15; control += 10; }
            case "GRAVE"     -> { lucidity -= 30; control += 20; }
            case "CRITICO"   -> { lucidity -= 40; control += 45; }
            default -> throw ApiException.badRequest("impactLevel invalido");
        }
        lucidity = Math.max(0, Math.min(100, lucidity));
        control  = Math.max(0, Math.min(100, control));
        p.setLucidity(lucidity);
        p.setControlLevel(control);

        // 5b. Nodo destino: RUPTURA_CUARTA_PARED o CRITICO -> glitch; el resto -> primary
        String destino = null;
        if (sourceNode != null) {
            destino = ("RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(req.impactLevel()))
                    ? sourceNode.getGlitchBranchCode()
                    : sourceNode.getPrimaryBranchCode();
        }
        d.setResolvedNodeCode(destino);   // se guarda aunque el nodo no exista

        // 5c. Estado de la partida EN ESTE ORDEN EXACTO
        StoryNode destinoNode = (destino == null) ? null : nodeRepo.findByNodeCode(destino).orElse(null);

        if (control >= 100) {                       // 1
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_PAC_SYMBOL");
        } else if (lucidity <= 0) {                 // 2
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_WHITE_BEAR");
        } else if (destinoNode == null) {           // 3
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_NETFLIX_CUT");
        } else {                                    // 4
            p.setCurrentNode(destinoNode);          // solo aqui se mueve
        }
        p.setUpdatedAt(Instant.now());

        // 6. Guardar la partida
        playthroughRepo.save(p);

        // 7. Guardar la decision
        d.setStatus("REGISTRADA");
        Decision saved = decisionRepo.save(d);

        // 8. Publicar el evento (el correo sale DESPUES del commit, en otro hilo)
        publisher.publishEvent(new DecisionCommittedEvent(
                saved.getId(),
                p.getUser().getEmail(),
                p.getUser().getDisplayName(),
                p.getPlayerTag(),
                branchType, req.impactLevel(), handlerUnit, outcomeCode,
                sourceNode == null ? null : sourceNode.getNodeCode(),
                destino,
                p.getStatus(), p.getLucidity(), p.getControlLevel(), p.getEndingCode(),
                saved.getRawInput(), saved.getCreatedAt(),
                "MAIL_FAILURE".equals(simulateHeader)
        ));

        return DecisionResponse.from(saved);
    }

    public PageResponse<DecisionResponse> list(String branchType, String impactLevel, String status,
                                               Long playthroughId, int page, int size) {
        User me = currentUserService.getCurrentUser();
        boolean admin = currentUserService.isAdmin(me);

        Specification<Decision> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!admin) ps.add(cb.equal(root.get("playthrough").get("user").get("id"), me.getId()));
            if (branchType != null)    ps.add(cb.equal(root.get("branchType"), branchType));
            if (impactLevel != null)   ps.add(cb.equal(root.get("impactLevel"), impactLevel));
            if (status != null)        ps.add(cb.equal(root.get("status"), status));
            if (playthroughId != null) ps.add(cb.equal(root.get("playthrough").get("id"), playthroughId));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        // Paginacion 0-BASED
        var pageResult = decisionRepo.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult, DecisionResponse::from);
    }

    public DecisionResponse getById(Long id) {
        return DecisionResponse.from(findReadable(id));
    }

    public List<RealityLogResponse> realityLogs(Long decisionId) {
        Decision d = findReadable(decisionId);
        return realityLogRepo.findByDecisionIdOrderByCreatedAtAsc(d.getId())
                .stream().map(RealityLogResponse::from).toList();
    }

    // LECTURA: dueno o admin
    private Decision findReadable(Long id) {
        Decision d = decisionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Decision no encontrada"));
        User me = currentUserService.getCurrentUser();
        if (!d.getPlaythrough().getUser().getId().equals(me.getId()) && !currentUserService.isAdmin(me)) {
            throw ApiException.forbidden("Esa decision no es tuya");
        }
        return d;
    }
}
