//ő a játszma bírója. tőle lehet megkérdezni hogy ki, mit tehet illetve, a játszma státuszát

package mill;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import mill.event.CommonEvent;

public class PlayJudge {

    private Play play;
    private Player[] players = new Player[3]; //használt indexek: 1 és 2!
    private int movedPlayer, gaveUpPlayer, startStopPlayer;
    private String[] pendingString = {"Indít", "Szünetel", "Folytat"};
    boolean pending, isPlayFinished;
    String lastState;
    
    public PlayJudge(Play play) {
        this.play = play;
        //FIGYELEM! ide nem kell init, mert a Play hívja meg a saját maga inicializálásakor!
    }
    
    //bíró inicializálása, újrainicializálása
    public void init() {
        createPlayers();
        this.gaveUpPlayer = 0; //nincs feladó
        this.startStopPlayer = 0; //nincs szüneteltető
        this.movedPlayer = 2; //kezdetben 2, hogy az első játékos léphessen
        this.pending = false;
        this.isPlayFinished = false;
        this.lastState = getGameState();
    }
    
    //létrehozza a játékosok tulajdonságait tároló objektumokat
    private void createPlayers() {
        for (int i = 1; i <= 2; i++) {
            players[i] = new Player();
        }
    }
    
    //létezhet-e bábú az adott koordinátában
    public boolean isNotGap(int row, int col) {
        return !isGap(row, col);
    }
    
    private boolean isGap(int row, int col) {
        if (row < 1 || row > 8 || col < 1 || col > 8) return true;
        return !((row % 2 != 0 && col % 2 == 0) || (row % 2 == 0 && col % 2 != 0));
    }
    
    //a játszma állapota, ami a listában jelenik meg
    public String getGameState() {
        int number = play.getPlayerNumber();
        if (gaveUpPlayer != 0) return "feladva";
        if (isPlayFinished()) return "befejezve";
        if (number == 0) return "üres";
        if (number == 1) return "várakozik";
        if (isPlayStarted()) {
            if (startStopPlayer == 0) return "folyamatban";
            else return "szüneteltetve";
        }
        else {
            return "kezdés előtt";
        }
    }
    
    public boolean isGameRunning() {
        return isPlayStarted() && !isPlayFinished() && startStopPlayer == 0 && gaveUpPlayer == 0 && play.getPlayerNumber() == 2;
    }
    
    //az aktuális állapot alapján üzenetet készít a kérő játékosnak
    public String createMessage(Storage s) {
        if (gaveUpPlayer != 0) { //ha nem 0, akkor valaki feladta
            return players[gaveUpPlayer].getName() + " feladta.";
        }
        if (startStopPlayer != 0) { //ha nem 0, akkor valaki szüneteltet
            return "Szünet.";
        }
        String message;
        int p = play.getPlayerIndex(s);
        String enemy = getAnotherPlayer(p).getName(); // a másik játékos (ellenfél) neve
        if (!isPlayStarted()) {
            if (!isPlayersConnected()) //ha nincs még mindkét játékos kapcsolódva
                message = "Várakozás a másik játékosra.";
            else {
                message = "Ön a ";
                message += p == 1 ? "piros" : "kék"; // 1-es játékos a piros, 2-es a kék
                message += " színnel van ";
                message += enemy;
                message += " ellen.";
            }
        }
        else {
            if (play.getPlayerNumber() != 2 && !isPlayFinished()) { //ha nincs mindkét játékos  és nincs még befejezve
                message = "Várakozás " + enemy + " csatlakozására.";
            }
            else {
                if (!isPlayFinished()) { //ha nincs még befejezve a játék
                    message = movedPlayer == p ? enemy : "Ön"; //ha az utoljára lépett user a kérő, akkor a másik játékos jön
                    message += " következik.";
                    if (isAbleHit(p) && movedPlayer != p) message += " Üssön!"; //ha az kérő játékos üthet
                }
                else {
                    String looser = isPlayerLost(1) ? players[1].getName() : players[2].getName(); //melyik játékos veszített
                    message = looser.equals(enemy) ? "Ön nyert!" : "Ön veszített!"; //a kérő veszített vagy nyert
                }
            }
        }
        return message;
    }
    
    private Player getAnotherPlayer(int player) {
        int index = player == 1 ? 2 : 1;
        return players[index];
    }
    
    private Player getAnotherPlayer(Player player) {
        return getAnotherPlayer(findPlayer(player.getName()));
    }
    
