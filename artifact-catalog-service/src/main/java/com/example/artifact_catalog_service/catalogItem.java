package com.example.artifact_catalog_service;

public class catalogItem {
    private String name;
    private String movieId;    // ← Changed from description to movieId
    private int rating;
    
    public catalogItem(String name, String movieId, int rating) {  // ← Changed parameter name
        this.name = name;
        this.movieId = movieId;  // ← Changed from description to movieId
        this.rating = rating;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMovieId() {    // ← Changed from getDescription()
        return movieId;
    }
    
    public void setMovieId(String movieId) {    // ← Changed from setDescription()
        this.movieId = movieId;
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
