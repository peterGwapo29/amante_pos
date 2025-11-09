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
import javafx.scene.image.ImageView;
import pos.CRUD.AddProductController;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadSidebar();
        conn = DBconnection.getConnection();
        setupTableColumns();
        loadProductData();
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/sidebar/Sidebar.fxml"));
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
        String sql = "SELECT id, name, description, categoryId, supplierId, sku, barcode, "
                   + "inventoryTracking, baseUnit, price, cost, initialStock, currentStock, "
                   + "image, isActive, createdAt FROM product";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product product = new Product(
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
                        rs.getBigDecimal("cost"),
                        rs.getInt("initialStock"),
                        rs.getInt("currentStock"),
                        rs.getString("image"),
                        rs.getBoolean("isActive"),
                        rs.getTimestamp("createdAt")
                );
                productList.add(product);
            }

            tableView.setItems(productList);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading products: " + e.getMessage());
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
}
