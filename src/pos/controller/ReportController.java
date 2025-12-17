package pos.controller;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import pos.model.DBconnection;

public class ReportController implements Initializable {

    @FXML private AnchorPane sidebarContainer;

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Button btnGenerate;
    @FXML private Button btnExportCsv;

    @FXML private TableView<SaleRow> tableView;
    @FXML private TableColumn<SaleRow, Timestamp> colDate;
    @FXML private TableColumn<SaleRow, String> colProduct;
    @FXML private TableColumn<SaleRow, Integer> colQty;
    @FXML private TableColumn<SaleRow, BigDecimal> colTotal;

    @FXML private Label lblTransactions;
    @FXML private Label lblItemsSold;
    @FXML private Label lblTotalSales;

    private Connection conn;
    private final ObservableList<SaleRow> rows = FXCollections.observableArrayList();
    @FXML
    private AnchorPane rootPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();
        loadSidebar();

        colDate.setCellValueFactory(data -> data.getValue().createdAtProperty());
        colProduct.setCellValueFactory(data -> data.getValue().productNameProperty());
        colQty.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());
        colTotal.setCellValueFactory(data -> data.getValue().totalProperty());

        tableView.setItems(rows);

        // default range: today
        LocalDate today = LocalDate.now();
        dpFrom.setValue(today);
        dpTo.setValue(today);

        generateReport();
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/sidebar/sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarContainer.getChildren().setAll(sidebar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void generateReport() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        if (from == null || to == null) {
            alert("Missing Date", "Please select both From and To dates.");
            return;
        }
        if (to.isBefore(from)) {
            alert("Invalid Range", "To date must be the same or after From date.");
            return;
        }

        rows.clear();

        LocalDateTime fromDT = LocalDateTime.of(from, LocalTime.MIN);
        LocalDateTime toDT = LocalDateTime.of(to, LocalTime.MAX);

        String sql =
            "SELECT s.createdAt, p.name AS productName, s.quantity, s.total " +
            "FROM sale s " +
            "JOIN product p ON p.id = s.productId " +
            "WHERE s.createdAt BETWEEN ? AND ? " +
            "ORDER BY s.createdAt DESC";

        int transactions = 0;
        int itemsSold = 0;
        BigDecimal totalSales = BigDecimal.ZERO;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fromDT));
            ps.setTimestamp(2, Timestamp.valueOf(toDT));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp createdAt = rs.getTimestamp("createdAt");
                    String productName = rs.getString("productName");
                    int qty = rs.getInt("quantity");
                    BigDecimal total = rs.getBigDecimal("total");

                    rows.add(new SaleRow(createdAt, productName, qty, total));

                    transactions++;
                    itemsSold += qty;
                    totalSales = totalSales.add(total);
                }
            }

            lblTransactions.setText(String.valueOf(transactions));
            lblItemsSold.setText(String.valueOf(itemsSold));
            lblTotalSales.setText(totalSales.toString());

        } catch (Exception e) {
            e.printStackTrace();
            alert("Error", "Failed to generate report: " + e.getMessage());
        }
    }

    @FXML
    private void exportCsv() {
        if (rows.isEmpty()) {
            alert("Nothing to Export", "Generate a report first.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("sales_report.csv");

        File file = fc.showSaveDialog(tableView.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter out = new PrintWriter(file)) {
            out.println("Date/Time,Product,Quantity,Total");
            for (SaleRow r : rows) {
                out.printf("\"%s\",\"%s\",%d,%s%n",
                        r.getCreatedAt(),
                        r.getProductName().replace("\"", "\"\""),
                        r.getQuantity(),
                        r.getTotal());
            }
            out.println();
            out.println("Transactions," + lblTransactions.getText());
            out.println("Items Sold," + lblItemsSold.getText());
            out.println("Total Sales," + lblTotalSales.getText());

            alert("Exported", "CSV saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            alert("Export Failed", e.getMessage());
        }
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
