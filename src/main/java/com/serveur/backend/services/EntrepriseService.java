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
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.serveur.backend.Config.JwtUtil;
import com.serveur.backend.Entity.Entreprise;

@Service
public class EntrepriseService {
	
	 private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
	    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	    private static final String TOKENS_DIRECTORY_PATH = "tokens";

	    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
	    private final String range = "Entreprises!A2:B";


	    /** Load credentials, handle expired refresh tokens */
	    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
	        // Load service account key from resources
	        InputStream in = getClass().getResourceAsStream("/service_account.json");
	        if (in == null) {
	            throw new IOException("Resource not found: service_account.json");
	        }

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
	    /** Example: authenticate and get data from Sheet */
	    public List<Entreprise> getData() throws IOException, GeneralSecurityException {
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	        Sheets service = getSheetsService();

	        ValueRange response = service.spreadsheets().values()
	                .get(spreadsheetId, range)
	                .execute();

	        List<Entreprise> result = new ArrayList();
	        List<List<Object>> values = response.getValues();

	     

	        for (List<Object> row : values) {
            Entreprise e = new Entreprise(row.getFirst().toString());
            result.add(e);
	        }

	        return result;
	    }
	    public void addRowToSheet(List<Object> rowData) throws IOException, GeneralSecurityException {
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	        Sheets service = getSheetsService();

	        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));
	        AppendValuesResponse result = service.spreadsheets().values()
	                .append(spreadsheetId, range, body)
	                .setValueInputOption("RAW")
	                .execute();

	    }

}
