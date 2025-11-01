/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package pos.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Devbyte
 */
public class DashboardController implements Initializable {

    @FXML
    private Button productButton;
    @FXML
    private Button saleButton;
    @FXML
    private Button reportButton;
    @FXML
    private Label adminName1;
    @FXML
    private Label adminName;
    @FXML
    private Button logoutButton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void handleLogoutButton(ActionEvent event) throws IOException {
        if(event.getSource() == logoutButton){
            Parent parent = FXMLLoader.load(getClass().getResource("/pos/view/Login.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(parent));
            stage.show();

            Stage logoutStage = (Stage) logoutButton.getScene().getWindow();
            logoutStage.close();
        }
    }

    @FXML
    private void handleProductAction(ActionEvent event) {
        
        if(event.getSource() == productButton){
            System.out.println("Welcome Admin");
        }
    }
    
    public void setAdminName(String username) {
        adminName.setText(username);
    }
    
}
