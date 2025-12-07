import java.util.List;

/**
 * Simple container for User information.
 */
public class UserData {
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

    // Help to format data for CSV storage
    public String toCsvLine() {
        return username + "," + password + "," +
               String.join(";", watchlist) + "," +
               String.join(";", history);
    }
}