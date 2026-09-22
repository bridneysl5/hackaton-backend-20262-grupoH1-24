package com.tuckersoft.branchengine.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Map;

@Component
public class BranchClassifier {

    // Paso 1: minusculas y sin tildes.
    public static String normalizar(String rawInput) {
        if (rawInput == null) return "";
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    // Paso 2: la PRIMERA regla que se cumple gana. EL ORDEN ES EL DEL ENUNCIADO.
    public String clasificar(String rawInput) {
        String t = normalizar(rawInput);

        // 1 - no contiene ninguna letra a-z
        boolean tieneLetra = false;
        for (char c : t.toCharArray()) {
            if (c >= 'a' && c <= 'z') { tieneLetra = true; break; }
        }
        if (!tieneLetra) return "ENTRADA_CORRUPTA";

        // 2 - RUPTURA_CUARTA_PARED  (va ANTES que REBELDIA: "destruye la camara" -> RUPTURA)
        if (t.contains("netflix") || t.contains("camara")
                || t.contains("espectador") || t.contains("videojuego")) {
            return "RUPTURA_CUARTA_PARED";
        }

        // 3 - SOSPECHA
        if (t.contains("vigilan") || t.contains("simbolo") || t.contains("conspiracion")) {
            return "SOSPECHA";
        }

        // 4 - REBELDIA
        if (t.contains("rechaza") || t.contains("destruye")
                || t.contains("desobedece") || t.contains("renuncia")) {
            return "REBELDIA";
        }

        // 5 - resto
        return "OBEDIENCIA";
    }

    // Paso 3: derivados. "Mesa de Guion" va SIN TILDE.
    private static final Map<String, String[]> TABLA = Map.of(
            "OBEDIENCIA",           new String[]{"Mesa de Guion",          "ADVANCE_MAIN_PATH"},
            "REBELDIA",             new String[]{"Control de Continuidad", "FORK_TIMELINE"},
            "SOSPECHA",             new String[]{"Oficina de Seguridad",   "INJECT_WHITE_BEAR_SYMBOL"},
            "RUPTURA_CUARTA_PARED", new String[]{"Departamento Netflix",   "BREAK_FOURTH_WALL"},
            "ENTRADA_CORRUPTA",     new String[]{"Archivo de Errores",     "DISCARD_INPUT"}
    );

    public String handlerUnit(String branchType) { return TABLA.get(branchType)[0]; }
    public String outcomeCode(String branchType) { return TABLA.get(branchType)[1]; }
}
