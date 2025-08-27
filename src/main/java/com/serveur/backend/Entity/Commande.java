package com.serveur.backend.Entity;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class Commande {
	@NonNull
	private String nomPrenom;
	@NonNull
	private long telephone;
	@NonNull
	private String address;
	@NonNull
	private String produit;
	@NonNull
	private int qtit;
	@NonNull
	private float prix;
	@NonNull
	private LocalDate date;
	@NonNull
	private String id;
	@NonNull
	private String statuts;
	private String note;

	
	
	
}
