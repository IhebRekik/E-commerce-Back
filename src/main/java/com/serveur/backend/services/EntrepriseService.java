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
import com.serveur.backend.Entity.Entreprise;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class EntrepriseService {

    private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SPREADSHEET_ID = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private static final String RANGE = "Entreprises!A2:B";

    /** Build an authorized Sheets service using service account credentials from env variable */
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

    /** Fetch all entreprises from the sheet */
    public List<Entreprise> getData() throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        List<Entreprise> result = new ArrayList<>();
        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                if (!row.isEmpty()) {
                    Entreprise e = new Entreprise(row.get(0).toString());
                    result.add(e);
                }
            }
        }
        return result;
    }

    /** Add a new row to the sheet */
    public void addRowToSheet(List<Object> rowData) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));
        AppendValuesResponse result = service.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("RAW")
                .execute();
    }
}
