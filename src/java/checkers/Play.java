package checkers;

import checkers.event.CommonEvent;
import checkers.database.table.Game;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import checkers.core.UserActions;
import checkers.database.Executor;

public class Play {
    // { PÉLDÁNYVÁLTOZÓK
    private Game game;
    private List<Storage> users = new ArrayList<Storage>();
    //a játszma bírója, aki vezérli a játszmát és megmondja, ki, mit tehet
    private PlayJudge judge = new PlayJudge(this);
    private List<Checker> checkers = new ArrayList<Checker>();
    private Date startDate;
    // }

    // { ALAPADATOK
    public Play(Game game) {
        this.game = game;
        init();
    }
    
    //inicializálás, újrainicializálás
    public void init() {
        initCheckers();
        startDate = null; //kezdetben nincs elindítva -> nincs kezdődátum
        judge.init(); //bíró inicializálása / újrainicializálása
        users.clear();
    }
    
    private void initCheckers() {
        checkers.clear(); //töröljük a létező bábúkat (csak újrainicializáláskor fontos)
        initCheckers(1, 3, 1); //1-es játékos bábúinak létrehozása
        initCheckers(6, 8, 2); //2-es játékos bábúinak létrehozása
    }

    private void initCheckers(int from, int to, int player) {
        for (int i = from; i <= to; i++) {
            for (int j = 1; j <= 8; j++) {
                if (judge.isNotGap(i, j)) {
                    checkers.add(new Checker(player, i, j));
                }
            }
        }
    }
    
    private Checker findChecker(int row, int col) {
        for (Checker c : checkers) {
            if (c.getRow() == row && c.getCol() == col)
                return c;
        }
        return null;
    }
    
    public String getName() {
        return firstToUpper(game.getName());
    }
    
    public PlayJudge getJudge() {
        return judge;
    }
    
    public List<Checker> getCheckerboard() {
        return checkers;
    }
    
