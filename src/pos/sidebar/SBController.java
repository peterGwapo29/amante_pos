package pos.sidebar;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import session.UserSession;

public class SBController implements Initializable {

    @FXML
    private AnchorPane sidebar;
    @FXML
    private ImageView profileImage;
    @FXML
    private Label cashierName;
    @FXML
    private Label roleType;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnProduct;
    @FXML
    private Button btnSales;
    @FXML
    private Button btnReport;
    @FXML
    private Button btnLogout;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cashierName.setText(UserSession.getEmail());
        roleType.setText(UserSession.getRole());

        String role = UserSession.getRole();
        boolean isCashier = role != null && role.equalsIgnoreCase("cashier");
        boolean isAdmin   = role != null && role.equalsIgnoreCase("admin");

        if (isCashier) {
            // CASHIER: only Transaction
            btnDashboard.setVisible(false); btnDashboard.setManaged(false);
            btnProduct.setVisible(false);   btnProduct.setManaged(false);
            btnReport.setVisible(false);    btnReport.setManaged(false);

            btnSales.setText("Transaction");

        } else if (isAdmin) {
            // ADMIN: hide Sales / Transaction
            btnSales.setVisible(false);
            btnSales.setManaged(false);
        }

        restoreActiveMenu();
    }

    
    @FXML
    private void handleLogoutAction(ActionEvent event) throws IOException {
        if(event.getSource() == btnLogout){
            Parent parent = FXMLLoader.load(getClass().getResource("/pos/view/Login.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(parent));
            stage.show();

            Stage logoutStage = (Stage) btnLogout.getScene().getWindow();
            logoutStage.close();
        }
    }

    @FXML
    private void handleProductAction(ActionEvent event) throws IOException {
        if (event.getSource() == btnProduct) {
            UserSession.setActiveMenu("PRODUCT");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/Product.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
    }
    
    @FXML
    private void handleDashboardAction(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos/view/Dashboard.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
    
    @FXML
    private void handleSalesAction(ActionEvent event) throws IOException {
        
        UserSession.setActiveMenu("SALES");
        boolean isCashier = UserSession.getRole() != null
                && UserSession.getRole().equalsIgnoreCase("cashier");

        String fxml = isCashier
                ? "/pos/view/Transaction.fxml"   
                : "/pos/view/pos.fxml";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    private void handleReportAction(javafx.event.ActionEvent event) throws java.io.IOException {
        UserSession.setActiveMenu("REPORT");
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/pos/view/Report.fxml"));
        javafx.scene.Parent root = loader.load();

        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
    }
    
    private void setActive(Button active) {
        btnProduct.getStyleClass().remove("menu-btn-active");
        btnSales.getStyleClass().remove("menu-btn-active");
        btnReport.getStyleClass().remove("menu-btn-active");

        if (!active.getStyleClass().contains("menu-btn-active")) {
            active.getStyleClass().add("menu-btn-active");
        }
    }

    private void restoreActiveMenu() {
        String active = UserSession.getActiveMenu();

        if (active == null) return;

        switch (active) {
            case "PRODUCT"   -> setActive(btnProduct);
            case "SALES"     -> setActive(btnSales);
            case "REPORT"    -> setActive(btnReport);
        }
    }

}
