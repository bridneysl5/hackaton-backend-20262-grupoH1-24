package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.*;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepo;
    private final StoryNodeRepository nodeRepo;
    private final DecisionRepository decisionRepo;
    private final CurrentUserService currentUserService;

    public PlaythroughService(PlaythroughRepository playthroughRepo, StoryNodeRepository nodeRepo,
                              DecisionRepository decisionRepo, CurrentUserService currentUserService) {
        this.playthroughRepo = playthroughRepo;
        this.nodeRepo = nodeRepo;
        this.decisionRepo = decisionRepo;
        this.currentUserService = currentUserService;
    }

    // Los 7 pasos del enunciado, en orden.
    @Transactional
    public PlaythroughResponse create(PlaythroughRequest req) {
        User owner = currentUserService.getCurrentUser();                      // 1

        StoryNode node = nodeRepo.findByNodeCode(req.startNodeCode())           // 2
                .orElseThrow(() -> ApiException.notFound("El startNodeCode no existe"));

        if (playthroughRepo.existsByPlayerTag(req.playerTag())) {               // 3
            throw ApiException.conflict("El playerTag ya existe");
        }
        if (node.getCurrentBranches() >= node.getBranchCapacity()) {            // 4  -> 400
            throw ApiException.badRequest("El nodo esta lleno");
        }

        Instant now = Instant.now();
        Playthrough p = new Playthrough();                                      // 5
        p.setPlayerTag(req.playerTag());
        p.setUser(owner);
        p.setCurrentNode(node);
        p.setStartNodeCode(node.getNodeCode());
        p.setLucidity(100);
        p.setControlLevel(0);
        p.setStatus("ACTIVA");
        p.setEndingCode(null);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);

        node.setCurrentBranches(node.getCurrentBranches() + 1);                 // 6
        nodeRepo.save(node);

        return PlaythroughResponse.from(playthroughRepo.save(p));               // 7
    }

    public List<PlaythroughResponse> list() {
        User me = currentUserService.getCurrentUser();
        List<Playthrough> result = currentUserService.isAdmin(me)
                ? playthroughRepo.findAllByOrderByCreatedAtDesc()   // ADMIN ve TODAS
                : playthroughRepo.findByUserOrderByCreatedAtDesc(me);
        return result.stream().map(PlaythroughResponse::from).toList();
    }

    public PlaythroughResponse getById(Long id) {
        return PlaythroughResponse.from(findReadable(id));
    }

    public PathResponse getPath(Long id) {
        Playthrough p = findReadable(id);
        var decisions = decisionRepo
                .findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(p.getId());

        List<PathStep> steps = new ArrayList<>();
        int order = 1;
        for (var d : decisions) {
            steps.add(new PathStep(order++, d.getId(),
                    d.getNode() == null ? null : d.getNode().getNodeCode(),
                    d.getResolvedNodeCode(), d.getBranchType(), d.getImpactLevel(), d.getCreatedAt()));
        }
        return new PathResponse(p.getId(), p.getPlayerTag(), p.getStatus(), p.getEndingCode(),
                p.getStartNodeCode(),
                p.getCurrentNode() == null ? null : p.getCurrentNode().getNodeCode(),
                steps);
    }

    // LECTURA: el dueno o el admin (el admin supervisa).
    public Playthrough findReadable(Long id) {
        Playthrough p = playthroughRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Partida no encontrada"));
        User me = currentUserService.getCurrentUser();
        if (!p.getUser().getId().equals(me.getId()) && !currentUserService.isAdmin(me)) {
            throw ApiException.forbidden("Esa partida no es tuya");
        }
        return p;
    }
}
