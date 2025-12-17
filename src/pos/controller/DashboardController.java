package pos.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import pos.model.DBconnection;
import pos.util.AlertUtil;
import session.UserSession;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;

public class DashboardController implements Initializable {

    @FXML private Label adminName;
    @FXML private Label roleText;

    @FXML private AnchorPane sidebarContainer;
    @FXML private AnchorPane rootPane;
    @FXML private AnchorPane contentArea;

    // Overview card values
    @FXML private Label lblTotalSalesToday;
    @FXML private Label lblSalesVsYesterday;
    @FXML private Label lblTransactionsToday;
    @FXML private Label lblItemsSoldToday;
    @FXML private Label lblLowStockCount;

    // Quick action cards
    @FXML private VBox cardNewTransaction;
    @FXML private VBox cardAddProduct;
    @FXML private VBox cardViewReports;
    @FXML private VBox cardLowStockList;

    private Connection conn;
    @FXML
    private VBox boxTopProducts;
    @FXML
    private VBox boxAlerts;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();

        loadSidebar();
        adminName.setText(UserSession.getEmail());
        roleText.setText(UserSession.getRole());

        wireQuickActions();
        refreshDashboard();
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/sidebar/sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarContainer.getChildren().setAll(sidebar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshDashboard() {
        loadOverviewCards();
        loadTopSellingProductsToday(4);
        loadAlertsToday();
        wireQuickActions();
        wireShortcuts();
    }

    private void loadOverviewCards() {
        try {
            BigDecimal todaySales = getSalesTotal(LocalDate.now());
            BigDecimal yesterdaySales = getSalesTotal(LocalDate.now().minusDays(1));

            int txnToday = getTransactionCount(LocalDate.now());
            int itemsToday = getItemsSold(LocalDate.now());
            int lowStock = getLowStockCount();

            lblTotalSalesToday.setText("₱ " + todaySales.setScale(2));
            lblTransactionsToday.setText(String.valueOf(txnToday));
            lblItemsSoldToday.setText(String.valueOf(itemsToday));
            lblLowStockCount.setText(String.valueOf(lowStock));

            // percent vs yesterday
            String pctText;
            if (yesterdaySales.compareTo(BigDecimal.ZERO) == 0) {
                pctText = todaySales.compareTo(BigDecimal.ZERO) == 0 ? "+0% vs yesterday" : "+100% vs yesterday";
            } else {
                BigDecimal diff = todaySales.subtract(yesterdaySales);
                BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                        .divide(yesterdaySales, 2, java.math.RoundingMode.HALF_UP);
                String sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                pctText = sign + pct + "% vs yesterday";
            }
            lblSalesVsYesterday.setText(pctText);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Dashboard Error", "Failed to load dashboard stats.\n" + e.getMessage());
        }
    }

    private BigDecimal getSalesTotal(LocalDate date) throws Exception {
        String sql =
            "SELECT COALESCE(SUM(total),0) AS totalSales " +
            "FROM sale " +
            "WHERE DATE(createdAt)=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("totalSales");
            }
        }
        return BigDecimal.ZERO;
    }

    private int getTransactionCount(LocalDate date) throws Exception {
        // you insert multiple rows per receiptNo, so count DISTINCT receiptNo
        String sql =
            "SELECT COUNT(DISTINCT receiptNo) AS txnCount " +
            "FROM sale " +
            "WHERE DATE(createdAt)=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("txnCount");
            }
        }
        return 0;
    }

    private int getItemsSold(LocalDate date) throws Exception {
        String sql =
            "SELECT COALESCE(SUM(quantity),0) AS itemsSold " +
            "FROM sale " +
            "WHERE DATE(createdAt)=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("itemsSold");
            }
        }
        return 0;
    }

    private int getLowStockCount() throws Exception {
        String sql =
            "SELECT COUNT(*) AS lowStock " +
            "FROM product " +
            "WHERE isActive=1 AND currentStock <= reorderLevel";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("lowStock");
        }
        return 0;
    }

    private void wireQuickActions() {
        makeClickable(cardNewTransaction, () -> openWindow("/pos/view/Transaction.fxml", "Transaction"));
        makeClickable(cardAddProduct, () -> openWindow("/pos/view/Product.fxml", "Products"));
        makeClickable(cardViewReports, () -> openWindow("/pos/view/Reports.fxml", "Reports"));
        makeClickable(cardLowStockList, () -> openWindow("/pos/view/Product.fxml", "Products (Low Stock)"));
    }

    private void openWindow(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Navigation Error", "Cannot open: " + title + "\n" + e.getMessage());
        }
    }
    
    private void loadTopSellingProductsToday(int limit) {
    if (boxTopProducts == null) return;
    boxTopProducts.getChildren().clear();

    String sql =
        "SELECT p.name, COALESCE(SUM(s.quantity),0) AS sold " +
        "FROM sale s " +
        "JOIN product p ON p.id = s.productId " +
        "WHERE DATE(s.createdAt)=CURDATE() " +
        "GROUP BY p.id, p.name " +
        "ORDER BY sold DESC " +
        "LIMIT ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, limit);
        try (ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            boolean any = false;

            while (rs.next()) {
                any = true;
                String name = rs.getString("name");
                int sold = rs.getInt("sold");

                HBox row = new HBox();
                row.getStyleClass().add("list-row");

                Label left = new Label(rank + ". " + name);
                left.getStyleClass().add("list-left");

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label right = new Label(sold + " sold");
                right.getStyleClass().add("list-right");

                row.getChildren().addAll(left, spacer, right);
                boxTopProducts.getChildren().add(row);
                rank++;
            }

            if (!any) {
                Label none = new Label("No sales yet today.");
                none.getStyleClass().add("card-sub");
                boxTopProducts.getChildren().add(none);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void loadAlertsToday() {
    if (boxAlerts == null) return;
    boxAlerts.getChildren().clear();

    try {
        int lowStock = getLowStockCount(); // you already have this method
        int outOfStock = getOutOfStockCount();
        int missingBarcode = getMissingBarcodeCount();
        int inactiveCount = getInactiveProductCount();

        boxAlerts.getChildren().add(alertRow("Low stock:", lowStock + " items below minimum"));
        boxAlerts.getChildren().add(alertRow("Out of stock:", outOfStock + " items"));
        boxAlerts.getChildren().add(alertRow("Missing barcode:", missingBarcode + " products"));
        boxAlerts.getChildren().add(alertRow("Inactive:", inactiveCount + " products"));

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private HBox alertRow(String label, String value) {
    HBox row = new HBox(8);
    row.getStyleClass().add("alert-row");

    Label l = new Label(label);
    l.getStyleClass().add("alert-label");

    Label v = new Label(value);
    v.getStyleClass().add("alert-value");

    row.getChildren().addAll(l, v);
    return row;
}

private int getOutOfStockCount() throws Exception {
    String sql = "SELECT COUNT(*) AS c FROM product WHERE isActive=1 AND currentStock <= 0";
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt("c") : 0;
    }
}

private int getMissingBarcodeCount() throws Exception {
    String sql = "SELECT COUNT(*) AS c FROM product WHERE isActive=1 AND (barcode IS NULL OR TRIM(barcode)='')";
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt("c") : 0;
    }
}

private int getInactiveProductCount() throws Exception {
    String sql = "SELECT COUNT(*) AS c FROM product WHERE isActive=0";
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt("c") : 0;
    }
}

private void makeClickable(VBox card, Runnable action) {
    if (card == null) return;
    card.setCursor(Cursor.HAND);
    card.setOnMouseClicked(e -> action.run());
}

private interface ControllerHook { void apply(Object controller); }

private void openWindow(String fxml, String title, ControllerHook hook) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (hook != null) hook.apply(controller);

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
        AlertUtil.error("Navigation Error", "Cannot open: " + title + "\n" + e.getMessage());
    }
}

private void wireShortcuts() {
    Platform.runLater(() -> {
        if (rootPane == null || rootPane.getScene() == null) return;

        rootPane.getScene().getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F1),
            () -> openWindow("/pos/view/Transaction.fxml", "Transaction", null)
        );

        rootPane.getScene().getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F2),
            () -> openWindow("/pos/view/Product.fxml", "Products", null)
        );

        rootPane.getScene().getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F3),
            () -> openWindow("/pos/view/Reports.fxml", "Reports", null)
        );
    });
}

}
