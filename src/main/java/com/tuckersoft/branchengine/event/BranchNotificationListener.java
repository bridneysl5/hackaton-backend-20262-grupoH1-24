package com.tuckersoft.branchengine.event;

import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

// Clase SEPARADA del DecisionService. El service nunca inyecta JavaMailSender.
@Component
public class BranchNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BranchNotificationListener.class);

    private final JavaMailSender mailSender;
    private final DecisionRepository decisionRepo;
    private final RealityLogRepository realityLogRepo;

    public BranchNotificationListener(JavaMailSender mailSender, DecisionRepository decisionRepo,
                                      RealityLogRepository realityLogRepo) {
        this.mailSender = mailSender;
        this.decisionRepo = decisionRepo;
        this.realityLogRepo = realityLogRepo;
    }

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)   // SIN esto la app no arranca
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent e) {
        String subject = "[TUCKERSOFT] " + e.branchType() + " en " + e.playerTag()
                + " | Impacto " + e.impactLevel();
        String statusFinal;

        Decision d = decisionRepo.findById(e.decisionId()).orElse(null);
        if (d == null) {
            log.error("[BRANCH] Decision {} no encontrada tras el commit", e.decisionId());
            return;
        }

        // 7. PROCESANDO
        d.setStatus("PROCESANDO");
        d.setUpdatedAt(Instant.now());
        decisionRepo.save(d);

        RealityLog rl = new RealityLog();
        rl.setDecision(d);
        rl.setRecipientEmail(e.recipientEmail());
        rl.setSubject(subject);
        rl.setCreatedAt(Instant.now());

        try {
            // 8. Envio real. El Modo QA lanza una excepcion REAL aqui dentro.
            enviarCorreo(e, subject);

            d.setStatus("ESTABILIZADA");
            rl.setLogStatus("SENT");
            rl.setSentAt(Instant.now());
            rl.setErrorMessage(null);
            statusFinal = "ESTABILIZADA";
        } catch (Exception ex) {
            d.setStatus("ERROR");
            rl.setLogStatus("FAILED");
            rl.setSentAt(null);
            rl.setErrorMessage(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            statusFinal = "ERROR";
            log.error("[BRANCH] Fallo el envio del Informe de Realidad de la decision {}: {}",
                    e.decisionId(), ex.getMessage());
        }

        d.setUpdatedAt(Instant.now());
        decisionRepo.save(d);
        realityLogRepo.save(rl);

        // 9. Log obligatorio en consola. El hilo debe ser branch-worker-X.
        System.out.println("[BRANCH-LOG] Decision ID: " + e.decisionId()
                + " | Player: " + e.playerTag()
                + " | Branch: " + e.branchType()
                + " | Impact: " + e.impactLevel()
                + " | Unit: " + e.handlerUnit()
                + " | Node: " + e.sourceNodeCode() + " -> " + e.resolvedNodeCode()
                + " | Thread: " + Thread.currentThread().getName()
                + " | Status: " + statusFinal);
    }

    private void enviarCorreo(DecisionCommittedEvent e, String subject) {
        if (e.simulateMailFailure()) {
            // Excepcion REAL, atrapada por el mismo catch que un fallo de verdad.
            throw new org.springframework.mail.MailSendException(
                    "Simulacion de fallo SMTP (X-Bandersnatch-Simulate: MAIL_FAILURE)");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(e.recipientEmail());          // el email del DUENO de la partida
        msg.setSubject(subject);
        msg.setText(cuerpo(e));
        mailSender.send(msg);
    }

    private String cuerpo(DecisionCommittedEvent e) {
        String ending = (e.endingCode() == null) ? "-" : e.endingCode();
        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                Decision original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                e.displayName(), e.decisionId(), e.playerTag(), e.branchType(), e.impactLevel(),
                e.handlerUnit(), e.outcomeCode(), e.sourceNodeCode(), e.resolvedNodeCode(),
                e.playthroughStatus(), e.lucidity(), e.controlLevel(), ending,
                e.createdAt(), e.rawInput());
    }
}
