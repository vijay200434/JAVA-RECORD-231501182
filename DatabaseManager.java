import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";
    
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Create expenses table
            String createExpensesTable = """
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    date DATE NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """;
                
            // Create budgets table
            String createBudgetsTable = """
                CREATE TABLE IF NOT EXISTS budgets (
                    user_id INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    limit_amount REAL NOT NULL,
                    PRIMARY KEY (user_id, category),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """;
                
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createExpensesTable);
                stmt.execute(createBudgetsTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean addExpense(Expense expense) {
        String sql = "INSERT INTO expenses (user_id, amount, category, description, date) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, expense.getUserId());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getCategory());
            pstmt.setString(4, expense.getDescription());
            pstmt.setDate(5, expense.getDate());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean setBudget(Budget budget) {
        String sql = "INSERT OR REPLACE INTO budgets (user_id, category, limit_amount) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, budget.getUserId());
            pstmt.setString(2, budget.getCategory());
            pstmt.setDouble(3, budget.getLimit());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<Expense> getExpenses(int userId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY date DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Expense expense = new Expense(
                    rs.getInt("user_id"),
                    rs.getDouble("amount"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getDate("date")
                );
                expense.setId(rs.getInt("id"));
                expenses.add(expense);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }
    
    public static Map<String, Double> getBudgets(int userId) {
        Map<String, Double> budgets = new HashMap<>();
        String sql = "SELECT category, limit_amount FROM budgets WHERE user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                budgets.put(rs.getString("category"), rs.getDouble("limit_amount"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return budgets;
    }
    
    public static Map<String, Double> getExpensesByCategory(int userId) {
        Map<String, Double> categoryTotals = new HashMap<>();
        String sql = "SELECT category, SUM(amount) as total FROM expenses WHERE user_id = ? GROUP BY category";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                categoryTotals.put(rs.getString("category"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryTotals;
    }
}