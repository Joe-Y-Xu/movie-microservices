package com.example.artifact_catalog_service;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import io.javabrains.movieinfoservice.models.Movie;
import io.javabrains.ratingsdataservice.model.Rating;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/catalog")
public class DemoController {
    
    private final WebClient webClient;
    
    public DemoController(WebClient.Builder webBuilder) {
        this.webClient = webBuilder.baseUrl("http://movieinfo-service:80").build();
    }
    
    @RequestMapping("/{userID}")
    public Flux<catalogItem> getCatalog(@PathVariable("userID") int userID) {
        
        List<Rating> ratings_arr = Arrays.asList(
            new Rating("1", 1),
            new Rating("2", 3)
        );
        
        // Convert list to Flux and process reactively
        return Flux.fromIterable(ratings_arr)
            .flatMap(rating -> 
                webClient.get()  
                    .uri("/movies/" + rating.getMovieId())
                    .retrieve()
                    .bodyToMono(Movie.class)
                    .map(movie -> new catalogItem(movie.getName(), movie.getMovieId(), rating.getRating()))
            );
    }
}