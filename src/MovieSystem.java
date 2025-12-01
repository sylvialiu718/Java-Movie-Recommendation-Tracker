import java.io.*;
import java.util.*;
/**
 * Watchlist class - Manages the user's movie watchlist
 * Stores movie IDs that the user wants to watch
 */
class Watchlist {
    private List<String> movieIds;

    public Watchlist() {
        this.movieIds = new ArrayList<>();
    }

    public Watchlist(List<String> movieIds) {
        this.movieIds = new ArrayList<>(movieIds);
    }

    public boolean addMovie(String movieId) {
        if (movieId == null || movieId.trim().isEmpty()) {
            System.out.println("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
            System.out.println("The movie is already in the watchlist");
            return false;
        }
        return movieIds.add(movieId);
    }

    public boolean removeMovie(String movieId) {
        if (movieId == null || movieId.trim().isEmpty()) {
            System.out.println("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (!movieIds.contains(movieId)) {
            System.out.println("The movie is not in the watchlist");
            return false;
        }
        return movieIds.remove(movieId);
    }

    public List<String> getMovieIds() {
        return new ArrayList<>(movieIds);
    }

    public String toCsvString() {
        return String.join(";", movieIds);
    }
}

/**
 * History class - Tracks movies that the user has watched
 */
class History {
    private List<String> movieIds;

    public History() {
        this.movieIds = new ArrayList<>();
    }

    public History(List<String> movieIds) {
        this.movieIds = new ArrayList<>(movieIds);
    }

    public boolean addWatchedMovie(String movieId) {
        if (movieId == null || movieId.trim().isEmpty()) {
            System.out.println("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
            System.out.println("The movie is already in the history");
            return false;
        }
        return movieIds.add(movieId);
    }

    public List<String> getWatchedMovieIds() {
        return new ArrayList<>(movieIds);
    }

    public String toCsvString() {
        return String.join(";", movieIds);
    }
}

/**
 * User class - Represents a registered user
 * Contains username, password, watchlist and history
 */
class User {
    private String username;
    private String password;
    private Watchlist watchlist;
    private History history;

    public User(String username, String rawPassword) {
        this.username = username;
        this.password = PasswordEncoder.encodePassword(rawPassword);
        this.watchlist = new Watchlist();
        this.history = new History();
    }

    public User(String username, String encryptedPassword, List<String> watchlistIds, List<String> historyIds) {
        this.username = username;
        this.password = encryptedPassword;
        this.watchlist = new Watchlist(watchlistIds);
        this.history = new History(historyIds);
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Watchlist getWatchlist() { return watchlist; }
    public History getHistory() { return history; }

    public void changePassword(String newRawPassword) {
        this.password = PasswordEncoder.encodePassword(newRawPassword);
    }

    public String toCsvLine() {
        String watchlistStr = watchlist.toCsvString();
        String historyStr = history.toCsvString();
        return String.format("%s,%s,%s,%s", username, password, watchlistStr, historyStr);
    }
}

class UserAuthentication {
    private UserFileHandler fileHandler;
    private Map<String, User> userData;
    private User currentUser;

    public UserAuthentication() {
        fileHandler = new UserFileHandler();
        try {
            userData = fileHandler.readAllUsers();
        } catch (Exception e) {
            System.out.println("User data initialization failed: " + e.getMessage());
            userData = new HashMap<>();
        }
    }

    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            System.out.println("Username and password cannot be empty");
            return false;
        }

        username = username.trim();
        if (userData.containsKey(username)) {
            System.out.println("Username already exists");
            return false;
        }

        User newUser = new User(username, password);
        userData.put(username, newUser);

        try {
            fileHandler.writeAllUsers(userData);
            System.out.println("Registration successful! Please login.");
            return true;
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
            userData.remove(username);
            return false;
        }
    }

    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            System.out.println("Username and password cannot be empty");
            return false;
        }

        username = username.trim();
        String inputPassword = password.trim();

        if (!userData.containsKey(username)) {
            System.out.println("Username does not exist");
            return false;
        }

        User user = userData.get(username);
        boolean passwordMatch = PasswordEncoder.verifyPassword(inputPassword, user.getPassword());

        if (passwordMatch) {
            currentUser = user;
            System.out.println("Login successful! Welcome, " + username + "!");
            return true;
        } else {
            System.out.println("Incorrect password");
            return false;
        }
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("Goodbye, " + currentUser.getUsername() + "!");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean saveUserChanges() {
        if (currentUser == null) {
            System.out.println("No user to save changes for");
            return false;
        }

        try {
            userData.put(currentUser.getUsername(), currentUser);
            fileHandler.writeAllUsers(userData);
            return true;
        } catch (Exception e) {
            System.out.println("Failed to save user data: " + e.getMessage());
            return false;
        }
    }
}

class UserFileHandler {
    private static final String USER_CSV_PATH = "data/users.csv";
    private static final String CSV_HEADER = "Username,Password,Watchlist,History";

