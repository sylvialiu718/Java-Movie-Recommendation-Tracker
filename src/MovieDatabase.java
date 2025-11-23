import java.io.*;
import java.util.*;

public class MovieDatabase {
    private static final String CSV_FILE_PATH = "/mnt/data/movies.csv";
    private ArrayList<Movie> movies;

    public MovieDatabase() {
        movies = new ArrayList<>();
        loadMoviesFromCSV(CSV_FILE_PATH);
    }

    public void loadMoviesFromCSV(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                int id = Integer.parseInt(data[0].trim());
                String title = data[1].trim();
                String genre = data[2].trim();
                int year = Integer.parseInt(data[3].trim());
                double rating = Double.parseDouble(data[4].trim());

                Movie movie = new Movie(id, title, genre, year, rating);
                movies.add(movie);
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Error parsing number in CSV: " + e.getMessage());
        }
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }
}
