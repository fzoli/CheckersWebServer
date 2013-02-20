//közös metódusok, amiket minden osztály használhat
//felhasználóhoz köthető műveletek

package mill.core;

import mill.event.CommonEvent;
import java.util.List;
import mill.Play;
import mill.Storage;
import mill.database.Executor;

public class UserActions {
    
    private static String lastAction;
    
    //ki az utolsó bejelentkező
    public static String getLastAction() {
        if (lastAction == null) return "";
        return lastAction;
    }
    
    public static boolean isUserExists(String user) {
        return Executor.isUserExists(user);
    }
    
    public static boolean isUserPasswordEquals(String user, String password) {
        return Executor.isPasswordEquals(user, password);
    }
    
    private static void setLastAction(String action) {
        lastAction = action;
        new CommonEvent().fire();
    }
    
    public static boolean isAdmin(Storage storage) {
        return Executor.isUserAdmin(storage.getUsername());
    }
    
    //bejelentkezés / regisztráció
    public static void signIn(Storage storage, String id, String password, boolean secured) {
        storage.setRequestedId(id);
        int code = Executor.signIn(id, password, secured);
        switch (code) {
            case 0:
                storage.setMessage(null);
                String uid = Executor.getUserId(id);
                String again = signOutOldLogins(id) ? " (újra)" : "";
                setLastAction(uid + again);
                storage.setUsername(uid);
                break;
            case 1:
                storage.setMessage("Hibásan formázott felhasználónév.");
                break;
            case 2:
                storage.setMessage("Hibásan formázott jelszó.");
                break;
            case 3:
                storage.setMessage("Hibásan formázott mindkét adat.");
                break;
            case 4:
                storage.setMessage("Nem megfelelő jelszó.");
                break;
            case 5:
                storage.setMessage("Adatbázis hiba. Kérem, később próbálja újra.");
        }
    }
    
    //kijelentkezteti a felhasználót a többi munkamenetből
    private static boolean signOutOldLogins(String id) {
        Storage oldStorage;
        boolean exists = false;
        while((oldStorage = StorageRegistry.getUserStorage(id)) != null) {
            UserActions.signOut(oldStorage);
            oldStorage.setMessage("A rendszer kijelentkeztette, mert máshol újra belépett.");
            exists = true;
        }
        return exists;
    }
    
    public static void signOut(Storage storage) {
        leavePlay(storage);
        storage.setUsername(null);
    }
    
    //kilépteti a felhasználót a játszmából, amiben van
    public static void leavePlay(Storage storage) {
        try { //ha nincs csatlakozva és mégis kilépést kér pl. DC eseménykezelő
            UserActions.getPlay(storage).removePlayer(storage);
        }
        catch(Exception ex) {}
    }
    
    //tagja-e egy játszmának a felhasználó (nem feltétlen kapcsolódott)
    public static boolean isUserInGame(Storage storage) {
        return getPlay(storage) != null;
    }
    
    //visszaadja, hogy melyik játék tagja a felhasználó
    //null, ha nem tag
    public static Play getPlay(Storage storage) {
        int index = findPlayIndex(storage);
        if (index == -1) return null;
        return PlayRegistry.getPlayList().get(index);
    }
    
    //név alapján játszma visszaadása
    public static Play getPlay(String name) {
        if (name == null) return null;
        int index = findPlayIndex(name);
        if (index == -1) return null;
        return PlayRegistry.getPlayList().get(index);
    }
    
    //megkeresi, hogy a munkamenetben lévő felhasználó melyik játszma tagja
    //a játéklistában lévő indexet adja vissza
    //ha -1 a válasz, akkor nem tagja egyik játszmának se
    private static int findPlayIndex(Storage storage) {
        List<Play> list = PlayRegistry.getPlayList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isPlayerInThisGame(storage)) {
                return i;
            }
        }
        return -1;
    }
    
    //megkeresi azt a játékot, aminek a neve a paraméterben megadott
    //és visszaadja a listában lévő indexét
    //-1, ha nincs benne
    private static int findPlayIndex(String name) {
        List<Play> list = PlayRegistry.getPlayList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
}