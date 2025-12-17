
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import pos.model.CategoryDAO;
import pos.model.DBconnection;
import session.Category;
import pos.controller.ProductController;
import pos.model.Supplier;
import pos.util.AlertUtil;
import pos.model.VariantRow;
import java.sql.ResultSet;
import java.sql.Statement;
import javafx.scene.control.cell.PropertyValueFactory;
import pos.model.Product;


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
    private ComboBox<Supplier> cmbSupplier;
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
    private ProductController productController;
    
    @FXML private TableView<VariantRow> variantTable;
    @FXML private TableColumn<VariantRow, String> vNameCol;
    @FXML private TableColumn<VariantRow, String> vSkuCol;
    @FXML private TableColumn<VariantRow, String> vBarcodeCol;
    @FXML private TableColumn<VariantRow, Double> vPriceCol;
    @FXML private TableColumn<VariantRow, Double> vCostCol;
    @FXML private TableColumn<VariantRow, Integer> vStockCol;
    @FXML private TableColumn<VariantRow, Boolean> vActiveCol;

    @FXML private TextField vTxtName, vTxtSku, vTxtBarcode, vTxtPrice, vTxtCost, vTxtStock;
    @FXML private CheckBox vChkActive;

    private final ObservableList<VariantRow> variantRows = FXCollections.observableArrayList();
    @FXML
    private TextField txtDiscount;

    private Integer editingProductId = null;

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();
        
        cmbInventoryTracking.getItems().addAll("track_stock", "no_track");
        cmbProductType.getItems().addAll("simple", "variable");
        cmbCategory.setItems(CategoryDAO.getAllCategories(conn));
        cmbSupplier.setItems(pos.model.SupplierDAO.getAllSuppliers(conn));
        
        Category selectedCategory = cmbCategory.getSelectionModel().getSelectedItem();
        if(selectedCategory != null){
            int categoryId = selectedCategory.getId();
            System.out.println("Selected Category ID: " + categoryId);
        }
        
        // setup variant columns
        vNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        vSkuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        vBarcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        vPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        vCostCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        vStockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        vActiveCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        variantTable.setItems(variantRows);

        // Disable variant section unless productType == variable
        cmbProductType.valueProperty().addListener((obs, old, val) -> {
            boolean isVariable = "variable".equalsIgnoreCase(val);
            variantTable.setDisable(!isVariable);
            vTxtName.setDisable(!isVariable);
            vTxtSku.setDisable(!isVariable);
            vTxtBarcode.setDisable(!isVariable);
            vTxtPrice.setDisable(!isVariable);
            vTxtCost.setDisable(!isVariable);
            vTxtStock.setDisable(!isVariable);
            vChkActive.setDisable(!isVariable);
        });

    }    

    @FXML
    private void handleProductSubmit(ActionEvent event) {
        if(event.getSource() == btnSubmit){
            submitProduct();
            System.out.println("Submitted");
        }
    }
    
    private void submitProduct() {
        double discountPercent = txtDiscount.getText().isEmpty()
        ? 0.0
        : Double.parseDouble(txtDiscount.getText());
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
            
            if (cmbSupplier.getSelectionModel().getSelectedItem() == null) {
                System.out.println("Please select a supplier.");
                return;
            }
            
            int supplierId = cmbSupplier.getSelectionModel().getSelectedItem().getId();
            String name = txtName.getText();
            String description = txtDescription.getText();
            Category category = cmbCategory.getSelectionModel().getSelectedItem();
            int categoryId = category.getId();
            
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

            boolean isEdit = (editingProductId != null);

            String sql;
            if (!isEdit) {
                sql = "INSERT INTO product (name,description,categoryId,supplierId,sku,barcode,inventoryTracking,baseUnit,price,discountPercent,cost,initialStock,currentStock,reorderLevel,productType,image,images,isActive,createdAt,updatedAt) " +
                      "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
            } else {
                sql = "UPDATE product SET name=?, description=?, categoryId=?, supplierId=?, sku=?, barcode=?, inventoryTracking=?, baseUnit=?, price=?, discountPercent=?, cost=?, initialStock=?, reorderLevel=?, productType=?, image=?, images=?, updatedAt=NOW() " +
                      "WHERE id=?";
            }
            
//            if (isEdit) {
//                stmt.setInt(18, editingProductId); // adjust index if needed based on your exact sets
//            
//            }


            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setInt(3, categoryId);
                stmt.setInt(4, supplierId);
                stmt.setString(5, sku);
                stmt.setString(6, barcode);
                stmt.setString(7, inventoryTracking);
                stmt.setString(8, baseUnit);
                stmt.setDouble(9, price);
                stmt.setDouble(10, discountPercent); // ✅ THIS WAS CORRECT
                stmt.setDouble(11, cost);
                stmt.setInt(12, initialStock);

                if (!isEdit) {
                    stmt.setInt(13, initialStock); // currentStock
                    stmt.setInt(14, reorderLevel);
                    stmt.setString(15, productType);
                    stmt.setString(16, imagePath);
                    stmt.setString(17, imagesJson);
                    stmt.setBoolean(18, true);
                } else {
                    stmt.setInt(13, reorderLevel);
                    stmt.setString(14, productType);
                    stmt.setString(15, imagePath);
                    stmt.setString(16, imagesJson);
                    stmt.setInt(17, editingProductId); // ✅ CORRECT PLACE
                }
                
                int rowsInserted = stmt.executeUpdate();
                
                if (rowsInserted > 0) {
                    ResultSet keys = stmt.getGeneratedKeys();
                    int newProductId = -1;
                    if (keys.next()) newProductId = keys.getInt(1);
                    keys.close();
                    
                    String selectedType = cmbProductType.getValue() != null ? cmbProductType.getValue() : "simple";
                    if ("variable".equalsIgnoreCase(selectedType)) {
                        if (variantRows.isEmpty()) {
                            AlertUtil.warning("Missing Variants", "Product type is variable. Please add at least 1 variant.");
                            return;
                        }
                        insertVariants(newProductId);
                    }

                    AlertUtil.info("Success", "Product saved successfully!");
                    
                    if (productController != null) productController.reloadTable();
                    
                    clearFields();
                    variantRows.clear(); // clear variant list too
                    closeWindow();
                }
            }


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
        txtDiscount.clear();
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
    
    public void setProductController(ProductController productController) {
        this.productController = productController;
    }

    @FXML
    private void addVariantRow() {
        String name = vTxtName.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.warning("Validation", "Variant name is required.");
            return;
        }

        double price = parseD(vTxtPrice.getText());
        double cost = parseD(vTxtCost.getText());
        int stock = parseI(vTxtStock.getText());

        variantRows.add(new VariantRow(
                name,
                vTxtSku.getText().trim(),
                vTxtBarcode.getText().trim(),
                price,
                cost,
                stock,
                vChkActive.isSelected()
        ));

        vTxtName.clear();
        vTxtSku.clear();
        vTxtBarcode.clear();
        vTxtPrice.clear();
        vTxtCost.clear();
        vTxtStock.clear();
        vChkActive.setSelected(true);

        AlertUtil.info("Added", "Variant added to list.");
    }

    @FXML
    private void removeVariantRow() {
        VariantRow selected = variantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("No Selection", "Please select a variant to remove.");
            return;
        }
        variantRows.remove(selected);
        AlertUtil.info("Removed", "Variant removed.");
    }

    private int parseI(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private double parseD(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }
    
    
    private void insertVariants(int productId) throws Exception {
        String sql = "INSERT INTO productvariant (productId, name, sku, barcode, price, cost, stock, isActive, createdAt, updatedAt) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (VariantRow v : variantRows) {
                ps.setInt(1, productId);
                ps.setString(2, v.getName());
                ps.setString(3, v.getSku().isEmpty() ? null : v.getSku());
                ps.setString(4, v.getBarcode().isEmpty() ? null : v.getBarcode());
                ps.setDouble(5, v.getPrice());
                ps.setDouble(6, v.getCost());
                ps.setInt(7, v.getStock());
                ps.setBoolean(8, v.getActive());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        AlertUtil.info("Variants Saved", "All variants saved successfully.");
    }
    
    public void setEditProduct(Product p) {
        editingProductId = p.getId();

        txtName.setText(p.getName());
        txtDescription.setText(p.getDescription());
        txtSKU.setText(p.getSku());
        txtBarcode.setText(p.getBarcode());
        txtBaseUnit.setText(p.getBaseUnit());
        txtPrice.setText(p.getPrice() != null ? p.getPrice().toString() : "");
        txtCost.setText(p.getCost() != null ? p.getCost().toString() : "");
        txtInitialStock.setText(String.valueOf(p.getInitialStock()));
        txtDiscount.setText(p.getDiscountPercent() != null ? p.getDiscountPercent().toString() : "");

        // ✅ Select ComboBox items by ID
        selectCategoryById(p.getCategoryId());
        selectSupplierById(p.getSupplierId());

        // ✅ Select strings
        cmbInventoryTracking.getSelectionModel().select(p.getInventoryTracking());
        cmbProductType.getSelectionModel().select(p.getProductType());

        // image
        lblImageName.setText(p.getImagePath());
        lblImageName.setUserData(p.getImagePath());

        btnSubmit.setText("Update");
    }

    private void selectCategoryById(int id) {
        for (Category c : cmbCategory.getItems()) {
            if (c.getId() == id) {
                cmbCategory.getSelectionModel().select(c);
                return;
            }
        }
    }

    private void selectSupplierById(int id) {
        for (Supplier s : cmbSupplier.getItems()) {
            if (s.getId() == id) {
                cmbSupplier.getSelectionModel().select(s);
                return;
            }
        }
    }


}
