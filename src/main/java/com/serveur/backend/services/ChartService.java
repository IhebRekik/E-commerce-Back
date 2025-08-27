package com.serveur.backend.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.serveur.backend.Entity.Chart;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class ChartService {

    private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // 👇 replace with your spreadsheet ID & range
    private static final String SPREADSHEET_ID = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private static final String RANGE = "Stat!A2:G";  

    /**
     * Build an authorized Sheets service using service account credentials.
     */
    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        // Load service account key from resources
      String json = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
if (json == null || json.isEmpty()) {
    throw new IOException("Environment variable GOOGLE_APPLICATION_CREDENTIALS_JSON not set");
}

// Convert JSON string to InputStream
InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

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

    /**
     * Fetch data from Google Sheets and map it into Chart entity
     */
    public Chart getData() throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();

        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        Chart chart = new Chart();
        List<List<Object>> values = response.getValues();

        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                // Assuming row has at least 7 columns
                chart.setTotal(Long.parseLong(row.get(0).toString()));
                chart.setConf(Long.parseLong(row.get(1).toString()));
                chart.setReje(Long.parseLong(row.get(2).toString()));
                chart.setAutre(
                        Long.parseLong(row.get(0).toString())
                                - (Long.parseLong(row.get(1).toString())
                                + Long.parseLong(row.get(2).toString())));
                chart.setNews(Long.parseLong(row.get(3).toString()));
                chart.setDup(Long.parseLong(row.get(4).toString()));
                chart.setBad(Long.parseLong(row.get(5).toString()));
                chart.setGood(Long.parseLong(row.get(6).toString()));
            }
        }

        return chart;
    }
}
