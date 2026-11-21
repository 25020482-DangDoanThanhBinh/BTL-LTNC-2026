package dao;
import java.util.ArrayList;
import java.util.List;
import manager.AuctionManager;
import model.User;
public class InMemoryUserDao implements UserDao{
    private final List<User> users;
    public InMemoryUserDao(){
        this.users=AuctionManager.getInstance().getUsers();
    }
    public InMemoryUserDao(List<User> users){
        this.users=users;
    }
    @Override
    public void save(User user){
        synchronized(users){
            users.add(user);
        }
    }
    @Override
    public void update(User user){
        if(user==null){
            return;
        }
        synchronized(users){
            for(int i=0;i<users.size();i++){
                if(users.get(i).getUserId().equals(user.getUserId())){
                    users.set(i,user);
                    return;
                }
            }
        }
    }
    @Override
    public void delete(String userId){
        if(userId==null){
            return;
        }
        synchronized(users){
            users.removeIf(u->u.getUserId().equals(userId));
        }
    }
    @Override
    public User findByUsername(String username){
        if(username==null){
            return null;
        }
        synchronized(users){
            for(int i=0;i<users.size();i++){
                if(users.get(i).getUsername().equalsIgnoreCase(username)){
                    return users.get(i);
                }
            }
        }
        return null;
    }
    @Override
    public User findById(String userId){
        if(userId==null){
            return null;
        }
        synchronized(users){
            for(int i=0;i<users.size();i++){
                if(users.get(i).getUserId().equals(userId)){
                    return users.get(i);
                }
            }
        }
        return null;
    }
    @Override
    public List<User> findAll(){
        synchronized(users){
            return new ArrayList<>(users);
        }
    }
    @Override
    public boolean existsByUsername(String username){
        return findByUsername(username)!=null;
    }
    @Override
    public void clear(){
        synchronized(users){
            users.clear();
        }
    }
}
