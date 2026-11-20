package service;
import dao.UserDao;
import exception.AuthenticationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import model.Bidder;
import model.Seller;
import model.User;
public class AuthService implements IAuthService{
    private final UserDao userDao;
    private final AtomicInteger idSeq;
    public AuthService(UserDao userDao){
        this.userDao=userDao;
        this.idSeq=new AtomicInteger(100);
        syncIdSequence();
    }
    public void syncIdSequence(){
        int maxId=100;
        List<User> existing=userDao.findAll();
        for(int i=0;i<existing.size();i++){
            String uid=existing.get(i).getUserId();
            if(uid!=null&&uid.startsWith("U")){
                try{
                    int num=Integer.parseInt(uid.substring(1));
                    if(num>maxId){
                        maxId=num;
                    }
                }catch(NumberFormatException ignored){
                }
            }
        }
        idSeq.set(maxId);
    }
    @Override
    public User login(String username,String password)throws AuthenticationException{
        if(username==null||username.trim().isEmpty()){
            throw new AuthenticationException("Ten dang nhap khong duoc de trong");
        }
        if(password==null||password.isEmpty()){
            throw new AuthenticationException("Mat khau khong duoc de trong");
        }
        User user=userDao.findByUsername(username.trim());
        if(user==null){
            throw new AuthenticationException("Tai khoan khong ton tai");
        }
        if(!user.isActive()){
            throw new AuthenticationException("Tai khoan da bi khoa");
        }
        if(!user.getPassword().equals(password)){
            throw new AuthenticationException("Mat khau khong chinh xac");
        }
        return user;
    }
    @Override
    public User register(String username,String password,String email,String role,double initialBalance)throws AuthenticationException{
        if(username==null||username.trim().isEmpty()){
            throw new AuthenticationException("Ten dang nhap khong duoc de trong");
        }
        if(password==null||password.length()<4){
            throw new AuthenticationException("Mat khau phai tu 4 ky tu tro len");
        }
        if(email==null||!email.contains("@")){
            throw new AuthenticationException("Email khong hop le");
        }
        if(userDao.existsByUsername(username.trim())){
            throw new AuthenticationException("Ten dang nhap da ton tai");
        }
        String newId="U"+idSeq.incrementAndGet();
        User created;
        if("SELLER".equalsIgnoreCase(role)){
            created=new Seller(newId,username.trim(),password,email.trim());
        }else if("BIDDER".equalsIgnoreCase(role)){
            if(initialBalance<0){
                throw new AuthenticationException("So du ban dau khong duoc am");
            }
            created=new Bidder(newId,username.trim(),password,email.trim(),initialBalance);
        }else{
            throw new AuthenticationException("Vai tro khong hop le: "+role);
        }
        userDao.save(created);
        return created;
    }
    @Override
    public List<User> getAllUsers(){
        return userDao.findAll();
    }
}
