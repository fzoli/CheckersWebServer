package checkers.servlet;

import java.io.PrintWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import checkers.Checker;
import checkers.Play;
import checkers.PlayJudge;
import checkers.Storage;
import checkers.core.InputValidator;
import checkers.core.PlayRegistry;
import checkers.core.UserActions;
import org.dom4j.dom.DOMDocument;
import org.w3c.dom.Element;

public class XmlRequestHandler extends AjaxRequestHandler {
    
    @Override
    public String getServletInfo() {
        return "XML request processor.";
    }
    
    @Override
    protected void process(HttpServletRequest request, HttpServletResponse response) {
        Storage storage = getStorage(request);
        setContentTypeToXml(response);
        PrintWriter out = getWriter(response);
        if (out == null) {
            return;
        }
        if (isAction(request, "test")) {
            out.print(createTestXml());
        }
        else if (isAction(request, "getPlayList")) {
            out.print(getXmlPlayList(storage));
        }
        else if (isAction(request, "createGame")) {
            out.print(getCreatePlayXml(request, storage));
        }
        else if (isAction(request, "getPlayData")) {
            out.print(getPlayDataXml(storage));
        }
        else if (isAction(request, "validateUser")) {
            out.print(getUserValidateXml(request));
        }
        else {
            if (isAction(request, "exitGame")) {
                leaveGame(storage);
            }
            else if (isAction(request, "start_stop")) {
                playStartStop(storage);
            }
            else if (isAction(request, "give_up")) {
                playGiveUp(storage);
            }
            else if (isAction(request, "move")) {
                moveChecker(request, storage);
            }
            out.print(getEmptyXmlResponse());
        }
        out.close();
    }

    private void leaveGame(Storage storage) {
        UserActions.leavePlay(storage);
    }
    
    private void playGiveUp(Storage storage) {
        UserActions.getPlay(storage).giveUp(storage);
    }
    
    private void playStartStop(Storage storage) {
        Play p = UserActions.getPlay(storage);
        if (p != null) {
            p.startStop(storage);
        }
    }
    
    private void moveChecker(HttpServletRequest request, Storage storage) {
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
    
    private String getXmlPlayList(Storage storage) {
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        String message = storage.getIsUserSet() ? storage.getMessage() : "Viszlát.";
        root.setAttribute("message", message);
        root.setAttribute("timeout", Integer.toString(getTimeout()));
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
    
    private String getUserValidateXml(HttpServletRequest request) {
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
    
    private static class PlayCreateData {
        
        public final String MESSAGE;
        public final boolean SUCCESS;

        public PlayCreateData(String message, boolean success) {
            MESSAGE = message;
            SUCCESS = success;
        }
        
    }
    
    private PlayCreateData createPlay(HttpServletRequest request, Storage storage) {
        String message = "Hibás kérés.";
        boolean playCreateSuccess = false;
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
        return new PlayCreateData(message, playCreateSuccess);
    }
    
    private String getCreatePlayXml(HttpServletRequest request, Storage storage) {
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        PlayCreateData data = createPlay(request, storage);
        root.setAttribute("message", data.MESSAGE);
        root.setAttribute("success", boolAsString(data.SUCCESS));
        return doc.asXML();
    }
    
    private String getPlayDataXml(Storage storage) {
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
                return getXmlPlayList(storage); // ... megpróbáljuk újra
            }
        }
        return doc.asXML();
    }
    
}