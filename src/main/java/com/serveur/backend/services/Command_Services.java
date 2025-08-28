package com.serveur.backend.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.serveur.backend.Entity.Commande;
import com.serveur.backend.Entity.Historique;
import com.serveur.backend.Entity.ToDelivered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Command_Services {

    @Autowired
    private HistoriqueService historiqueService;

    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final String spreadsheetId = "1u4gvDZ5Jr8uBjfZismap_egJV4r0bpHkG0i8Ydar2vc";
    private final String confermation = "Confirmation!A2:J";
    private final String historique = "Historique!A2:I";
    private final String toDelivered = "To be delivered!A2:I";

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

    public List<Commande> getCommandeToConfermed() throws GeneralSecurityException, IOException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, confermation).execute();
        List<List<Object>> values = response.getValues();
        List<Commande> list = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            for (List<Object> row : values) {
                LocalDate date = LocalDate.parse(row.get(6).toString(), formatter);
                float value = Float.parseFloat(row.get(5).toString().replace(",", "."));
                Commande c = new Commande(
                        row.get(0).toString(),
                        Long.parseLong(row.get(1).toString()),
                        row.get(2).toString(),
                        row.get(3).toString(),
                        Integer.parseInt(row.get(4).toString()),
                        value,
                        date,
                        row.get(7).toString(),
                        row.size() > 8 ? row.get(8).toString() : "",
                        row.size() > 9 ? row.get(9).toString() : ""
                );
                list.add(c);
            }
        }
        return list;
    }

    public void updateCommandes(List<Commande> changedCommandes, String userId) throws Exception {
        Sheets service = getSheetsService();

        // Column mapping
        final int COL_NOM = 0, COL_TELEPHONE = 1, COL_ADDRESS = 2, COL_PRODUIT = 3, 
                  COL_QTIT = 4, COL_PRIX = 5, COL_DATE = 6, COL_ID = 7, COL_STATUTS = 8;

        Map<String, String> changedMap = changedCommandes.stream()
                .collect(Collectors.toMap(Commande::getId, Commande::getStatuts));

        List<Commande> allCommandes = getCommandeToConfermed();
        List<List<Object>> values = new ArrayList<>();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<Historique> h = new ArrayList<>();

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
            row.set(COL_STATUTS, changedMap.getOrDefault(c.getId(), c.getStatuts()));

            if (changedMap.containsKey(c.getId())) {
                String newStatus = changedMap.get(c.getId());
                if ("confirmed".equals(newStatus) || "rejected".equals(newStatus)) {
                    Historique his = new Historique();
                    his.setUser(userId);
                    his.setActivity(newStatus);
                    his.setDate(LocalDate.now());
                    h.add(his);
                }
            }
            values.add(row);
        }

        historiqueService.addRowToSheet(h);

        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(spreadsheetId, confermation, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public List<Commande> getCommandeHistorique() throws GeneralSecurityException, IOException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, historique).execute();
        List<List<Object>> values = response.getValues();
        List<Commande> list = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            for (List<Object> row : values) {
                LocalDate date = LocalDate.parse(row.get(6).toString(), formatter);
                Commande c = new Commande(
                        row.get(0).toString(),
                        Integer.parseInt(row.get(1).toString()),
                        row.get(2).toString(),
                        row.get(3).toString(),
                        Integer.parseInt(row.get(4).toString()),
                        Float.parseFloat(row.get(5).toString()),
                        date,
                        row.get(7).toString(),
                        row.size() > 8 ? row.get(8).toString() : ""
                );
                list.add(c);
            }
        }
        return list;
    }

    public List<ToDelivered> getToDelivred() throws GeneralSecurityException, IOException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, toDelivered).execute();
        List<List<Object>> values = response.getValues();
        List<ToDelivered> list = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            for (List<Object> row : values) {
                ToDelivered d = new ToDelivered(
                        row.get(0).toString(),
                        row.get(1).toString(),
                        row.get(2).toString(),
                        row.get(3).toString(),
                        row.get(4).toString(),
                        row.get(5).toString(),
                        Integer.parseInt(row.get(6).toString()),
                        Double.parseDouble(row.get(7).toString()),
                        row.get(8).toString()
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
        Sheets service = getSheetsService();
        ClearValuesRequest requestBody = new ClearValuesRequest();
        service.spreadsheets().values().clear(spreadsheetId, toDelivered, requestBody).execute();
    }
}
