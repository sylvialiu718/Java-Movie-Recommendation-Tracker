import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
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
import java.util.List;

/**
 * MovieSystemFXGUI - Main GUI application for the Movie Recommendation System
 * 
 * We built this graphical user interface using JavaFX framework to provide users with
 * a way to browse movies, manage their watchlists, and track viewing history.
 * 
 * The GUI consists of two main screens:
 * 1. Login/Registration screen - where users can authenticate or create new accounts
 * 2. Main application screen - where users can browse, search, and interact with movies
 * 
 * We chose JavaFX as required
 */
public class MovieSystemFXGUI extends Application {
    
    // Movie data storage - we use a List to store all movies and a Map to organize them by genre
    private List<MovieData> movies = new ArrayList<>();
    private Map<String, List<MovieData>> genreMap = new HashMap<>();
    
    // User data and current session - we track all registered users and the currently logged-in user
    private Map<String, UserData> userData = new HashMap<>();
    private UserData currentUser = null;
    
    // UI Components - we declare these as instance variables so they can be accessed across methods
    private TableView<MovieData> movieTable;           // We use TableView to display movies in a tabular format
    private ObservableList<MovieData> movieTableData;  // Observable list that automatically updates the table when modified
    private ListView<String> watchlistView;            // We use ListView to show the user's watchlist
    private ObservableList<String> watchlistData;      // Observable list for watchlist items
    private ListView<String> historyView;              // ListView for viewing history
    private ObservableList<String> historyData;        // Observable list for history items
    private TextField searchField;                     // Text input field for movie search
    private ComboBox<String> genreComboBox;            // Dropdown menu for genre filtering
    private Label statusLabel;                         // Status bar at the bottom showing current state
    private Label userLabel;                           // Label displaying the logged-in username
    private BorderPane mainPane;                       // Main layout container using BorderPane for header/center/sidebar structure
    private VBox loginPane;                            // Login screen container using VBox for vertical layout
    private Stage primaryStage;                        // The main application window
    
    // File paths for data persistence
    private static final String MOVIES_FILE = "data/movies.csv";
    private static final String USERS_FILE = "data/users.csv";
    
    public static void main(String[] args) {
        launch(args);
    }
    
    /**
     * We override the start method from Application class - this is the entry point for JavaFX applications.
     * Here we initialize the primary stage (main window) and set up the initial scene.
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Movie Recommendation System");
        
        // We load data from CSV files before creating the UI
        loadMovies();
        loadUsers();
        
        // We create both panes but only show the login pane initially
        createLoginPane();
        createMainPane();
        
        // We create the scene with the login pane as the root node, setting window size to 1200x800 pixels
        Scene scene = new Scene(loginPane, 1200, 800);
        // We attempt to load external CSS stylesheet for additional styling
        scene.getStylesheets().add(getClass().getResource("style.css") != null ? 
            getClass().getResource("style.css").toExternalForm() : "");
        
        // We set the scene and display the window
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Loads movie data from CSV file into memory
     * Parses each line and creates MovieData objects
     */
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
                        