    private boolean isPlayersConnected() {
        for (int i = 1; i <= 2; i++) {
            if (!players[i].isConnected())
                return false;
        }
        return true;
    }
    
    //a játékos általánosan mozoghat-e. atombiztos :D
    private boolean isAbleMove(int player) {
        // ő következik, nincs befejezve a játszma, elkezdődött a játszma, nincs szüneteltetve a játszma, mindketten csatlakozva vannak
        return isNextPlayer(player) && !isPlayFinished() && isPlayStarted() && startStopPlayer == 0 && play.getPlayerNumber() == 2;
    }
    
    private boolean isNextPlayer(int player) {
        return movedPlayer != player;
    }
    
    //a bábúval léphet-e a felhasználó a kért pontra
    private boolean isAbleMove(Checker checker, int row, int col) {
        //benne van-e a megléphető pontokban a kért pozíció
        return getMoveablePoints(checker).contains(new Point(row, col));
    }
    
    //a figura objektum elkérése koordináta alapján
    private Checker getChecker(int row, int col) {
        List<Checker> board = play.getCheckerboard();
        Point position = new Point(row, col);
        for (Checker c : board) {
            if(c.getPosition().equals(position)) {
                return c;
            }
        }
        return null;
    }
    
    //szabad-e a cella (nincs bábú rajta)
    private boolean isCellFree(int row, int col) {
        return getChecker(row, col) == null;
    }
    
    private List<Point> getMoveablePoints(Checker checker) {
        //ha a bábúval lehet ütni, akkor az átlépési pontok kellenek (azért lista, mert több is lehetséges egyes esetekben)
        //különben az alap pozíciók listája kell
        List<Point> pos = isAbleHit(checker.getPlayer()) ? getHitPoints(checker) : getPossiblePoints(checker);
        //ebből el kell még távolítani a nem szabad pozíciókat
        removeNotFreePoints(pos);
        return pos; //ha ez üres, akkor nem léphet a bábú sehova
    }
    
    private void removeNotFreePoints(List<Point> pos) {
        for (Point p : pos) {
            if (!isCellFree(p.x, p.y)) {
                pos.remove(p); //nem szabad foreach ciklusban az éppen bejárt objektumból törölni, mert elcsúszik
                removeNotFreePoints(pos);
                break; //ezért rekurzióval pucolom ki és amikor visszatér, kilépek a ciklusból
            }
        }
    }
    
    //a kért bábú alapesetben megléphető pozícióit adja vissza
    private List<Point> getPossiblePoints(Checker checker) {
        List<Point> points = new ArrayList<Point>();
        Point p = checker.getPosition();
        int level = checker.getLevel();
        switch (level) {
            case 1: //ha a figura nem dáma
                int rowWay = getBasicRowWay(checker);
                for (int col = -1; col <= 1; col += 2) {
                    points.add(new Point(p.x + rowWay, p.y + col));
                }
                break;
            case 2: //ha a figura dáma
                for (int row = -1; row <= 1; row += 2) {
                    for (int col = -1; col <= 1; col += 2) {
                        points.add(new Point(p.x + row, p.y + col));
                    }
                }
                break;
        }
        removeGapPositions(points); //a nem megléphető pozíciókat eltávolítjuk
        //ennek még tartalmazni kell a nem szabad pozíciókat is, hogy lehessen detektálni, az ütés lehetőségét
        //ezért itt még véletlenül sem törlöm a nem szabad pozíciókat!
        return points;
    }
    
    private void removeGapPositions(List<Point> points) {
        for (Point p : points) {
            if (isGap(p.x, p.y)) {
                points.remove(p); //magam alatt vágom a fát megint xD
                removeGapPositions(points);
                break;
            }
        }
    }
    
    //üthet-e az adott játékos
    private boolean isAbleHit(int player) {
        List<Checker> checkers = play.getCheckerboard();
        for (Checker c : checkers) {
            if (c.getPlayer() == player) {
                List<Point> hit = getHitPoints(c);
                //ha nem üres a lista, akkor üthet
                if (!hit.isEmpty()) return true;
            }
        }
        return false;
    }
    
    //ütőpozíciók lekérése figuránként
    private List<Point> getHitPoints(Checker c) {
        List<Point> points = new ArrayList<Point>();
        List<Point> possible = getPossiblePoints(c);
        for (Point p : possible) { //az alapesetben megüthető pozíciók listáján végigmegyünk...
            Point hit = getHitPosition(c, p); //ha ebben az irányban lehet ütni, akkor listához adjuk a pozíciót
            if (hit != null) points.add(hit);
        }
        return points;
    }
    
