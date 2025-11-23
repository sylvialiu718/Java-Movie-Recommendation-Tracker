import java.util.*;

public class MovieRecommendationSystem {
    public static void main(String[] args) {
        MovieDatabase movieDatabase = new MovieDatabase();
        RecommendationEngine recommendationEngine = new RecommendationEngine(movieDatabase);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to the Movie Recommendation System!");

        System.out.println("Enter genre to get recommendations: ");
        String genre = scanner.nextLine();
        System.out.println("Enter number of top recommendations (N): ");
        int topN = scanner.nextInt();

        List<Movie> recommendations = recommendationEngine.recommendMoviesByGenre(genre, topN);
        System.out.println("Top " + topN + " recommended movies based on genre '" + genre + "':");
        for (Movie movie : recommendations) {
            System.out.println(movie);
        }

        recommendations = recommendationEngine.recommendMoviesByRating(topN);
        System.out.println("\nTop " + topN + " recommended movies by rating:");
        for (Movie movie : recommendations) {
            System.out.println(movie);
        }

        scanner.close();
    }
}
