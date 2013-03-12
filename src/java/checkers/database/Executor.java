//ez az osztály kommunikál az adatbázissal hibernate segítségével

package checkers.database;

import java.util.List;
import checkers.core.InputValidator;
import checkers.database.table.Game;
import checkers.database.table.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class Executor {

    public static boolean isUserAdmin(String id) {
        User u = getUser(id);
        if (u != null) return u.isAdmin();
        else return false;
    }
    
    //a game tábla összes sora - mint objektum - listában!
    public static List<Game> getGameList() {
        Session sess = createSession();
        Query query = sess.createQuery("from Game game");
        return query.list();
    }
    
    //játszma létrehozása
    public static boolean createGame(Game g) {
        return addObject(g);
    }

    /*
    //játszma törlése
    //igazat ad vissza, ha ki lett törölve 1 sor az adatbázisból
    //BUGOS! Oka: mivel egy Play nevének az első karaktere nagybetű és az adatbázisban meg lehet, hogy kicsi, emiatt előfordul, hogy nem töröl semmit.
    public static boolean deleteGame(String name) {
        Session sess = createSession();
        Transaction t = sess.beginTransaction();
        try {
            String hql = "delete from Game where name = :name";
            Query query = sess.createQuery(hql);
            query.setString("name", name);
            int i = query.executeUpdate();
            System.out.println("asd asd asd "+i);
            t.commit();
            return i == 1;
        }
        catch (HibernateException ex) {
            //t.rollback();
            return false;
        }
    }
    */
    
    public static boolean deleteGame(Game g) {
        return delObject(g);
    }
    
    //felhasználó bejelentkeztetése
    //ha nem létezik a felhasználó, létrehozza
    //hibakódot ad vissza:
    // 0: nincs hiba, sikeres bejelentkezés
    // 1: hibásan formázott felhasználónév
    // 2: hibásan formázott jelszó
    // 3: hibásan formázott felhasználónév és jelszó
    // 4: hibás jelszó
    // 5: SQL hiba
    public static int signIn(String name, String password, boolean secured) {
        boolean validName = InputValidator.isUserIdValid(name);
        boolean validPass = InputValidator.isPasswordValid(password);
        if (!validName && !validPass && !secured) return 3;
        if (!validName) return 1;
        if (!validPass && !secured) return 2;
        if (isUserExists(name)) {
            if (!isPasswordEquals(name, password, secured)) return 4;
        }
        else {
            if (!signUp(name, getRealPassword(password, secured))) return 5;
        }
        return 0;
    }
    
    public static boolean isUserExists(String id) {
        return getUser(id) != null;
    }
    
    public static String getUserId(String id) {
        return getUser(id).getId();
    }
    
    public static User getUser(String id) {
        if (id == null) return null;
        List<User> users = getUserList();
        for (User u : users) {
            if (u.getId().toUpperCase().equals(id.toUpperCase()))
                return u;
        }
        return null;
    }
    
    private static List<User> getUserList() {
        Session sess = createSession();
        Query query = sess.createQuery("from User user");
        return query.list();
    }
    
    //a kliens program naplózója kérdezheti ezt
    public static boolean isPasswordEquals(String name, String password) {
        return isPasswordEquals(name, password, false);
    }
    
    //egyezik-e a jelszó az adatbázisban lévővel
    private static boolean isPasswordEquals(String name, String password, boolean secured) {
        String realPass = getRealPassword(password, secured);
        User user = getUser(name);
        if (user == null) return false;
        return user.getPassword().equals(realPass);
    }
    
    private static String getRealPassword(String password, boolean secured) {
        return secured ? password : encrypt(password);
    }
    
    //felhasználó létrehozása
    //hamissal tér vissza, ha nem sikerül valamiért létrehozni
    private static boolean signUp(String name, String password) {
        User u = new User();
        u.setId(name);
        u.setPassword(password);
        return addObject(u);
    }

    //ez nem az igazi, mert pontos egyezést keres, nekem meg kis-nagybetűkre nem érzékeny keresés kell
    //de még máskor jól jöhet, ezért bent hagytam
    /*
    private static User getUser(String id) {
        return (User)getObject(User.class, id);
    }

    private static Object getObject(Class table, String key) {
        Session sess = createSession();
        Transaction t = sess.beginTransaction();
        Object o = null;
        try {
            o = sess.get(table, key);
            t.commit();
        }
        catch(HibernateException ex) {}
        sess.close();
        return o;
    }
    */
    
    private static boolean addObject(Object object) {
        return addOrDeleteObject(object, true);
    }
    
    private static boolean delObject(Object object) {
        return addOrDeleteObject(object, false);
    }
    
    //adatbázishoz ad hozzá egy objektumot
    private static boolean addOrDeleteObject(Object object, boolean add) {
        Session sess = createSession();
        Transaction t = sess.beginTransaction();
        if (add) sess.save(object);
        else sess.delete(object);
        try {
            t.commit();
        }
        catch (HibernateException ex) {
            t.rollback();
            return false;
        }
        sess.close();
        return true;
    }
    
    //hogy ne kelljen több helyen mindig sessiont létrehozni
    private static Session createSession() {
        SessionFactory factory = HibernateUtil.getSessionFactory();
        return factory.openSession();
    }
    
    //SHA-1 titkosító
    public static String encrypt(String str) {
        if (str == null) return null;
        return DigestUtils.sha256Hex(str);
    }
    
}