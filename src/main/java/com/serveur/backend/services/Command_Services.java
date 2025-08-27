package com.serveur.backend.services;

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
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
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
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.serveur.backend.Entity.Commande;
import com.serveur.backend.Entity.Historique;
import com.serveur.backend.Entity.ToDelivered;

@Service
public class Command_Services {

	@Autowired
	private HistoriqueService historiqueService;
	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = AuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
				.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
	private final String confermation = "Confirmation!A2:J";
	private final String historique = "Historique!A2:I";
	private final String toDelivered = "To be delivered!A2:I";

	
	
	public List<Commande> getCommandeToConfermed() throws GeneralSecurityException, IOException {

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		ValueRange response = service.spreadsheets().values().get(spreadsheetId, confermation).execute();
		List<List<Object>> values = response.getValues();
		List<Commande> list = new ArrayList<>();
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {

			for (List row : values) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
				LocalDate date = LocalDate.parse(row.get(6).toString(), formatter);
				float value = Float.parseFloat(row.get(5).toString().replace(",", "."));
				Commande c = new Commande(row.get(0).toString(), Long.parseLong(row.get(1).toString()),
						row.get(2).toString(), row.get(3).toString(), Integer.parseInt(row.get(4).toString()),
						value, date, row.get(7).toString(),
						row.size() > 8 && row.get(8) != null && !row.get(8).toString().isEmpty() ? row.get(8).toString()
								: "" , row.size() > 9 && row.get(9) != null && !row.get(9).toString().isEmpty() ? row.get(9).toString()
										: "" );
				list.add(c);
			}
			
		}
		return list;

	}

	public void updateCommandes(List<Commande> changedCommandes,String id) throws Exception {
	    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	    Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	            .setApplicationName(APPLICATION_NAME)
	            .build();
	    
	    // Column mapping
	    final int COL_NOM = 0;
	    final int COL_TELEPHONE = 1;
	    final int COL_ADDRESS = 2;
	    final int COL_PRODUIT = 3;
	    final int COL_QTIT = 4;
	    final int COL_PRIX = 5;
	    final int COL_DATE = 6;
	    final int COL_ID = 7;
	    final int COL_STATUTS = 8;

	    // Prepare a map of changed IDs → new statuses
	    Map<String, String> changedMap = changedCommandes.stream()
	            .collect(Collectors.toMap(Commande::getId, Commande::getStatuts));

	    // Fetch all commandes to be confirmed
	    List<Commande> allCommandes = getCommandeToConfermed();
	    List<List<Object>> values = new ArrayList<>();
	    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	    List<Historique> h = new ArrayList<>();
	    // Build the rows
	    for (Commande c : allCommandes) {
	        List<Object> row = new ArrayList<>(Collections.nCopies(9, ""));
	        row.set(COL_NOM, c.getNomPrenom());
	        row.set(COL_TELEPHONE, c.getTelephone());
	        row.set(COL_ADDRESS, c.getAddress());
	        row.set(COL_PRODUIT, c.getProduit());
	        row.set(COL_QTIT, c.getQtit());
	        row.set(COL_PRIX, c.getPrix());
	        row.set(COL_DATE, c.getDate().format(outputFormatter));
	        row.set(COL_ID, c.getId());

	        // If this commande was changed, use new status; otherwise keep original
	        row.set(COL_STATUTS, changedMap.getOrDefault(c.getId(), c.getStatuts()));
	        
	        if (changedMap.containsKey(c.getId())) {
	        	String newStatus = changedMap.getOrDefault(c.getId(), c.getStatuts());
	        	if(newStatus.equals("confirmed") || newStatus.equals("rejected")) {
	            Historique his = new Historique();
	            his.setUser(id); // or use logged-in user
	            his.setActivity(newStatus);
	            his.setDate(LocalDate.now()); // current date
	            h.add(his);}
	        }
	        values.add(row);
	    }
	    historiqueService.addRowToSheet(h);
	    // Update the Google Sheet
	    ValueRange body = new ValueRange().setValues(values);
	    service.spreadsheets().values()
	            .update(spreadsheetId, confermation, body)
	            .setValueInputOption("RAW")
	            .execute();
	}


	public List<Commande> getCommandeHistorique() throws GeneralSecurityException, IOException {

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		ValueRange response = service.spreadsheets().values().get(spreadsheetId, historique).execute();
		List<List<Object>> values = response.getValues();
		List<Commande> list = new ArrayList<>();
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {

			for (List row : values) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
				LocalDate date = LocalDate.parse(row.get(6).toString(), formatter);
				Commande c = new Commande(row.get(0).toString(), Integer.parseInt(row.get(1).toString()),
						row.get(2).toString(), row.get(3).toString(), Integer.parseInt(row.get(4).toString()),
						Float.parseFloat(row.get(5).toString()), date, row.get(7).toString(),
						row.size() > 8 && row.get(8) != null && !row.get(8).toString().isEmpty() ? row.get(8).toString()
								: "");
				list.add(c);
			}
		}
		return list;

	}

	public List<ToDelivered> getToDelivred() throws GeneralSecurityException, IOException{
		
		 final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		    
		    Sheets service =
		        new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
		            .setApplicationName(APPLICATION_NAME)
		            .build();
		    ValueRange response = service.spreadsheets().values()
		        .get(spreadsheetId, toDelivered)
		        .execute();
		    List<List<Object>> values = response.getValues();
		    List<ToDelivered> list = new ArrayList<>();
		    if (values == null || values.isEmpty()) {
		      System.out.println("No data found.");
		    } else {
		      
		    	for (List<Object> row : values) {
		    	    ToDelivered d = new ToDelivered(
		    	        row.get(0).toString(),                   // nom
		    	        row.get(1).toString(),                   // adresse
		    	        row.get(2).toString(),
		    	        row.get(3).toString(),// gouvernorat
		    	        row.get(4).toString(),                   // telephone1
		    	        row.get(5).toString(),                   // telephone2
		    	        Integer.parseInt(row.get(6).toString()),// nbre de commande
		    	        Double.parseDouble(row.get(7).toString()), // prix
		    	        row.get(8).toString()                    // designation
		    	    );
		    	    list.add(d);
		    	}

		    }
		    return list;
	
		
	}
	public List<Historique> getUserHistorique(String entreprise) throws GeneralSecurityException, IOException {

	
		return historiqueService.getData(entreprise);

	}
	public void clearToDelivered() throws GeneralSecurityException, IOException {
	    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

	    Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	            .setApplicationName(APPLICATION_NAME)
	            .build();

	    // Efface toutes les valeurs de la plage "toDelivered"
	    ClearValuesRequest requestBody = new ClearValuesRequest();

	    service.spreadsheets().values()
	            .clear(spreadsheetId, toDelivered, requestBody)
	            .execute();

	}
}
