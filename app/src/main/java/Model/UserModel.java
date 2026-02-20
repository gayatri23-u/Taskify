package Model;

public class UserModel {
    public String fullName;
    public String email;

    public UserModel() {} // required for Firebase

    public UserModel(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }
}