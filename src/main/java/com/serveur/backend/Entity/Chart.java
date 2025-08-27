package com.serveur.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Chart {

	private long total;
	private long conf;
	private long reje;
	private long autre;
	private long news;
	private long dup;
	private long bad;
	private long good;
	
	
	
}
