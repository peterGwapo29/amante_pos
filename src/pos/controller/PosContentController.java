package pos.controller;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import pos.model.*;

public class PosContentController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private TextField txtQty;

    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colPName;
    @FXML private TableColumn<Product, BigDecimal> colPPrice;
    @FXML private TableColumn<Product, Integer> colPStock;

    @FXML private TableView<CartItem> tblCart;
    @FXML private TableColumn<CartItem, String> colCName;
    @FXML private TableColumn<CartItem, Integer> colCQty;
    @FXML private TableColumn<CartItem, BigDecimal> colCPrice;
    @FXML private TableColumn<CartItem, BigDecimal> colCTotal;

    @FXML private Label lblGrandTotal;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private Connection conn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();

        colPName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));

        colCName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colCPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        tblProducts.setItems(products);
        tblCart.setItems(cart);

        loadProducts("");

        txtSearch.textProperty().addListener((obs, o, n) -> loadProducts(n == null ? "" : n.trim()));
    }

    private void loadProducts(String keyword) {
        products.clear();
        
        
        String sql =
            "SELECT id,name,description,categoryId,supplierId,sku,barcode,inventoryTracking,baseUnit," +
            "price,discountPercent,cost,initialStock,currentStock,image,isActive,createdAt " +
            "FROM product WHERE isActive=1 AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?) ORDER BY name";



        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addToCart() {
        Product p = tblProducts.getSelectionModel().getSelectedItem();
        if (p == null) return;

        int qty = 1;
        try { qty = Integer.parseInt(txtQty.getText().trim()); } catch (Exception ignored) {}
        if (qty <= 0) qty = 1;

        // merge if already exists
        for (CartItem ci : cart) {
            if (ci.getProductId() == p.getId()) {
                ci.setQuantity(ci.getQuantity() + qty);
                tblCart.refresh();
                updateTotal();
                return;
            }
        }

        cart.add(new CartItem(p.getId(), p.getName(), qty, p.getPrice()));
        updateTotal();
    }

    @FXML
    private void removeSelected() {
        CartItem selected = tblCart.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cart.remove(selected);
            updateTotal();
        }
    }

    @FXML
    private void clearCart() {
        cart.clear();
        updateTotal();
    }

    @FXML
    private void checkout() {
        try {
            SaleDAO.checkout(conn, cart);
            clearCart();
            loadProducts(txtSearch.getText() == null ? "" : txtSearch.getText().trim());
            alert("Success", "Checkout complete!");
        } catch (Exception e) {
            alert("Checkout Failed", e.getMessage());
        }
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart) total = total.add(ci.getLineTotal());
        lblGrandTotal.setText(total.toString());
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
