package com.serveur.backend.controlleurs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.Json;
import com.serveur.backend.Config.JwtUtil;
import com.serveur.backend.Entity.Commande;
import com.serveur.backend.Entity.LoginForm;
import com.serveur.backend.Entity.Users;
import com.serveur.backend.services.AuthService;

import io.jsonwebtoken.impl.lang.Services;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
//@CrossOrigin(origins = "https://classy-brigadeiros-f5e54e.netlify.app")
@CrossOrigin(origins = "http://http://localhost:5173")

public class AuthControlleur {

	@Autowired
	private AuthService service;
	@PostMapping("/")
	public ResponseEntity<?> getUser(@RequestBody LoginForm login) throws IOException, GeneralSecurityException {
	    Map<String, String> rep = service.getData(login.getEmail(), login.getMotdepass());
	    if("ok".equals(rep.get("message"))) {
            return ResponseEntity.status(200).body(rep);
	    } else {
	        return ResponseEntity.status(401).body(rep);
	    }
	}

	@PostMapping("/offline")
	public void setOffline(@RequestBody String body) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
	    Map<String, String> map = mapper.readValue(body, Map.class);
	    String value = map.get("username");
	    service.setOffline(value);

	}
	@PostMapping("/online")
	public void setOnline(@RequestBody String body) throws Exception {
	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, String> map = mapper.readValue(body, Map.class);
	    String usernameJson = map.get("username");
	    Map<String, Object> usernameMap = mapper.readValue(usernameJson, Map.class);
	    String value = (String) usernameMap.get("value");
	    service.setOnline(value);
	    
	}

	 @PostMapping("/add")
	    public ResponseEntity<?> addRow(@RequestBody Users user) throws Exception {
	    
		 List<Users> list = service.getUsers();
		 for (Users u : list) {
			if(u.getUserName().equals(user.getUserName())) {
				return ResponseEntity.status(401).body(null);
			}
			
		}
	        List<Object> row = Arrays.asList(
	                user.getNom(),
	                user.getPrenom(),
	                user.getUserName(),
	                user.getMotdepass(),
	                user.getTelephone(),
	                "offline",
	                user.getRole(),
	                user.getEntreprise(),
	                list.size() + 1
	              
	        );

	        service.addRowToSheet(row);
	        return ResponseEntity.status(200).body("String");
	    }
	 @PostMapping("/all")
		public ResponseEntity<List<Users>> getAll(@RequestBody Map<String, String> body) throws GeneralSecurityException, IOException {
		 
		 String token = body.get("token");	
		 List<Users> comm = service.getUsers(token);
			if(comm == null  || comm.size() == 0 ) {
		        return ResponseEntity.status(401).body(null);
			}else {
				return ResponseEntity.status(200).body(comm);
			}
		}
	
	
}
