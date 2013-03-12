//az összes munkamenetet tárolja

package checkers.core;

import java.util.ArrayList;
import java.util.List;
import checkers.Storage;

public class StorageRegistry {
    
    private static List<Storage> storages = new ArrayList<Storage>();
    
    public static void addStorage(Storage storage) {
        storages.add(storage);
    }
    
    public static void removeStorage(Storage storage) {
        storages.remove(storage);
    }

    //a megadott nevű felhasználó melyik játékhoz van csatlakozva
    public static Storage getUserStorage(String username) {
        if (username == null) return null;
        for (Storage s : storages) {
            if (s.getIsUserSet()) {
                if (s.getUsername().toUpperCase().equals(username.toUpperCase()))
                    return s;
            }
        }
        return null;
    }
    
}