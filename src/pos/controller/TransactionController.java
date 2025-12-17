package pos.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.*;
import java.util.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pos.model.DBconnection;
import pos.model.CartItem;
import pos.util.AlertUtil;

public class TransactionController implements Initializable {

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<ProductRow> productTable;
    @FXML
    private TableColumn<ProductRow, Integer> pIdCol;
    @FXML
    private TableColumn<ProductRow, String> pNameCol;
    @FXML
    private TableColumn<ProductRow, BigDecimal> pPriceCol;
    @FXML
    private TableColumn<ProductRow, BigDecimal> pDiscountCol;
    @FXML
    private TableColumn<ProductRow, Integer> pStockCol;
    @FXML
    private TableColumn<ProductRow, String> pTypeCol;
    @FXML
    private TableColumn<ProductRow, Void> pActionCol;

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> cNameCol;
    @FXML private TableColumn<CartItem, Integer> cQtyCol;
    @FXML private TableColumn<CartItem, BigDecimal> cPriceCol;
    @FXML private TableColumn<CartItem, BigDecimal> cDiscCol;
    @FXML private TableColumn<CartItem, BigDecimal> cSubCol;
    @FXML private TableColumn<CartItem, Void> cRemoveCol;

    @FXML private Label lblTotal;
    @FXML private TextField txtPayment;
    @FXML private Label lblChange;

    @FXML private ComboBox<CustomerRow> cmbCustomer;

    private Connection conn;

    private final ObservableList<ProductRow> products = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();

        // products table
        pIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        pNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        pPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        pDiscountCol.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        pStockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        pTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        // cart table
        cNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        cQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        cDiscCol.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        cSubCol.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        productTable.setItems(products);
        cartTable.setItems(cartItems);
        
                // Keep action columns tight
        pActionCol.setMaxWidth(90);
        pActionCol.setMinWidth(80);
        cRemoveCol.setMaxWidth(110);
        cRemoveCol.setMinWidth(90);

        // Optional: align numeric columns right (cleaner)
        String right = "-fx-alignment: CENTER-RIGHT;";
        pPriceCol.setStyle(right);
        pDiscountCol.setStyle(right);
        pStockCol.setStyle(right);

        cQtyCol.setStyle("-fx-alignment: CENTER;");
        cPriceCol.setStyle(right);
        cDiscCol.setStyle(right);
        cSubCol.setStyle(right);


