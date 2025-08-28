package com.serveur.backend.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.serveur.backend.Entity.Historique;
import com.serveur.backend.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class HistoriqueService {

    @Autowired
    private AuthService authService;

    private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SPREADSHEET_ID = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private static final String RANGE = "User_Historique!A2:C";

    /** Build Sheets service from environment variable JSON */
    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        String json = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
        if (json == null || json.isEmpty()) {
            throw new IOException("Environment variable GOOGLE_APPLICATION_CREDENTIALS_JSON not set");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ServiceAccountCredentials credentials = (ServiceAccountCredentials) ServiceAccountCredentials
                .fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /** Get historical data filtered by enterprise */
    public List<Historique> getData(String entreprise) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        List<Historique> result = new ArrayList<>();
        List<List<Object>> values = response.getValues();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<Users> users = authService.getUsers(entreprise);
        if (values != null) {
            for (List<Object> row : values) {
                LocalDate date = null;
                if (row.size() > 2 && row.get(2) != null && !row.get(2).toString().isEmpty()) {
                    date = LocalDate.parse(row.get(2).toString(), formatter);
                }

                for (Users u : users) {
                    if (row.get(0).toString().equals(String.valueOf(u.getId())) && "user".equals(u.getRole())) {
                        Historique e = new Historique(
                                u.getUserName(),
                                row.get(1).toString(),
                                date
                        );
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    /** Append new historique entries to Google Sheet */
    public void addRowToSheet(List<Historique> historiques) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (Historique h : historiques) {
            String user = h.getUser() != null ? h.getUser() : "";
            String activity = h.getActivity() != null ? h.getActivity() : "";
            String date = h.getDate() != null ? h.getDate().format(formatter) : "";
            rows.add(Arrays.asList(user, activity, date));
        }

        ValueRange body = new ValueRange().setValues(rows);
        service.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("RAW")
                .execute();
    }
}
