import java.util.*;
import java.util.stream.Collectors;

public class RecommendationEngine {
    private MovieDatabase movieDatabase;

    public RecommendationEngine(MovieDatabase movieDatabase) {
        this.movieDatabase = movieDatabase;
    }

    public List<Movie> recommendMoviesByGenre(String genre, int topN) {
        List<Movie> filteredMovies = movieDatabase.getMovies().stream()
                .filter(movie -> movie.getGenre().equalsIgnoreCase(genre))
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .collect(Collectors.toList());

        return filteredMovies.stream().limit(topN).collect(Collectors.toList());
    }

    public List<Movie> recommendMoviesByRating(int topN) {
        return movieDatabase.getMovies().stream()
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public List<Movie> recommendMoviesByYear(int year, int topN) {
        return movieDatabase.getMovies().stream()
                .filter(movie -> movie.getYear() == year)
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }
}
