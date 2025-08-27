package com.serveur.backend.controlleurs;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.serveur.backend.Entity.Chart;
import com.serveur.backend.services.ChartService;

@RestController
@RequestMapping("data")
public class ChartControlleur {

	@Autowired
	private ChartService service;
	
	@GetMapping("/")
	public ResponseEntity<Chart> getStat() throws IOException, GeneralSecurityException{
		
		Chart c = service.getData();
		if(c == null ) {
			return ResponseEntity.status(401).body(c);
		}
		return ResponseEntity.status(200).body(c);
	}
	
}
