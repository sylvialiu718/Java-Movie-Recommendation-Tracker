import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MovieSystem {
    private List<Movie> movies;
    private String movieFilePath;
    private Map<String, List<Movie>> genreMap;

    private Scanner scanner;

    public MovieSystem() {
        this.movies = new ArrayList<>();
        this.movieFilePath = "movies.csv";
        this.genreMap = new HashMap<>();
        this.scanner = new Scanner(System.in);
        loadMovies();
        buildGenreMap();
    }


    static class Movie {
        protected String id;
        protected String title;
        protected String genre;
        protected int year;
        protected double rating;

        public Movie(String id, String title, String genre, int year, double rating) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.year = year;
            this.rating = rating;
        }


        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public int getYear() { return year; }
        public double getRating() { return rating; }

        public void setTitle(String title) { this.title = title; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setYear(int year) { this.year = year; }
        public void setRating(double rating) { this.rating = rating; }

        @Override
        public String toString() {
            return String.format("ID: %s | Title: %-45s | Genre: %-12s | Year: %d | Rating: %.1f",
                    id, title.length() > 45 ? title.substring(0, 42) + "..." : title,
                    genre, year, rating);
        }

        public String toCSVString() {
            return String.format("%s,%s,%s,%d,%.1f", id, title, genre, year, rating);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Movie movie = (Movie) obj;
            return id.equals(movie.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }


    public void loadMovies() {
        movies.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(movieFilePath))) {
            String line;
            boolean isFirstLine = true;
            int lineCount = 0;

            while ((line = br.readLine()) != null) {
                lineCount++;
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 跳过标题行
                }

                try {
                    Movie movie = parseMovieFromCSV(line);
                    if (movie != null) {
                        movies.add(movie);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing movie line " + lineCount + ": " + line);
                    System.err.println("Error details: " + e.getMessage());
                }
            }
            System.out.println("Successfully loaded " + movies.size() + " movies from " + movieFilePath);

        } catch (FileNotFoundException e) {
            System.err.println("Movie file not found: " + movieFilePath);
            System.err.println("Please ensure the movie CSV file exists at: " + new File(movieFilePath).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error reading movie file: " + e.getMessage());
        }
    }


    private Movie parseMovieFromCSV(String csvLine) {
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 5) {
            System.err.println("Invalid movie data (insufficient columns): " + csvLine);
            return null;
        }

        try {
            String id = parts[0].trim();
            String title = parts[1].trim();
            String genre = parts[2].trim();
            int year = Integer.parseInt(parts[3].trim());
            double rating = Double.parseDouble(parts[4].trim());

            return new Movie(id, title, genre, year, rating);

        } catch (NumberFormatException e) {
            System.err.println("Error parsing numbers in movie data: " + csvLine);
            System.err.println("Make sure year and rating are valid numbers.");
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing movie data: " + csvLine);
            e.printStackTrace();
            return null;
        }
    }


    private void buildGenreMap() {
        genreMap.clear();
        for (Movie movie : movies) {
            String genre = movie.getGenre();
            genreMap.computeIfAbsent(genre, k -> new ArrayList<>()).add(movie);
        }
    }


    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies);
    }


    public Movie getMovieById(String id) {
        for (Movie movie : movies) {
            if (movie.getId().equalsIgnoreCase(id)) {
                return movie;
            }
        }
        return null;
    }


    public List<Movie> searchMoviesByTitle(String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return movies.stream()
                .filter(movie -> movie.getTitle().toLowerCase().contains(lowerSearchTerm))
                .collect(Collectors.toList());
    }


    public List<Movie> getMoviesByGenre(String genre) {
        return genreMap.getOrDefault(genre, new ArrayList<>());
    }


    public List<Movie> getMoviesByYearRange(int startYear, int endYear) {
        return movies.stream()
                .filter(movie -> movie.getYear() >= startYear && movie.getYear() <= endYear)
                .collect(Collectors.toList());
    }


    public List<Movie> getMoviesByMinRating(double minRating) {
        return movies.stream()
                .filter(movie -> movie.getRating() >= minRating)
                .collect(Collectors.toList());
    }


    public List<Movie> getTopRatedMovies(int count) {
        return movies.stream()
                .sorted((m1, m2) -> Double.compare(m2.getRating(), m1.getRating()))
                .limit(count)
                .collect(Collectors.toList());
    }


    public Set<String> getAllGenres() {
        return genreMap.keySet();
    }


    public void displayMovieStatistics() {
        System.out.println("\n=== Movie Database Statistics ===");
        System.out.println("Total movies: " + movies.size());
        System.out.println("Available genres: " + getAllGenres().size());

        System.out.println("\nMovies by genre:");
        genreMap.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .forEach(entry ->
                        System.out.printf("  %-12s: %d movies%n", entry.getKey(), entry.getValue().size()));

        int minYear = movies.stream().mapToInt(Movie::getYear).min().orElse(0);
        int maxYear = movies.stream().mapToInt(Movie::getYear).max().orElse(0);
        System.out.printf("Year range: %d - %d%n", minYear, maxYear);

        double avgRating = movies.stream().mapToDouble(Movie::getRating).average().orElse(0);
        System.out.printf("Average rating: %.2f%n", avgRating);
    }


    public void displayAllMovies() {
        if (movies.isEmpty()) {
            System.out.println("No movies available in the database.");
            return;
        }

        System.out.println("\n=== All Movies (" + movies.size() + " total) ===");
        System.out.println("==================================================================================================================");
        System.out.printf("%-6s %-45s %-12s %-6s %-6s%n", "ID", "Title", "Genre", "Year", "Rating");
        System.out.println("==================================================================================================================");

        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            String displayTitle = movie.getTitle();
            if (displayTitle.length() > 45) {
                displayTitle = displayTitle.substring(0, 42) + "...";
            }
            System.out.printf("%-6s %-45s %-12s %-6d %-6.1f%n",
                    movie.getId(), displayTitle, movie.getGenre(), movie.getYear(), movie.getRating());
        }
    }


    public void displayMoviesPaginated(int pageSize) {
        if (movies.isEmpty()) {
            System.out.println("No movies available.");
            return;
        }

        int totalPages = (int) Math.ceil((double) movies.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, movies.size());

            System.out.println("\n=== Movies Page " + currentPage + " of " + totalPages + " ===");
            System.out.println("Showing " + (startIndex + 1) + " to " + endIndex + " of " + movies.size() + " movies");
            System.out.println("==================================================================================================================");

            for (int i = startIndex; i < endIndex; i++) {
                System.out.println((i + 1) + ". " + movies.get(i));
            }

            System.out.println("\nNavigation: [N]ext page, [P]revious page, [G]o to page, [Q]uit");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim().toLowerCase();

            switch (choice) {
                case "n":
                    if (currentPage < totalPages) currentPage++;
                    break;
                case "p":
                    if (currentPage > 1) currentPage--;
                    break;
                case "g":
                    System.out.print("Enter page number (1-" + totalPages + "): ");
                    try {
                        int page = Integer.parseInt(scanner.nextLine().trim());
                        if (page >= 1 && page <= totalPages) {
                            currentPage = page;
                        } else {
                            System.out.println("Invalid page number!");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number!");
                    }
                    break;
                case "q":
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }


    public void displayMoviesSortedByRating() {
        List<Movie> sortedMovies = movies.stream()
                .sorted((m1, m2) -> Double.compare(m2.getRating(), m1.getRating()))
                .collect(Collectors.toList());

        System.out.println("\n=== Top Rated Movies ===");
        System.out.println("==================================================================================================================");
        for (int i = 0; i < Math.min(20, sortedMovies.size()); i++) {
            System.out.println((i + 1) + ". " + sortedMovies.get(i));
        }
    }


    public void displayMoviesSortedByYear() {
        List<Movie> sortedMovies = movies.stream()
                .sorted((m1, m2) -> Integer.compare(m2.getYear(), m1.getYear()))
                .collect(Collectors.toList());

        System.out.println("\n=== Recent Movies (Sorted by Year) ===");
        System.out.println("==================================================================================================================");
        for (int i = 0; i < Math.min(20, sortedMovies.size()); i++) {
            System.out.println((i + 1) + ". " + sortedMovies.get(i));
        }
    }


    public boolean isValidMovieId(String movieId) {
        return getMovieById(movieId) != null;
    }


    public int getMovieCount() {
        return movies.size();
    }


    public void startMainSystem() {
        displayWelcomeMessage();

        boolean running = true;
        while (running) {
            displayMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayAllMovies();
                    break;
                case "2":
                    displayMoviesPaginated(10);
                    break;
                case "3":
                    displayMoviesSortedByRating();
                    break;
                case "4":
                    displayMoviesSortedByYear();
                    break;
                case "5":
                    searchMovies();
                    break;
                case "6":
                    browseByGenre();
                    break;
                case "7":
                    displayMovieStatistics();
                    break;
                case "8":
                    testMovieFunctions();
                    break;
                case "0":
                    System.out.println("Thank you for using the Movie System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }

            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }


    public void startSimpleBrowser() {
        System.out.println("=== Simple Movie Browser ===");

        boolean browsing = true;
        while (browsing) {
            System.out.println("\nOptions:");
            System.out.println("1. Show all movies");
            System.out.println("2. Search movies");
            System.out.println("3. Browse by genre");
            System.out.println("4. Show statistics");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    displayAllMovies();
                    break;
                case "2":
                    simpleSearchMovies();
                    break;
                case "3":
                    simpleBrowseByGenre();
                    break;
                case "4":
                    displayMovieStatistics();
                    break;
                case "5":
                    browsing = false;
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }

        System.out.println("Goodbye!");
    }

    private void displayWelcomeMessage() {
        System.out.println("\n" +
                "  __  __                 _                   \n" +
                " |  \\/  |               | |                  \n" +
                " | \\  / | _____   ____ _| |_ ___  _ __ ___   \n" +
                " | |\\/| |/ _ \\ \\ / / _` | __/ _ \\| '_ ` _ \\  \n" +
                " | |  | | (_) \\ V / (_| | || (_) | | | | | | \n" +
                " |_|  |_|\\___/ \\_/ \\__,_|\\__\\___/|_| |_| |_| \n" +
                "                                            \n");
        System.out.println("Movie database loaded successfully!");
        System.out.println("Ready to explore " + getMovieCount() + " amazing movies!");
    }

    private void displayMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. View All Movies");
        System.out.println("2. Browse Movies (Paginated)");
        System.out.println("3. Top Rated Movies");
        System.out.println("4. Recent Movies");
        System.out.println("5. Search Movies");
        System.out.println("6. Browse by Genre");
        System.out.println("7. Database Statistics");
        System.out.println("8. Test Movie Functions");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private void searchMovies() {
        System.out.print("\nEnter movie title to search: ");
        String searchTerm = scanner.nextLine().trim();

        if (searchTerm.isEmpty()) {
            System.out.println("Search term cannot be empty!");
            return;
        }

        var results = searchMoviesByTitle(searchTerm);
        if (results.isEmpty()) {
            System.out.println("No movies found matching: " + searchTerm);
        } else {
            System.out.println("\n=== Search Results for \"" + searchTerm + "\" (" + results.size() + " found) ===");
            for (int i = 0; i < results.size(); i++) {
                System.out.println((i + 1) + ". " + results.get(i));
            }
        }
    }

    private void simpleSearchMovies() {
        System.out.print("Enter search term: ");
        String term = scanner.nextLine();
        var results = searchMoviesByTitle(term);

        if (results.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Found " + results.size() + " movies:");
            results.forEach(System.out::println);
        }
    }

    private void browseByGenre() {
        var genres = getAllGenres();
        System.out.println("\n=== Available Genres ===");

        List<String> genreList = new ArrayList<>(genres);
        for (int i = 0; i < genreList.size(); i++) {
            System.out.println((i + 1) + ". " + genreList.get(i));
        }

        System.out.print("\nEnter genre name or number: ");
        String input = scanner.nextLine().trim();

        String selectedGenre;
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= genreList.size()) {
                selectedGenre = genreList.get(choice - 1);
            } else {
                System.out.println("Invalid number!");
                return;
            }
        } catch (NumberFormatException e) {
            selectedGenre = input;
        }

        var genreMovies = getMoviesByGenre(selectedGenre);
        if (genreMovies.isEmpty()) {
            System.out.println("No movies found in genre: " + selectedGenre);
            System.out.println("Available genres: " + String.join(", ", genres));
        } else {
            System.out.println("\n=== " + selectedGenre + " Movies (" + genreMovies.size() + " total) ===");
            for (int i = 0; i < genreMovies.size(); i++) {
                System.out.println((i + 1) + ". " + genreMovies.get(i));
            }
        }
    }

    private void simpleBrowseByGenre() {
        System.out.println("Available genres: " + String.join(", ", getAllGenres()));
        System.out.print("Enter genre: ");
        String genre = scanner.nextLine();

        var movies = getMoviesByGenre(genre);
        if (movies.isEmpty()) {
            System.out.println("No movies found in that genre.");
        } else {
            System.out.println("Found " + movies.size() + " movies in " + genre + ":");
            movies.forEach(System.out::println);
        }
    }

    private void testMovieFunctions() {
        System.out.println("\n=== Testing Movie Functions ===");

        System.out.println("\n1. Testing movie lookup by ID:");
        Movie movie = getMovieById("M001");
        if (movie != null) {
            System.out.println("Found: " + movie);
        } else {
            System.out.println("Movie not found!");
        }

        System.out.println("\n2. Testing high-rated movies (8.5+):");
        var highRated = getMoviesByMinRating(8.5);
        System.out.println("Found " + highRated.size() + " movies with rating 8.5+");

        System.out.println("\n3. Testing movies from 2020 onwards:");
        var recentMovies = getMoviesByYearRange(2020, 2025);
        System.out.println("Found " + recentMovies.size() + " movies from 2020 onwards");

        System.out.println("\n4. Testing top 5 rated movies:");
        var topMovies = getTopRatedMovies(5);
        for (int i = 0; i < topMovies.size(); i++) {
            System.out.println((i + 1) + ". " + topMovies.get(i));
        }

        System.out.println("\nAll movie function tests completed!");
    }


    public static void main(String[] args) {
        MovieSystem system = new MovieSystem();

        System.out.println("Choose mode:");
        System.out.println("1. Full Movie System");
        System.out.println("2. Simple Movie Browser");
        System.out.print("Enter choice: ");

        Scanner modeScanner = new Scanner(System.in);
        String mode = modeScanner.nextLine().trim();

        if ("1".equals(mode)) {
            system.startMainSystem();
        } else {
            system.startSimpleBrowser();
        }

        modeScanner.close();
    }

}
