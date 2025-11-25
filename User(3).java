

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


class AuthModuleTest {
    public static void main(String[] args) {

    }
}