package pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartItem {
    private final int productId;
    private Integer variantId;              // nullable
    private final String name;
    private int quantity;

    private final BigDecimal basePrice;     // product price OR variant price
    private BigDecimal modifierExtra = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO; // from product table
    private String modifiersJson;           // optional

    private boolean bundle;                 // true if this product is a bundle

    public CartItem(int productId, String name, int quantity, BigDecimal basePrice) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.basePrice = basePrice;
    }

    // REQUIRED by your UI/table
    public int getProductId() { return productId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return getFinalUnitPrice(); } // what cashier pays per unit

    // Extra fields
    public Integer getVariantId() { return variantId; }
    public void setVariantId(Integer variantId) { this.variantId = variantId; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
    }

    public BigDecimal getModifierExtra() { return modifierExtra; }
    public void setModifierExtra(BigDecimal modifierExtra) {
        this.modifierExtra = modifierExtra == null ? BigDecimal.ZERO : modifierExtra;
    }

    public String getModifiersJson() { return modifiersJson; }
    public void setModifiersJson(String modifiersJson) { this.modifiersJson = modifiersJson; }

    public boolean isBundle() { return bundle; }
    public void setBundle(boolean bundle) { this.bundle = bundle; }

    public void setQuantity(int q) { this.quantity = q; }

    // Calculations
    public BigDecimal getFinalUnitPrice() {
        BigDecimal unit = basePrice.add(modifierExtra);
        BigDecimal disc = unit.multiply(discountPercent).divide(BigDecimal.valueOf(100));
        return unit.subtract(disc);
//        return basePrice.add(modifierExtra);
    }

    public BigDecimal getLineTotal() {
        return getFinalUnitPrice().multiply(BigDecimal.valueOf(quantity));
//    BigDecimal subtotal = getFinalUnitPrice().multiply(BigDecimal.valueOf(quantity));
//
//       BigDecimal disc = subtotal.multiply(discountPercent)
//               .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
//
//       return subtotal.subtract(disc);
    }
    
    public BigDecimal getDiscountAmount() {
        BigDecimal subtotal = getFinalUnitPrice().multiply(BigDecimal.valueOf(quantity));
        return subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100));
    }

}