    public Map<String, User> readAllUsers() throws IOException {
        Map<String, User> users = new HashMap<>();
        File file = new File(USER_CSV_PATH);

        if (!file.exists()) {
            createNewUserFile();
            return users;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    User user = parseCsvLine(line);
                    users.put(user.getUsername(), user);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid CSV format in line: " + line);
                }
            }
        }
        return users;
    }

    public void writeAllUsers(Map<String, User> users) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_CSV_PATH))) {
            bw.write(CSV_HEADER);
            bw.newLine();
            for (User user : users.values()) {
                bw.write(user.toCsvLine());
                bw.newLine();
            }
        }
    }

    private User parseCsvLine(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Field count mismatch");
        }

        String username = fields[0].trim();
        String password = fields[1].trim();
        List<String> watchlistIds = parseIdList(fields[2].trim());
        List<String> historyIds = parseIdList(fields[3].trim());

        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Username/password cannot be empty");
        }

        return new User(username, password, watchlistIds, historyIds);
    }

    private List<String> parseIdList(String str) {
        List<String> ids = new ArrayList<>();
        if (!str.isEmpty()) {
            String[] idArr = str.split(";");
            for (String id : idArr) {
                id = id.trim();
                if (id.contains("@")) {
                    id = id.split("@")[0].trim();
                }
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private void createNewUserFile() throws IOException {
        File file = new File(USER_CSV_PATH);
        file.createNewFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(CSV_HEADER);
            bw.newLine();
        }
    }
}

class PasswordEncoder {
    public static String encodePassword(String rawPassword) {
        char[] chars = rawPassword.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] + (i % 7) + 3);
        }
        return new String(chars);
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword.equals(storedPassword)) {
            return true;
        }
        String encodedInput = encodePassword(rawPassword);
        return encodedInput.equals(storedPassword);
    }
}

/**
 * MovieSystem - Main CLI application class for movie browsing and recommendations
 */
public class MovieSystem {
    private List<Movie> movies;                    // List of all movies loaded from CSV
    private String movieFilePath;                  // Path to movies CSV file
    private Map<String, List<Movie>> genreMap;     // Map from genre to movies
    private BufferedReader reader;                 // Console input reader
    private UserAuthentication userAuth;           // User authentication handler

    public MovieSystem() {
        this.movies = new ArrayList<>();
        this.movieFilePath = "data/movies.csv";
        this.genreMap = new HashMap<>();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.userAuth = new UserAuthentication();
        loadMovies();
        buildGenreMap();
    }

    /** Inner class representing a movie with id, title, genre, year and rating */
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

        public String toString() {
            return String.format("ID: %s | Title: %-45s | Genre: %-10s | Year: %d | Rating: %.1f",
                    id, title.length() > 45 ? title.substring(0, 42) + "..." : title,
                    genre, year, rating);
        }

        public String toCsvString() {
            return String.format("%s,%s,%s,%d,%.1f", id, title, genre, year, rating);
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Movie movie = (Movie) obj;
            return id.equals(movie.id);
        }

