import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * MovieSystemSwingGUI - Main GUI application for the Movie Recommendation System
 * Provides a graphical interface for browsing movies, managing watchlists and viewing history
 * Built using Java Swing framework
 */
public class MovieSystemSwingGUI extends JFrame {
    
    // Movie data storage
    private List<MovieData> movies = new ArrayList<>();
    private Map<String, List<MovieData>> genreMap = new HashMap<>();
    
    // User data and current session
    private Map<String, UserData> userData = new HashMap<>();
    private UserData currentUser = null;
    
    // UI Components
    private JTable movieTable;
    private DefaultTableModel tableModel;
    private JList<String> watchlistView;
    private DefaultListModel<String> watchlistModel;
    private JList<String> historyView;
    private DefaultListModel<String> historyModel;
    private JTextField searchField;
    private JComboBox<String> genreComboBox;
    private JLabel statusLabel;
    private JLabel userLabel;
    private JPanel mainPanel;
    private JPanel loginPanel;
    
    // File paths for data persistence
    private static final String MOVIES_FILE = "data/movies.csv";
    private static final String USERS_FILE = "data/users.csv";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new MovieSystemSwingGUI();
            }
        });
    }
    
    public MovieSystemSwingGUI() {
        setTitle("Movie Recommendation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        loadMovies();
        loadUsers();
        
        createLoginPanel();
        createMainPanel();
        
        setContentPane(loginPanel);
        setVisible(true);
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
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not load movies: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Could not load users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Could not save users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Creates the login or registration panel
     * Displayed when the application starts
     */
    private void createLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(26, 26, 46));
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(new Color(40, 40, 70));
        formPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        
        JLabel titleLabel = new JLabel("Movie System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Login or Register to continue");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        final JTextField usernameField = new JTextField(20);
        usernameField.setMaximumSize(new Dimension(300, 35));
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        final JPasswordField passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(300, 35));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(39, 174, 96));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setOpaque(true);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));
        loginBtn.setPreferredSize(new Dimension(120, 35));
        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLogin(usernameField.getText(), new String(passwordField.getPassword()));
            }
        });
        
        JButton registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(41, 128, 185));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorderPainted(false);
        registerBtn.setOpaque(true);
        registerBtn.setFont(new Font("Arial", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(120, 35));
        registerBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRegister(usernameField.getText(), new String(passwordField.getPassword()));
            }
        });
        
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        
        formPanel.add(titleLabel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(subtitleLabel);
        formPanel.add(Box.createVerticalStrut(30));
        formPanel.add(new JLabel("Username:") {{ setForeground(Color.WHITE); setAlignmentX(Component.CENTER_ALIGNMENT); }});
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(usernameField);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(new JLabel("Password:") {{ setForeground(Color.WHITE); setAlignmentX(Component.CENTER_ALIGNMENT); }});
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(passwordField);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(buttonPanel);
        
        loginPanel.add(formPanel);
    }
    
    /**
     * Handles user login authentication
     * Supports both plain text and encoded passwords for backward compatibility
     */
    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        UserData user = userData.get(username);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "User not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String storedPwd = user.getPassword();
        String encoded = encodePassword(password);
        
        if (!password.equals(storedPwd) && !encoded.equals(storedPwd)) {
            JOptionPane.showMessageDialog(this, "Incorrect password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        currentUser = user;
        userLabel.setText("Welcome, " + username);
        refreshUserLists();
        setContentPane(mainPanel);
        revalidate();
        repaint();
    }
    
    /**
     * Handles new user registration
     * Creates new user account with encoded password
     */
    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (userData.containsKey(username)) {
            JOptionPane.showMessageDialog(this, "Username already exists", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        UserData newUser = new UserData(username, encodePassword(password), new ArrayList<>(), new ArrayList<>());
        userData.put(username, newUser);
        saveUsers();
        
        JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
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
    
    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(26, 26, 46));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel logo = new JLabel("Movie System");
        logo.setFont(new Font("Arial", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightHeader.setOpaque(false);
        
        userLabel = new JLabel("Welcome");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(231, 76, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setOpaque(true);
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 12));
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveUsers();
                currentUser = null;
                setContentPane(loginPanel);
                revalidate();
                repaint();
            }
        });
        
        rightHeader.add(userLabel);
        rightHeader.add(Box.createHorizontalStrut(15));
        rightHeader.add(logoutBtn);
        
        header.add(logo, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        searchField = new JTextField(20);
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchMovies();
            }
        });
        
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(41, 128, 185));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.setOpaque(true);
        searchBtn.setFont(new Font("Arial", Font.BOLD, 12));
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchMovies();
            }
        });
        
        genreComboBox = new JComboBox<>();
        genreComboBox.addItem("All Genres");
        for (String genre : genreMap.keySet()) {
            genreComboBox.addItem(genre);
        }
        genreComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filterByGenre();
            }
        });
        
        JButton topRatedBtn = new JButton("Top Rated");
        topRatedBtn.setBackground(new Color(243, 156, 18));
        topRatedBtn.setForeground(Color.WHITE);
        topRatedBtn.setFocusPainted(false);
        topRatedBtn.setBorderPainted(false);
        topRatedBtn.setOpaque(true);
        topRatedBtn.setFont(new Font("Arial", Font.BOLD, 12));
        topRatedBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showTopRated();
            }
        });
        
        JButton recommendBtn = new JButton("Get Recommendations");
        recommendBtn.setBackground(new Color(39, 174, 96));
        recommendBtn.setForeground(Color.WHITE);
        recommendBtn.setFocusPainted(false);
        recommendBtn.setBorderPainted(false);
        recommendBtn.setOpaque(true);
        recommendBtn.setFont(new Font("Arial", Font.BOLD, 12));
        recommendBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showRecommendations();
            }
        });
        
        JButton refreshBtn = new JButton("Show All");
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setOpaque(true);
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 12));
        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshMovieTable();
            }
        });
        
        filterBar.add(searchField);
        filterBar.add(searchBtn);
        filterBar.add(genreComboBox);
        filterBar.add(topRatedBtn);
        filterBar.add(recommendBtn);
        filterBar.add(refreshBtn);
        
        String[] columns = {"ID", "Title", "Genre", "Year", "Rating", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
        
        movieTable = new JTable(tableModel);
        movieTable.setRowHeight(30);
        movieTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        movieTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        movieTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        movieTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        movieTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        movieTable.getColumnModel().getColumn(5).setPreferredWidth(180);
        
        movieTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        movieTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane tableScroll = new JScrollPane(movieTable);
        
        centerPanel.add(filterBar, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        rightPanel.setBackground(Color.WHITE);
        
        JLabel watchlistLabel = new JLabel("My Watchlist");
        watchlistLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        watchlistModel = new DefaultListModel<>();
        watchlistView = new JList<>(watchlistModel);
        watchlistView.setVisibleRowCount(8);
        JScrollPane watchlistScroll = new JScrollPane(watchlistView);
        watchlistScroll.setPreferredSize(new Dimension(260, 150));
        
        JPanel watchlistBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton removeWatchlistBtn = new JButton("Remove");
        removeWatchlistBtn.setBackground(new Color(192, 57, 43));
        removeWatchlistBtn.setForeground(Color.WHITE);
        removeWatchlistBtn.setFocusPainted(false);
        removeWatchlistBtn.setBorderPainted(false);
        removeWatchlistBtn.setOpaque(true);
        removeWatchlistBtn.setFont(new Font("Arial", Font.BOLD, 11));
        removeWatchlistBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeFromWatchlist();
            }
        });
        
        JButton moveToHistoryBtn = new JButton("Mark Watched");
        moveToHistoryBtn.setBackground(new Color(39, 174, 96));
        moveToHistoryBtn.setForeground(Color.WHITE);
        moveToHistoryBtn.setFocusPainted(false);
        moveToHistoryBtn.setBorderPainted(false);
        moveToHistoryBtn.setOpaque(true);
        moveToHistoryBtn.setFont(new Font("Arial", Font.BOLD, 11));
        moveToHistoryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveToHistory();
            }
        });
        
        watchlistBtns.add(removeWatchlistBtn);
        watchlistBtns.add(moveToHistoryBtn);
        
        JLabel historyLabel = new JLabel("Viewing History");
        historyLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        historyModel = new DefaultListModel<>();
        historyView = new JList<>(historyModel);
        historyView.setVisibleRowCount(8);
        JScrollPane historyScroll = new JScrollPane(historyView);
        historyScroll.setPreferredSize(new Dimension(260, 150));
        
        rightPanel.add(watchlistLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(watchlistScroll);
        rightPanel.add(watchlistBtns);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(new JSeparator());
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(historyLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(historyScroll);
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusLabel.setBackground(new Color(224, 224, 224));
        statusLabel.setOpaque(true);
        
        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        refreshMovieTable();
    }
    
    private void refreshMovieTable() {
        tableModel.setRowCount(0);
        for (MovieData movie : movies) {
            tableModel.addRow(new Object[]{
                movie.getId(), movie.getTitle(),
                movie.getGenre(), movie.getYear(), movie.getRating(), "Actions"
            });
        }
        statusLabel.setText("Showing all " + movies.size() + " movies");
    }
    
    private void searchMovies() {
        String term = searchField.getText().toLowerCase().trim();
        if (term.isEmpty()) {
            refreshMovieTable();
            return;
        }
        
        tableModel.setRowCount(0);
        int count = 0;
        for (MovieData movie : movies) {
            if (movie.getTitle().toLowerCase().contains(term)) {
                tableModel.addRow(new Object[]{
                    movie.getId(), movie.getTitle(),
                    movie.getGenre(), movie.getYear(), movie.getRating(), "Actions"
                });
                count++;
            }
        }
        statusLabel.setText("Found " + count + " movies matching '" + term + "'");
    }
    
    private void filterByGenre() {
        String genre = (String) genreComboBox.getSelectedItem();
        if (genre == null || genre.equals("All Genres")) {
            refreshMovieTable();
            return;
        }
        
        tableModel.setRowCount(0);
        List<MovieData> genreMovies = genreMap.getOrDefault(genre, new ArrayList<>());
        for (MovieData movie : genreMovies) {
            tableModel.addRow(new Object[]{
                movie.getId(), movie.getTitle(),
                movie.getGenre(), movie.getYear(), movie.getRating(), "Actions"
            });
        }
        statusLabel.setText("Showing " + genreMovies.size() + " " + genre + " movies");
    }
    
    private void showTopRated() {
        List<MovieData> sorted = new ArrayList<MovieData>(movies);
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = 0; j < sorted.size() - i - 1; j++) {
                if (sorted.get(j).getRating() < sorted.get(j + 1).getRating()) {
                    MovieData temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                }
            }
        }
        
        tableModel.setRowCount(0);
        int count = Math.min(20, sorted.size());
        for (int i = 0; i < count; i++) {
            MovieData movie = sorted.get(i);
            tableModel.addRow(new Object[]{
                movie.getId(), movie.getTitle(),
                movie.getGenre(), movie.getYear(), movie.getRating(), "Actions"
            });
        }
        statusLabel.setText("Showing top 20 rated movies");
    }
    
    private void showRecommendations() {
        if (currentUser == null) return;
        
        List<String> history = currentUser.getHistory();
        
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Watch some movies first to get personalized recommendations!\nShowing top rated movies instead.",
                "Recommendations", JOptionPane.INFORMATION_MESSAGE);
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
        
        for (int i = 0; i < recommendations.size() - 1; i++) {
            for (int j = 0; j < recommendations.size() - i - 1; j++) {
                if (recommendations.get(j).getRating() < recommendations.get(j + 1).getRating()) {
                    MovieData temp = recommendations.get(j);
                    recommendations.set(j, recommendations.get(j + 1));
                    recommendations.set(j + 1, temp);
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "You've watched all " + favoriteGenre + " movies! Try other genres.",
                "Recommendations", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        tableModel.setRowCount(0);
        for (MovieData movie : recommendations) {
            tableModel.addRow(new Object[]{
                movie.getId(), movie.getTitle(),
                movie.getGenre(), movie.getYear(), movie.getRating(), "Actions"
            });
        }
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
        String selected = watchlistView.getSelectedValue();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        currentUser.getWatchlist().remove(movieId);
        saveUsers();
        refreshUserLists();
        statusLabel.setText("Removed from watchlist");
    }
    
    private void moveToHistory() {
        String selected = watchlistView.getSelectedValue();
        if (selected == null || currentUser == null) return;
        
        String movieId = selected.split(" - ")[0];
        addToHistory(movieId);
    }
    
    private void refreshUserLists() {
        if (currentUser == null) return;
        
        watchlistModel.clear();
        for (String id : currentUser.getWatchlist()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                watchlistModel.addElement(id + " - " + movie.getTitle());
            }
        }
        
        historyModel.clear();
        for (String id : currentUser.getHistory()) {
            MovieData movie = getMovieById(id);
            if (movie != null) {
                historyModel.addElement(id + " - " + movie.getTitle());
            }
        }
    }
    
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton watchlistBtn = new JButton("+ Watchlist");
        private JButton watchedBtn = new JButton("Watched");
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
            
            watchlistBtn.setBackground(new Color(52, 152, 219));
            watchlistBtn.setForeground(Color.WHITE);
            watchlistBtn.setFocusPainted(false);
            watchlistBtn.setBorderPainted(false);
            watchlistBtn.setOpaque(true);
            watchlistBtn.setMargin(new Insets(2, 6, 2, 6));
            watchlistBtn.setFont(new Font("Arial", Font.BOLD, 10));
            
            watchedBtn.setBackground(new Color(46, 204, 113));
            watchedBtn.setForeground(Color.WHITE);
            watchedBtn.setFocusPainted(false);
            watchedBtn.setBorderPainted(false);
            watchedBtn.setOpaque(true);
            watchedBtn.setMargin(new Insets(2, 6, 2, 6));
            watchedBtn.setFont(new Font("Arial", Font.BOLD, 10));
            
            add(watchlistBtn);
            add(watchedBtn);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton watchlistBtn;
        private JButton watchedBtn;
        private String movieId;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            
            watchlistBtn = new JButton("+ Watchlist");
            watchlistBtn.setBackground(new Color(52, 152, 219));
            watchlistBtn.setForeground(Color.WHITE);
            watchlistBtn.setFocusPainted(false);
            watchlistBtn.setBorderPainted(false);
            watchlistBtn.setOpaque(true);
            watchlistBtn.setMargin(new Insets(2, 6, 2, 6));
            watchlistBtn.setFont(new Font("Arial", Font.BOLD, 10));
            watchlistBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addToWatchlist(movieId);
                    fireEditingStopped();
                }
            });
            
            watchedBtn = new JButton("Watched");
            watchedBtn.setBackground(new Color(46, 204, 113));
            watchedBtn.setForeground(Color.WHITE);
            watchedBtn.setFocusPainted(false);
            watchedBtn.setBorderPainted(false);
            watchedBtn.setOpaque(true);
            watchedBtn.setMargin(new Insets(2, 6, 2, 6));
            watchedBtn.setFont(new Font("Arial", Font.BOLD, 10));
            watchedBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addToHistory(movieId);
                    fireEditingStopped();
                }
            });
            
            panel.add(watchlistBtn);
            panel.add(watchedBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            movieId = (String) table.getValueAt(row, 0);
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }
    
    /**
     * MovieData class：Represents a movie entity in the GUI
     * Stores movie attributes: id, title, genre, year, and rating
     */
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
    
    /**
     * UserData class： Represents a user account in the GUI
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
