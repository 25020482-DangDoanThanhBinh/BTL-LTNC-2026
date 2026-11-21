package dao;
import java.util.List;
import model.User;
public interface UserDao{
    void save(User user);
    User findByUsername(String username);
    User findById(String userId);
    List<User> findAll();
    boolean existsByUsername(String username);
    void clear();
}