                        if (!genreMap.containsKey(genre)) {
                            genreMap.put(genre, new ArrayList<MovieData>());
                        }
                        genreMap.get(genre).add(movie);
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load movies: " + e.getMessage());
        }
    }
    
    /**
     * Loads user data from CSV file
     * Show username, password, watchlist and history for each user
     */
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
                    
                    List<String> watchlist = new ArrayList<String>();
                    if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                        String[] watchlistArr = parts[2].trim().split(";");
                        for (String id : watchlistArr) {
                            id = id.trim();
                            if (!id.isEmpty()) {
                                watchlist.add(id);
                            }
                        }
                    }
                    
                    List<String> history = new ArrayList<String>();
                    if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                        String[] historyArr = parts[3].trim().split(";");
                        for (String item : historyArr) {
                            item = item.trim();
                            if (item.contains("@")) {
                                item = item.split("@")[0].trim();
                            }
                            if (!item.isEmpty()) {
                                history.add(item);
                            }
                        }
                    }
                    
                    userData.put(username, new UserData(username, password, watchlist, history));
                }
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load users: " + e.getMessage());
        }
    }
    
    /**
     * Saves all user data to CSV file
     * Persists watchlists and viewing history
     */
    private void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            bw.write("Username,Password,Watchlist,History");
            bw.newLine();
            
            for (UserData user : userData.values()) {
                bw.write(user.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save users: " + e.getMessage());
        }
    }
    
    /**
     * We create the login/registration panel that is displayed when the application starts.
     * This method builds a centered form with username/password fields and login/register buttons.
     * We use VBox for vertical layout and style it with a dark theme for a modern look.
     */
    private void createLoginPane() {
        // We create the main container using VBox with 20px spacing between children
        loginPane = new VBox(20);
        loginPane.setAlignment(Pos.CENTER);  // We center all content in the pane
        loginPane.setStyle("-fx-background-color: #1a1a2e;");  // We set a dark blue background
        
        // We create a form box to hold the login form elements with rounded corners
        VBox formBox = new VBox(15);  // 15px spacing between form elements
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(40, 50, 40, 50));  // We add padding around the form
        formBox.setStyle("-fx-background-color: #282846; -fx-background-radius: 10;");  // Rounded corners
        formBox.setMaxWidth(400);  // We limit the form width for better appearance
        
        // We create the title label with large bold font
        Label titleLabel = new Label("Movie System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        
        // We add a subtitle to guide users
        Label subtitleLabel = new Label("Login or Register to continue");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.LIGHTGRAY);
        
        // We create the username input field with a label
        Label usernameLabel = new Label("Username:");
        usernameLabel.setTextFill(Color.WHITE);
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");  // We show placeholder text when empty
        usernameField.setMaxWidth(300);
        usernameField.setPrefHeight(35);
        
        // We create the password input field using PasswordField which masks input
        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(Color.WHITE);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(300);
        passwordField.setPrefHeight(35);
        
        // We create a horizontal box to hold the login and register buttons side by side
        HBox buttonBox = new HBox(15);  // 15px spacing between buttons
        buttonBox.setAlignment(Pos.CENTER);
        
        // We create the Login button with green background styling
        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        loginBtn.setPrefSize(120, 35);
        // We attach an event handler to the login button using anonymous inner class
        loginBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                handleLogin(usernameField.getText(), passwordField.getText());
            }
        });
        
        // We create the Register button with blue background styling
        Button registerBtn = new Button("Register");
        registerBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        registerBtn.setPrefSize(120, 35);
        // We attach an event handler to the register button
        registerBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                handleRegister(usernameField.getText(), passwordField.getText());
            }
        });
        
        // We add both buttons to the button container
        buttonBox.getChildren().addAll(loginBtn, registerBtn);
        
        // We create spacer regions to add vertical spacing in the form
        Region spacer1 = new Region();
        spacer1.setPrefHeight(20);
        Region spacer2 = new Region();
        spacer2.setPrefHeight(10);
        
        // We add all form elements to the form box in order
        formBox.getChildren().addAll(
            titleLabel, subtitleLabel,
            spacer1,
            usernameLabel, usernameField,
            passwordLabel, passwordField,
            spacer2,
            buttonBox
        );
        
        // We add the form box to the main login pane
        loginPane.getChildren().add(formBox);
    }
    
    /**
     * Handles user login authentication
     * Supports both plain text and encoded passwords for backward compatibility
     */
    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter username and password");
            return;
        }
        
        UserData user = userData.get(username);
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User not found");
            return;
        }
        
        String storedPwd = user.getPassword();
        String encoded = encodePassword(password);
        
        if (!password.equals(storedPwd) && !encoded.equals(storedPwd)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Incorrect password");
            return;
        }
        
        currentUser = user;
        userLabel.setText("Welcome, " + username);
        refreshUserLists();
        primaryStage.getScene().setRoot(mainPane);
    }
    
    /**
     * Handles new user registration
     * Creates new user account with encoded password
     */
    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter username and password");
            return;
        }
        
        if (userData.containsKey(username)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username already exists");
            return;
        }
        
        UserData newUser = new UserData(username, encodePassword(password), new ArrayList<>(), new ArrayList<>());
        userData.put(username, newUser);
        saveUsers();
        
        showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! Please login.");
    }
    
    /**
     * Encodes password using character shifting algorithm
     * @param raw The plain text password
     * @return The encoded password string
     */
    private String encodePassword(String raw) {
        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] + (i % 7) + 3);
        }
        return new String(chars);
    }
    
    /**
     * We create the main application panel that users see after logging in.
     * This method builds a BorderPane layout with:
     * - Top: Header bar with logo, username, and logout button
     * - Center: Movie table with search/filter controls
     * - Right: Sidebar with watchlist and viewing history
     * - Bottom: Status bar showing current state
     * 
     * We chose BorderPane because it naturally divides the screen into logical regions.
     */
    private void createMainPane() {
        // We use BorderPane as the main layout container - it has top, center, right, bottom regions
        mainPane = new BorderPane();
        
        // We create a header bar using HBox for horizontal layout
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));  // We add padding around the header
        header.setStyle("-fx-background-color: #1a1a2e;");  // Dark blue background matching login screen
        
        // We create the application logo/title
        Label logo = new Label("Movie System");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        logo.setTextFill(Color.WHITE);
        
        // We use a Region with HGrow to push elements to the right side
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);  // This makes the spacer expand to fill available space
        
        // We create a label to display the logged-in username
        userLabel = new Label("Welcome");
        userLabel.setTextFill(Color.WHITE);
        userLabel.setFont(Font.font("Arial", 14));
        
        // We create the logout button with red styling to indicate it's a destructive action
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        // We handle logout by saving data and switching back to login screen
        logoutBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                saveUsers();  // We save user data before logging out
                currentUser = null;
                primaryStage.getScene().setRoot(loginPane);  // We switch to login pane
            }
        });
        
        // We add a small spacer between username and logout button
        Region headerSpacer = new Region();
        headerSpacer.setPrefWidth(15);
        // We add all header elements to the header container
        header.getChildren().addAll(logo, spacer, userLabel, headerSpacer, logoutBtn);
    
        // We create a VBox to hold the filter bar and movie table vertically
        VBox centerBox = new VBox(10);  // 10px spacing between filter bar and table
        centerBox.setPadding(new Insets(15));
        
        // We create a filter bar using HBox for horizontal arrangement of controls
        HBox filterBar = new HBox(10);  // 10px spacing between controls
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        // We create a search text field for users to search movies by title
        searchField = new TextField();
        searchField.setPromptText("Search movies...");  // Placeholder text
        searchField.setPrefWidth(200);
        // We allow users to press Enter to search
        searchField.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                searchMovies();
            }
        });
        
        // We create a Search button with blue styling
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        searchBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                searchMovies();
            }
        });
        
        // We create a ComboBox (dropdown) for genre filtering
        genreComboBox = new ComboBox<>();
        genreComboBox.getItems().add("All Genres");  // We add default option first
        genreComboBox.getItems().addAll(genreMap.keySet());  // We add all available genres
        genreComboBox.setValue("All Genres");  // We set default selection
        // We filter movies when user selects a genre
        genreComboBox.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                filterByGenre();
            }
        });
        
        // We create a Top Rated button with orange styling to show highest rated movies
        Button topRatedBtn = new Button("Top Rated");
        topRatedBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        topRatedBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                showTopRated();
            }
        });
        
        // We create a Recommendations button with green styling for personalized suggestions
        Button recommendBtn = new Button("Get Recommendations");
        recommendBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        recommendBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                showRecommendations();
            }
        });
        
        // We create a Show All button with gray styling to reset filters
        Button refreshBtn = new Button("Show All");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                refreshMovieTable();
            }
        });
        
        // We add all filter controls to the filter bar
        filterBar.getChildren().addAll(searchField, searchBtn, genreComboBox, topRatedBtn, recommendBtn, refreshBtn);
        
        // We create a TableView to display movies in a spreadsheet-like format
        movieTable = new TableView<>();
        // We use ObservableList so the table automatically updates when we modify the list
        movieTableData = FXCollections.observableArrayList();
        movieTable.setItems(movieTableData);
        
        // We define table columns - each column displays a specific movie attribute
        // ID Column - displays movie ID
        TableColumn<MovieData, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));  // We bind to the "id" property
        idCol.setPrefWidth(50);
        
        // Title Column - displays movie title (wider to accommodate long titles)
        TableColumn<MovieData, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(300);
        
        // Genre Column - displays movie genre
        TableColumn<MovieData, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreCol.setPrefWidth(100);
        
        // Year Column - displays release year
        TableColumn<MovieData, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearCol.setPrefWidth(60);
        
        // Rating Column - displays movie rating
        TableColumn<MovieData, Double> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ratingCol.setPrefWidth(60);
        
        // Actions Column - we create custom cells with buttons for each row
        TableColumn<MovieData, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(180);
        // We use a Callback to create custom table cells with action buttons
        actionsCol.setCellFactory(new javafx.util.Callback<TableColumn<MovieData, Void>, TableCell<MovieData, Void>>() {
            public TableCell<MovieData, Void> call(TableColumn<MovieData, Void> col) {
                return new TableCell<MovieData, Void>() {
                    // We create two action buttons for each row
                    private final Button watchlistBtn = new Button("+ Watchlist");
                    private final Button watchedBtn = new Button("Watched");
                    private final HBox pane = new HBox(5, watchlistBtn, watchedBtn);  // We arrange buttons horizontally
                    
                    // Instance initializer block - we set up button styles and event handlers
                    {
                        // We style the watchlist button with blue color
                        watchlistBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                        // We style the watched button with green color
                        watchedBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                        pane.setAlignment(Pos.CENTER);
                        
                        // We need to reference 'this' cell in the event handler
                        final TableCell<MovieData, Void> cell = this;
                        // We handle adding movie to watchlist when button is clicked
                        watchlistBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                            public void handle(javafx.event.ActionEvent e) {
                                MovieData movie = cell.getTableView().getItems().get(cell.getIndex());
                                addToWatchlist(movie.getId());
                            }
                        });
                        
                        // We handle marking movie as watched when button is clicked
                        watchedBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                            public void handle(javafx.event.ActionEvent e) {
                                MovieData movie = cell.getTableView().getItems().get(cell.getIndex());
                                addToHistory(movie.getId());
                            }
                        });
                    }
                    
                    // We override updateItem to show/hide buttons based on whether the row has data
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);  // We hide buttons for empty rows
                        } else {
                            setGraphic(pane);  // We show buttons for rows with data
                        }
                    }
                };
            }
        });
        
        // We add all columns to the table
        movieTable.getColumns().addAll(idCol, titleCol, genreCol, yearCol, ratingCol, actionsCol);
        // We make the table expand to fill available vertical space
        VBox.setVgrow(movieTable, Priority.ALWAYS);
        
        // We add the filter bar and table to the center container
        centerBox.getChildren().addAll(filterBar, movieTable);
        
        // We create a sidebar panel to display user's watchlist and viewing history
        VBox rightPanel = new VBox(15);  // 15px spacing between sections
        rightPanel.setPadding(new Insets(15, 10, 15, 10));
        rightPanel.setPrefWidth(280);  // We set a fixed width for the sidebar
        rightPanel.setStyle("-fx-background-color: white;");  // White background for contrast
        
        // We create the Watchlist section header
        Label watchlistLabel = new Label("My Watchlist");
        watchlistLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // We create a ListView to display watchlist items
        watchlistData = FXCollections.observableArrayList();
        watchlistView = new ListView<>(watchlistData);
        watchlistView.setPrefHeight(150);
        
        // We create buttons for watchlist management
        HBox watchlistBtns = new HBox(5);
        
        // We create a Remove button with red styling to remove items from watchlist
        Button removeWatchlistBtn = new Button("Remove");
        removeWatchlistBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        removeWatchlistBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                removeFromWatchlist();
            }
        });
        
        // We create a Mark Watched button to move items from watchlist to history
        Button moveToHistoryBtn = new Button("Mark Watched");
        moveToHistoryBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        moveToHistoryBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            public void handle(javafx.event.ActionEvent e) {
                moveToHistory();
            }
        });
        
        watchlistBtns.getChildren().addAll(removeWatchlistBtn, moveToHistoryBtn);
        
        // We add a separator line between watchlist and history sections
        Separator separator = new Separator();
        
        // We create the Viewing History section header
        Label historyLabel = new Label("Viewing History");
        historyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // We create a ListView to display viewing history
        historyData = FXCollections.observableArrayList();
        historyView = new ListView<>(historyData);
        historyView.setPrefHeight(150);
        
        // We add all sidebar elements to the right panel
        rightPanel.getChildren().addAll(watchlistLabel, watchlistView, watchlistBtns, separator, historyLabel, historyView);

        // We create a status bar at the bottom to show current state and feedback
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(8, 10, 8, 10));
        statusLabel.setStyle("-fx-background-color: #e0e0e0;");  // Light gray background
        statusLabel.setMaxWidth(Double.MAX_VALUE);  // We make it span the full width

        // We assign each section to its position in the BorderPane
        mainPane.setTop(header);        // Header at the top
        mainPane.setCenter(centerBox);  // Movie table in the center
        mainPane.setRight(rightPanel);  // Sidebar on the right
        mainPane.setBottom(statusLabel); // Status bar at the bottom
        
        // We load and display all movies initially
        refreshMovieTable();
    }
    
    /**
     * We refresh the movie table to show all movies.
     * This method clears the current table data and reloads all movies from our list.
     */
    private void refreshMovieTable() {
        movieTableData.clear();  // We clear existing data
        movieTableData.addAll(movies);  // We add all movies back
        statusLabel.setText("Showing all " + movies.size() + " movies");  // We update status bar
    }
    
    /**
     * We search movies by title based on user input in the search field.
     * The search is case-insensitive and matches partial titles.
     */
    private void searchMovies() {
        String term = searchField.getText().toLowerCase().trim();
        if (term.isEmpty()) {
            refreshMovieTable();
            return;
        }
        
        movieTableData.clear();
        int count = 0;
        for (MovieData movie : movies) {
            if (movie.getTitle().toLowerCase().contains(term)) {
                movieTableData.add(movie);
                count++;
            }
        }
        statusLabel.setText("Found " + count + " movies matching '" + term + "'");
    }
    
    /**
     * We filter movies by the genre selected in the dropdown ComboBox.
     * If "All Genres" is selected, we show all movies instead.
     */
    private void filterByGenre() {
        String genre = genreComboBox.getValue();
        if (genre == null || genre.equals("All Genres")) {
            refreshMovieTable();
            return;
        }
        
        movieTableData.clear();
        List<MovieData> genreMovies = genreMap.getOrDefault(genre, new ArrayList<>());
        movieTableData.addAll(genreMovies);
        statusLabel.setText("Showing " + genreMovies.size() + " " + genre + " movies");
    }
    
    /**
     * We display the top 20 highest-rated movies.
     * We find the best movies by repeatedly selecting the highest rated one not yet selected.
     */
    private void showTopRated() {
        // Find top 20 movies by repeatedly finding the highest rated unwatched movie
        movieTableData.clear();
        List<MovieData> selected = new ArrayList<MovieData>();
        
        for (int i = 0; i < Math.min(20, movies.size()); i++) {
            MovieData best = null;
            for (MovieData movie : movies) {
                if (!selected.contains(movie)) {
                    if (best == null || movie.getRating() > best.getRating()) {
                        best = movie;
                    }
                }
            }
            if (best != null) {
                selected.add(best);
                movieTableData.add(best);
            }
        }
        statusLabel.setText("Showing top 20 rated movies");
    }
    
    /**
     * We generate personalized movie recommendations based on the user's viewing history.
     * We analyze which genre the user watches most and recommend unwatched movies from that genre.
     */
    private void showRecommendations() {
        if (currentUser == null) return;
        
        List<String> history = currentUser.getHistory();
        
        if (history.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Recommendations", 
                "Watch some movies first to get personalized recommendations!\nShowing top rated movies instead.");
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
        
        // Find top recommendations by repeatedly selecting highest rated
        List<MovieData> topRecommendations = new ArrayList<MovieData>();
        List<MovieData> remaining = new ArrayList<MovieData>(recommendations);
        while (!remaining.isEmpty() && topRecommendations.size() < recommendations.size()) {
            MovieData best = null;
            for (MovieData movie : remaining) {
                if (best == null || movie.getRating() > best.getRating()) {
                    best = movie;
                }
            }
            if (best != null) {
                topRecommendations.add(best);
                remaining.remove(best);
            }
        }
        recommendations = topRecommendations;
        
        if (recommendations.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Recommendations", 
                "You've watched all " + favoriteGenre + " movies! Try other genres.");
            return;
        }
        
        movieTableData.clear();
        movieTableData.addAll(recommendations);
        statusLabel.setText("Recommendations based on your love for " + favoriteGenre + " movies");
    }
    
    /**
     * We find and return a movie by its ID.
     * This helper method is used throughout the GUI to look up movie details.
     */
    private MovieData getMovieById(String id) {
        for (MovieData movie : movies) {
            if (movie.getId().equals(id)) {
                return movie;
            }
        }
        return null;
    }
    
    /**
     * We add a movie to the current user's watchlist.
     * We check for duplicates and save changes to the CSV file.
     */
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
    
    /**
     * We add a movie to the current user's viewing history.
     * If the movie was in the watchlist, we automatically remove it.
     */
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
    
    /**
     * We remove the selected movie from the user's watchlist.
     * The user must first select an item in the watchlist ListView.
     */
    private void removeFromWatchlist() {
        String selected = watchlistView.getSelectionModel().getSelectedItem();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        currentUser.getWatchlist().remove(movieId);
        saveUsers();
        refreshUserLists();
        statusLabel.setText("Removed from watchlist");
    }
    
    /**
     * We move the selected movie from watchlist to viewing history.
     * This is a convenience method that combines remove and add operations.
     */
    private void moveToHistory() {
        String selected = watchlistView.getSelectionModel().getSelectedItem();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        addToHistory(movieId);
    }
    
    /**
     * We refresh both the watchlist and history ListViews to reflect current data.
     * This method is called after any modification to user lists.
     */
    private void refreshUserLists() {
        if (currentUser == null) return;
        
        watchlistData.clear();
        for (String id : currentUser.getWatchlist()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                watchlistData.add(id + " - " + movie.getTitle());
            }
        }
        
        historyData.clear();
        for (String id : currentUser.getHistory()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                historyData.add(id + " - " + movie.getTitle());
            }
        }
    }
    
    /**
     * We display an alert dialog to show messages to the user.
     * This is used for error messages, success notifications, and information dialogs.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * MovieData class: Represents a movie entity in the GUI
     * Stores movie attributes: id, title, genre, year, and rating
     */
    public static class MovieData {
        private final StringProperty id;
        private final StringProperty title;
        private final StringProperty genre;
        private final IntegerProperty year;
        private final DoubleProperty rating;
        
        public MovieData(String id, String title, String genre, int year, double rating) {
            this.id = new SimpleStringProperty(id);
            this.title = new SimpleStringProperty(title);
            this.genre = new SimpleStringProperty(genre);
            this.year = new SimpleIntegerProperty(year);
            this.rating = new SimpleDoubleProperty(rating);
        }
        
        public String getId() { return id.get(); }
        public StringProperty idProperty() { return id; }
        
        public String getTitle() { return title.get(); }
        public StringProperty titleProperty() { return title; }
        
        public String getGenre() { return genre.get(); }
        public StringProperty genreProperty() { return genre; }
        
        public int getYear() { return year.get(); }
        public IntegerProperty yearProperty() { return year; }
        
        public double getRating() { return rating.get(); }
        public DoubleProperty ratingProperty() { return rating; }
    }
    
    /**
     * UserData class: Represents a user account in the GUI
     * Stores username, password, watchlist and viewing history
     */
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
                   String.join(";", watchlist) + "," + 
                   String.join(";", history);
        }
    }
}
