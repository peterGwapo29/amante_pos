
package pos.CRUD;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import javafx.stage.FileChooser;
import java.io.File;

import java.sql.PreparedStatement;
import java.sql.Connection;
import javafx.stage.Stage;
import pos.model.CategoryDAO;
import pos.model.DBconnection;
import session.Category;

public class AddProductController implements Initializable {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField txtName;
    @FXML
    private TextArea txtDescription;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private ComboBox<?> cmbSupplier;
    @FXML
    private TextField txtSKU;
    @FXML
    private TextField txtBarcode;
    @FXML
    private ComboBox<String> cmbInventoryTracking;
    @FXML
    private TextField txtBaseUnit;
    @FXML
    private TextField txtPrice;
    @FXML
    private TextField txtCost;
    @FXML
    private TextField txtInitialStock;
    @FXML
    private TextField txtReorderLevel;
    @FXML
    private ComboBox<String> cmbProductType;
    @FXML
    private Button btnUploadImage;
    @FXML
    private Label lblImageName;
    @FXML
    private Button btnSubmit;
    
    private Connection conn;
    
//    cmbCategory

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();
        
        cmbInventoryTracking.getItems().addAll("track_stock", "no_track");
        cmbProductType.getItems().addAll("simple", "variable");
        cmbCategory.setItems(CategoryDAO.getAllCategories(conn));
        
        Category selectedCategory = cmbCategory.getSelectionModel().getSelectedItem();
        if(selectedCategory != null){
            int categoryId = selectedCategory.getId();
            System.out.println("Selected Category ID: " + categoryId);
        }
    }    

    @FXML
    private void handleProductSubmit(ActionEvent event) {
        if(event.getSource() == btnSubmit){
            submitProduct();
            System.out.println("Submitted");
        }
    }
    
    private void submitProduct() {
        try {
            // Check if image is selected
            Object imagePathObj = lblImageName.getUserData();
            if (imagePathObj == null) {
                System.out.println("Please select a product image.");
                return;
            }
            String imagePath = imagePathObj.toString();

            // Required fields validation
            if (txtName.getText().isEmpty()) {
                System.out.println("Product name is required.");
                return;
            }
            if (cmbCategory.getSelectionModel().getSelectedItem() == null) {
                System.out.println("Please select a category.");
                return;
            }

            String name = txtName.getText();
            String description = txtDescription.getText();
            Category category = cmbCategory.getSelectionModel().getSelectedItem();
            int categoryId = category.getId();
            int supplierId = 1;
            String sku = txtSKU.getText();
            String barcode = txtBarcode.getText();
            String inventoryTracking = cmbInventoryTracking.getValue() != null ? cmbInventoryTracking.getValue() 
                                        : "track_stock";
            
            String baseUnit = txtBaseUnit.getText().isEmpty() ? "piece" 
                                : txtBaseUnit.getText();
            
            double price = txtPrice.getText().isEmpty() ? 0.0 
                                : Double.parseDouble(txtPrice.getText());
            
            double cost = txtCost.getText().isEmpty() ? 0.0 
                                : Double.parseDouble(txtCost.getText());
            
            int initialStock = txtInitialStock.getText().isEmpty() ? 0 
                                : Integer.parseInt(txtInitialStock.getText());
            
            int reorderLevel = txtReorderLevel.getText().isEmpty() ? 0 
                                : Integer.parseInt(txtReorderLevel.getText());
            
            String productType = cmbProductType.getValue() != null ?
                                 cmbProductType.getValue() : "simple";
            boolean isActive = true;

            String imagesJson = "[\"" + imagePath.replace("\\", "/") + "\"]";

            String sql = "INSERT INTO product "
                       + "(name, description, categoryId, supplierId, sku, barcode, inventoryTracking, baseUnit, price, cost, initialStock, currentStock, reorderLevel, productType, image, images, isActive, createdAt, updatedAt) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, categoryId);
            stmt.setInt(4, supplierId);
            stmt.setString(5, sku);
            stmt.setString(6, barcode);
            stmt.setString(7, inventoryTracking);
            stmt.setString(8, baseUnit);
            stmt.setDouble(9, price);
            stmt.setDouble(10, cost);
            stmt.setInt(11, initialStock);
            stmt.setInt(12, initialStock);
            stmt.setInt(13, reorderLevel);
            stmt.setString(14, productType);
            stmt.setString(15, imagePath);
            stmt.setString(16, imagesJson);
            stmt.setBoolean(17, isActive);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Product Successful");
                alert.setHeaderText(null);
                alert.setContentText("Product added successfully!");
                alert.showAndWait();
                clearFields();
                closeWindow();
            }

            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            lblImageName.setText(selectedFile.getName());
            lblImageName.setUserData(selectedFile.getAbsolutePath()); // store path
        }
    }
    
    private void clearFields(){
        txtName.clear();
        txtDescription.clear();
        txtSKU.clear();
        txtBarcode.clear();
        txtBaseUnit.clear();
        txtPrice.clear();
        txtCost.clear();
        txtInitialStock.clear();
        txtReorderLevel.clear();

        cmbCategory.getSelectionModel().clearSelection();
        cmbSupplier.getSelectionModel().clearSelection();
        cmbInventoryTracking.getSelectionModel().clearSelection();
        cmbProductType.getSelectionModel().clearSelection();

        lblImageName.setText("");
        lblImageName.setUserData(null);
    }
    
    private void closeWindow(){
        Stage loginStage = (Stage) btnSubmit.getScene().getWindow();
        loginStage.close();
    }
}
