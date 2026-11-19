package model;
import java.io.Serializable;
public abstract class User implements Serializable{
    private String userId;
    private String username;
    private String password;
    private String email;
    private boolean active;
    public User(String userId,String username,String password,String email){
        if(username==null||username.trim().isEmpty()){
            throw new IllegalArgumentException("Ten dang nhap khong duoc de trong");
        }
        this.userId=userId;
        this.username=username;
        this.password=password;
        this.email=email;
        this.active=true;
    }
    public String getUserId(){
        return userId;
    }
    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        if(username==null||username.trim().isEmpty()){
            throw new IllegalArgumentException("Ten dang nhap khong hop le");
        }
        this.username=username;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        if(password==null||password.length()<4){
            throw new IllegalArgumentException("Mat khau phai tu 4 ky tu tro len");
        }
        this.password=password;
    }
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
    public boolean isActive(){
        return active;
    }
    public void setActive(boolean active){
        this.active=active;
    }
    public abstract String getRole();
    public abstract void displayDashboard();
}