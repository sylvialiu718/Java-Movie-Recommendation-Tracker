import javafx.beans.property.*;

/**
 * Model class for a Movie.
 * Uses JavaFX properties for UI binding.
 */
public class MovieData {
    // Properties allow the TableView to automatically update
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

    // Getters and property accessors
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