        // product action button
//        pActionCol.setCellFactory(col -> new TableCell<>() {
//            private final Button btn = new Button("Add");
//            {
//                btn.setOnAction(e -> {
//                    ProductRow p = getTableView().getItems().get(getIndex());
//                    Integer qty = askQuantity(p.getName());
//                    if (qty == null) return;
//                    addProductToCartFlow(p, qty);
//
//                });
//            }
//            @Override protected void updateItem(Void item, boolean empty) {
//                super.updateItem(item, empty);
//                setGraphic(empty ? null : btn);
//            }
//        });
            pActionCol.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Add");
                {
                    btn.getStyleClass().add("table-action-add");  // ✅
                    btn.setOnAction(e -> {
                        ProductRow p = getTableView().getItems().get(getIndex());
                        Integer qty = askQuantity(p.getName());
                        if (qty == null) return;
                        addProductToCartFlow(p, qty);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });


        // cart remove button
//        cRemoveCol.setCellFactory(col -> new TableCell<>() {
//            private final Button btn = new Button("X");
//            {
//                btn.setOnAction(e -> {
//                    CartItem item = getTableView().getItems().get(getIndex());
//                    cartItems.remove(item);
//                    cartTable.refresh();
//                    updateTotalAndChange();
//                    AlertUtil.info("Cart", "Item removed.");
//                });
//            }
//            @Override protected void updateItem(Void item, boolean empty) {
//                super.updateItem(item, empty);
//                setGraphic(empty ? null : btn);
//            }
//        });
cRemoveCol.setCellFactory(col -> new TableCell<>() {
    private final Button btn = new Button("Remove");
    {
        btn.getStyleClass().add("table-action-remove"); // ✅
        btn.setOnAction(e -> {
            CartItem item = getTableView().getItems().get(getIndex());
            cartItems.remove(item);
            cartTable.refresh();
            updateTotalAndChange();
        });
    }
    @Override protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : btn);
    }
});


        // double click product to add
            productTable.setRowFactory(tv -> {
                TableRow<ProductRow> row = new TableRow<>();
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) {
                        ProductRow p = row.getItem();
                        Integer qty = askQuantity(p.getName());
                        if (qty == null) return;
                        addProductToCartFlow(p, qty);
                    }
                });
                return row;
            });


        loadProducts();
        loadCustomers();
        updateTotalAndChange();
    }

    // -----------------------
    // LOADERS
    // -----------------------
    private void loadProducts() {
        products.clear();

        // Detect bundle using EXISTS on ProductBundle
        String sql =
            "SELECT p.id, p.name, p.price, p.discountPercent, p.currentStock, p.productType, " +
            "EXISTS(SELECT 1 FROM productbundle b WHERE b.bundleProductId = p.id) AS isBundle " +
            "FROM product p WHERE p.isActive=1 ORDER BY p.name";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(new ProductRow(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getBigDecimal("discountPercent"),
                    rs.getInt("currentStock"),
                    rs.getString("productType"),
                    rs.getInt("isBundle") == 1
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("DB Error", "Failed to load products.");
        }
    }

    private void loadCustomers() {
        ObservableList<CustomerRow> list = FXCollections.observableArrayList();
        list.add(new CustomerRow(0, "Walk-in Customer"));

        String sql = "SELECT id, firstname, lastname FROM customer ORDER BY firstname, lastname";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("firstname") + " " + rs.getString("lastname");
                list.add(new CustomerRow(id, name));
            }
        } catch (Exception e) {
            // customer table is optional; if error, just keep Walk-in
        }

        cmbCustomer.setItems(list);
        cmbCustomer.getSelectionModel().selectFirst();
    }

    // -----------------------
    // SEARCH
    // -----------------------
    @FXML
    private void searchProducts() {
        String key = txtSearch.getText().trim().toLowerCase();
        loadProducts();
        if (!key.isEmpty()) {
            products.removeIf(p -> !p.getName().toLowerCase().contains(key));
        }
    }

    // -----------------------
    // PAYMENT CHANGE
    // -----------------------
    @FXML
    private void computeChange() {
        updateTotalAndChange();
    }

    private void updateTotalAndChange() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) total = total.add(item.getLineTotal());

        lblTotal.setText(total.setScale(2, RoundingMode.HALF_UP).toString());

        BigDecimal pay = parseMoney(txtPayment.getText());
        BigDecimal change = pay.subtract(total);

        if (change.compareTo(BigDecimal.ZERO) < 0) change = BigDecimal.ZERO;
        lblChange.setText(change.setScale(2, RoundingMode.HALF_UP).toString());
    }

    // -----------------------
    // CART FLOW (variants/modifiers/bundles)
    // -----------------------
    private void addProductToCartFlow(ProductRow p, int qty) {
        if (p.getStock() <= 0 && !p.isBundle()) {
            AlertUtil.warning("Out of Stock", "This product has no stock.");
            return;
        }

        // 1) choose variant if variable
        VariantChoice chosenVariant = null;
        BigDecimal unitBasePrice = p.getPrice();

        if ("variable".equalsIgnoreCase(p.getType())) {
            chosenVariant = pickVariant(p.getId(), p.getName());
            if (chosenVariant == null) return; // canceled
            if (chosenVariant.stock <= 0) {
                AlertUtil.warning("Out of Stock", "Selected variant has no stock.");
                return;
            }
            unitBasePrice = chosenVariant.price;
        }

        // 2) pick modifiers (optional) if product has modifiers
        ModifierResult mod = pickModifiers(p.getId(), p.getName());
        // if product has modifiers and user cancels, mod == null
        if (mod == ModifierResult.CANCELED) return;

        // 3) add to cart (merge by product+variant+modifiersJson)
        String displayName = p.getName();
        Integer variantId = null;

        if (chosenVariant != null) {
            displayName = p.getName() + " - " + chosenVariant.name;
            variantId = chosenVariant.id;
        }

        String modifiersJson = mod == null ? null : mod.json;
        BigDecimal modifierExtra = mod == null ? BigDecimal.ZERO : mod.extra;

        CartItem existing = findSameCartItem(p.getId(), variantId, modifiersJson);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
        } else {
            CartItem item = new CartItem(p.getId(), displayName, qty, unitBasePrice);
            item.setVariantId(variantId);
            item.setDiscountPercent(p.getDiscountPercent());
            item.setModifierExtra(modifierExtra);
            item.setModifiersJson(modifiersJson);
            item.setBundle(p.isBundle());
            cartItems.add(item);
        }

        cartTable.refresh();
        updateTotalAndChange();
        AlertUtil.info("Cart", "Added to cart.");
    }

    private CartItem findSameCartItem(int productId, Integer variantId, String modifiersJson) {
        for (CartItem c : cartItems) {
            boolean sameProduct = c.getProductId() == productId;
            boolean sameVariant = Objects.equals(c.getVariantId(), variantId);
            boolean sameMods = Objects.equals(c.getModifiersJson(), modifiersJson);
            if (sameProduct && sameVariant && sameMods) return c;
        }
        return null;
    }

    // -----------------------
    // VARIANT PICKER (simple ChoiceDialog)
    // -----------------------
    private VariantChoice pickVariant(int productId, String productName) {
        List<VariantChoice> variants = new ArrayList<>();

        String sql = "SELECT id, name, price, stock FROM productvariant WHERE productId=? AND isActive=1 ORDER BY name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    variants.add(new VariantChoice(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Variant Error", "Failed to load variants.");
            return null;
        }

        if (variants.isEmpty()) {
            AlertUtil.warning("No Variants", "This variable product has no variants saved.");
            return null;
        }

        ChoiceDialog<VariantChoice> dialog = new ChoiceDialog<>(variants.get(0), variants);
        dialog.setTitle("Select Variant");
        dialog.setHeaderText(productName);
        dialog.setContentText("Choose variant:");

        return dialog.showAndWait().orElse(null);
    }

    // -----------------------
    // MODIFIER PICKER (basic)
    // - if no modifiers -> returns null
    // - if user cancels -> returns ModifierResult.CANCELED
    // -----------------------
    private ModifierResult pickModifiers(int productId, String productName) {

        // load modifiers
        List<ModifierDef> mods = new ArrayList<>();
        String sql = "SELECT id, name, type, required FROM productmodifier WHERE productId=? ORDER BY id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mods.add(new ModifierDef(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getBoolean("required")
                    ));
                }
            }
        } catch (Exception e) {
            return null;
        }

        if (mods.isEmpty()) return null; // no modifiers

        // Build dialog UI
        Dialog<ModifierResult> dialog = new Dialog<>();
        dialog.setTitle("Modifiers");
        dialog.setHeaderText(productName);

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        VBox box = new VBox(10);
        box.setStyle("-fx-padding:10;");

        // store selections
        Map<Integer, List<ModifierOption>> picked = new LinkedHashMap<>();

        for (ModifierDef m : mods) {
            Label lbl = new Label(m.name + (m.required ? " *" : ""));
            lbl.setStyle("-fx-font-weight:bold;");
            box.getChildren().add(lbl);

            List<ModifierOption> opts = loadModifierOptions(m.id);

            if ("single".equalsIgnoreCase(m.type)) {
                ToggleGroup tg = new ToggleGroup();
                VBox radios = new VBox(4);
                for (ModifierOption o : opts) {
                    RadioButton rb = new RadioButton(o.name + " (+" + o.price + ")");
                    rb.setToggleGroup(tg);
                    rb.setUserData(o);
                    radios.getChildren().add(rb);
                }
                box.getChildren().add(radios);

                // capture on result later
                dialog.setResultConverter(bt -> {
                    if (bt != ok) return ModifierResult.CANCELED;

                    // validate required
                    if (m.required && tg.getSelectedToggle() == null) {
                        AlertUtil.warning("Required", "Please choose: " + m.name);
                        return ModifierResult.CANCELED;
                    }

                    picked.put(m.id, tg.getSelectedToggle() == null
                            ? List.of()
                            : List.of((ModifierOption) tg.getSelectedToggle().getUserData()));

                    return buildModifierResult(mods, picked);
                });

            } else { // multiple
                VBox checks = new VBox(4);
                List<CheckBox> cbList = new ArrayList<>();
                for (ModifierOption o : opts) {
                    CheckBox cb = new CheckBox(o.name + " (+" + o.price + ")");
                    cb.setUserData(o);
                    cbList.add(cb);
                    checks.getChildren().add(cb);
                }
                box.getChildren().add(checks);

                dialog.setResultConverter(bt -> {
                    if (bt != ok) return ModifierResult.CANCELED;

                    List<ModifierOption> selected = new ArrayList<>();
                    for (CheckBox cb : cbList) {
                        if (cb.isSelected()) selected.add((ModifierOption) cb.getUserData());
                    }
                    if (m.required && selected.isEmpty()) {
                        AlertUtil.warning("Required", "Please choose at least 1: " + m.name);
                        return ModifierResult.CANCELED;
                    }
                    picked.put(m.id, selected);

                    return buildModifierResult(mods, picked);
                });
            }
        }

        dialog.getDialogPane().setContent(box);

        ModifierResult res = dialog.showAndWait().orElse(ModifierResult.CANCELED);
        if (res == ModifierResult.CANCELED) return ModifierResult.CANCELED;
        return res;
    }

    private List<ModifierOption> loadModifierOptions(int modifierId) {
        List<ModifierOption> opts = new ArrayList<>();
        String sql = "SELECT id, name, price FROM productmodifieroption WHERE modifierId=? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modifierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    opts.add(new ModifierOption(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price")
                    ));
                }
            }
        } catch (Exception e) { }
        return opts;
    }

    private ModifierResult buildModifierResult(List<ModifierDef> defs, Map<Integer, List<ModifierOption>> picked) {
        BigDecimal extra = BigDecimal.ZERO;
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (ModifierDef d : defs) {
            List<ModifierOption> sel = picked.getOrDefault(d.id, List.of());
            for (ModifierOption o : sel) {
                extra = extra.add(o.price);
                if (!first) json.append(",");
                first = false;
                json.append("{\"modifier\":\"").append(escape(d.name))
                   .append("\",\"option\":\"").append(escape(o.name))
                   .append("\",\"price\":").append(o.price).append("}");
            }
        }
        json.append("]");

        ModifierResult r = new ModifierResult();
        r.extra = extra;
        r.json = first ? null : json.toString(); // null if nothing selected
        return r;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    // -----------------------
    // CHECKOUT (handles stock for simple/variant/bundle + payment/change)
    // -----------------------
    @FXML
    private void checkout() {

        if (cartItems.isEmpty()) {
            AlertUtil.warning("Empty Cart", "No items to checkout.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) total = total.add(item.getLineTotal());

        BigDecimal pay = parseMoney(txtPayment.getText());
        if (pay.compareTo(total) < 0) {
            AlertUtil.warning("Payment 부족", "Payment is not enough.");
            return;
        }

        BigDecimal change = pay.subtract(total);

        String receiptNo = "R-" + System.currentTimeMillis();
        int customerId = (cmbCustomer.getValue() == null) ? 0 : cmbCustomer.getValue().id;
        Integer customerIdNullable = customerId == 0 ? null : customerId;

        try {
            conn.setAutoCommit(false);

            // 1) stock validations + deductions
            for (CartItem item : cartItems) {
                if (item.isBundle()) {
                    ensureBundleStock(item.getProductId(), item.getQuantity());
                } else if (item.getVariantId() != null) {
                    ensureVariantStock(item.getVariantId(), item.getQuantity());
                } else {
                    ensureProductStock(item.getProductId(), item.getQuantity());
                }
            }

            // 2) deduct stock
            for (CartItem item : cartItems) {
                if (item.isBundle()) {
                    deductBundleStock(item.getProductId(), item.getQuantity());
                } else if (item.getVariantId() != null) {
                    deductVariantStock(item.getVariantId(), item.getQuantity());
                } else {
                    deductProductStock(item.getProductId(), item.getQuantity());
                }
            }

            // 3) insert sales rows
            String saleSql =
                "INSERT INTO sale (receiptNo, customerId, productId, variantId, quantity, unitPrice, discountPercent, modifiers, total, payment, changeAmount, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            try (PreparedStatement ps = conn.prepareStatement(saleSql)) {
                for (CartItem item : cartItems) {
                    ps.setString(1, receiptNo);
                    if (customerIdNullable == null) ps.setNull(2, Types.INTEGER);
                    else ps.setInt(2, customerIdNullable);

                    ps.setInt(3, item.getProductId());
                    if (item.getVariantId() == null) ps.setNull(4, Types.INTEGER);
                    else ps.setInt(4, item.getVariantId());

                    ps.setInt(5, item.getQuantity());
                    ps.setBigDecimal(6, item.getFinalUnitPrice());    
                    ps.setBigDecimal(7, item.getDiscountPercent());      
                    if (item.getModifiersJson() == null) ps.setNull(8, Types.VARCHAR);
                    else ps.setString(8, item.getModifiersJson());

                    ps.setBigDecimal(9, item.getLineTotal());
                    ps.setBigDecimal(10, pay);
                    ps.setBigDecimal(11, change);

                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);

            cartItems.clear();
            cartTable.refresh();
            txtPayment.clear();
            txtPayment.clear();
            lblChange.setText("0.00");
            lblTotal.setText("0.00");
            updateTotalAndChange();
            loadProducts();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Checkout Successful");
            alert.setHeaderText("Transaction Completed");
            alert.setContentText(
                    "Receipt No: " + receiptNo + "\n\n" +
                    "Total: " + total.setScale(2) + "\n" +
                    "Payment: " + pay.setScale(2) + "\n" +
                    "Change: " + change.setScale(2)
            );
            alert.showAndWait();


        } catch (Exception e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (Exception ex) {}
            e.printStackTrace();
            AlertUtil.error("Checkout Failed", e.getMessage());
        }
        
        
    }

    // -----------------------
    // STOCK HELPERS
    // -----------------------
    private void ensureProductStock(int productId, int qty) throws Exception {
        String sql = "SELECT currentStock FROM product WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new Exception("Product not found: " + productId);
                int stock = rs.getInt(1);
                if (stock < qty) throw new Exception("Insufficient stock (productId=" + productId + ")");
            }
        }
    }

    private void deductProductStock(int productId, int qty) throws Exception {
        String sql = "UPDATE product SET currentStock = currentStock - ? WHERE id=? AND currentStock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new Exception("Stock update failed (productId=" + productId + ")");
        }
    }

    private void ensureVariantStock(int variantId, int qty) throws Exception {
        String sql = "SELECT stock FROM productvariant WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new Exception("Variant not found: " + variantId);
                int stock = rs.getInt(1);
                if (stock < qty) throw new Exception("Insufficient stock (variantId=" + variantId + ")");
            }
        }
    }

    private void deductVariantStock(int variantId, int qty) throws Exception {
        String sql = "UPDATE productvariant SET stock = stock - ? WHERE id=? AND stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, variantId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new Exception("Variant stock update failed (variantId=" + variantId + ")");
        }
    }

    private void ensureBundleStock(int bundleProductId, int qty) throws Exception {
        // need enough stock for each bundle item
        String sql =
            "SELECT itemProductId, quantity FROM productbundle WHERE bundleProductId=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bundleProductId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int itemId = rs.getInt("itemProductId");
                    int itemQty = rs.getInt("quantity") * qty;
                    ensureProductStock(itemId, itemQty);
                }
                if (!any) throw new Exception("Bundle has no items configured.");
            }
        }
    }

    private void deductBundleStock(int bundleProductId, int qty) throws Exception {
        String sql = "SELECT itemProductId, quantity FROM productbundle WHERE bundleProductId=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bundleProductId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("itemProductId");
                    int itemQty = rs.getInt("quantity") * qty;
                    deductProductStock(itemId, itemQty);
                }
            }
        }
    }

    // -----------------------
    // UTIL
    // -----------------------
    private BigDecimal parseMoney(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to logout?",
                    ButtonType.YES, ButtonType.NO);

            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

            // clear session
            session.UserSession.clearSession();

            // load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Logout Failed", e.getMessage());
        }
    }

    // -----------------------
    // INNER MODELS
    // -----------------------
    public static class ProductRow {
        private final int id;
        private final String name;
        private final BigDecimal price;
        private final BigDecimal discountPercent;
        private final int stock;
        private final String type;
        private final boolean bundle;

        public ProductRow(int id, String name, BigDecimal price, BigDecimal discountPercent, int stock, String type, boolean bundle) {
            this.id = id;
            this.name = name;
            this.price = price == null ? BigDecimal.ZERO : price;
            this.discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
            this.stock = stock;
            this.type = type;
            this.bundle = bundle;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getDiscountPercent() { return discountPercent; }
        public int getStock() { return stock; }
        public String getType() { return type; }
        public boolean isBundle() { return bundle; }
    }

    public static class CustomerRow {
        public final int id;
        public final String name;
        public CustomerRow(int id, String name) { this.id=id; this.name=name; }
        @Override public String toString() { return name; }
    }

    private static class VariantChoice {
        final int id;
        final String name;
        final BigDecimal price;
        final int stock;
        VariantChoice(int id, String name, BigDecimal price, int stock) {
            this.id=id; this.name=name; this.price=price; this.stock=stock;
        }
        @Override public String toString() {
            return name + " | ₱" + price + " | stock:" + stock;
        }
    }

    private static class ModifierDef {
        final int id; final String name; final String type; final boolean required;
        ModifierDef(int id, String name, String type, boolean required) {
            this.id=id; this.name=name; this.type=type; this.required=required;
        }
    }

    private static class ModifierOption {
        final int id; final String name; final BigDecimal price;
        ModifierOption(int id, String name, BigDecimal price) {
            this.id=id; this.name=name; this.price=price == null ? BigDecimal.ZERO : price;
        }
    }

    private static class ModifierResult {
        BigDecimal extra = BigDecimal.ZERO;
        String json;
        static final ModifierResult CANCELED = new ModifierResult();
    }
    
    private Integer askQuantity(String itemName) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Quantity");
        dialog.setHeaderText("Enter quantity for:");
        dialog.setContentText(itemName);

        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty()) return null; // canceled

        try {
            int qty = Integer.parseInt(res.get().trim());
            if (qty <= 0) {
                AlertUtil.warning("Invalid Quantity", "Quantity must be at least 1.");
                return null;
            }
            return qty;
        } catch (Exception e) {
            AlertUtil.warning("Invalid Quantity", "Please enter a valid number.");
            return null;
        }
    }

}
