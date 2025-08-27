package com.serveur.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToDelivered {

	 private String nom;
	    private String adresse;
	    private String gouvernorat; 
	    private String gouvernorat2; 
	    private String telephone1;
	    private String telephone2;
	    private int nbreDeCommande;
	    private double prix;
	    private String designation;
	
}
