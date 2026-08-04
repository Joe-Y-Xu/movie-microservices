package com.example.artifact_catalog_service;

public class catalogItem {
	private String name;
	private String description;
	private int rating;
		
	
	public catalogItem(String name, String description, int rating) {
		super();
		this.name = name;
		this.description = description;
		this.rating = rating;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getRating() {
		return rating;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	
	
}


//public class Rating (int movieID, int rating) {
//	
//	private int moiveID ;
//	private int rating;
//	
//	public int getMoiveID() {
//		return moiveID;
//	}
//	public void setMoiveID(int moiveID) {
//		this.moiveID = moiveID;
//	}
//	public int getRating() {
//		return rating;
//	}
//	public void setRating(int rating) {
//		this.rating = rating;
//	}
//	
//}
