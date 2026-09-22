package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class StoryNodeService {

    private final StoryNodeRepository repo;

    public StoryNodeService(StoryNodeRepository repo) { this.repo = repo; }

    @Transactional
    public StoryNodeResponse create(StoryNodeRequest req) {
        if (repo.existsByNodeCode(req.nodeCode())) {
            throw ApiException.conflict("El nodeCode ya existe");
        }
        StoryNode n = new StoryNode();
        n.setNodeCode(req.nodeCode());
        n.setTitle(req.title());
        n.setSceneText(req.sceneText());
        n.setBranchCapacity(req.branchCapacity());
        n.setCurrentBranches(0);                       // SIEMPRE arranca en 0
        n.setPrimaryBranchCode(req.primaryBranchCode());
        n.setGlitchBranchCode(req.glitchBranchCode());
        n.setCreatedAt(Instant.now());
        return StoryNodeResponse.from(repo.save(n));
    }

    public List<StoryNodeResponse> listAll() {
        return repo.findAll().stream().map(StoryNodeResponse::from).toList();
    }

    public StoryNodeResponse getById(Long id) {
        return StoryNodeResponse.from(repo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Nodo no encontrado")));
    }
}
