package com.serveur.backend.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.google.api.services.sheets.v4.model.ValueRange;
import com.serveur.backend.Entity.Chart;
import com.serveur.backend.Entity.Historique;

@Service
public class ChartService {
	 private static final String APPLICATION_NAME = "Google Sheets API Java Backend";
	    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	    private static final String TOKENS_DIRECTORY_PATH = "tokens";

	    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
	    private final String range = "Stat!A2:G";


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
	    
	    public Chart getData() throws IOException, GeneralSecurityException {
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	                .setApplicationName(APPLICATION_NAME)
	                .build();

	        ValueRange response = service.spreadsheets().values()
	                .get(spreadsheetId, range)
	                .execute();

	        Chart c = new Chart();
	        List<List<Object>> values = response.getValues();
	        
	        if(values != null) {
	        	
	        	for(List<Object> row : values) {
	        		
	        		c.setTotal(Long.parseLong(row.get(0).toString()));
	        		c.setConf(Long.parseLong(row.get(1).toString()));
	        		c.setReje(Long.parseLong(row.get(2).toString()));
	        		c.setAutre(Long.parseLong(row.get(0).toString())-(Long.parseLong(row.get(2).toString())+Long.parseLong(row.get(1).toString())));
	        		c.setNews(Long.parseLong(row.get(3).toString()));
	        		c.setDup(Long.parseLong(row.get(4).toString()));
	        		c.setBad(Long.parseLong(row.get(5).toString()));
	        		c.setGood(Long.parseLong(row.get(6).toString()));
	        	}
	        }
	        return c;
	    }

}
