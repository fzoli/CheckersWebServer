package checkers;

//Ez az osztály a JSPX oldalak babja, mint munkamenetkezelő

import checkers.core.PlayRegistry;
import checkers.core.InputValidator;
import checkers.core.StorageRegistry;
import checkers.core.UserActions;
import checkers.event.DisconnectListener;
import checkers.event.Eventable;
import checkers.event.UserEvent;
import javax.swing.Timer;
import checkers.database.Executor;

public class Storage {
    
    //kezdetben:
    // - nincs felhasználó belépve
    // - nincs üzenet (se játékhoz, se adminisztrációhoz)
    // - nincs id, amivel megpróbáltak belépni
    // - nincs játszma név és jelszó, amihez csatlakozni próbáltak
    private String reqId, reqGameName, reqGamePass, message, username;
    //eseménykezeléshez
    private Eventable gameEvent = new UserEvent();
    private Eventable dcEvent = new UserEvent();
    private DisconnectListener dcListener = new DisconnectListener(this);
    private Timer dcTimer = new Timer(15000, dcListener);

    //amikor létrejön, nyilvántartásba kerül
    public Storage() {
        StorageRegistry.addStorage(this);
    }

    //amikor megöli a GC, kikerül a nyilvántartásból
    @Override
    protected void finalize() throws Throwable {
        StorageRegistry.removeStorage(this);
        super.finalize();
    }
    
    //a belépett felhasználó neve
    //A PlayRegistry fogja felhasználni a játék létrehozásnál:
    //kiolvassa a munkamenetből a felhasználónevet
    //ezt az azonosítót fogja hozzáadni létrehozó felhasználónak
    public String getUsername() {
        syncUser();
        if (username == null) return "";
        return username;
    }
    
    private void syncUser() {
        if (!Executor.isUserExists(username)) username = null;
    }
    
    //beállít a munkamenethez egy felhasználónevet
    public void setUsername(String username) {
        this.username = username;
    }
    
    //az utolsó belépéskor megadott id-t adja vissza
    //arra kell, hogy ha esetleg rossz jelszót ad meg a user,
    //ne kelljen újra a nevét beírni
    public String getRequestedId() {
        return reqId;
    }

    //bejelentkezéskor állítódik be
    public void setRequestedId(String id) {
        reqId = id;
    }

    //az utolsó játszmához csatlakozáskor beírt játszmanév
    public String getRequestedGameName() {
        return reqGameName;
    }

    //játszmához csatlakozáskor állítódik be
    public void setRequestedGameName(String name) {
        reqGameName = name;
    }
    
    public String getRequestedGamePassword() {
        return reqGamePass;
    }
    
    //játszmához csatlakozáskor állítódik be
    //ImHere jelzésre csatlakoztatjuk az usert, de csak ha beléphet
    //ha jelszóvédett a játék, ahhoz kell ez az érték
    public void setRequestedGamePassword(String password) {
        reqGamePass = password;
    }
    
    //visszaadja az üzenetet, és megsemmisíti,
    //hogy a következő kérésnél már ne legyen üzenet
    public String getMessage() {
        String m = message;
        message = null;
        return m;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    //csak arra kell, hogy a kezdőlapról a beléptetőre irányítás feltételét
    //ne kelljen a kezdőlapon kiértékelnem
    public boolean getIsUserSet() {
        return !getUsername().equals("");
    }
    
    public boolean getIsRequestedGameNameNull() {
        return getRequestedGameName() == null;
    }
    
    //arra kell, hogy a beléptető oldalon tudjuk, hogy van-e error üzenet
    public boolean getIsMessageNull() {
        return message == null;
    }
    
    //a munkamenetben lévő felhasználó csatlakozva van-e egy játékhoz
    public boolean getIsUserInGame() {
        return UserActions.isUserInGame(this);
    }
    
    public boolean getIsUserNotOwner() {
        return PlayRegistry.isUserNotOwner(this);
    }
    
    public String getUserIdConstrain() {
        return InputValidator.getUserIdConstrain();
    }
    
    public String getPasswordConstrain() {
        return InputValidator.getPasswordConstrain();
    }

    public String getGameNameConstrain() {
        return InputValidator.getGameNameConstrain();
    }
    
    public String getGamePasswordConstrain() {
        return InputValidator.getGamePasswordConstrain();
    }
    
    public Eventable getGameEvent() {
        return gameEvent;
    }

    public Eventable getDisconnectEvent() {
        return dcEvent;
    }
    
    public void startListenDisconnect() {
        updateLastConnectSign();
        dcTimer.start();
    }
    
    private void stopListenDisconnect() {
        dcTimer.stop();
    }
    
    public void userDisconnected() {
        stopListenDisconnect();
        UserActions.leavePlay(this);
    }
    
    public void updateLastConnectSign() {
        dcListener.updateLastSign();
    }
    
    public int getDisconnectDelay() {
        return dcTimer.getDelay();
    }
    
}