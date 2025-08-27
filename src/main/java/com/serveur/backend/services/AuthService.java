package com.serveur.backend.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.serveur.backend.Config.JwtUtil;
import com.serveur.backend.Entity.Users;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AuthService {

    private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private final String range = "Account!A2:J";

    private final JwtUtil jwtUtil;

    /** Load credentials, handle expired refresh tokens */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets
        InputStream in = AuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")      // must get a refresh token
                .setApprovalPrompt("force")    // forces Google to issue new refresh token
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        try {
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (TokenResponseException e) {
            if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
                // Refresh token invalid → clear stored tokens
                File tokensDir = new File(TOKENS_DIRECTORY_PATH);
                if (tokensDir.exists()) {
                    for (File f : tokensDir.listFiles()) {
                        f.delete();
                    }
                }
                throw new RuntimeException("Google token expired/revoked. Please re-authenticate.", e);
            }
            throw e;
        }
    }

    /** Example: authenticate and get data from Sheet */
    public Map<String, String> getData(String email, String motdepass) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

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
                result.put("entreprise",row.get(7).toString());
                result.put("userName",row.get(2).toString());
                result.put("message", "ok");

                // mark user online in sheet
                row.set(5, "online");
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
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));
        AppendValuesResponse result = service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();

    }

    public List<Users> getUsers(String entreprise) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        List<Users> list = new ArrayList<>();
        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                if (row.size() >= 6) {
                	if(row.get(7).toString().equals(entreprise)) {
                    Users u = new Users(
                            row.get(0).toString(),
                            row.get(1).toString(),
                            row.get(2).toString(),
                            row.get(4).toString(),
                            row.get(5).toString()
                           
                    );
                    u.setId(Long.parseLong(row.get(8).toString()));
                    u.setRole(row.get(6).toString());
                    list.add(u);}
                }
            }
        }
        return list;
    }
    /** List users */
    public List<Users> getUsers() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

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

    /** Mark user offline */
    public void setOffline(String code) throws IOException, GeneralSecurityException {
        setStatus(code, "offline");
    }

    /** Mark user online */
    public void setOnline(String code) throws IOException, GeneralSecurityException {
        setStatus(code, "online");
    }

    private void setStatus(String code, String status) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

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
