package mill.servlet;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mill.Checker;
import mill.Play;
import mill.PlayJudge;
import mill.Storage;
import mill.core.InputValidator;
import mill.core.PlayRegistry;
import mill.core.UserActions;
import org.dom4j.dom.DOMDocument;
import org.w3c.dom.Element;

public class XmlRequestHandler extends AjaxRequestHandler {

    private Storage storage;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private boolean playCreateSuccess;
    private int timeout = getTimeout();
    
    @Override
    public String getServletInfo() {
        return "XML request processor.";
    }
    
    @Override
    protected void process() {
        storage = getStorage();
        request = getRequest();
        response = getResponse();
        setContentTypeToXml();
        PrintWriter out = getWriter();
        if (out == null) {
            return;
        }
        if (isAction("test")) {
            out.print(createTestXml());
        }
        else if (isAction("getPlayList")) {
            out.print(getXmlPlayList());
        }
        else if (isAction("createGame")) {
            out.print(getCreatePlayXml());
        }
        else if (isAction("getPlayData")) {
            out.print(getPlayDataXml());
        }
        else if (isAction("validateUser")) {
            out.print(getUserValidateXml());
        }
        else {
            if (isAction("exitGame")) {
                leaveGame();
            }
            else if (isAction("start_stop")) {
                playStartStop();
            }
            else if (isAction("give_up")) {
                playGiveUp();
            }
            else if (isAction("move")) {
                moveChecker();
            }
            out.print(getEmptyXmlResponse());
        }
        out.close();
    }

    private void leaveGame() {
        UserActions.leavePlay(storage);
    }
    
    private void playGiveUp() {
        UserActions.getPlay(storage).giveUp(storage);
    }
    
    private void playStartStop() {
        Play p = UserActions.getPlay(storage);
        if (p != null) {
            p.startStop(storage);
        }
    }
    
    private void moveChecker() {
        int fromRow, fromCol, toRow, toCol;
        try {
            fromRow = Integer.parseInt(request.getParameter("fromRow"));
            fromCol = Integer.parseInt(request.getParameter("fromCol"));
            toRow = Integer.parseInt(request.getParameter("toRow"));
            toCol = Integer.parseInt(request.getParameter("toCol"));
            Play p = UserActions.getPlay(storage);
            p.move(storage, fromRow, fromCol, toRow, toCol);
        }
        catch(Exception ex) {}
    }

    private String boolAsString(boolean value) {
        return value ? "true" : "false";
    }
    
    private String createTestXml() {
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        root.setAttribute("message", "wellcome");
        return doc.asXML();
    }
    
    private String getXmlPlayList() {
        Storage storage = this.storage; //látszólag semmi értelme xD
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        String message = storage.getIsUserSet() ? storage.getMessage() : "Viszlát.";
        root.setAttribute("message", message);
        root.setAttribute("timeout", Integer.toString(timeout));
        root.setAttribute("user", storage.getUsername());
        root.setAttribute("lastAction", UserActions.getLastAction());
        root.setAttribute("isPlayerInGame", boolAsString(storage.getIsUserInGame()));
        root.setAttribute("isAdmin", boolAsString(UserActions.isAdmin(storage)));
        List<Play> playes = PlayRegistry.getPlayList();
        for (Play p : playes) {
            Element child = doc.createElement("play");
            child.setAttribute("name", p.getName());
            child.setAttribute("owner", p.getOwner());
            child.setAttribute("protected", boolAsString(p.isPasswordProtected()));
            child.setAttribute("state", p.getJudge().getGameState());
            child.setAttribute("playerNumber", Integer.toString(p.getPlayerNumber()));
            root.appendChild(child);
        }
        return doc.asXML();
    }
    
    private String getUserValidateXml() {
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        boolean userExists = false;
        boolean validPassword = false;
        if (user != null && password != null) {
            userExists = UserActions.isUserExists(user);
            validPassword = UserActions.isUserPasswordEquals(user, password);
        }
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        root.setAttribute("userExists", boolAsString(userExists));
        root.setAttribute("validPassword", boolAsString(validPassword));
        return doc.asXML();
    }
    
    private String createPlay() {
        String message = "Hibás kérés.";
        playCreateSuccess = false;
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        if (name != null) {
            if (InputValidator.isGameNameValid(name)) {
                if (password != null) {
                    if (InputValidator.isGamePasswordValid(password)) {
                        int code;
                        if (password.length() == 0) {
                            code = PlayRegistry.addPlay(storage, name);
                        }
                        else {
                            code = PlayRegistry.addPlay(storage, name, password);
                        }
                        switch (code) {
                            case 0:
                                message = "Sikeresen létrehozta a játszmát.";
                                playCreateSuccess = true;
                                break;
                            case 1:
                                message = "Ön nincs bejelentkezve.";
                                break;
                            case 2:
                                message = "Nem hozhat létre több játszmát.";
                                break;
                            case 3:
                                message = "Ilyen nevű játék már létezik.";
                                break;
                            case 4:
                                message = "Adatbázishiba.";
                                break;
                        }
                    }
                    else {
                        message = "Hibásan formázott jelszó.";
                    }
                }
            }
            else {
                message = "Hibásan formázott név.";
            }
        }
        return message;
    }
    
    private String getCreatePlayXml() {
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        root.setAttribute("message", createPlay());
        root.setAttribute("success", boolAsString(playCreateSuccess));
        return doc.asXML();
    }
    
    private String getPlayDataXml() {
        Storage storage = this.storage; //ez az egy apróság megoldja a bugot??? -.-
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        Element data = doc.createElement("data");
        root.appendChild(data);
        data.setAttribute("timeout", Integer.toString(storage.getDisconnectDelay()));
        boolean isInGame = storage.getIsUserInGame();
        data.setAttribute("isPlayerInGame", boolAsString(isInGame));
        try {
            Play p = UserActions.getPlay(storage);
            PlayJudge judge = p.getJudge();
            data.setAttribute("isAbleStartStop", boolAsString(judge.isAbleStartStop(storage)));
            data.setAttribute("isGameFinished", boolAsString(judge.isPlayFinished()));
            data.setAttribute("gamePending", judge.getPending());
            data.setAttribute("startTime", p.getStartTime());
            data.setAttribute("isGameStarted", boolAsString(judge.isPlayStarted()));
            data.setAttribute("isGameRunning", boolAsString(judge.isGameRunning()));
            data.setAttribute("message", judge.createMessage(storage));
            Element board = doc.createElement("checkerboard");
            root.appendChild(board);
            for (Checker c : p.getCheckerboard()) {
                Element checker = doc.createElement("checker");
                checker.setAttribute("row", Integer.toString(c.getRow()));
                checker.setAttribute("col", Integer.toString(c.getCol()));
                checker.setAttribute("player", Integer.toString(c.getPlayer()));
                checker.setAttribute("type", c.getType());
                board.appendChild(checker);
            }
        }
        catch(NullPointerException ex) {
            if (!isInGame) { //ha játékban van a felhasználó, akkor a lista elfoglalt volt a kéréskor ...
                data.setAttribute("isAbleStartStop", "false");
                data.setAttribute("isGameFinished", "true");
                data.setAttribute("gamePending", "-");
                data.setAttribute("message", "Játszmából kilépve.");
            }
            else {
                return getXmlPlayList(); // ... megpróbáljuk újra
            }
        }
        return doc.asXML();
    }
    
}