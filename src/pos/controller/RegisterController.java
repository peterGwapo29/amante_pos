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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import pos.model.DBconnection;

/**
 * FXML Controller class
 *
 * @author Devbyte
 */
public class RegisterController implements Initializable {

    @FXML
    private TextField textEmailaddress;
    @FXML
    private PasswordField textPassword;
    @FXML
    private PasswordField textConfirmPassword;
    @FXML
    private Button SignupButton;
    @FXML
    private Hyperlink SigninLink;
    
    private Connection conn;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();
    }    

    @FXML
    private void handleSignUpButton(ActionEvent event) {
        
        if (event.getSource() == SignupButton) {
            String email = textEmailaddress.getText();
            String pass = textPassword.getText();
            String cPass = textConfirmPassword.getText();
            
            if( email.isEmpty() ||
                pass.isEmpty() ){
                
                System.out.println("Please fill in all fields.");
                return;
            }
            if(!isValidEmail(email)){
                System.out.println("Invalid Email Address.");
                return;
            }
            
            if( checkEmail(email) ){
                System.out.println("Email is already taken.");
                return;
            }
            
            if( pass.length() <= 8 ){
                System.out.println("Password must be at least 8 characters long");
                return;
            }
            if( !pass.equals(cPass) ){
                System.out.println("Passwords do not match.");
                return;
            }
            
            registerUser(email, pass);
            clearFields();

        }
    }
    
    private void clearFields(){
        textEmailaddress.setText("");
        textPassword.setText("");
        textConfirmPassword.setText("");
    }
    
    public static boolean isValidEmail(String email){
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    private boolean checkEmail(String email){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            String checkEmail = "SELECT email FROM user WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkEmail);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            return rs.next();
        }catch(Exception e){
            System.out.println("Error: " + e);
            return false;
        }
    }
    
    void registerUser(String email, String pass) {    
        String role = "Cashier";
        try{
            
            String sql = "INSERT INTO user (email, password, role) VALUES (?, ?, ?)";
            
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, email);
            st.setString(2, pass);
            st.setString(3, role); 
            
            int rows = st.executeUpdate();
            
            if (rows > 0) {
                System.out.println("Registration successfully.");
            } else {
                System.out.println("Registration failed.");
            }
        }catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }
   
    @FXML
    private void handleSigninButton(ActionEvent event) throws IOException {
        if(event.getSource() == SigninLink){
            Parent parent = FXMLLoader.load(getClass().getResource("/pos/view/Login.fxml"));
            
            Stage signUpStage = (Stage) SignupButton.getScene().getWindow();
            signUpStage.close();
            
            Stage stage = new Stage();
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.show();
        }
    }
}
