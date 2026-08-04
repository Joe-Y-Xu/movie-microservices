//package io.javabrains.movieinfoservice.resources;
//
//import io.javabrains.movieinfoservice.models.Movie;
//import io.javabrains.movieinfoservice.models.MovieSummary;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//
//@RestController
//@RequestMapping("/movies")
//public class MovieResource {
//
//    @Value("${api.key}")
//    private String apiKey;
//
////    @Autowired
//    private RestTemplate restTemplate;
//
////    @RequestMapping("/{movieId}")
////    public Movie getMovieInfo(@PathVariable("movieId") String movieId) {
//////        MovieSummary movieSummary = restTemplate.getForObject("https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" +  apiKey, MovieSummary.class);
//////        return new Movie(movieId, movieSummary.getTitle(), movieSummary.getOverview());
//////        return new Movie(movieId, "Transformer", "not bad. huh");
////    	System.out.println("MoiveInfo Service movieID:"+ movieId);
////    	return new Movie(movieId, "Transformer", "not bad. huh");
////
////    }
//    
//    @RequestMapping("/movies/{movieId}")
//    public String getMovieInfo(@PathVariable("movieId") String movieId) {
////        MovieSummary movieSummary = restTemplate.getForObject("https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" +  apiKey, MovieSummary.class);
////        return new Movie(movieId, movieSummary.getTitle(), movieSummary.getOverview());
////        return new Movie(movieId, "Transformer", "not bad. huh");
//    	System.out.println("MoiveInfo Service movieID:"+ movieId);
//    	return "xxx";
//    }
//    
//
//}

package com.example.movieinfo_service.resources;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.movieinfo_service.models.Movie;

@RestController
@RequestMapping("/movies")
public class MovieResource {

    @Value("${api.key}")
    private String apiKey;

    // THIS IS THE ACTIVE ENDPOINT
    @RequestMapping("/{movieId}")
    public Movie getMovieInfo(@PathVariable("movieId") String movieId) {
        System.out.println("MovieInfo Service movieID: " + movieId);
        
        // Returning a hardcoded Movie object for now
        return new Movie(movieId, "Transformer", "not bad. huh");
    }

}
