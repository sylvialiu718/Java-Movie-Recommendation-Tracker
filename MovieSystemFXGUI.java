import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

/**
 * Movie Recommendation System
 * Main entry point for the JavaFX application.
 * Handles UI layout, user authentication, and movie browsing logic.
 *
 * @author [Your Name]
 * @version 1.0
 */
public class MovieSystemFXGUI extends Application {

    // Core data structures
    private List<MovieData> allMovies = new ArrayList<>();
    private Map<String, List<MovieData>> moviesByGenre = new HashMap<>();
    private Map<String, UserData> systemUsers = new HashMap<>();
    
    // Session state
    private UserData currentUser = null;

    // UI Elements
    private TableView<MovieData> movieTable;
    private ObservableList<MovieData> tableModel; // Data source for the table
    private ListView<String> watchlistView;
    private ObservableList<String> watchlistModel;
    private ListView<String> historyView;
    private ObservableList<String> historyModel;
    
    // Controls
    private TextField searchInput;
    private ComboBox<String> genreSelector;
    private Label statusLabel;
    private Label welcomeLabel;
    
    // Layout Containers
    private BorderPane mainLayout;
    private VBox loginLayout;
    private Stage primaryStage;

    private static final String MOVIES_PATH = "data/movies.csv";
    private static final String USERS_PATH = "data/users.csv";

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Application entry point.
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Movie Recommendation System");

        // Initialize system data and UI components
        initSystem();

        Scene scene = new Scene(loginLayout, 1200, 800);
        
        // Try to load CSS if it exists, otherwise ignore
        try {
            if (getClass().getResource("style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Warning: style.css not found.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Helper to clean up the start method.
     * Loads data and builds the initial views.
     */
    private void initSystem() {
        loadMovieDatabase();
        loadUserDatabase();
        
        buildLoginScreen();
        buildMainDashboard();
    }

    private void loadMovieDatabase() {
        allMovies.clear();
        moviesByGenre.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(MOVIES_PATH))) {
            String record;
            boolean headerSkipped = false;

            while ((record = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] tokens = record.split(",", -1);
                if (tokens.length >= 5) {
                    try {
                        String id = tokens[0].trim();
                        String title = tokens[1].trim();
                        String genre = tokens[2].trim();
                        int year = Integer.parseInt(tokens[3].trim());
                        double rating = Double.parseDouble(tokens[4].trim());

                        MovieData movie = new MovieData(id, title, genre, year, rating);
                        allMovies.add(movie);

                        moviesByGenre.computeIfAbsent(genre, k -> new ArrayList<>()).add(movie);

                    } catch (NumberFormatException ex) {
                        System.err.println("Skipping malformed row: " + record);
                    }
                }
            }
        } catch (IOException e) {
            // Just print trace here, blocking UI on startup isn't ideal
            e.printStackTrace(); 
            showAlert(Alert.AlertType.ERROR, "Startup Error", "Failed to load movie database.");
        }
    }

    private void loadUserDatabase() {
        systemUsers.clear();
        File dbFile = new File(USERS_PATH);
        
        // Create empty file if it doesn't exist to prevent crashes
        if (!dbFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String record;
            boolean headerSkipped = false;

            while ((record = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // Split into max 4 parts: User, Pass, Watchlist, History
                String[] fields = record.split(",", 4);
                if (fields.length >= 2) {
                    String username = fields[0].trim();
                    String password = fields[1].trim();

                    // Parse Watchlist
                    List<String> watchlist = new ArrayList<>();
                    if (fields.length > 2 && !fields[2].trim().isEmpty()) {
                        String[] ids = fields[2].trim().split(";");
                        for (String id : ids) {
                            if (!id.trim().isEmpty()) watchlist.add(id.trim());
                        }
                    }

                    // Parse History (clean up timestamps if any exist using @)
                    List<String> history = new ArrayList<>();
                    if (fields.length > 3 && !fields[3].trim().isEmpty()) {
                        String[] items = fields[3].trim().split(";");
                        for (String item : items) {
                            String cleanId = item.contains("@") ? item.split("@")[0].trim() : item.trim();
                            if (!cleanId.isEmpty()) history.add(cleanId);
                        }
                    }

                    systemUsers.put(username, new UserData(username, password, watchlist, history));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to load user records.");
        }
    }

    private void saveUserDatabase() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_PATH))) {
            writer.write("Username,Password,Watchlist,History");
            writer.newLine();

            for (UserData user : systemUsers.values()) {
                writer.write(user.toCsvLine());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save user data: " + e.getMessage());
        }
    }

    /**
     * Builds the login/registration interface.
     */
    private void buildLoginScreen() {
        loginLayout = new VBox(20);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setStyle("-fx-background-color: #1a1a2e;"); // Dark theme

        // Container for the form elements
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(40, 50, 40, 50));
        formBox.setStyle("-fx-background-color: #282846; -fx-background-radius: 10;");
        formBox.setMaxWidth(400);

        Label titleLabel = new Label("Movie System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Login or Register to continue");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.LIGHTGRAY);

