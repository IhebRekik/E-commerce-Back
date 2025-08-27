package com.serveur.backend.controlleurs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.serveur.backend.Config.JwtUtil;
import com.serveur.backend.Entity.Commande;
import com.serveur.backend.Entity.Historique;
import com.serveur.backend.Entity.ToDelivered;
import com.serveur.backend.services.Command_Services;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("commande")
@RequiredArgsConstructor

public class Command_Controlleur {

	@Autowired
	private Command_Services services;
	
	private final JwtUtil jw;
	
	
	@GetMapping("/confermation")
	public ResponseEntity<List<Commande>> getMethodName() throws GeneralSecurityException, IOException {
		List<Commande> comm = services.getCommandeToConfermed();
		if(comm == null  || comm.size() == 0 ) {
	        return ResponseEntity.status(401).body(null);
		}else {
			return ResponseEntity.status(200).body(comm);
		}
	}
	@PutMapping("/confermation/{user}")
	public void updateCommandes(@PathVariable String user, @RequestBody List<Commande> commandes) throws Exception {
	    // use 'user' here
		String id = jw.getUsernameFromToken(user);
	    services.updateCommandes(commandes,id);
	}
	@GetMapping("/")
	public ResponseEntity<List<Commande>> getHistorique() throws GeneralSecurityException, IOException {
		List<Commande> comm = services.getCommandeHistorique();
		if(comm == null  || comm.size() == 0 ) {
	        return ResponseEntity.status(401).body(null);
		}else {
			return ResponseEntity.status(200).body(comm);
		}
	}
	
	@GetMapping("/delivered")
	public ResponseEntity<List<ToDelivered>> getToDelivered() throws GeneralSecurityException, IOException {
		List<ToDelivered> comm = services.getToDelivred();
		if(comm == null  || comm.size() == 0 ) {
	        return ResponseEntity.status(401).body(null);
		}else {
			return ResponseEntity.status(200).body(comm);
		}
	}
	@GetMapping("/historique/{entreprise}")
	public ResponseEntity<List<Historique>> getUserHistorique(@PathVariable String entreprise) throws GeneralSecurityException, IOException {
	    List<Historique> h = services.getUserHistorique(entreprise);
	    if (h == null || h.isEmpty()) {
	        return ResponseEntity.noContent().build();
	    }
	    return ResponseEntity.ok(h);
	}
	   @DeleteMapping("/clear-to-delivered")
	    public ResponseEntity<String> clearToDelivered() {
	        try {
	            services.clearToDelivered();
	            return ResponseEntity.ok("Les données de 'toDelivered' ont été effacées.");
	        } catch (Exception e) {
	            return ResponseEntity.status(500).body("Erreur : " + e.getMessage());
	        }
	    }
}
