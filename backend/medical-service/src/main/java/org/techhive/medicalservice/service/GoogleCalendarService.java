package org.techhive.medicalservice.service;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CalendarStatusDTO;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.DoctorCalendar;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.DoctorCalendarRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    private final DoctorCalendarRepository doctorCalendarRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    @Value("${google.calendar.credentials-path}")
    private String credentialsPath;

    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/calendar");

    public GoogleCalendarService(DoctorCalendarRepository doctorCalendarRepository,
            AppointmentRepository appointmentRepository) {
        this.doctorCalendarRepository = doctorCalendarRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Génère l'URL de connexion Google
     */
    public String generateAuthUrl(String doctorId) {
        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, SCOPES);
        url.setAccessType("offline"); // Essentiel pour obtenir refresh token
        url.set("prompt", "consent"); // Force l'obtention d'un refresh token
        url.setState(doctorId);
        return url.build();
    }

    /**
     * Échange le code contre des tokens
     */
    @Transactional
    public void handleCallback(String code, String doctorId) throws IOException {
        GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                code,
                redirectUri);

        GoogleTokenResponse tokenResponse = tokenRequest.execute();

        // Récupérer les infos du calendrier
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(new NetHttpTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .build()
                .setAccessToken(tokenResponse.getAccessToken());

        Calendar calendarService = new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();

        CalendarListEntry calendarListEntry = calendarService.calendarList().get("primary").execute();

        // Sauvegarder ou mettre à jour
        DoctorCalendar calendar = doctorCalendarRepository.findByDoctorId(doctorId)
                .orElse(new DoctorCalendar());

        calendar.setDoctorId(doctorId);
        calendar.setAccessToken(tokenResponse.getAccessToken());
        calendar.setRefreshToken(tokenResponse.getRefreshToken());
        calendar.setTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        calendar.setGoogleEmail(calendarListEntry.getSummary());
        calendar.setGoogleCalendarId(calendarListEntry.getId());
        calendar.setActive(true);
        calendar.setLastSync(LocalDateTime.now());

        doctorCalendarRepository.save(calendar);
    }

    /**
     * Récupère un service Calendar avec gestion automatique du refresh
     */
    private Calendar getCalendarService(String doctorId) throws Exception {
        DoctorCalendar doctorCal = doctorCalendarRepository
                .findByDoctorIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new RuntimeException("Médecin non connecté à Google Calendar"));

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(new NetHttpTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                .build()
                .setAccessToken(doctorCal.getAccessToken())
                .setRefreshToken(doctorCal.getRefreshToken());

        // Refresh automatique si token expiré
        if (doctorCal.isAccessTokenExpired()) {
            credential.refreshToken();

            // Mettre à jour en base
            doctorCal.setAccessToken(credential.getAccessToken());
            doctorCal.setTokenExpiry(LocalDateTime.now().plusSeconds(
                    credential.getExpiresInSeconds() != null ? credential.getExpiresInSeconds() : 3600));
            doctorCalendarRepository.save(doctorCal);
        }

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();
    }

    /**
     * Crée un événement dans Google Calendar
     */
    public void createEvent(String doctorId, Appointment appointment) {
        try {
            Calendar service = getCalendarService(doctorId);
            DoctorCalendar doctorCal = doctorCalendarRepository.findByDoctorId(doctorId).get();

            Event event = new Event()
                    .setSummary(appointment.getTitle())
                    .setDescription(appointment.getDescription() != null ? appointment.getDescription() : "");

            // Convert to com.google.api.client.util.DateTime properly
            Date startDate = Date.from(appointment.getStartTime().atZone(ZoneId.of("Europe/Paris")).toInstant());
            Date endDate = Date.from(appointment.getEndTime().atZone(ZoneId.of("Europe/Paris")).toInstant());

            event.setStart(new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(startDate))
                    .setTimeZone("Europe/Paris"))
                    .setEnd(new EventDateTime()
                            .setDateTime(new com.google.api.client.util.DateTime(endDate))
                            .setTimeZone("Europe/Paris"));

            Event created = service.events().insert(doctorCal.getGoogleCalendarId(), event).execute();

            // Mettre à jour lastSync après un événement réussi
            doctorCal.setLastSync(LocalDateTime.now());
            doctorCalendarRepository.save(doctorCal);

            // Optionnel: stocker l'ID de l'événement Google dans l'appointment
            appointment.setGoogleEventId(created.getId());
            // Note: C'est le AppointmentServiceImpl qui s'occupe de sauvegarder
            // l'appointment ensuite

        } catch (Exception e) {
            log.error("Erreur création événement Google Calendar pour compte " + doctorId, e);
            // Ne pas bloquer le processus principal
        }
    }

    /**
     * Récupère le statut de connexion
     */
    public CalendarStatusDTO getStatus(String doctorId) {
        Optional<DoctorCalendar> doctorCal = doctorCalendarRepository.findByDoctorIdAndActiveTrue(doctorId);

        CalendarStatusDTO status = new CalendarStatusDTO();
        status.setConnected(doctorCal.isPresent());

        doctorCal.ifPresent(cal -> {
            status.setGoogleEmail(cal.getGoogleEmail());
            status.setLastSync(cal.getLastSync());
            // Compter les rendez-vous synchronisés
            long count = appointmentRepository.countByDoctorIdAndGoogleEventIdIsNotNull(doctorId);
            status.setSyncedAppointments((int) count);
        });

        return status;
    }

    /**
     * Déconnecte le compte Google
     */
    @Transactional
    public void disconnect(String doctorId) {
        doctorCalendarRepository.findByDoctorId(doctorId).ifPresent(cal -> {
            cal.setActive(false);
            doctorCalendarRepository.save(cal);
        });
    }
}
