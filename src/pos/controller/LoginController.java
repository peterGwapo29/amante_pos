
package pos.controller;

import pos.model.DBconnection;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML
    private AnchorPane loginBody;
    @FXML
    private PasswordField textPassword;
    @FXML
    private Button loginButton;

    private Connection conn;
    @FXML
    private Hyperlink signupLink;
    @FXML
    private TextField textEmail;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conn = DBconnection.getConnection();
    }

    @FXML
    private void handleLoginButton(ActionEvent event) throws IOException {
        
        if (event.getSource() == loginButton) {
            String email = textEmail.getText().trim();
            String password = textPassword.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                System.out.println("Please fill in all fields!");
                return;
            }

            try {
                if (conn == null || conn.isClosed()) {
                    conn = DBconnection.getConnection();
                }

                String sql = "SELECT * FROM user WHERE email = ? AND password = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String role = rs.getString("role");
                    checkRole(role, email);
                    storeUser(email, password, role);
                } else {
                    System.out.println("Invalid email or password.");
                }
                rs.close();
                ps.close();

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSignupLink(ActionEvent event) throws IOException {
        if(event.getSource() == signupLink){
            
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();
                    
            Parent parent = FXMLLoader.load(getClass().getResource("/pos/view/Register.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(parent));
            stage.show();
        }
    }
    
    private void checkRole(String role, String email)throws IOException{
        
        Parent root;
        FXMLLoader loader;
        
        if(role.equalsIgnoreCase("admin")){
            loader = new FXMLLoader(getClass().getResource("/pos/view/Dashboard.fxml"));
            root = loader.load();
            
        }else{
            loader = new FXMLLoader(getClass().getResource("/pos/view/pos.fxml"));
            root = loader.load();
        }
        
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();

        closeWindow();
    }
    
    private void switchScene(String fxmlPath) throws IOException {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        currentStage.close();

        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }
    
    private void closeWindow(){
        Stage loginStage = (Stage) loginButton.getScene().getWindow();
        loginStage.close();
    }
    
    private void storeUser(String email, String password, String role){
        String status = "active";
        try{
            String sql = "INSERT INTO user_log (email, password, role, status) VALUES (?, ?, ?, ?)";
            
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, email);
            st.setString(2, password);
            st.setString(3, role);
            st.setString(4, status);
            
            st.executeUpdate();
            
        }catch(Exception e){
            System.out.println("Error: " + e);
        }
    }
    
}