    //adott bábú, adott irányban hova léphet úgy, hogy üt
    private Point getHitPosition(Checker c, Point to) {
        Checker another = getChecker(to.x, to.y); //a kért pozícióban lévő bábú
        //ha nem üres a pozíció és ellenséges a bábú
        if (another != null && another.getPlayer() != c.getPlayer()) {
            Point from = c.getPosition(); //honnan lépünk
            Point hitPoint = new Point(getHitNumber(from.x, to.x), getHitNumber(from.y, to.y)); //és hova léphetünk ütéssel
            if (isNotGap(hitPoint.x, hitPoint.y) && isCellFree(hitPoint.x, hitPoint.y)) return hitPoint; //ha szabad az a hely ahova léphetünk és lehetséges oda lépni, akkor meg is van a pont
        }
        return null; //nincs ilyen pont
    }
    
    //lényegében nem 1 egységet lép, hanem 2 egységet, ha ezt a koordinátár használja
    private int getHitNumber(int from, int to) {
        return from + (2 * (to - from));
    }
    
    //szintet kell-e lépnie a bábúnak (dámává válni)
    public boolean isLevelUp(Checker checker) {
        return checker.getRow() == getCheckerLevelUpRow(checker);
    }
    
    //hol léphet szintet a figura
    private int getCheckerLevelUpRow(Checker checker) {
        return getBasicRowWay(checker) == 1 ? 8 : 1; // vagy az első sorban, vagy a 8. sorban. figurától függ
    }
    
    //a figura irányát adja meg (1 mint lefelé, -1 mint felfelé a tábla soraiban)
    private int getBasicRowWay(Checker checker) {
        return checker.getPlayer() == 1 ? 1 : -1;
    }
    
    //lépés kérése előtti ellenőrzés
    //figura szinten léphet-e a játékos
    public boolean tryMove(int player, Checker checker, int row, int col) {
        //játékos szinten léphet-e ÉS tulajdonosa-e a bábúnak ÉS bábú léphet-e oda
        if (isAbleMove(player) && checker.getPlayer() == player && isAbleMove(checker, row, col)) {
            return true;
        }
        return false;
    }
    
    //mozgás előtt
    public void moving(Checker c, int row, int col) {
        List<Point> p = getHitPoints(c);
        if (!p.isEmpty()) { //ha a mozgatandó figurával figurát fognak ütni
            play.removeChecker(findProstrateChecker(c, row, col)); //kinyírandó figura megölése
            getPlayer(c).setHit(true); //ütés beállítása
        }
        else {
            getPlayer(c).setHit(false);
        }
    }
    
    //a figurához tartozó játékos objektum
    private Player getPlayer(Checker c) {
        return players[c.getPlayer()];
    }
    
    //adott bábúval adott helyre lépve (ütési pont) melyik bábút kell megsemmisíteni
    private Checker findProstrateChecker(Checker c, int row, int col) {
        Point from = c.getPosition(); //honnan,
        Point to = new Point(row, col); // hova lép
        int rowWay = (from.x - to.x > 0) ? -1 : 1; //a sor koordinátát növelni vagy csökkenteni kell eggyel
        int colWay = (from.y - to.y > 0) ? -1 : 1; // az oszloppal ugyan az a kérdés
        Point pos = new Point(from.x + rowWay, from.y + colWay); //ezen a koordinátán lévőt kell megőlni
        return getChecker(pos.x, pos.y); //és végül: koordináta alapján őt kell megőlni
    }
    
    //mozgás után
    public void moved(Checker c) {
        if (isLevelUp(c)) {
            c.levelUp(); //ha a mozgatott figurának szintet kell lépni, akkor szintlépés
        }
        int player = c.getPlayer();
        List<Point> p = getHitPoints(c);
        if (!(!p.isEmpty() && players[player].getHit())) { //ha nem jön láncütés
            movedPlayer = player; //a következő játékos jön
        }
        else { //láncütés:
            //nem kell tenni semmit, mert még mindig az adott játékos jön és még mindig kötelező neki ütni
        }
    }
    
