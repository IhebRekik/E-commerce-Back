package com.serveur.backend.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.serveur.backend.Entity.Historique;
import com.serveur.backend.Entity.Users;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Service
public class HistoriqueService {
	
	@Autowired
	private AuthService service;
	
	 private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
	    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	    private static final String TOKENS_DIRECTORY_PATH = "tokens";

	    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
	    private final String range = "User_Historique!A2:C";


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
	    public List<Historique> getData(String entreprise) throws IOException, GeneralSecurityException {
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	        Sheets service = getSheetsService();

	        ValueRange response = service.spreadsheets().values()
	                .get(spreadsheetId, range)
	                .execute();

	        List<Historique> result = new ArrayList<>();
	        List<List<Object>> values = response.getValues();

	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // match your sheet format
	        List<Users> users = this.service.getUsers(entreprise);
	        if (values != null) {
	            for (List<Object> row : values) {
	                // Safely parse the date column (index 2)
	                LocalDate date = null;
	                if (row.size() > 2 && row.get(2) != null && !row.get(2).toString().isEmpty()) {
	                    date = LocalDate.parse(row.get(2).toString(), formatter);
	                }
	              for(Users u : users) {

	                if(row.get(0).toString().equals(String.valueOf(u.getId()))) {
	                	if(u.getRole().equals("user")) {
	                	Historique e = new Historique(
	    	                    u.getUserName(), // user
	    	                    row.get(1).toString(), // activity
	    	                    date                   // parsed date
	    	                );
	    	                result.add(e);
	    	                }
	                }
	            }}
	        }

	        return result;
	    }

	    public void addRowToSheet(List<Historique> his) throws IOException, GeneralSecurityException {
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	        Sheets service = getSheetsService();

	        List<List<Object>> rows = new ArrayList<>();
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // format date

	        for (Historique h : his) {
	            String user = h.getUser() != null ? h.getUser() : "";
	            String activity = h.getActivity() != null ? h.getActivity() : "";
	            String date = h.getDate() != null ? h.getDate().format(formatter) : "";

	            rows.add(Arrays.asList(user, activity, date));
	        }

	        ValueRange body = new ValueRange().setValues(rows);

	        service.spreadsheets().values()
	                .append(spreadsheetId, range, body)
	                .setValueInputOption("RAW")
	                .execute();
	    }

	

}
