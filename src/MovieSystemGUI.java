import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class MovieSystemGUI extends Application {
    
    private List<MovieData> movies = new ArrayList<>();
    private Map<String, List<MovieData>> genreMap = new HashMap<>();
    private Map<String, UserData> userData = new HashMap<>();
    private UserData currentUser = null;
    
    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;
    private TableView<MovieData> movieTable;
    private ListView<String> watchlistView;
    private ListView<String> historyView;
    private TextField searchField;
    private ComboBox<String> genreComboBox;
    private Label statusLabel;
    private Label userLabel;
    
    private static final String MOVIES_FILE = "movies.csv";
    private static final String USERS_FILE = "users.csv";
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Movie Recommendation System");
        
        loadMovies();
        loadUsers();
        
        loginScene = createLoginScene();
        mainScene = createMainScene();
        
        primaryStage.setScene(loginScene);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.show();
    }
    
    private void loadMovies() {
        movies.clear();
        genreMap.clear();
        
        try (BufferedReader br = new BufferedReader(new FileReader(MOVIES_FILE))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    try {
                        String id = parts[0].trim();
                        String title = parts[1].trim();
                        String genre = parts[2].trim();
                        int year = Integer.parseInt(parts[3].trim());
                        double rating = Double.parseDouble(parts[4].trim());
                        
                        MovieData movie = new MovieData(id, title, genre, year, rating);
                        movies.add(movie);
                        
                        genreMap.computeIfAbsent(genre, k -> new ArrayList<>()).add(movie);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            showAlert("Error", "Could not load movies: " + e.getMessage());
        }
    }
    
    private void loadUsers() {
        userData.clear();
        
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                String[] parts = line.split(",", 4);
                if (parts.length >= 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    List<String> watchlist = parts.length > 2 && !parts[2].trim().isEmpty() 
                        ? new ArrayList<>(Arrays.asList(parts[2].trim().split(","))) 
                        : new ArrayList<>();
                    List<String> history = parts.length > 3 && !parts[3].trim().isEmpty() 
                        ? new ArrayList<>(Arrays.asList(parts[3].trim().split(","))) 
                        : new ArrayList<>();
                    
                    userData.put(username, new UserData(username, password, watchlist, history));
                }
            }
        } catch (IOException e) {
            showAlert("Error", "Could not load users: " + e.getMessage());
        }
    }
    
    private void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            bw.write("Username,Password,Watchlist,History");
            bw.newLine();
            
            for (UserData user : userData.values()) {
                bw.write(user.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            showAlert("Error", "Could not save users: " + e.getMessage());
        }
    }
    
    private Scene createLoginScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");
        
        Label titleLabel = new Label("Movie System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.WHITE);
        
        Label subtitleLabel = new Label("Login or Register to continue");
        subtitleLabel.setFont(Font.font("Arial", 18));
        subtitleLabel.setTextFill(Color.LIGHTGRAY);
        
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10; -fx-padding: 30;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-font-size: 14; -fx-padding: 10 30; -fx-cursor: hand;");
        loginBtn.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText()));
        
        Button registerBtn = new Button("Register");
        registerBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 14; -fx-padding: 10 30; -fx-cursor: hand;");
        registerBtn.setOnAction(e -> handleRegister(usernameField.getText(), passwordField.getText()));
        
        buttonBox.getChildren().addAll(loginBtn, registerBtn);
        formBox.getChildren().addAll(usernameField, passwordField, buttonBox);
        
        root.getChildren().addAll(titleLabel, subtitleLabel, formBox);
        
        return new Scene(root);
    }
    
    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter username and password");
            return;
        }
        
        UserData user = userData.get(username);
        if (user == null) {
            showAlert("Error", "User not found");
            return;
        }
        
        String encoded = encodePassword(password);
        if (!encoded.equals(user.getPassword())) {
            showAlert("Error", "Incorrect password");
            return;
        }
        
        currentUser = user;
        userLabel.setText("Welcome, " + username);
        refreshUserLists();
        primaryStage.setScene(mainScene);
    }
    
    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter username and password");
            return;
        }
        
        if (userData.containsKey(username)) {
            showAlert("Error", "Username already exists");
            return;
        }
        
        UserData newUser = new UserData(username, encodePassword(password), new ArrayList<>(), new ArrayList<>());
        userData.put(username, newUser);
        saveUsers();
        
        showAlert("Success", "Registration successful! Please login.");
    }
    
    private String encodePassword(String raw) {
        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] + (i % 7) + 3);
        }
        return new String(chars);
    }
    
    private Scene createMainScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");
        
        root.setTop(createHeader());
        
        root.setCenter(createCenterContent());
        
        root.setRight(createUserPanel());
        
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(10));
        statusLabel.setStyle("-fx-background-color: #e0e0e0; -fx-font-size: 12;");
        root.setBottom(statusLabel);
        
        return new Scene(root);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1a1a2e;");
        
        Label logo = new Label("Movie System");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        logo.setTextFill(Color.WHITE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        userLabel = new Label("Welcome");
        userLabel.setTextFill(Color.WHITE);
        userLabel.setFont(Font.font("Arial", 14));
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            saveUsers();
            currentUser = null;
            primaryStage.setScene(loginScene);
        });
        
        header.getChildren().addAll(logo, spacer, userLabel, logoutBtn);
        return header;
    }
    
    private VBox createCenterContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        searchField = new TextField();
        searchField.setPromptText("Search movies...");
        searchField.setPrefWidth(250);
        searchField.setOnAction(e -> searchMovies());
        
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchMovies());
        
        genreComboBox = new ComboBox<>();
        genreComboBox.getItems().add("All Genres");
        genreComboBox.getItems().addAll(genreMap.keySet());
        genreComboBox.setValue("All Genres");
        genreComboBox.setOnAction(e -> filterByGenre());
        
        Button topRatedBtn = new Button("Top Rated");
        topRatedBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
        topRatedBtn.setOnAction(e -> showTopRated());
        
        Button recommendBtn = new Button("Get Recommendations");
        recommendBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        recommendBtn.setOnAction(e -> showRecommendations());
        
        Button refreshBtn = new Button("Show All");
        refreshBtn.setOnAction(e -> refreshMovieTable());
        
        filterBar.getChildren().addAll(searchField, searchBtn, genreComboBox, topRatedBtn, recommendBtn, refreshBtn);
        
        movieTable = createMovieTable();
        VBox.setVgrow(movieTable, Priority.ALWAYS);
        
        content.getChildren().addAll(filterBar, movieTable);
        return content;
    }
    
    private TableView<MovieData> createMovieTable() {
        TableView<MovieData> table = new TableView<>();
        table.setPlaceholder(new Label("No movies to display"));
        
        TableColumn<MovieData, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        
        TableColumn<MovieData, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(300);
        
        TableColumn<MovieData, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreCol.setPrefWidth(100);
        
        TableColumn<MovieData, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearCol.setPrefWidth(80);
        
        TableColumn<MovieData, Double> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ratingCol.setPrefWidth(80);
        
        TableColumn<MovieData, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button addWatchlistBtn = new Button("+ Watchlist");
            private final Button markWatchedBtn = new Button("Watched");
            private final HBox pane = new HBox(5, addWatchlistBtn, markWatchedBtn);
            
            {
                addWatchlistBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 10;");
                markWatchedBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 10;");
                
                addWatchlistBtn.setOnAction(e -> {
                    MovieData movie = getTableView().getItems().get(getIndex());
                    addToWatchlist(movie.getId());
                });
                
                markWatchedBtn.setOnAction(e -> {
                    MovieData movie = getTableView().getItems().get(getIndex());
                    addToHistory(movie.getId());
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        table.getColumns().addAll(idCol, titleCol, genreCol, yearCol, ratingCol, actionCol);
        
        table.setItems(FXCollections.observableArrayList(movies));
        
        return table;
    }
    
    private VBox createUserPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-width: 0 0 0 1;");
        
        Label watchlistLabel = new Label("My Watchlist");
        watchlistLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        watchlistView = new ListView<>();
        watchlistView.setPrefHeight(200);
        
        HBox watchlistBtns = new HBox(5);
        Button removeWatchlistBtn = new Button("Remove");
        removeWatchlistBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        removeWatchlistBtn.setOnAction(e -> removeFromWatchlist());
        
        Button moveToHistoryBtn = new Button("Mark Watched");
        moveToHistoryBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        moveToHistoryBtn.setOnAction(e -> moveToHistory());
        
        watchlistBtns.getChildren().addAll(removeWatchlistBtn, moveToHistoryBtn);
        
        Label historyLabel = new Label("Viewing History");
        historyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        historyView = new ListView<>();
        historyView.setPrefHeight(200);
        
        panel.getChildren().addAll(
            watchlistLabel, watchlistView, watchlistBtns,
            new Separator(),
            historyLabel, historyView
        );
        
        return panel;
    }
    
    private void refreshMovieTable() {
        movieTable.setItems(FXCollections.observableArrayList(movies));
        statusLabel.setText("Showing all " + movies.size() + " movies");
    }
    
    private void searchMovies() {
        String term = searchField.getText().toLowerCase().trim();
        if (term.isEmpty()) {
            refreshMovieTable();
            return;
        }
        
        List<MovieData> results = new ArrayList<>();
        for (MovieData movie : movies) {
            if (movie.getTitle().toLowerCase().contains(term)) {
                results.add(movie);
            }
        }
        
        movieTable.setItems(FXCollections.observableArrayList(results));
        statusLabel.setText("Found " + results.size() + " movies matching '" + term + "'");
    }
    
    private void filterByGenre() {
        String genre = genreComboBox.getValue();
        if (genre == null || genre.equals("All Genres")) {
            refreshMovieTable();
            return;
        }
        
        List<MovieData> genreMovies = genreMap.getOrDefault(genre, new ArrayList<>());
        movieTable.setItems(FXCollections.observableArrayList(genreMovies));
        statusLabel.setText("Showing " + genreMovies.size() + " " + genre + " movies");
    }
    
    private void showTopRated() {
        List<MovieData> sorted = new ArrayList<>(movies);
        sorted.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        
        List<MovieData> top20 = sorted.subList(0, Math.min(20, sorted.size()));
        movieTable.setItems(FXCollections.observableArrayList(top20));
        statusLabel.setText("Showing top 20 rated movies");
    }
    
    private void showRecommendations() {
        if (currentUser == null) return;
        
        List<String> history = currentUser.getHistory();
        
        if (history.isEmpty()) {
            showAlert("Recommendations", "Watch some movies first to get personalized recommendations!\nShowing top rated movies instead.");
            showTopRated();
            return;
        }
        
        Map<String, Integer> genreCount = new HashMap<>();
        for (String movieId : history) {
            MovieData movie = getMovieById(movieId);
            if (movie != null) {
                genreCount.put(movie.getGenre(), genreCount.getOrDefault(movie.getGenre(), 0) + 1);
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
            showTopRated();
            return;
        }
        
        List<MovieData> recommendations = new ArrayList<>();
        List<MovieData> genreMovies = genreMap.getOrDefault(favoriteGenre, new ArrayList<>());
        
        for (MovieData movie : genreMovies) {
            if (!history.contains(movie.getId())) {
                recommendations.add(movie);
            }
        }
        
        recommendations.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        
        if (recommendations.isEmpty()) {
            showAlert("Recommendations", "You've watched all " + favoriteGenre + " movies! Try other genres.");
            return;
        }
        
        movieTable.setItems(FXCollections.observableArrayList(recommendations));
        statusLabel.setText("Recommendations based on your love for " + favoriteGenre + " movies");
    }
    
    private MovieData getMovieById(String id) {
        for (MovieData movie : movies) {
            if (movie.getId().equals(id)) {
                return movie;
            }
        }
        return null;
    }
    
    private void addToWatchlist(String movieId) {
        if (currentUser == null) return;
        
        if (currentUser.getWatchlist().contains(movieId)) {
            statusLabel.setText("Movie already in watchlist");
            return;
        }
        
        currentUser.getWatchlist().add(movieId);
        saveUsers();
        refreshUserLists();
        statusLabel.setText("Added to watchlist");
    }
    
    private void addToHistory(String movieId) {
        if (currentUser == null) return;
        
        if (currentUser.getHistory().contains(movieId)) {
            statusLabel.setText("Movie already in history");
            return;
        }
        
        currentUser.getHistory().add(movieId);
        currentUser.getWatchlist().remove(movieId);
        saveUsers();
        refreshUserLists();
        statusLabel.setText("Marked as watched");
    }
    
    private void removeFromWatchlist() {
        String selected = watchlistView.getSelectionModel().getSelectedItem();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        currentUser.getWatchlist().remove(movieId);
        saveUsers();
        refreshUserLists();
        statusLabel.setText("Removed from watchlist");
    }
    
    private void moveToHistory() {
        String selected = watchlistView.getSelectionModel().getSelectedItem();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        addToHistory(movieId);
    }
    
    private void refreshUserLists() {
        if (currentUser == null) return;
        
        ObservableList<String> watchlistItems = FXCollections.observableArrayList();
        for (String id : currentUser.getWatchlist()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                watchlistItems.add(id + " - " + movie.getTitle());
            }
        }
        watchlistView.setItems(watchlistItems);
        
        ObservableList<String> historyItems = FXCollections.observableArrayList();
        for (String id : currentUser.getHistory()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                historyItems.add(id + " - " + movie.getTitle());
            }
        }
        historyView.setItems(historyItems);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static class MovieData {
        private String id;
        private String title;
        private String genre;
        private int year;
        private double rating;
        
        public MovieData(String id, String title, String genre, int year, double rating) {
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
    }
    
    public static class UserData {
        private String username;
        private String password;
        private List<String> watchlist;
        private List<String> history;
        
        public UserData(String username, String password, List<String> watchlist, List<String> history) {
            this.username = username;
            this.password = password;
            this.watchlist = watchlist;
            this.history = history;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public List<String> getWatchlist() { return watchlist; }
        public List<String> getHistory() { return history; }
        
        public String toCsvLine() {
            return username + "," + password + "," + 
                   String.join(",", watchlist) + "," + 
                   String.join(",", history);
        }
    }
}
