package pos.controller;

import java.sql.Connection;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.control.cell.PropertyValueFactory;
import pos.model.DBconnection;
import pos.model.Product;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import pos.CRUD.AddProductController;
import pos.util.AlertUtil;


public class ProductController implements Initializable {

    private Button logoutButton;
    private Label cashierName;
    private Label roleType;
    @FXML
    private AnchorPane sidebarContainer;

    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> descriptionColumn;
    @FXML private TableColumn<Product, Integer> categoryColumn;
    @FXML private TableColumn<Product, Integer> supplierColumn;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> barcodeColumn;
    @FXML private TableColumn<Product, String> inventoryColumn;
    @FXML private TableColumn<Product, String> baseUnitColumn;
    @FXML private TableColumn<Product, BigDecimal> priceColumn;
    @FXML private TableColumn<Product, BigDecimal> costColumn;
    @FXML private TableColumn<Product, Integer> initialStockColumn;
    @FXML private TableColumn<Product, Integer> currentStockColumn;
    @FXML private TableColumn<Product, ImageView> imageColumn;
    @FXML private TableColumn<Product, Boolean> activeColumn;
    @FXML private TableColumn<Product, String> createdAtColumn;

    private Connection conn;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private Button addProductButton;
    @FXML
    private ComboBox<String> cmbStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadSidebar();
        conn = DBconnection.getConnection();
        
        cmbStatus.getItems().addAll(
            "Active",
            "Inactive",
            "All"
        );

        cmbStatus.getSelectionModel().select("Active");

        setupTableColumns();
        loadProductData();
        cmbStatus.setOnAction(e -> loadProductData());
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

    private void handleLogoutButton(ActionEvent event) throws IOException {
        Parent parent = FXMLLoader.load(getClass().getResource("/pos/view/Login.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(parent));
        stage.show();

        Stage logoutStage = (Stage) logoutButton.getScene().getWindow();
        logoutStage.close();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        inventoryColumn.setCellValueFactory(new PropertyValueFactory<>("inventoryTracking"));
        baseUnitColumn.setCellValueFactory(new PropertyValueFactory<>("baseUnit"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        initialStockColumn.setCellValueFactory(new PropertyValueFactory<>("initialStock"));
        currentStockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image")); 
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void loadProductData() {
        ObservableList<Product> productList = FXCollections.observableArrayList();

        String status = cmbStatus.getValue(); // Active / Inactive / All

        String sql =
            "SELECT id, name, description, categoryId, supplierId, sku, barcode, " +
            "inventoryTracking, baseUnit, price, discountPercent, cost, " +
            "initialStock, currentStock, image, isActive, productType, createdAt " +
            "FROM product ";

        if (!"All".equals(status)) {
            sql += "WHERE isActive = ? ";
        }

        sql += "ORDER BY name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!"All".equals(status)) {
                ps.setBoolean(1, "Active".equals(status));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                productList.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("categoryId"),
                    rs.getInt("supplierId"),
                    rs.getString("sku"),
                    rs.getString("barcode"),
                    rs.getString("inventoryTracking"),
                    rs.getString("baseUnit"),
                    rs.getBigDecimal("price"),
                    rs.getBigDecimal("discountPercent"),
                    rs.getBigDecimal("cost"),
                    rs.getInt("initialStock"),
                    rs.getInt("currentStock"),
                    rs.getString("image"),
                    rs.getBoolean("isActive"),
                    rs.getString("productType"),
                    rs.getTimestamp("createdAt")
                ));
            }

            tableView.setItems(productList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void addProductModal(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/modal/AddProductModal.fxml"));
            Parent root = loader.load();

            AddProductController addProductController = loader.getController();
            addProductController.setProductController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void reloadTable() {
    loadProductData();
}

    @FXML
    private void openCategoryManager() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/CategoryManagement.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @FXML
    private void openSupplierManager() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/SupplierManagement.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @FXML
    private void openVariantManager() {
        Product p = tableView.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertUtil.warning("No Product Selected", "Please select a product first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/VariantManagement.fxml"));
            Parent root = loader.load();

            VariantManagementController c = loader.getController();
            c.setProduct(p.getId(), p.getName());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            AlertUtil.info("Opened", "Variant manager opened.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Unable to open Variant Manager.\n" + e.getMessage());
        }
    }


    @FXML
    private void openModifierManager() {
        Product p = tableView.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertUtil.warning("No Product Selected", "Please select a product first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/ModifierManagement.fxml"));
            Parent root = loader.load();

            ModifierManagementController c = loader.getController();
            c.setProduct(p.getId(), p.getName());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            AlertUtil.info("Opened", "Modifier manager opened.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Unable to open Modifier Manager.\n" + e.getMessage());
        }
    }


    @FXML
    private void openBundleManager() {
        Product p = tableView.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertUtil.warning("No Product Selected", "Please select a product first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/BundleManagement.fxml"));
            Parent root = loader.load();

            BundleManagementController c = loader.getController();
            c.setBundleProduct(p.getId(), p.getName());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            AlertUtil.info("Opened", "Bundle manager opened.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Unable to open Bundle Manager.\n" + e.getMessage());
        }
    }

    @FXML
    private void updateProduct(ActionEvent event) {
        Product p = tableView.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertUtil.warning("No Product Selected", "Please select a product to update.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/modal/AddProductModal.fxml"));
            Parent root = loader.load();

            AddProductController c = loader.getController();
            c.setProductController(this);

            // IMPORTANT: you must create this method in AddProductController
            c.setEditProduct(p);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Unable to open update modal.\n" + e.getMessage());
        }
    }


    @FXML
    private void deleteProduct() {

        Product p = tableView.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertUtil.warning("No Selection", "Please select a product.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deactivate this product?\n\nProduct will no longer appear in POS.",
                ButtonType.YES, ButtonType.NO);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        String sql = "UPDATE product SET isActive=0, updatedAt=NOW() WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();

            AlertUtil.info("Deactivated", "Product has been deactivated.");
            reloadTable();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Error", "Failed to deactivate product.");
        }
    }
    
    
    public void showLowStockOnly() {
    // optional: if you already have status dropdown
    if (cmbStatus != null) cmbStatus.getSelectionModel().select("Active");

    loadLowStockData();
}

private void loadLowStockData() {
    ObservableList<Product> productList = FXCollections.observableArrayList();

    String sql =
        "SELECT id, name, description, categoryId, supplierId, sku, barcode, " +
        "inventoryTracking, baseUnit, price, discountPercent, cost, " +
        "initialStock, currentStock, image, isActive, createdAt " +
        "FROM product " +
        "WHERE isActive=1 AND currentStock <= reorderLevel " +
        "ORDER BY name";

    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            productList.add(new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("categoryId"),
                rs.getInt("supplierId"),
                rs.getString("sku"),
                rs.getString("barcode"),
                rs.getString("inventoryTracking"),
                rs.getString("baseUnit"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("discountPercent"),
                rs.getBigDecimal("cost"),
                rs.getInt("initialStock"),
                rs.getInt("currentStock"),
                rs.getString("image"),
                rs.getBoolean("isActive"),
                rs.getString("productType"),
                rs.getTimestamp("createdAt")
            ));
        }

        tableView.setItems(productList);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

}