        public int hashCode() {
            return id.hashCode();
        }
    }

    /** Loads all movies from CSV file, skipping the header line */
    public void loadMovies() {
        movies.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(movieFilePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
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
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    private Movie parseMovieFromCsv(String csvLine) {
        String[] parts = csvLine.split(",", -1);

        if (parts.length < 5) {
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
        for (int i = 0; i < sortedMovies.size() - 1; i++) {
            for (int j = i + 1; j < sortedMovies.size(); j++) {
                if (sortedMovies.get(i).getRating() < sortedMovies.get(j).getRating()) {
                    Movie temp = sortedMovies.get(i);
                    sortedMovies.set(i, sortedMovies.get(j));
                    sortedMovies.set(j, temp);
                }
            }
        }

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
        List<String> genres = new ArrayList<>(genreMap.keySet());
        for (int i = 0; i < genres.size() - 1; i++) {
            for (int j = i + 1; j < genres.size(); j++) {
                if (genreMap.get(genres.get(i)).size() < genreMap.get(genres.get(j)).size()) {
                    String temp = genres.get(i);
                    genres.set(i, genres.get(j));
                    genres.set(j, temp);
                }
            }
        }

        for (String genre : genres) {
            System.out.printf("  %-12s: %d movies%n", genre, genreMap.get(genre).size());
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
            try {
                String choice = reader.readLine().trim().toLowerCase();

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
                            int page = Integer.parseInt(reader.readLine().trim());
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
            } catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
                return;
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
        for (int i = 0; i < sortedMovies.size() - 1; i++) {
            for (int j = i + 1; j < sortedMovies.size(); j++) {
                if (sortedMovies.get(i).getYear() < sortedMovies.get(j).getYear()) {
                    Movie temp = sortedMovies.get(i);
                    sortedMovies.set(i, sortedMovies.get(j));
                    sortedMovies.set(j, temp);
                }
            }
        }

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

    private String readInput() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            return "";
        }
    }

    private void showAuthenticationMenu() {
        while (true) {
            System.out.println("\n=== User Authentication ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");

            String choice = readInput().trim();

            switch (choice) {
                case "1":
                    if (performLogin()) {
                        return;
                    }
                    break;
                case "2":
                    performRegistration();
                    break;
                case "3":
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private boolean performLogin() {
        System.out.print("Username: ");
        String username = readInput().trim();
        System.out.print("Password: ");
        String password = readInput().trim();

        return userAuth.login(username, password);
    }

    private void performRegistration() {
        System.out.print("Choose username: ");
        String username = readInput().trim();
        System.out.print("Choose password: ");
        String password = readInput().trim();

        userAuth.register(username, password);
    }

    /**
     * Starts the main system with authentication
     * Show menu in a loop until exit
     */
    public void startMainSystem() {
        showAuthenticationMenu();

        System.out.println("Welcome, " + userAuth.getCurrentUser().getUsername() + "!");
        displayWelcomeMessage();

        boolean running = true;
        while (running) {
            displayMainMenu();
            String choice = readInput().trim();

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
                    getPersonalizedRecommendations();
                    break;
                case "8":
                    recommendByGenre();
                    break;
                case "9":
                    recommendByYear();
                    break;
                case "10":
                    manageUserWatchlist();
                    break;
                case "11":
                    manageUserHistory();
                    break;
                case "12":
                    displayMovieStatistics();
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
        }
    }

    /** Sub-menu for managing user's watchlist: view, add, remove, mark as watched */
    private void manageUserWatchlist() {
        User currentUser = userAuth.getCurrentUser();
        Watchlist watchlist = currentUser.getWatchlist();
        History history = currentUser.getHistory();

        while (true) {
            System.out.println("\n=== Manage Watchlist ===");
            System.out.println("1. View watchlist");
            System.out.println("2. Add movie to watchlist");
            System.out.println("3. Remove movie from watchlist");
            System.out.println("4. Mark movie as watched");
            System.out.println("0. Back to main menu");
            System.out.print("Choose option: ");

            String choice = readInput().trim();
            switch (choice) {
                case "0":
                    return;
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
                    String movieIdToAdd = readInput().trim();
                    if (isValidMovieId(movieIdToAdd)) {
                        if (watchlist.addMovie(movieIdToAdd)) {
                            System.out.println("Movie added to watchlist.");
                            userAuth.saveUserChanges();
                        }
                    } else {
                        System.out.println("Invalid movie ID.");
                    }
                    break;
                case "3":
                    System.out.print("Enter movie ID to remove: ");
                    String movieIdToRemove = readInput().trim();
                    if (watchlist.removeMovie(movieIdToRemove)) {
                        System.out.println("Movie removed from watchlist.");
                        userAuth.saveUserChanges();
                    } else {
                        System.out.println("Movie not found in watchlist.");
                    }
                    break;
                case "4":
                    System.out.print("Enter movie ID to mark as watched: ");
                    String movieIdToWatch = readInput().trim();
                    if (!isValidMovieId(movieIdToWatch)) {
                        System.out.println("Invalid movie ID.");
                        break;
                    }
                    if (!watchlist.getMovieIds().contains(movieIdToWatch)) {
                        System.out.println("Movie is not in your watchlist.");
                        break;
                    }
                    watchlist.removeMovie(movieIdToWatch);
                    history.addWatchedMovie(movieIdToWatch);
                    if (userAuth.saveUserChanges()) {
                        System.out.println("Movie marked as watched. Moved to viewing history.");
                    } else {
                        System.out.println("Failed to save changes.");
                    }
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /**
     * Sub-menu for viewing and adding movies to user's history
     */
    private void manageUserHistory() {
        User currentUser = userAuth.getCurrentUser();
        History history = currentUser.getHistory();

        while (true) {
            System.out.println("\n=== Manage Viewing History ===");
            System.out.println("1. View history");
            System.out.println("2. Add movie to history");
            System.out.println("0. Back to main menu");
            System.out.print("Choose option: ");

            String choice = readInput().trim();
            switch (choice) {
                case "0":
                    return;
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
                    String movieIdToAdd = readInput().trim();
                    if (isValidMovieId(movieIdToAdd)) {
                        if (history.addWatchedMovie(movieIdToAdd)) {
                            System.out.println("Movie added to viewing history.");
                            userAuth.saveUserChanges();
                        }
                    } else {
                        System.out.println("Invalid movie ID.");
                    }
                    break;
                default:
                    System.out.println("Invalid option.");
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

            String choice = readInput();
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
        System.out.println("Movie database loaded successfully!");
        System.out.println("Ready to explore " + getMovieCount() + " amazing movies!");
    }

    private void displayMainMenu() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      MAIN MENU                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  [Browse Movies]                                             ║");
        System.out.println("║    1. View All Movies                                        ║");
        System.out.println("║    2. Browse Movies (Paginated)                              ║");
        System.out.println("║    3. Top Rated Movies                                       ║");
        System.out.println("║    4. Recent Movies                                          ║");
        System.out.println("║    5. Search Movies                                          ║");
        System.out.println("║    6. Browse by Genre                                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  [Recommendations]                                           ║");
        System.out.println("║    7. Get Personalized Recommendations                       ║");
        System.out.println("║    8. Recommend by Genre                                     ║");
        System.out.println("║    9. Recommend by Year                                      ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  [User Features]                                             ║");
        System.out.println("║   10. Manage Watchlist                                       ║");
        System.out.println("║   11. Manage Viewing History                                 ║");
        System.out.println("║   12. Database Statistics                                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║    0. Logout & Exit                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.print("Enter your choice: ");
    }

    private void searchMovies() {
        System.out.print("\nEnter movie title to search: ");
        String searchTerm = readInput().trim();

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
        String term = readInput();
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
        String input = readInput().trim();

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
        String genre = readInput();

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

    private void getPersonalizedRecommendations() {
        User currentUser = userAuth.getCurrentUser();
        if (currentUser == null) {
            System.out.println("Please login first!");
            return;
        }

        List<String> historyIds = currentUser.getHistory().getWatchedMovieIds();
        if (historyIds.isEmpty()) {
            System.out.println("\n=== Personalized Recommendations ===");
            System.out.println("You haven't watched any movies yet.");
            System.out.println("Watch some movies first to get personalized recommendations!");
            System.out.println("\nMeanwhile, here are the top rated movies:");
            displayMoviesSortedByRating();
            return;
        }

        Map<String, Integer> genreCount = new HashMap<>();
        for (String movieId : historyIds) {
            Movie movie = getMovieById(movieId);
            if (movie != null) {
                String genre = movie.getGenre();
                genreCount.put(genre, genreCount.getOrDefault(genre, 0) + 1);
            }
        }

        String favoriteGenre = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteGenre = entry.getKey();
            }
        }

        if (favoriteGenre == null) {
            System.out.println("Unable to determine your preferences. Here are top rated movies:");
            displayMoviesSortedByRating();
            return;
        }

        System.out.println("\n=== Personalized Recommendations ===");
        System.out.println("Based on your watch history, you seem to enjoy " + favoriteGenre + " movies!");
        System.out.println();

        List<Movie> genreMovies = getMoviesByGenre(favoriteGenre);
        List<Movie> recommendations = new ArrayList<>();

        for (Movie movie : genreMovies) {
            if (!historyIds.contains(movie.getId())) {
                recommendations.add(movie);
            }
        }

        for (int i = 0; i < recommendations.size() - 1; i++) {
            for (int j = i + 1; j < recommendations.size(); j++) {
                if (recommendations.get(i).getRating() < recommendations.get(j).getRating()) {
                    Movie temp = recommendations.get(i);
                    recommendations.set(i, recommendations.get(j));
                    recommendations.set(j, temp);
                }
            }
        }

        if (recommendations.isEmpty()) {
            System.out.println("You've watched all " + favoriteGenre + " movies! Try exploring other genres.");
            return;
        }

        System.out.println("Recommended " + favoriteGenre + " movies you haven't watched:");
        System.out.println("==================================================================================================================");
        int count = Math.min(10, recommendations.size());
        for (int i = 0; i < count; i++) {
            System.out.println((i + 1) + ". " + recommendations.get(i));
        }
    }

    private void recommendByGenre() {
        System.out.println("\n=== Recommend by Genre ===");
        Set<String> genres = getAllGenres();
        System.out.println("Available genres: ");
        
        List<String> genreList = new ArrayList<>(genres);
        for (int i = 0; i < genreList.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + genreList.get(i));
        }

        System.out.print("\nEnter genre name or number: ");
        String input = readInput().trim();

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

        System.out.print("How many recommendations? (default 10): ");
        String countInput = readInput().trim();
        int count = 10;
        if (!countInput.isEmpty()) {
            try {
                count = Integer.parseInt(countInput);
                if (count <= 0) count = 10;
            } catch (NumberFormatException e) {
                count = 10;
            }
        }

        List<Movie> genreMovies = getMoviesByGenre(selectedGenre);
        if (genreMovies.isEmpty()) {
            System.out.println("No movies found in genre: " + selectedGenre);
            return;
        }

        // Sort by rating
        List<Movie> sortedMovies = new ArrayList<>(genreMovies);
        for (int i = 0; i < sortedMovies.size() - 1; i++) {
            for (int j = i + 1; j < sortedMovies.size(); j++) {
                if (sortedMovies.get(i).getRating() < sortedMovies.get(j).getRating()) {
                    Movie temp = sortedMovies.get(i);
                    sortedMovies.set(i, sortedMovies.get(j));
                    sortedMovies.set(j, temp);
                }
            }
        }

        System.out.println("\n=== Top " + Math.min(count, sortedMovies.size()) + " " + selectedGenre + " Movies ===");
        System.out.println("==================================================================================================================");
        for (int i = 0; i < Math.min(count, sortedMovies.size()); i++) {
            System.out.println((i + 1) + ". " + sortedMovies.get(i));
        }
    }

    private void recommendByYear() {
        System.out.println("\n=== Recommend by Year ===");

        // Find year range
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        for (Movie movie : movies) {
            if (movie.getYear() < minYear) minYear = movie.getYear();
            if (movie.getYear() > maxYear) maxYear = movie.getYear();
        }

        System.out.println("Available year range: " + minYear + " - " + maxYear);
        System.out.print("Enter year: ");
        String yearInput = readInput().trim();

        int year;
        try {
            year = Integer.parseInt(yearInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid year!");
            return;
        }

        System.out.print("How many recommendations? (default 10): ");
        String countInput = readInput().trim();
        int count = 10;
        if (!countInput.isEmpty()) {
            try {
                count = Integer.parseInt(countInput);
                if (count <= 0) count = 10;
            } catch (NumberFormatException e) {
                count = 10;
            }
        }

        // Get movies from that year
        List<Movie> yearMovies = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getYear() == year) {
                yearMovies.add(movie);
            }
        }

        if (yearMovies.isEmpty()) {
            System.out.println("No movies found from year " + year);
            return;
        }

        // Sort by rating
        for (int i = 0; i < yearMovies.size() - 1; i++) {
            for (int j = i + 1; j < yearMovies.size(); j++) {
                if (yearMovies.get(i).getRating() < yearMovies.get(j).getRating()) {
                    Movie temp = yearMovies.get(i);
                    yearMovies.set(i, yearMovies.get(j));
                    yearMovies.set(j, temp);
                }
            }
        }

        System.out.println("\n=== Top " + Math.min(count, yearMovies.size()) + " Movies from " + year + " ===");
        System.out.println("==================================================================================================================");
        for (int i = 0; i < Math.min(count, yearMovies.size()); i++) {
            System.out.println((i + 1) + ". " + yearMovies.get(i));
        }
    }

    public static void main(String[] args) {
        MovieSystem system = new MovieSystem();

        System.out.println("Choose mode:");
        System.out.println("1. Full Movie System (with authentication)");
        System.out.println("2. Simple Movie Browser (no authentication)");
        System.out.print("Enter choice: ");

        try {
            String mode = system.reader.readLine().trim();

            if ("1".equals(mode)) {
                system.startMainSystem();
            } else {
                system.startSimpleBrowser();
            }
        } catch (IOException e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }
}