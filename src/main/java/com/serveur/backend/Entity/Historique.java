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
@AllArgsConstructor

@RequiredArgsConstructor
@ToString
public class Historique {

	private String user;
	private String activity;
	private LocalDate date;
	
}