    //az első karakterét a stringnek nagybetűvé alakítja
    private String firstToUpper(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
    
    public Game getGame() {
        return game;
    }
    
    private String getPassword() {
        return game.getPassword();
    }
    
    public boolean isPasswordProtected() {
        return getPassword() != null;
    }
    
    public String getOwner() {
        return game.getOwner().getId();
    }
    // }
    
    // { CSATLAKOZÁS, KILÉPÉS RÉSZLEG
    
    public int addPlayer(Storage storage) {
        return addPlayer(storage, null);
    }
    
    //csatlakozás játékhoz
    //hibakód:
    // 0: nincs hiba
    // 1: már csatlakozva van a felhasználó egy játszmához
    // 2: megtelt (2 játékos van benne)
    // 3: hibás a jelszó
    public int addPlayer(Storage storage, String password) {
        if (storage.getIsUserInGame()) {
            return 1;
        }
        if (getPlayerNumber() == 2) {
            return 2;
        }
        if (isPasswordProtected() && !isPasswordEquals(password) && !isOwner(storage)) {
            return 3;
        }
        listAdd(storage);
        judge.playerConnected(storage);
        new CommonEvent().fire();
        firePlayers();
        storage.startListenDisconnect();
        return 0;
    }    
    
    private void reinitIfFinishedAndEmpty() {
        if (getPlayerNumber() == 0 && judge.isPlayFinished())
            init();
    }
    
    private int getLastIndex(Storage s) {
        int index = judge.getLastIndex(s);
        if (index != -1) index--;
        return index;
    }
    
    private void listAdd(Storage s) {
        reinitIfFinishedAndEmpty();
        int lastIndex = getLastIndex(s);
        boolean reinited = reinitIfNoOneLeft(lastIndex);
        int empty = (reinited || lastIndex == -1) ? getEmptyIndex() : lastIndex;
        if (empty != -1) users.set(empty, s);
        else users.add(s);
    }
    
    private boolean isPasswordEquals(String password) {
        if (password == null) return false;
        return getPassword().equals(Executor.encrypt(password));
    }
    
    private int getEmptyIndex() {
        return users.indexOf(null);
    }
    
    public void removePlayer(Storage storage) {
        users.set(users.indexOf(storage), null); //listában null érték megadás, hogy ne csússzon el az index
        judge.playerDisconnected(storage); //bírónak szólunk, hogy kilépett a játékos
        firePlayers(); //és végül...
        firePlayer(storage);
        new CommonEvent().fire(); //... mindenkinek szólunk, hogy esemény történt, a játékosoknak is
    }
    
    public void removeChecker(Checker c) {
        checkers.remove(c);
    }
    
    //ha már senki nincs játékban, újrainicializálás
    private boolean reinitIfNoOneLeft(int lastIndex) {
        if (getPlayerNumber() == 0 && lastIndex == -1) {
            init();
            return true;
        }
        return false;
    }
    
    public void firePlayers() {
        firePlayers(users);
    }
    
    private void firePlayers(List<Storage> s) {
        for (Storage u : s) {
            if (u != null ) firePlayer(u);
        }
    }
    
    private void firePlayer(Storage storage) {
        try {
            storage.getGameEvent().fire();
        }
        catch(NullPointerException ex) {}
    }
    
    //lemásolja a felhasználók listáját
    //arra kell, hogy szólhassak mindkét játékosnak miután kiürítttem az eredeti listát
    private List<Storage> getUsersCopy() {
        List<Storage> temp = new ArrayList<Storage>();
        temp.addAll(users);
        return temp;
    }
    
    //kilépteti a felhasználókat a játszmából
    //ezt csak a létrehozó teheti meg
    public boolean restart(Storage storage) {
        if (isOwner(storage) || UserActions.isAdmin(storage)) {
            List<Storage> temp = getUsersCopy();
            users.clear();
            //new CommonEvent().fire(); //nem kell, mert a bíró elvégzi, HA a státusz megváltozott
            firePlayers(temp);
            init();
            return true;
        }
        return false;
    }
    
    //akkor tulajdonosa a játszmának a kérő, ha a tulajdonos neve és a kérő neve egyezik
    public boolean isOwner(Storage storage) {
        return getOwner().equals(storage.getUsername());
    }
    
    //csatlakozva van-e a sessionben lévő felhasználó a játékhoz
    //ezt használjuk arra, hogy kiderítsük, melyik játszmába van csatlakozva az user
    public boolean isPlayerInThisGame(Storage storage) {
        String username = storage.getUsername();
        if (username == null) return false;
        return users.contains(storage);
    }
    
    // }
    
    public int getPlayerIndex(Storage storage) {
        return users.indexOf(storage) + 1;
    }
    
    public int getPlayerNumber() {
        int count = 0;
        for (Storage s : users) {
            if (s != null) count++;
        }
        return count;
    }
    
    //mozgatás: kicsoda, honnan, hova
    public void move(Storage storage, int fromRow, int fromCol, int toRow, int toCol) {
        Checker c = findChecker(fromRow, fromCol);
        int p = getPlayerIndex(storage);
        if (c == null || p == -1) return; //ha nincs bábú a kért helyen vagy nincs belépve a felhasználó
        if (judge.tryMove(getPlayerIndex(storage), c, toRow, toCol)) { //ha mozoghat a bíró szerint:
            judge.moving(c, toRow, toCol); //mozgás előtt a bíró intézze el a dolgait
            c.setPosition(toRow, toCol); //mozgás!
            judge.moved(c); //mozgás után is inzézkedjen a bíró, ha akar
            firePlayers(); //sikerült a lépés, pozíciók frissítése minden játékosnál
        }
        else {
            firePlayer(storage); //nem sikerült a lépés, pozíció frissítése a próbálni lépő játékosnál
        }
    }
    
    public void updateStartDate() {
        startDate = new Date();
    }
    
    public String getStartTime() {
        if (startDate == null) return "null";
        return Long.toString(startDate.getTime());
    }
    
    public void startStop(Storage storage) {
        boolean ok = judge.startStop(storage);
        if (ok) firePlayers();
    }
    
    public void giveUp(Storage storage) {
        judge.giveUp(storage);
        firePlayers();
    }
    
}