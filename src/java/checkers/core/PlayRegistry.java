//a játszmákat tárolja és szinkronizál az adatbázissal

package checkers.core;

import checkers.event.CommonEvent;
import java.util.List;
import java.util.ArrayList;
import checkers.Play;
import checkers.Storage;
import checkers.database.Executor;
import checkers.database.table.Game;

public class PlayRegistry {
    
    private static List<Play> playes = new ArrayList<Play>();
    
    public static boolean isUserNotOwner(Storage storage) {
        for (Play p : playes) {
            if (p.getOwner().equals(storage.getUsername()))
                return false;
        }
        return true;
    }
    
    public static int addPlay(Storage storage, String name) {
        return addPlay(storage, name, null);
    }
    
    //hibakódok:
    // 0: nincs hiba
    // 1: nincs bejelentkezett felhasználó
    // 2: már hozott létre játszmát
    // 3: létező játék
    // 4: sql hiba miatt nem hozható létre:
    //    Okai:
    //      - egyedi owner kulcs (már hozott létre a felhasználó játékot)
    //      - adatbázis nem válaszol
    //    Felhasználónak szánt üzenet ez esetre: Nem hozható létre a játszma
    public static int addPlay(Storage storage, String name, String password) {
        if (!storage.getIsUserSet())
            return 1;
        if (!isUserNotOwner(storage))
            return 2;
        if (isPlayExists(name))
            return 3;
        Game g = new Game();
        g.setName(name);
        g.setPassword(Executor.encrypt(password));
        g.setOwner(Executor.getUser(storage.getUsername()));
        Play p = new Play(g);
        if (Executor.createGame(g)) {
            playes.add(p);
            new CommonEvent().fire();
            return 0;
        }
        return 4;
    }
    
    //addPlay metódusnak megállapítja, van-e már ilyen nevű játék
    private static boolean isPlayExists(String name) {
        syncPlayList();
        //normál esetben ez előtt már lefut a getPlayList, ami már lefuttatta,
        //de a biztonság kedvéért kitettem ide is
        for (Play p : playes)
            if (equals(name, p.getName()))
                return true;
        return false;
    }
    
    //törli a kért nevű játékot
    //ennek sikerességét adja vissza
    //hamis, ha adatbázisból nem törölhető vagy nem jogosult a kérő
    public static boolean removePlay(Storage storage, String name) {
        for (Play p : playes) {
            if (name.equals(p.getName())) {
                if (p.isOwner(storage) || UserActions.isAdmin(storage)) {
                    if (Executor.deleteGame(p.getGame())) {
                        Play temp = p;
                        playes.remove(p);
                        new CommonEvent().fire();
                        temp.firePlayers();
                        return true;
                    }
                }
                else {
                    return false;
                }
                break;
            }
        }
        return false;
    }
    
    //a létező játékok listája
    public static List<Play> getPlayList() {
        syncPlayList();
        return playes;
    }
    
    //szinkronizálja az adatbázist és a nyilvántartó osztályt
    private static void syncPlayList() {
        List<Game> games = Executor.getGameList();
        delPlayList(games);
        fillPlayList(games);
    }
    
    //az osztályváltozó játéklistában nem szereplő játékok
    //- amik az adatbázisban viszont léteznek - hozzáadása a játéklistához
    private static void fillPlayList(List<Game> games) {
        boolean exists;
        for (Game g : games) {
            exists = false;
            for (Play p : playes) {
                if (equals(g.getName(), p.getName())) {
                    exists = true;
                }
            }
            if (!exists) {
                playes.add(new Play(g));
            }
        }
    }
     
    //törli azokat a példányokat, amik nem léteznek az adatbázisban
    private static void delPlayList(List<Game> games) {
        boolean exists;
        for (Play p : playes) {
            exists = false;
            for (Game g : games) {
                if (equals(g.getName(), p.getName())) {
                    exists = true;
                }
            }
            if (!exists) {
                playes.remove(p);
                //foreach cikluson belül, amit iterálunk, abból törlünk.
                //magam alatt vágom a fát, ezért... rekurzió, hogy kezdje elölről
                delPlayList(games);
                //most, hogy visszatértünk a rekurzióból, ezt a ciklust értelmetlen folytatni
                break;
            }
        }
    }
    
    private static boolean equals(String val1, String val2) {
        return val1.toUpperCase().equals(val2.toUpperCase());
    }
    
}