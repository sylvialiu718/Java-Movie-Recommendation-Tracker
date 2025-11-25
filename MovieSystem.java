import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MovieSystem {
    private List<Movie> movies;
    private String movieFilePath;
    private Map<String, List<Movie>> genreMap;
    private Scanner scanner;
    private UserAuthentication userAuth;

    public MovieSystem() {
        this.movies = new ArrayList<>();
        this.movieFilePath = "movies.csv";
        this.genreMap = new HashMap<>();
        this.scanner = new Scanner(System.in);
        this.userAuth = new UserAuthentication();
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

        @Override
        public String toString() {
            return String.format("ID: %s | Title: %-45s | Genre: %-12s | Year: %d | Rating: %.1f",
                    id, title.length() > 45 ? title.substring(0, 42) + "..." : title,
                    genre, year, rating);
        }

        public String toCsvString() {
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
                    continue;
                }

                try {
                    Movie movie = parseMovieFromCsv(line);
                    if (movie != null) {
                        movies.add(movie);
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing movie line " + lineCount + ": " + line + " - " + e.getMessage());
                }
            }
            System.out.println("Successfully loaded " + movies.size() + " movies from " + movieFilePath);

        } catch (FileNotFoundException e) {
            System.out.println("Movie file not found: " + movieFilePath);
        } catch (IOException e) {
            System.out.println("Error reading movie file: " + e.getMessage());
        }
    }

    private Movie parseMovieFromCsv(String csvLine) {
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 5) {
            System.out.println("Invalid movie data (insufficient columns): " + csvLine);
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
            System.out.println("Error parsing numbers in movie data: " + csvLine);
            return null;
        } catch (Exception e) {
            System.out.println("Unexpected error parsing movie data: " + csvLine);
            return null;
        }
    }

    private void buildGenreMap() {
        genreMap.clear();
        for (Movie movie : movies) {
            String genre = movie.getGenre();
            if (!genreMap.containsKey(genre)) {
                genreMap.put(genre, new ArrayList<>());
            }
            genreMap.get(genre).add(movie);
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
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getTitle().toLowerCase().contains(lowerSearchTerm)) {
                results.add(movie);
            }
        }
        return results;
    }

    public List<Movie> getMoviesByGenre(String genre) {
        List<Movie> result = genreMap.get(genre);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    public List<Movie> getTopRatedMovies(int count) {
        List<Movie> sortedMovies = new ArrayList<>(movies);
        // 使用传统的排序方法
        Collections.sort(sortedMovies, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                return Double.compare(m2.getRating(), m1.getRating());
            }
        });

        List<Movie> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, sortedMovies.size()); i++) {
            result.add(sortedMovies.get(i));
        }
        return result;
    }

    public Set<String> getAllGenres() {
        return genreMap.keySet();
    }

    public void displayMovieStatistics() {
        System.out.println("\n=== Movie Database Statistics ===");
        System.out.println("Total movies: " + movies.size());
        System.out.println("Available genres: " + getAllGenres().size());

        System.out.println("\nMovies by genre:");
        List<Map.Entry<String, List<Movie>>> genreEntries = new ArrayList<>(genreMap.entrySet());
        Collections.sort(genreEntries, new Comparator<Map.Entry<String, List<Movie>>>() {
            @Override
            public int compare(Map.Entry<String, List<Movie>> e1, Map.Entry<String, List<Movie>> e2) {
                return Integer.compare(e2.getValue().size(), e1.getValue().size());
            }
        });

        for (Map.Entry<String, List<Movie>> entry : genreEntries) {
            System.out.printf("  %-12s: %d movies%n", entry.getKey(), entry.getValue().size());
        }

        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        double totalRating = 0;

        for (Movie movie : movies) {
            if (movie.getYear() < minYear) minYear = movie.getYear();
            if (movie.getYear() > maxYear) maxYear = movie.getYear();
            totalRating += movie.getRating();
        }

        if (movies.size() > 0) {
            System.out.printf("Year range: %d - %d%n", minYear, maxYear);
            double avgRating = totalRating / movies.size();
            System.out.printf("Average rating: %.2f%n", avgRating);
        }
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
        List<Movie> sortedMovies = getTopRatedMovies(movies.size());

        System.out.println("\n=== Top Rated Movies ===");
        System.out.println("==================================================================================================================");
        for (int i = 0; i < Math.min(20, sortedMovies.size()); i++) {
            System.out.println((i + 1) + ". " + sortedMovies.get(i));
        }
    }

    public void displayMoviesSortedByYear() {
        List<Movie> sortedMovies = new ArrayList<>(movies);
        Collections.sort(sortedMovies, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                return Integer.compare(m2.getYear(), m1.getYear());
            }
        });

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

    private void showAuthenticationMenu() {
        while (true) {
            System.out.println("\n=== User Authentication ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    if (performLogin()) {
                        return; // Login successful, proceed to main system
                    }
                    break;
                case "2":
                    performRegistration();
                    break;
                case "3":
                    System.out.println("Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private boolean performLogin() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        return userAuth.login(username, password);
    }

    private void performRegistration() {
        System.out.print("Choose username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Choose password: ");
        String password = scanner.nextLine().trim();

        userAuth.register(username, password);
    }

    public void startMainSystem() {
        showAuthenticationMenu(); // This will now show the authentication menu

        System.out.println("Welcome, " + userAuth.getCurrentUser().getUsername() + "!");
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
                    manageUserWatchlist();
                    break;
                case "9":
                    manageUserHistory();
                    break;
                case "0":
                    System.out.println("Thank you for using the Movie System. Goodbye!");
                    userAuth.saveUserChanges();
                    userAuth.logout();
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

    private void manageUserWatchlist() {
        User currentUser = userAuth.getCurrentUser();
        Watchlist watchlist = currentUser.getWatchlist();

        System.out.println("\n=== Manage Watchlist ===");
        System.out.println("1. View watchlist");
        System.out.println("2. Add movie to watchlist");
        System.out.println("3. Remove movie from watchlist");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                List<String> watchlistIds = watchlist.getMovieIds();
                if (watchlistIds.isEmpty()) {
                    System.out.println("Your watchlist is empty.");
                } else {
                    System.out.println("Your watchlist:");
                    for (String movieId : watchlistIds) {
                        Movie movie = getMovieById(movieId);
                        if (movie != null) {
                            System.out.println(" - " + movie.getTitle() + " (ID: " + movieId + ")");
                        }
                    }
                }
                break;
            case "2":
                System.out.print("Enter movie ID to add: ");
                String movieIdToAdd = scanner.nextLine().trim();
                if (isValidMovieId(movieIdToAdd)) {
                    if (watchlist.addMovie(movieIdToAdd)) {
                        System.out.println("Movie added to watchlist.");
                    }
                } else {
                    System.out.println("Invalid movie ID.");
                }
                break;
            case "3":
                System.out.print("Enter movie ID to remove: ");
                String movieIdToRemove = scanner.nextLine().trim();
                if (watchlist.removeMovie(movieIdToRemove)) {
                    System.out.println("Movie removed from watchlist.");
                } else {
                    System.out.println("Movie not found in watchlist.");
                }
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void manageUserHistory() {
        User currentUser = userAuth.getCurrentUser();
        History history = currentUser.getHistory();

        System.out.println("\n=== Manage Viewing History ===");
        System.out.println("1. View history");
        System.out.println("2. Add movie to history");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                List<String> historyIds = history.getWatchedMovieIds();
                if (historyIds.isEmpty()) {
                    System.out.println("Your viewing history is empty.");
                } else {
                    System.out.println("Your viewing history:");
                    for (String movieId : historyIds) {
                        Movie movie = getMovieById(movieId);
                        if (movie != null) {
                            System.out.println(" - " + movie.getTitle() + " (ID: " + movieId + ")");
                        }
                    }
                }
                break;
            case "2":
                System.out.print("Enter movie ID to add to history: ");
                String movieIdToAdd = scanner.nextLine().trim();
                if (isValidMovieId(movieIdToAdd)) {
                    if (history.addWatchedMovie(movieIdToAdd)) {
                        System.out.println("Movie added to viewing history.");
                    }
                } else {
                    System.out.println("Invalid movie ID.");
                }
                break;
            default:
                System.out.println("Invalid option.");
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
        System.out.println("8. Manage Watchlist");
        System.out.println("9. Manage Viewing History");
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

        List<Movie> results = searchMoviesByTitle(searchTerm);
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
        List<Movie> results = searchMoviesByTitle(term);

        if (results.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Found " + results.size() + " movies:");
            for (Movie movie : results) {
                System.out.println(movie);
            }
        }
    }

    private void browseByGenre() {
        Set<String> genres = getAllGenres();
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

        List<Movie> genreMovies = getMoviesByGenre(selectedGenre);
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

        List<Movie> movies = getMoviesByGenre(genre);
        if (movies.isEmpty()) {
            System.out.println("No movies found in that genre.");
        } else {
            System.out.println("Found " + movies.size() + " movies in " + genre + ":");
            for (Movie movie : movies) {
                System.out.println(movie);
            }
        }
    }

    public static void main(String[] args) {
        MovieSystem system = new MovieSystem();

        System.out.println("Choose mode:");
        System.out.println("1. Full Movie System (with authentication)");
        System.out.println("2. Simple Movie Browser (no authentication)");
        System.out.print("Enter choice: ");

        String mode = system.scanner.nextLine().trim();

        if ("1".equals(mode)) {
            system.startMainSystem();
        } else {
            system.startSimpleBrowser();
        }
    }
}
