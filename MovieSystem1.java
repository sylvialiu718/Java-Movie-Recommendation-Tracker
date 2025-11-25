import java.io.*;
import java.util.*;

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
            ExceptionHandler.handleRuntimeException("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
            ExceptionHandler.handleRuntimeException("The movie is already in the watchlist");
            return false;
        }
        return movieIds.add(movieId);
    }

    public boolean removeMovie(String movieId) {
        if (movieId == null || movieId.trim().isEmpty()) {
            ExceptionHandler.handleRuntimeException("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (!movieIds.contains(movieId)) {
            ExceptionHandler.handleRuntimeException("The movie is not in the watchlist");
            return false;
        }
        return movieIds.remove(movieId);
    }

    public List<String> getMovieIds() {
        return new ArrayList<>(movieIds);
    }

    public String toCsvString() {
        return String.join(",", movieIds);
    }
}

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
            ExceptionHandler.handleRuntimeException("Movie ID cannot be empty");
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
            ExceptionHandler.handleRuntimeException("The movie is already in the history");
            return false;
        }
        return movieIds.add(movieId);
    }

    public List<String> getWatchedMovieIds() {
        return new ArrayList<>(movieIds);
    }

    public String toCsvString() {
        return String.join(",", movieIds);
    }
}

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
            ExceptionHandler.handleException("User data initialization failed", e);
            userData = new HashMap<>();
        }
    }

    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            ExceptionHandler.handleRuntimeException("Username and password cannot be empty");
            return false;
        }

        username = username.trim();
        String inputPassword = password.trim();

        if (!userData.containsKey(username)) {
            ExceptionHandler.handleRuntimeException("The username does not exist");
            return false;
        }

        User user = userData.get(username);
        boolean passwordMatch = PasswordEncoder.verifyPassword(inputPassword, user.getPassword());

        if (passwordMatch) {
            currentUser = user;
            return true;
        } else {
            ExceptionHandler.handleRuntimeException("The password is wrong");
            return false;
        }
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            ExceptionHandler.handleRuntimeException("There is no currently logged-in user");
        }
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean saveUserChanges() {
        if (currentUser == null) {
            ExceptionHandler.handleRuntimeException("No logged-in user can be saved");
            return false;
        }

        try {
            userData.put(currentUser.getUsername(), currentUser);
            fileHandler.writeAllUsers(userData);
            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException("Failed to save user data", e);
            return false;
        }
    }
}

class UserFileHandler {
    private static final String USER_CSV_PATH = "users.csv";
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
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
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
                    ExceptionHandler.handleCsvFormatException(line, lineNumber);
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
            String[] idArr = str.split(",");
            for (String id : idArr) {
                id = id.trim();
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
        ExceptionHandler.handleRuntimeException("User data file missing - new file auto-created");
    }
}

class PasswordEncoder {
    // 自定义简单加密实现：字符移位+固定盐值
    private static final String SALT = "mov13579";
    private static final int SHIFT = 3; // 字符移位量

    public static String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            return "";
        }
        // 1. 字符移位加密
        char[] chars = rawPassword.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] + SHIFT);
        }
        String shifted = new String(chars);
        // 2. 拼接盐值
        return shifted + SALT;
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        // 1. 检查是否包含盐值
        if (!storedPassword.endsWith(SALT)) {
            return rawPassword.equals(storedPassword); // 兼容未加密情况
        }
        // 2. 移除盐值
        String shifted = storedPassword.substring(0, storedPassword.length() - SALT.length());
        // 3. 字符移位解密
        char[] chars = shifted.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] - SHIFT);
        }
        String decoded = new String(chars);
        // 4. 比较原始密码
        return decoded.equals(rawPassword);
    }
}

class ExceptionHandler {

    public static void handleException(String message, Exception e) {
        System.out.println("[ERROR] " + message + "：" + e.getMessage());
    }

    public static void handleRuntimeException(String message) {
        System.out.println("[WARNING] " + message);
    }

    public static void handleCsvFormatException(String line, int lineNumber) {
        System.out.println("[ERROR] The " + lineNumber + " row CSV format invalid：" + line);
    }
}

class MovieSystem1 {
    private List<Movie> movies;
    private String movieFilePath;
    private Map<String, List<Movie>> genreMap;
    private Scanner scanner;

    public MovieSystem1() {
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
                    continue; // 跳过标题行
                }

                try {
                    Movie movie = parseMovieFromCsv(line);
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

    private Movie parseMovieFromCsv(String csvLine) {
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
            if (!genreMap.containsKey(genre)) {
                genreMap.put(genre, new ArrayList<Movie>());
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
        List<Movie> genreMovies = genreMap.get(genre);
        return genreMovies != null ? new ArrayList<>(genreMovies) : new ArrayList<Movie>();
    }

    public List<Movie> getMoviesByYearRange(int startYear, int endYear) {
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getYear() >= startYear && movie.getYear() <= endYear) {
                results.add(movie);
            }
        }
        return results;
    }

    public List<Movie> getMoviesByMinRating(double minRating) {
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getRating() >= minRating) {
                results.add(movie);
            }
        }
        return results;
    }

    public List<Movie> getTopRatedMovies(int count) {
        List<Movie> sortedMovies = new ArrayList<>(movies);
        Collections.sort(sortedMovies, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                return Double.compare(m2.getRating(), m1.getRating());
            }
        });
        return sortedMovies.subList(0, Math.min(count, sortedMovies.size()));
    }

    public Set<String> getAllGenres() {
        return genreMap.keySet();
    }

    public void displayMovieStatistics() {
        System.out.println("\n=== Movie Database Statistics ===");
        System.out.println("Total movies: " + movies.size());
        System.out.println("Available genres: " + getAllGenres().size());

        System.out.println("\nMovies by genre:");
        // 创建按电影数量排序的条目列表
        List<Map.Entry<String, List<Movie>>> sortedEntries = new ArrayList<>(genreMap.entrySet());
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, List<Movie>>>() {
            @Override
            public int compare(Map.Entry<String, List<Movie>> e1, Map.Entry<String, List<Movie>> e2) {
                return Integer.compare(e2.getValue().size(), e1.getValue().size());
            }
        });

        for (Map.Entry<String, List<Movie>> entry : sortedEntries) {
            System.out.printf("  %-12s: %d movies%n", entry.getKey(), entry.getValue().size());
        }

        // 计算年份范围
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        for (Movie movie : movies) {
            int year = movie.getYear();
            if (year < minYear) minYear = year;
            if (year > maxYear) maxYear = year;
        }
        if (minYear == Integer.MAX_VALUE) minYear = 0;
        if (maxYear == Integer.MIN_VALUE) maxYear = 0;

        System.out.printf("Year range: %d - %d%n", minYear, maxYear);

        // 计算平均评分
        double totalRating = 0;
        for (Movie movie : movies) {
            totalRating += movie.getRating();
        }
        double avgRating = movies.isEmpty() ? 0 : totalRating / movies.size();
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
        List<Movie> sortedMovies = new ArrayList<>(movies);
        Collections.sort(sortedMovies, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                return Double.compare(m2.getRating(), m1.getRating());
            }
        });

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
            for (Movie movie : results) {
                System.out.println(movie);
            }
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
            for (Movie movie : movies) {
                System.out.println(movie);
            }
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