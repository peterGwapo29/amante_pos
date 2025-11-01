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
import javafx.scene.layout.AnchorPane;
import pos.model.DBconnection;

/**
 * FXML Controller class
 *
 * @author Devbyte
 */
public class RegisterController implements Initializable {

    @FXML
    private TextField textMiddlename;
    @FXML
    private TextField textFirstname;
    @FXML
    private TextField textLastname;
    @FXML
    private TextField textEmailaddress;
    @FXML
    private TextField textUsername;
    @FXML
    private PasswordField textPassword;
    @FXML
    private PasswordField textConfirmPassword;
    @FXML
    private Button SignupButton;
    @FXML
    private Hyperlink SigninLink;
    
    private Connection conn;
    @FXML
    private ComboBox<String> combobox;
    ObservableList<String> role_list = FXCollections.observableArrayList("Admin", "Cashier");

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        combobox.setItems(role_list);
        conn = DBconnection.getConnection();
    }    

    @FXML
    private void handleSignUpButton(ActionEvent event) {
        String role = combobox.getValue();
        
        if (event.getSource() == SignupButton) {
            String fname = textFirstname.getText();
            String mname = textMiddlename.getText();
            String lname = textLastname.getText();
            String email = textEmailaddress.getText();
            String uname = textUsername.getText();
            String pass = textPassword.getText();
            String cPass = textConfirmPassword.getText();
            
            if( fname.isEmpty() ||
                mname.isEmpty() ||
                lname.isEmpty() ||
                email.isEmpty() ||
                uname.isEmpty() ||
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

            if (role == null || role.isEmpty()) {
                System.out.println("Please select a role.");
                return;
            }
            
            if( checkUsername(uname) ){
                System.out.println("Username is already taken.");
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
            
            registerUser(fname, lname, mname, email, uname, pass, role);
            clearFields();

        }
    }
    
    private void clearFields(){
        textFirstname.setText("");
        textMiddlename.setText("");
        textLastname.setText("");
        textEmailaddress.setText("");
        textUsername.setText("");
        textPassword.setText("");
        textConfirmPassword.setText("");
        combobox.getSelectionModel().clearSelection();
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
    
    private boolean checkUsername(String uname){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            String checkUsername = "SELECT username FROM user WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUsername);
            checkStmt.setString(1, uname);
            ResultSet rs = checkStmt.executeQuery();

            return rs.next();
        }catch(Exception e){
            System.out.println("Error: " + e);
            return false;
        }
    }
    
    void registerUser(String fname, String lname, String mname, String email, String uname, String pass, String role) {        
        try{
            String sql = "INSERT INTO user (first_name, middle_name, last_name, email, username, password, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, fname);
            st.setString(2, mname);
            st.setString(3, lname);
            st.setString(4, email);
            st.setString(5, uname);
            st.setString(6, pass);
            st.setString(7, role); 
            
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