        // Input fields
        TextField userField = new TextField();
        userField.setPromptText("Username");
        userField.setMaxWidth(300);
        userField.setPrefHeight(35);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setMaxWidth(300);
        passField.setPrefHeight(35);

        // Action Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        loginBtn.setPrefSize(120, 35);
        loginBtn.setOnAction(e -> handleLogin(userField.getText(), passField.getText()));

        Button registerBtn = new Button("Register");
        registerBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        registerBtn.setPrefSize(120, 35);
        registerBtn.setOnAction(e -> handleRegister(userField.getText(), passField.getText()));

        buttonBox.getChildren().addAll(loginBtn, registerBtn);

        // Spacers for visual separation
        Region spacer = new Region();
        spacer.setPrefHeight(20);

        formBox.getChildren().addAll(
            titleLabel, subtitleLabel, 
            spacer, 
            new Label("Username:") {{ setTextFill(Color.WHITE); }}, 
            userField, 
            new Label("Password:") {{ setTextFill(Color.WHITE); }}, 
            passField, 
            new Region() {{ setPrefHeight(10); }}, 
            buttonBox
        );

        loginLayout.getChildren().add(formBox);
    }

    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Username and password required.");
            return;
        }

        UserData user = systemUsers.get(username);
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "User not found.");
            return;
        }

        // Check both raw (for old accounts) and encoded passwords
        // TODO: Enforce encoding for all users in next version
        String encoded = simpleEncode(password);
        if (!password.equals(user.getPassword()) && !encoded.equals(user.getPassword())) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect password.");
            return;
        }

        currentUser = user;
        welcomeLabel.setText("Welcome, " + username);
        updateUserLists();
        
        // Switch to main view
        primaryStage.getScene().setRoot(mainLayout);
    }

    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill in all fields.");
            return;
        }

        if (systemUsers.containsKey(username)) {
            showAlert(Alert.AlertType.ERROR, "Registration", "Username already taken.");
            return;
        }

        UserData newUser = new UserData(username, simpleEncode(password), new ArrayList<>(), new ArrayList<>());
        systemUsers.put(username, newUser);
        saveUserDatabase();

        showAlert(Alert.AlertType.INFORMATION, "Success", "Account created! Please log in.");
    }

    // TODO: Upgrade this to SHA-256 later for better security
    private String simpleEncode(String raw) {
        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] + (i % 7) + 3);
        }
        return new String(chars);
    }

    /**
     * Builds the main dashboard layout.
     * Uses BorderPane: Top=Header, Center=Table, Right=Sidebar.
     */
    private void buildMainDashboard() {
        mainLayout = new BorderPane();

        // 1. Header Section
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setStyle("-fx-background-color: #1a1a2e;");

        Label logo = new Label("Movie System");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        logo.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        welcomeLabel = new Label("Welcome");
        welcomeLabel.setTextFill(Color.WHITE);
        welcomeLabel.setFont(Font.font("Arial", 14));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutBtn.setOnAction(e -> {
            saveUserDatabase();
            currentUser = null;
            primaryStage.getScene().setRoot(loginLayout);
        });

        header.getChildren().addAll(logo, spacer, welcomeLabel, new Region() {{ setPrefWidth(15); }}, logoutBtn);

        // 2. Center Section (Filter + Table)
        VBox centerContent = new VBox(10);
        centerContent.setPadding(new Insets(15));

        // Filter Controls
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchInput = new TextField();
        searchInput.setPromptText("Search title...");
        searchInput.setPrefWidth(200);
        searchInput.setOnAction(e -> executeSearch());

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        searchBtn.setOnAction(e -> executeSearch());

        genreSelector = new ComboBox<>();
        genreSelector.getItems().add("All Genres");
        genreSelector.getItems().addAll(moviesByGenre.keySet());
        genreSelector.setValue("All Genres");
        genreSelector.setOnAction(e -> filterByGenre());

        Button topRatedBtn = new Button("Top Rated");
        topRatedBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        topRatedBtn.setOnAction(e -> showTopRated());

        Button recommendBtn = new Button("Get Recommendations");
        recommendBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        recommendBtn.setOnAction(e -> generateRecommendations());

        Button resetBtn = new Button("Show All");
        resetBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        resetBtn.setOnAction(e -> resetTable());

        filterBar.getChildren().addAll(searchInput, searchBtn, genreSelector, topRatedBtn, recommendBtn, resetBtn);

        // Movie Table Configuration
        movieTable = new TableView<>();
        tableModel = FXCollections.observableArrayList();
        movieTable.setItems(tableModel);

        TableColumn<MovieData, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<MovieData, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(300);

        TableColumn<MovieData, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreCol.setPrefWidth(100);

        TableColumn<MovieData, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearCol.setPrefWidth(60);

        TableColumn<MovieData, Double> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ratingCol.setPrefWidth(60);

        // Custom Buttons Column
        TableColumn<MovieData, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(col -> new TableCell<MovieData, Void>() {
            private final Button btnWatch = new Button("+ Watchlist");
            private final Button btnHistory = new Button("Watched");
            private final HBox container = new HBox(5, btnWatch, btnHistory);

            {
                btnWatch.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px;");
                btnHistory.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 10px;");
                container.setAlignment(Pos.CENTER);

                btnWatch.setOnAction(e -> addToWatchlist(getTableView().getItems().get(getIndex()).getId()));
                btnHistory.setOnAction(e -> markAsWatched(getTableView().getItems().get(getIndex()).getId()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        movieTable.getColumns().addAll(idCol, titleCol, genreCol, yearCol, ratingCol, actionCol);
        VBox.setVgrow(movieTable, Priority.ALWAYS);

        centerContent.getChildren().addAll(filterBar, movieTable);

        // 3. Right Sidebar (User Data)
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(15, 10, 15, 10));
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-background-color: white;");

        watchlistModel = FXCollections.observableArrayList();
        watchlistView = new ListView<>(watchlistModel);
        watchlistView.setPrefHeight(150);

        HBox sidebarBtns = new HBox(5);
        Button btnRemove = new Button("Remove");
        btnRemove.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px;");
        btnRemove.setOnAction(e -> removeFromWatchlist());

        Button btnMove = new Button("Mark Watched");
        btnMove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11px;");
        btnMove.setOnAction(e -> moveWatchlistToHistory());

        sidebarBtns.getChildren().addAll(btnRemove, btnMove);

        historyModel = FXCollections.observableArrayList();
        historyView = new ListView<>(historyModel);
        historyView.setPrefHeight(150);

        sidebar.getChildren().addAll(
            new Label("My Watchlist") {{ setFont(Font.font("Arial", FontWeight.BOLD, 16)); }},
            watchlistView, sidebarBtns,
            new Separator(),
            new Label("Viewing History") {{ setFont(Font.font("Arial", FontWeight.BOLD, 16)); }},
            historyView
        );

        // 4. Status Bar
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(8));
        statusLabel.setStyle("-fx-background-color: #e0e0e0;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        // Assemble Layout
        mainLayout.setTop(header);
        mainLayout.setCenter(centerContent);
        mainLayout.setRight(sidebar);
        mainLayout.setBottom(statusLabel);

        resetTable();
    }

    private void resetTable() {
        tableModel.clear();
        tableModel.addAll(allMovies);
        statusLabel.setText("Total movies: " + allMovies.size());
    }

    private void executeSearch() {
        String query = searchInput.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            resetTable();
            return;
        }

        tableModel.clear();
        for (MovieData m : allMovies) {
            if (m.getTitle().toLowerCase().contains(query)) {
                tableModel.add(m);
            }
        }
        statusLabel.setText("Search results: " + tableModel.size());
    }

    private void filterByGenre() {
        String selected = genreSelector.getValue();
        if (selected == null || selected.equals("All Genres")) {
            resetTable();
            return;
        }

        tableModel.clear();
        List<MovieData> matches = moviesByGenre.getOrDefault(selected, Collections.emptyList());
        tableModel.addAll(matches);
        statusLabel.setText("Genre '" + selected + "': " + matches.size() + " movies");
    }

    private void showTopRated() {
        tableModel.clear();
        // Sorting approach is O(N^2) but fine for small datasets
        List<MovieData> sorted = new ArrayList<>(allMovies);
        sorted.sort((m1, m2) -> Double.compare(m2.getRating(), m1.getRating())); // Descending

        // Take top 20
        int limit = Math.min(20, sorted.size());
        tableModel.addAll(sorted.subList(0, limit));
        statusLabel.setText("Showing Top 20 Rated Movies");
    }

    private void generateRecommendations() {
        if (currentUser == null) return;
        List<String> userHistory = currentUser.getHistory();

        if (userHistory.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Cold Start", 
                "Your history is empty. Here are some top rated movies to get started!");
            showTopRated();
            return;
        }

        // 1. Find favorite genre
        Map<String, Integer> genreCounts = new HashMap<>();
        for (String id : userHistory) {
            MovieData m = findMovie(id);
            if (m != null) {
                genreCounts.put(m.getGenre(), genreCounts.getOrDefault(m.getGenre(), 0) + 1);
            }
        }

        String topGenre = null;
        int max = -1;
        for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                topGenre = entry.getKey();
            }
        }

        if (topGenre == null) {
            showTopRated();
            return;
        }

        // 2. Filter unwatched movies from that genre
        List<MovieData> candidates = new ArrayList<>();
        List<MovieData> inGenre = moviesByGenre.getOrDefault(topGenre, Collections.emptyList());
        
        for (MovieData m : inGenre) {
            if (!userHistory.contains(m.getId())) {
                candidates.add(m);
            }
        }

        // 3. Sort by rating
        candidates.sort((m1, m2) -> Double.compare(m2.getRating(), m1.getRating()));

        if (candidates.isEmpty()) {
            statusLabel.setText("You've seen all top movies in " + topGenre + "!");
            return;
        }

        tableModel.clear();
        tableModel.addAll(candidates);
        statusLabel.setText("Recommended because you watch " + topGenre);
    }

    private MovieData findMovie(String id) {
        for (MovieData m : allMovies) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }

    // --- List Management Helpers ---

    private void addToWatchlist(String id) {
        if (currentUser == null) return;
        List<String> list = currentUser.getWatchlist();

        if (!list.contains(id)) {
            list.add(id);
            saveUserDatabase();
            updateUserLists();
            statusLabel.setText("Added to Watchlist");
        } else {
            statusLabel.setText("Already in Watchlist");
        }
    }

    private void markAsWatched(String id) {
        if (currentUser == null) return;
        
        if (!currentUser.getHistory().contains(id)) {
            currentUser.getHistory().add(id);
            currentUser.getWatchlist().remove(id); // Auto-remove from watchlist
            saveUserDatabase();
            updateUserLists();
            statusLabel.setText("Marked as Watched");
        }
    }

    private void removeFromWatchlist() {
        String selection = watchlistView.getSelectionModel().getSelectedItem();
        if (selection != null) {
            String id = selection.split(" - ")[0];
            currentUser.getWatchlist().remove(id);
            saveUserDatabase();
            updateUserLists();
        }
    }

    private void moveWatchlistToHistory() {
        String selection = watchlistView.getSelectionModel().getSelectedItem();
        if (selection != null) {
            String id = selection.split(" - ")[0];
            markAsWatched(id);
        }
    }

    private void updateUserLists() {
        if (currentUser == null) return;

        watchlistModel.clear();
        for (String id : currentUser.getWatchlist()) {
            MovieData m = findMovie(id);
            if (m != null) watchlistModel.add(id + " - " + m.getTitle());
        }

        historyModel.clear();
        for (String id : currentUser.getHistory()) {
            MovieData m = findMovie(id);
            if (m != null) historyModel.add(id + " - " + m.getTitle());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}