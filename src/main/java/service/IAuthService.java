package service;
import exception.AuthenticationException;
import java.util.List;
import model.User;
public interface IAuthService{
    User login(String username,String password)throws AuthenticationException;
    User register(String username,String password,String email,String role,double initialBalance)throws AuthenticationException;
    List<User> getAllUsers();
}
