package pos.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Product {
    private int id;
    private String name;
    private String description;
    private int categoryId;
    private int supplierId;
    private String sku;
    private String barcode;
    private String inventoryTracking;
    private String baseUnit;
    private BigDecimal price;
    private BigDecimal cost;
    private int initialStock;
    private int currentStock;
    private String imagePath;
    private boolean isActive;
    private Timestamp createdAt;
    private BigDecimal discountPercent;
    private String productType;


    public Product(
        int id, String name, String description,
        int categoryId, int supplierId,
        String sku, String barcode,
        String inventoryTracking, String baseUnit,
        BigDecimal price, BigDecimal discountPercent, BigDecimal cost,
        int initialStock, int currentStock,
        String imagePath, boolean isActive, String productType, Timestamp createdAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        this.sku = sku;
        this.barcode = barcode;
        this.inventoryTracking = inventoryTracking;
        this.baseUnit = baseUnit;
        this.price = price;
        this.discountPercent = discountPercent;   // ✅ IMPORTANT
        this.cost = cost;
        this.initialStock = initialStock;
        this.currentStock = currentStock;
        this.imagePath = imagePath;
        this.isActive = isActive;
        this.productType = productType;
        this.createdAt = createdAt;
    }


    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCategoryId() { return categoryId; }
    public int getSupplierId() { return supplierId; }
    public String getSku() { return sku; }
    public String getBarcode() { return barcode; }
    public String getInventoryTracking() { return inventoryTracking; }
    public String getBaseUnit() { return baseUnit; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCost() { return cost; }
    public int getInitialStock() { return initialStock; }
    public int getCurrentStock() { return currentStock; }
    public boolean getIsActive() { return isActive; }
    public Timestamp getCreatedAt() { return createdAt; }
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
    public String getProductType() {
    return productType;
}

    
     public String getImagePath() { return imagePath; }

    public ImageView getImage() {
        if (imagePath == null || imagePath.isEmpty()) return null;

        ImageView imageView = new ImageView();
        try {
            Image img = new Image("file:" + imagePath, 50, 50, true, true);
            imageView.setImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageView;
    }
}
