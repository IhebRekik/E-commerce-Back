package com.serveur.backend.controlleurs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.serveur.backend.Entity.Entreprise;
import com.serveur.backend.Entity.Users;
import com.serveur.backend.services.EntrepriseService;

@RestController
@RequestMapping("entreprise")
public class EntrepriseControlleur {

	@Autowired
	private EntrepriseService service;
	
	@GetMapping("/")
	public ResponseEntity<List<Entreprise>> getAll() throws IOException, GeneralSecurityException{
		
		List<Entreprise> resultat = service.getData();
		if(resultat == null || resultat.size() == 0) {
			return ResponseEntity.status(400).body(resultat);
		}
		return ResponseEntity.status(200).body(resultat);
	}
	@PostMapping("/")
    public ResponseEntity<?> addRow(@RequestBody Entreprise entreprise) throws Exception {
	    
        List<Object> row = Arrays.asList(
              entreprise.getName()
        );

        service.addRowToSheet(row);
        return ResponseEntity.status(200).body("String");
    }
	
}
