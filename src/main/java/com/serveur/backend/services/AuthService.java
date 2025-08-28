package com.serveur.backend.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.serveur.backend.Config.JwtUtil;
import com.serveur.backend.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

@RequiredArgsConstructor
@Service
public class AuthService {

    private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private final String range = "Account!A2:J";

    private final JwtUtil jwtUtil;

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

    public Map<String, String> getData(String email, String motdepass) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

        Map<String, String> result = new HashMap<>();
        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            result.put("message", "No data found.");
            return result;
        }

        for (List<Object> row : values) {
            if (row.size() > 3 && row.get(2).equals(email) && row.get(3).equals(motdepass)) {
                String token = jwtUtil.generateToken(row.get(8).toString());
                result.put("token", token);
                result.put("token2", row.get(6).toString());
                result.put("entreprise", row.get(7).toString());
                result.put("userName", row.get(2).toString());
                result.put("message", "ok");

                row.set(5, "online");  // mark user online
                ValueRange body = new ValueRange().setValues(values);
                service.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption("RAW").execute();

                return result;
            }
        }
        result.put("message", "Nom d'utilisateur ou mot de passe incorrect");
        return result;
    }

    public void addRowToSheet(List<Object> rowData) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));
        AppendValuesResponse result = service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public List<Users> getUsers(String entreprise) throws GeneralSecurityException, IOException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        List<Users> list = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                if (row.size() >= 6 && row.get(7).toString().equals(entreprise)) {
                    Users u = new Users(
                            row.get(0).toString(),
                            row.get(1).toString(),
                            row.get(2).toString(),
                            row.get(4).toString(),
                            row.get(5).toString()
                    );
                    u.setId(Long.parseLong(row.get(8).toString()));
                    u.setRole(row.get(6).toString());
                    list.add(u);
                }
            }
        }
        return list;
    }

    public List<Users> getUsers() throws GeneralSecurityException, IOException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        List<Users> list = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                if (row.size() >= 6) {
                    Users u = new Users(
                            row.get(0).toString(),
                            row.get(1).toString(),
                            row.get(2).toString(),
                            row.get(4).toString(),
                            row.get(5).toString()
                    );
                    list.add(u);
                }
            }
        }
        return list;
    }

    public void setOffline(String code) throws IOException, GeneralSecurityException {
        setStatus(code, "offline");
    }

    public void setOnline(String code) throws IOException, GeneralSecurityException {
        setStatus(code, "online");
    }

    private void setStatus(String code, String status) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            String id = jwtUtil.getUsernameFromToken(code);
            for (List<Object> row : values) {
                if (row.size() > 6 && row.get(8).equals(id)) {
                    row.set(5, status);
                    ValueRange body = new ValueRange().setValues(values);
                    service.spreadsheets().values().update(spreadsheetId, range, body)
                            .setValueInputOption("RAW").execute();
                    break;
                }
            }
        }
    }
}
