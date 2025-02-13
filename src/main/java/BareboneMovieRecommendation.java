package one.workshop;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.apache.commons.lang3.ArrayUtils;

public class BareboneMovieRecommendation {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/moviedb";
    private static final String USER = "postgres";
    private static final String PASS = "<YOUR_POSTGRES_DB_PASSWORD>";

    public static void main(String[] args) {
        try {
            // Ensure pgvector extension is installed
            createPgVectorExtension();

            // Reset the table if exists
            resetMovieTable();

            // Create table and insert sample data
            createMovieTable();
            insertSampleMovies();

            // Generate embeddings and update the database
            updateMovieEmbeddings();

            // Perform similarity search
            String queryMovie = "Inception";
            List<String> similarMovies = findSimilarMovies(queryMovie);

            System.out.println("Movies similar to " + queryMovie + ":");
            for (String movie : similarMovies) {
                System.out.println(movie);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createPgVectorExtension() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
        }
    }

    private static void createMovieTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS movies (" +
                     "id SERIAL PRIMARY KEY, " +
                     "title TEXT, " +
                     "description TEXT, " +
                     "embedding vector(100))";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void resetMovieTable() throws SQLException {
        String sql = "DROP TABLE IF EXISTS movies";

        try (
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void insertSampleMovies() throws SQLException {
        String sql = "INSERT INTO movies (title, description) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "Inception");
            pstmt.setString(2, "A thief who enters the dreams of others to steal secrets from their subconscious.");
            pstmt.executeUpdate();

            pstmt.setString(1, "The Matrix");
            pstmt.setString(2, "A computer programmer discovers the shocking truth about his simulated reality.");
            pstmt.executeUpdate();

            // Add more movies as needed
        }
    }

    private static void updateMovieEmbeddings() throws SQLException {
        String selectSql = "SELECT id, description FROM movies";
        String updateSql = "UPDATE movies SET embedding = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String description = rs.getString("description");
                float[] embedding = generateEmbedding(description);

                Array pgArray = conn.createArrayOf("float4", ArrayUtils.toObject(embedding));
                pstmt.setArray(1, pgArray);
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
        }
    }

    private static float[] generateEmbedding(String text) {
        // This is a simplified embedding generation.
        // In a real-world scenario, you'd use a more sophisticated model.
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text.toLowerCase());
        float[] embedding = new float[100];
        Random random = new Random(text.hashCode());
        for (int i = 0; i < 100; i++) {
            embedding[i] = random.nextFloat();
        }
        return embedding;
    }

    private static List<String> findSimilarMovies(String queryMovie) throws SQLException {
        String sql = "SELECT title FROM movies " +
                     "WHERE title != ? " +
                     "ORDER BY embedding <-> (SELECT embedding FROM movies WHERE title = ?) " +
                     "LIMIT 5";

        List<String> similarMovies = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, queryMovie);
            pstmt.setString(2, queryMovie);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    similarMovies.add(rs.getString("title"));
                }
            }
        }

        return similarMovies;
    }
}
