package com.serveur.backend.Entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class Users {

	private long id;
	@NonNull
	private String prenom;
	@NonNull
	private String nom;
	@NonNull
	private String userName;
	private String motdepass;
	@NonNull
	private String telephone;
	@NonNull
	private String statut;
	private String role;
	private String entreprise;

	
	
}
