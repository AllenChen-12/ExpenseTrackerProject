import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

// Good Practice: Descriptive class and variable names
public class ExpenseTracker {
    private static final String DB_URL = "jdbc:sqlite:expense_tracker.db";

    public static void main(String[] args) {
        initializeDatabase();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        // Programming Construct: Iteration (while loop for main menu)
        while (running) {
            System.out.println("\n--- Personal Expense Tracker ---");
            System.out.println("1. Add Expense");
            System.out.println("2. View All Expenses");
            System.out.println("3. Delete Expense");
            System.out.println("4. View Monthly Trends");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            // Good Practice: Data validation and error handling
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                
                // Programming Construct: If/Else statements
                if (choice == 1) {
                    addExpense(scanner);
                } else if (choice == 2) {
                    viewExpenses();
                } else if (choice == 3) {
                    deleteExpense(scanner);
                } else if (choice == 4) {
                    viewMonthlyTrends();
                } else if (choice == 5) {
                    running = false;
                    System.out.println("Exiting tracker. Goodbye!");
                } else {
                    System.out.println("Invalid option. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            }
        }
        scanner.close();
    }

    // Good Practice: Decomposition (breaking database init into its own method)
    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Good Practice: Single-line comments explaining SQL execution
            // Create categories table
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                         "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "category_name TEXT UNIQUE NOT NULL)");
                         
            // Create expenses table
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                         "expense_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "category_id INTEGER, " +
                         "amount REAL NOT NULL, " +
                         "expense_date TEXT NOT NULL, " +
                         "description TEXT, " +
                         "FOREIGN KEY(category_id) REFERENCES categories(category_id))");
            
            // Populate basic categories if empty
            stmt.execute("INSERT OR IGNORE INTO categories (category_name) VALUES ('Food'), ('Transport'), ('Entertainment')");
            
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }

    private static void addExpense(Scanner scanner) {
        System.out.print("Enter Category ID (1=Food, 2=Transport, 3=Entertainment): ");
        int catId = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());
        System.out.print("Enter Date (YYYY-MM-DD): ");
        String date = scanner.nextLine();
        System.out.print("Enter Description: ");
        String desc = scanner.nextLine();

        String sql = "INSERT INTO expenses(category_id, amount, expense_date, description) VALUES(?, ?, ?, ?)";
        
        // Programming Construct: PreparedStatement (secure against SQL injection)
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, catId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, date);
            pstmt.setString(4, desc);
            pstmt.executeUpdate();
            System.out.println("Expense added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding expense: " + e.getMessage());
        }
    }

    private static void viewExpenses() {
        String sql = "SELECT e.expense_id, c.category_name, e.amount, e.expense_date, e.description " +
                     "FROM expenses e JOIN categories c ON e.category_id = c.category_id";
        
        // Programming Construct: ArrayLists (storing results temporarily)
        ArrayList<String> records = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n--- All Expenses ---");
            while (rs.next()) {
                String record = String.format("ID: %d | Category: %s | Amount: $%.2f | Date: %s | Desc: %s",
                        rs.getInt("expense_id"),
                        rs.getString("category_name"),
                        rs.getDouble("amount"),
                        rs.getString("expense_date"),
                        rs.getString("description"));
                records.add(record);
                System.out.println(record);
            }
            
            if (records.isEmpty()) {
                System.out.println("No expenses found.");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving expenses: " + e.getMessage());
        }
    }

    private static void deleteExpense(Scanner scanner) {
        System.out.print("Enter the ID of the expense to delete: ");
        int id = Integer.parseInt(scanner.nextLine());
        String sql = "DELETE FROM expenses WHERE expense_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Expense deleted successfully.");
            } else {
                System.out.println("Expense ID not found.");
            }
        } catch (SQLException e) {
             System.out.println("Error deleting expense: " + e.getMessage());
        }
    }

    private static void viewMonthlyTrends() {
        String sql = "SELECT strftime('%Y-%m', expense_date) as month, SUM(amount) as total " +
                     "FROM expenses GROUP BY month ORDER BY month";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n--- Monthly Trends ---");
            while (rs.next()) {
                System.out.printf("Month: %s | Total Spent: $%.2f%n", 
                                  rs.getString("month"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
             System.out.println("Error generating trends: " + e.getMessage());
        }
    }
}