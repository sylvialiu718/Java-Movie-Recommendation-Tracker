import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
            return false;
        }
        return movieIds.add(movieId);
    }

    public boolean removeMovie(String movieId) {
        if (movieId == null || movieId.trim().isEmpty()) {
            return false;
        }
        movieId = movieId.trim();
        if (!movieIds.contains(movieId)) {
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
            return false;
        }
        movieId = movieId.trim();
        if (movieIds.contains(movieId)) {
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

public class User {
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Watchlist getWatchlist() {
        return watchlist;
    }

    public History getHistory() {
        return history;
    }

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
            userData = new HashMap<>();
        }
    }

    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }

        username = username.trim();
        String inputPassword = password.trim();

        if (!userData.containsKey(username)) {
            return false;
        }

        User user = userData.get(username);
        boolean passwordMatch = PasswordEncoder.verifyPassword(inputPassword, user.getPassword());

        if (passwordMatch) {
            currentUser = user;
            return true;
        } else {
            return false;
        }
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            return null;
        }
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean saveUserChanges() {
        if (currentUser == null) {
            return false;
        }

        try {
            userData.put(currentUser.getUsername(), currentUser);
            fileHandler.writeAllUsers(userData);
            return true;
        } catch (Exception e) {
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

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                if (line.isEmpty()) continue;

                try {
                    User user = parseCsvLine(line);
                    users.put(user.getUsername(), user);
                } catch (IllegalArgumentException e) {
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
            throw new IllegalArgumentException("字段数量错误");
        }

        String username = fields[0].trim();
        String password = fields[1].trim();
        List<String> watchlistIds = parseIdList(fields[2].trim());
        List<String> historyIds = parseIdList(fields[3].trim());

        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("用户名/密码不能为空");
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
    }
}

class PasswordEncoder {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    public static String encodePassword(String rawPassword) {
        try {
            byte[] salt = generateSalt();
            byte[] hash = hashPassword(rawPassword.getBytes(), salt);
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return rawPassword;
        }
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":", 2);
            if (parts.length != 2) {
                return rawPassword.equals(storedPassword);
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);
            byte[] inputHash = hashPassword(rawPassword.getBytes(), salt);
            return MessageDigest.isEqual(inputHash, storedHash);
        } catch (NoSuchAlgorithmException e) {
            return rawPassword.equals(storedPassword);
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPassword(byte[] password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        digest.update(salt);
        return digest.digest(password);
    }
}

class ExceptionHandler {
    public static void handleException(String message, Exception e) {
        System.err.println("[ERROR] " + message + "：" + e.getMessage());
    }

    public static void handleRuntimeException(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void handleCsvFormatException(String line, int lineNumber) {
        System.err.println("[ERROR] CSV格式错误, 行号: " + lineNumber + ", 内容: " + line);
    }
}
