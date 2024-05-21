package org.example.Network;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/myDatabase";
    private static final String USER = "root";
    private static final String PASSWORD = "000000";

    /**
     * Executes the given SQL statement.
     *
     * @param sqlString the SQL statement to be executed
     * @return the result of the query if it is a SELECT statement, otherwise the number of affected rows
     */
    public static Object executeSQL(String sqlString) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Load and register JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Create a connection
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // Create a statement
            stmt = conn.createStatement();
            // Determine if the SQL statement is a query
            if (sqlString.trim().toUpperCase().startsWith("SELECT")) {
                // Execute the query
                rs = stmt.executeQuery(sqlString);

                // Process the result set
                StringBuilder result = new StringBuilder();
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(rs.getString(i)).append("\t");
                    }
                    result.append("\n");
                }
                return result.toString();
            } else {
                // Execute the update
                int affectedRows = stmt.executeUpdate(sqlString);
                return affectedRows;
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes the given SQL statement.
     *
     * @param conn      the connection to the database
     * @param sqlString the SQL statement to be executed
     */
    private static void executeSQL(Connection conn, String sqlString) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sqlString);
        }
    }

    /**
     * Deletes all tables in the database.
     *
     * @param conn the connection to the database
     * @throws SQLException if a database access error occurs
     */
    public static void deleteAllTables(Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();

            // Disable foreign key checks
            stmt.execute("SET foreign_key_checks = 0");

            // Get all table names
            String getTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'myDatabase'";
            rs = stmt.executeQuery(getTablesQuery);

            // Use a separate statement for dropping tables
            Statement dropStmt = conn.createStatement();
            try {
                // Drop each table
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String dropTableQuery = "DROP TABLE IF EXISTS " + tableName;
                    dropStmt.execute(dropTableQuery);
                }
            } finally {
                // Close the drop statement
                if (dropStmt != null) dropStmt.close();
            }
            // Re-enable foreign key checks
            stmt.execute("SET foreign_key_checks = 1");
            System.out.println("All tables deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Rethrow the exception after logging
        } finally {
            // Close resources
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }


    /**
     * Initializes the database by deleting all tables and creating necessary tables.
     */
    public static void initDatabase() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            deleteAllTables(conn);
            createTables(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates necessary tables in the database.
     */
    private static void createTables(Connection conn) throws SQLException {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS Users ("
                + "UserId INT AUTO_INCREMENT PRIMARY KEY, "
                + "UserName VARCHAR(100) NOT NULL, "
                + "UserPassword VARCHAR(100) NOT NULL"
                + ")";

        String createImagesTable = "CREATE TABLE IF NOT EXISTS Images ("
                + "ImageId INT AUTO_INCREMENT PRIMARY KEY, "
                + "ImageName VARCHAR(255) NOT NULL, "
                + "ImageType VARCHAR(50) NOT NULL, "
                + "ImageSize INT NOT NULL, "
                + "ImagePath VARCHAR(255) NOT NULL"
                + ")";

        String createUserImagesTable = "CREATE TABLE IF NOT EXISTS UserImages ("
                + "UserId INT NOT NULL, "
                + "ImageId INT NOT NULL, "
                + "PRIMARY KEY (UserId, ImageId), "
                + "FOREIGN KEY (UserId) REFERENCES Users(UserId), "
                + "FOREIGN KEY (ImageId) REFERENCES Images(ImageId)"
                + ")";

        executeSQL(conn, createUsersTable);
        executeSQL(conn, createImagesTable);
        executeSQL(conn, createUserImagesTable);
        System.out.println("All tables created successfully.");
    }


    /**
     * Checks if the connection to the database is successful.
     *
     * @return true if the connection is successful, false otherwise
     */
    public static boolean isDatabaseConnected() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // If connection is successful
            return true;
        } catch (SQLException e) {
            // If connection fails
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new user into the Users table.
     *
     * @param userName     the username to be inserted
     * @param userPassword the user password to be inserted
     * @return  if successfully create a User in users
     * @throws SQLException if a database access error occurs
     */
    public static boolean insertUser(String userName, String userPassword) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String insertQuery = "INSERT INTO Users (UserName, UserPassword) VALUES (?, ?)";

            // Create PreparedStatement with RETURN_GENERATED_KEYS option
            pstmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, userName);
            pstmt.setString(2, userPassword);

            // Execute insert query
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                return false; // Inserting user failed, no rows affected
            }

            // Get auto-generated UserId
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1); // Get the auto-generated UserId
                System.out.println("User inserted successfully with UserId: " + userId);
                return true;
            } else {
                return false; // Inserting user failed, no UserId obtained
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            // Close resources
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }


    /**
     * Checks if the Users table contains a row with the given UserId and UserPassword.
     *
     * @param userId the user ID to check
     * @param userPassword the user password to check
     * @return true if the user exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public static boolean userExists(int userId, String userPassword) throws SQLException {
        String query = "SELECT COUNT(*) FROM Users WHERE UserId = ? AND UserPassword = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return false;
    }

    /**
     * Inserts a new image into the Images table and associates it with a user in the UserImages table.
     *
     * @param userId the ID of the user
     * @param userPassword the password of the user
     * @param imageName the name of the image
     * @param imageSize the size of the image
     * @param imageType the type of the image (e.g., jpg, png)
     * @return true if the image was successfully sent and associated with the user, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public static String sendUserImage(int userId, String userPassword, String imageName, int imageSize, String imageType) throws SQLException {
        // Check if user exists
        if (!userExists(userId, userPassword)) {
            System.out.println("User does not exist.");
            return "";
        }

        Connection conn = null;
        PreparedStatement insertImageStmt = null;
        PreparedStatement updateImagePathStmt = null;
        PreparedStatement insertUserImageStmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            conn.setAutoCommit(false); // Start transaction

            String insertImageQuery = "INSERT INTO Images (ImageName, ImageType, ImageSize, ImagePath) VALUES (?, ?, ?, '')";
            insertImageStmt = conn.prepareStatement(insertImageQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            insertImageStmt.setString(1, imageName);
            insertImageStmt.setString(2, imageType);
            insertImageStmt.setInt(3, imageSize);

            // Execute insert query for image
            int affectedRows = insertImageStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting image failed, no rows affected.");
            }

            // Get auto-generated ImageId
            rs = insertImageStmt.getGeneratedKeys();
            int imageId;
            if (rs.next()) {
                imageId = rs.getInt(1);
            } else {
                throw new SQLException("Inserting image failed, no ImageId obtained.");
            }

            // Construct ImagePath
            String imagePath = "src/main/java/com/example/network/Server/Images/" + imageId + "." + imageType;

            // Update image with the constructed ImagePath
            String updateImagePathQuery = "UPDATE Images SET ImagePath = ? WHERE ImageId = ?";
            updateImagePathStmt = conn.prepareStatement(updateImagePathQuery);
            updateImagePathStmt.setString(1, imagePath);
            updateImagePathStmt.setInt(2, imageId);
            updateImagePathStmt.executeUpdate();

            // Insert into UserImages table
            String insertUserImageQuery = "INSERT INTO UserImages (UserId, ImageId) VALUES (?, ?)";
            insertUserImageStmt = conn.prepareStatement(insertUserImageQuery);
            insertUserImageStmt.setInt(1, userId);
            insertUserImageStmt.setInt(2, imageId);
            insertUserImageStmt.executeUpdate();

            // Commit transaction
            conn.commit();

            System.out.println("Image sent and associated with user successfully.");
            return imagePath;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback transaction on error
            }
            e.printStackTrace();
            throw e;
        } finally {
            // Close resources
            if (rs != null) rs.close();
            if (insertImageStmt != null) insertImageStmt.close();
            if (updateImagePathStmt != null) updateImagePathStmt.close();
            if (insertUserImageStmt != null) insertUserImageStmt.close();
            if (conn != null) conn.close();
        }
    }



    /**
     * Finds and returns the IDs of all images with the given name.
     *
     * @param name the name of the images to find
     * @return a list of image IDs with the given name
     * @throws SQLException if a database access error occurs
     */
    public static List<Integer> findImagesByName(String name) throws SQLException {
        List<Integer> imageIds = new ArrayList<>();
        String query = "SELECT ImageId FROM Images WHERE ImageName = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int imageId = rs.getInt("ImageId");
                    imageIds.add(imageId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return imageIds;
    }

















    public static void main(String[] args) throws SQLException {
//        System.out.println("Database linked: "+isDatabaseConnected());
//        initDatabase();
//        System.out.println("if successfully create a user: "+  insertUser("sensei", "sensei"));
//        System.out.println("User exist: "+userExists("sensei", "sensei"));
//        String result = sendUserImage(1, "sensei", "exampleImage", 12345, "jpg");
//        System.out.println("Image sent Path: " + result);

        List<Integer> resultInts = findImagesByName("exampleImage");
        for (int i = 0; i < resultInts.size(); i++) {
            System.out.print(resultInts.get(i)+" ");
        }


    }

}
