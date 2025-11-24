import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendEngine {
    private List<Movie> movies;
    private Map<String, List<Movie>> genreMap;
    private Scanner scanner;

    public RecommendEngine() {
        this.movies = new ArrayList<>();
        this.genreMap = new HashMap<>();
        this.scanner = new Scanner(System.in);
        loadMovies();
        buildGenreMap();
    }

    // Movie class: Store movie information
    static class Movie {
        private String id;
        private String title;
        private String genre;
        private double rating;
        private LocalDate releaseDate;

        public Movie(String id, String title, String genre, double rating, LocalDate releaseDate) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.rating = rating;
            this.releaseDate = releaseDate;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public double getRating() { return rating; }
        public LocalDate getReleaseDate() { return releaseDate; }

        @Override
        public String toString() {
            return String.format("ID: %s | Title: %-30s | Genre: %-12s | Rating: %.1f | Release: %s",
                    id, title, genre, rating, releaseDate.toString());
        }
    }

    // Load movies from CSV file
    private void loadMovies() {
        File file = new File("movies.csv");
        if (!file.exists()) {
            System.err.println("Movie file not found: movies.csv");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the header line
                }
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 5);
                if (parts.length != 5) {
                    System.err.println("Invalid movie data: " + line);
                    continue;
                }

                try {
                    String id = parts[0].trim();
                    String title = parts[1].trim();
                    String genre = parts[2].trim();
                    double rating = Double.parseDouble(parts[3].trim());
                    LocalDate releaseDate = LocalDate.parse(parts[4].trim(), formatter);

                    movies.add(new Movie(id, title, genre, rating, releaseDate));
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.err.println("Error parsing movie data: " + line);
                }
            }
            System.out.println("Loaded " + movies.size() + " movies successfully!");
        } catch (IOException e) {
            System.err.println("Error reading movie file: " + e.getMessage());
        }
    }

    // Build a genre map to store movies by genre
    private void buildGenreMap() {
        for (Movie movie : movies) {
            String genre = movie.getGenre().toLowerCase();
            genreMap.computeIfAbsent(genre, k -> new ArrayList<>()).add(movie);
        }
    }

    // Get movies by genre
    public List<Movie> getMoviesByGenre(String genre) {
        return genreMap.getOrDefault(genre.toLowerCase(), new ArrayList<>());
    }

    // Get all movies
    public List<Movie> getMovies() {
        return movies;
    }

    // Movie recommendation by genre
    public List<Movie> recommendMoviesByGenre(String genre, int topN) {
        List<Movie> filteredMovies = getMoviesByGenre(genre).stream()
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .collect(Collectors.toList());

        return filteredMovies.stream().limit(topN).collect(Collectors.toList());
    }

    // Movie recommendation by rating
    public List<Movie> recommendMoviesByRating(int topN) {
        return getMovies().stream()
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    // Movie recommendation by year
    public List<Movie> recommendMoviesByYear(int year, int topN) {
        return getMovies().stream()
                .filter(movie -> movie.getReleaseDate().getYear() == year)
                .sorted(Comparator.comparingDouble(Movie::getRating).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    // Display the main menu for user interaction
    private void displayMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. View All Movies");
        System.out.println("2. Browse by Genre");
        System.out.println("3. Recommend Movies by Genre");
        System.out.println("4. Recommend Top Rated Movies");
        System.out.println("5. Recommend Movies by Year");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    // Display all movies
    private void displayAllMovies() {
        System.out.println("\n=== All Movies ===");
        movies.forEach(System.out::println);
    }

    // Browse movies by genre
    private void browseByGenre() {
        System.out.print("\nEnter genre to browse: ");
        String genre = scanner.nextLine().trim().toLowerCase();
        List<Movie> genreMovies = getMoviesByGenre(genre);

        if (genreMovies.isEmpty()) {
            System.out.println("No movies found for genre: " + genre);
        } else {
            System.out.println("\n=== Movies in Genre: " + genre + " ===");
            genreMovies.forEach(System.out::println);
        }
    }

    // Recommend movies based on genre
    private void recommendMoviesByGenre() {
        System.out.print("Enter genre to get recommendations: ");
        String genre = scanner.nextLine().trim();
        System.out.print("Enter number of top recommendations: ");
        int topN = Integer.parseInt(scanner.nextLine().trim());

        List<Movie> recommendations = recommendMoviesByGenre(genre, topN);
        recommendations.forEach(System.out::println);
    }

    // Recommend top rated movies
    private void recommendTopRatedMovies() {
        System.out.print("Enter number of top rated movies to recommend: ");
        int topN = Integer.parseInt(scanner.nextLine().trim());

        List<Movie> recommendations = recommendMoviesByRating(topN);
        recommendations.forEach(System.out::println);
    }

    // Recommend movies by a specific year
    private void recommendMoviesByYear() {
        System.out.print("Enter year to get recommendations: ");
        int year = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter number of top recommendations: ");
        int topN = Integer.parseInt(scanner.nextLine().trim());

        List<Movie> recommendations = recommendMoviesByYear(year, topN);
        recommendations.forEach(System.out::println);
    }

    // Start the main system
    public void startMainSystem() {
        System.out.println("Welcome to Movie System!");

        while (true) {
            displayMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayAllMovies();
                    break;
                case "2":
                    browseByGenre();
                    break;
                case "3":
                    recommendMoviesByGenre();
                    break;
                case "4":
                    recommendTopRatedMovies();
                    break;
                case "5":
                    recommendMoviesByYear();
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        MovieSystem system = new MovieSystem();
        system.startMainSystem();
    }
}
