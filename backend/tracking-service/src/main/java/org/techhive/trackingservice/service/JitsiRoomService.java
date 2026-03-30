package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Jitsi Meet room service — 100% gratuit, aucune clé API requise.
 *
 * Jitsi crée automatiquement les salles à la première connexion.
 * Aucun appel API nécessaire — on génère juste un nom de salle unique.
 *
 * URL publique : https://meet.jit.si/{roomName}
 */
@Service
@Slf4j
public class JitsiRoomService {

    private static final String JITSI_BASE_URL = "https://meet.jit.si";

    /**
     * Génère un nom de salle unique pour Jitsi.
     * Format : tfakkarni-{8 chars UUID}
     */
    public String generateRoomName() {
        return "tfakkarni-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Construit l'URL complète de la salle Jitsi avec la config optimale.
     * La salle est créée automatiquement par Jitsi au premier accès.
     */
    public String getRoomUrl(String roomName) {
        return JITSI_BASE_URL + "/" + roomName
             + "#config.prejoinPageEnabled=false"
             + "&config.startWithAudioMuted=false"
             + "&config.startWithVideoMuted=false"
             + "&config.enableWelcomePage=false"
             + "&interfaceConfig.SHOW_JITSI_WATERMARK=false"
             + "&interfaceConfig.SHOW_WATERMARK_FOR_GUESTS=false"
             + "&interfaceConfig.TOOLBAR_BUTTONS=[%22microphone%22,%22camera%22,"
             + "%22desktop%22,%22fullscreen%22,%22fodeviceselection%22,"
             + "%22hangup%22,%22chat%22,%22recording%22,%22participants-pane%22]";
    }

    /**
     * Génère un nom d'affichage formaté pour un participant.
     */
    public String formatDisplayName(String firstName, String lastName, String role) {
        String name = (firstName + " " + lastName).trim();
        if (name.isBlank()) name = "Participant";
        return "doctor".equalsIgnoreCase(role) ? "Dr. " + name : name;
    }

    /**
     * Vérifie si une salle peut être considérée comme active
     * (Jitsi ferme les salles automatiquement quand tout le monde part).
     */
    public boolean isRoomPotentiallyActive(String roomName) {
        return roomName != null && roomName.startsWith("tfakkarni-");
    }
}
