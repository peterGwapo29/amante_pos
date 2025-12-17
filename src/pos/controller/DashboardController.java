package pos.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import pos.model.DBconnection;
import pos.util.AlertUtil;
import session.UserSession;

public class DashboardController implements Initializable {

    // Header
    @FXML private Label adminName;
    @FXML private Label roleText;

    // Layout
    @FXML private AnchorPane sidebarContainer;
    @FXML private AnchorPane rootPane;
    @FXML private AnchorPane contentArea;

    // Overview cards
    @FXML private Label lblTotalSalesToday;
    @FXML private Label lblSalesVsYesterday;
    @FXML private Label lblTransactionsToday;
    @FXML private Label lblItemsSoldToday;
    @FXML private Label lblLowStockCount;

    private Connection conn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();

        loadSidebar();
        adminName.setText(UserSession.getEmail());
        roleText.setText(UserSession.getRole());

        loadOverviewCards();
        wireShortcuts();
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/pos/sidebar/sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarContainer.getChildren().setAll(sidebar);

            AnchorPane.setTopAnchor(sidebar, 0.0);
            AnchorPane.setBottomAnchor(sidebar, 0.0);
            AnchorPane.setLeftAnchor(sidebar, 0.0);
            AnchorPane.setRightAnchor(sidebar, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadOverviewCards() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            BigDecimal todaySales = getSalesTotal(today);
            BigDecimal yesterdaySales = getSalesTotal(yesterday);

            lblTotalSalesToday.setText("₱ " + formatMoney(todaySales));
            lblTransactionsToday.setText(String.valueOf(getTransactionCount(today)));
            lblItemsSoldToday.setText(String.valueOf(getItemsSold(today)));
            lblLowStockCount.setText(String.valueOf(getLowStockCount()));

            lblSalesVsYesterday.setText(
                    calculateVsYesterday(todaySales, yesterdaySales));

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error(
                "Dashboard Error",
                "Failed to load dashboard data.\n" + e.getMessage()
            );
        }
    }

    // ---------------- DATABASE ----------------

    private BigDecimal getSalesTotal(LocalDate date) throws Exception {
        String sql =
            "SELECT COALESCE(SUM(total),0) FROM sale WHERE DATE(createdAt)=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    private int getTransactionCount(LocalDate date) throws Exception {
        String sql =
            "SELECT COUNT(DISTINCT receiptNo) FROM sale WHERE DATE(createdAt)=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int getItemsSold(LocalDate date) throws Exception {
        String sql =
            "SELECT COALESCE(SUM(quantity),0) FROM sale WHERE DATE(createdAt)=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int getLowStockCount() throws Exception {
        String sql =
            "SELECT COUNT(*) FROM product WHERE isActive=1 AND currentStock <= reorderLevel";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ---------------- HELPERS ----------------

    private String formatMoney(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String calculateVsYesterday(BigDecimal today, BigDecimal yesterday) {
        if (yesterday.compareTo(BigDecimal.ZERO) == 0) {
            return today.compareTo(BigDecimal.ZERO) == 0
                    ? "+0% vs yesterday"
                    : "+100% vs yesterday";
        }

        BigDecimal pct = today.subtract(yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 2, RoundingMode.HALF_UP);

        return (pct.signum() >= 0 ? "+" : "") + pct + "% vs yesterday";
    }

    // ---------------- SHORTCUTS ----------------

    private void openWindow(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            AlertUtil.error("Navigation Error", e.getMessage());
        }
    }

    private void wireShortcuts() {
        Platform.runLater(() -> {
            if (rootPane.getScene() == null) return;

            rootPane.getScene().getAccelerators().put(
                new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.F1),
                () -> openWindow("/pos/view/Transaction.fxml", "Transaction")
            );

            rootPane.getScene().getAccelerators().put(
                new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.F2),
                () -> openWindow("/pos/view/Product.fxml", "Products")
            );

            rootPane.getScene().getAccelerators().put(
                new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.F3),
                () -> openWindow("/pos/view/Reports.fxml", "Reports")
            );
        });
    }
}