    //a kérő felhasználó szüneteltetheti-e a játszmát
    public boolean isAbleStartStop(Storage player) {
        Player p = getPlayer(player);
        int i = play.getPlayerIndex(player);
        if (p.isAccepted() && !isPlayStarted()) return false; //ha elfogadta a játékot de még nem kezdődött el, akkor nem nyomhatja meg újra a gombot
        if (startStopPlayer != 0 && startStopPlayer != i && isPlayStarted()) return false; //ha valaki szünetelteti a játékot ÉS nem ő szünetelteti (és el is kezdődött a játék), akkor nem ő szünetelteti szóval: NEM
        if (isPlayFinished()) return false; //ha vége a játéknak, akkor se!
        if (startStopPlayer == 0 && !isGameRunning() && p.isAccepted()) return false; //ha nincs folyamatban  (pl. disconnectelt a másik játékos) a játék és már elkezdték
        return true; //minden más esetben nyugodtan megnyomhatja :D
    }
    
    //mi legyen a StartStop gomb felirata
    public String getPending() {
        return pendingString[getPendingCode()]; //az adott indexen lévő felirat lesz a gomb felirata
    }
    
    //0: Indít
    //1: Szünetel
    //2: Folytat
    private int getPendingCode() {
        if (!isPlayStarted()) return 0;
        return pending ? 2 : 1;
    }
    
    //akkor kezdődött el a játszma, ha minden játékos a kezdésre rákattintott
    public boolean isPlayStarted() {
        for (int i = 1; i <= 2; i++) {
            if (!players[i].isAccepted())
                return false;
        }
        return true;
    }
    
    private void manageStartDate(Player p) {
        Player ap = getAnotherPlayer(p);
        if (ap != null && ap.isAccepted()) play.updateStartDate();
    }
    
    //ha a kérő start-stoppolhat, akkor start-stop...
    public boolean startStop(Storage s) {
        if (!isAbleStartStop(s)) return false; //elküldi a fenébe a kérőt, bár mivel tiltott a gomb, csak crackereknek xD
        int i = play.getPlayerIndex(s);
        Player p = getPlayer(s);
        if (isPlayStarted()) { //ha elkezdődött a játék, akkor
            pending = !pending; //állapot billentés
            startStopPlayer = pending ? i : 0; // ki az aki start-stoppolt
        }
        else { //egyébként elfogadás történik, nem start-stop
            p.accept();
            manageStartDate(p);
        }
        //és végül! HA megváltozott a játszma jtátusza, akkor kiabálunk mindenkinek, aki a listát nézi, hogy cserélje le a feliratot
        if (!lastState.equals(getGameState()))
            new CommonEvent().fire();
        return true; //végrehajtva
    }
    
    //játszma feladása
    public void giveUp(Storage s) {
        isPlayFinished = true;
        gaveUpPlayer = play.getPlayerIndex(s);
    }
    
    //megkeresi azt, hogy a kért játékosnak mi volt az utolsó indexe, tehát 1-es vagy 2-es volt
    public int getLastIndex(Storage s) {
        return findPlayer(s.getUsername());
    }
    
    //név alapján keresés...
    private int findPlayer(String name) {
        for (int i = 1; i <= 2; i++) {
            if (players[i].getName() != null && players[i].getName().equals(name)) //ha egyezik a név
                return i;
        }
        return -1; //nem talált ilyen nevű játékost
    }
    
    private Player getPlayer(Storage s) {
        int index = play.getPlayerIndex(s);
        if (index == -1) return null;
        return players[index];
    }
    
    //akkor van befejezve a játszma, ha veszített valaki vagy fel lett adva
    public boolean isPlayFinished() {
        return isPlayFinished || isPlayerLost(1) || isPlayerLost(2);
    }

    private boolean isPlayerLost(int player) {
        boolean lost = true; //kezdetben tegyük fel, hogy veszített
        for (Checker c : play.getCheckerboard()) {
            if (c.getPlayer() == player) {
                List<Point> p = getMoveablePoints(c);
                if (!p.isEmpty()) { //ha van megléphető pozíciója a játékosnak valamelyik bábúval
                    lost = false; 
                    break; //nem vesztett (még xD)
                }
            }
        }
        return lost;
    }
    
    //kapcsolódás
    public void playerConnected(Storage storage) {
        Player p = getPlayer(storage);
        if (p != null) {
            p.setName(storage.getUsername());
            p.connect();
        }
    }
    
    //kilépés
    public void playerDisconnected(Storage storage) {
        Player p = getPlayer(storage);
        if (p != null) {
            p.disconnect();
        }
    }
    
}