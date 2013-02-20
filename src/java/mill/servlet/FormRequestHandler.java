//form-ból küldött post feldolgozása

package mill.servlet;

import javax.servlet.http.HttpServletRequest;
import mill.Play;
import mill.Storage;
import mill.core.PlayRegistry;
import mill.core.UserActions;

public class FormRequestHandler extends RequestHandler {

    private Storage storage;
    private HttpServletRequest request;
    
    @Override
    public String getServletInfo() {
        return "Form request processor.";
    }

    @Override
    protected void process() {
        storage = getStorage();
        request = getRequest();
        if (isPosted("join_game")) {
            joinGame();
            return; //nem szükséges, de hangsúlyozásra jó
        }
        else {
            if (isPosted("sign_in")) {
                signIn();
            }
            else if (isPosted("sign_out")) {
                signOut();
            }
            else if (isPosted("remove_game")) {
                delPlay();
            }
            else if (isPosted("clear_game")) {
                clearPlay();
            }
            redirectToIndex();
        }
    }
    
    private String getGameName() {
        String game = request.getParameter("game_name");
        if (game == null) game = storage.getRequestedGameName();
        else storage.setRequestedGameName(game);
        return game;
    }
    
    private String getGamePass() {
        String game = request.getParameter("game_password");
        if (game == null) game = storage.getRequestedGamePassword();
        return game;
    }
    
    private void redirectToJoinPlay() {
        redirect("join_play.jspx");
    }
    
    private void redirectToGame() {
        redirect("game.jspx");
    }
    
    private void joinGame() {
        if (!storage.getIsUserSet()) return;
        Play p = UserActions.getPlay(getGameName());
        int code = -1;
        if (p != null) {
            if (p.isPasswordProtected() && !p.isOwner(storage)) {
                String password = getGamePass();
                if (password != null) {
                    code = p.addPlayer(storage, password);
                    storage.setRequestedGamePassword(password);
                }
                else {
                    storage.setRequestedGameName(p.getName());
                    redirectToJoinPlay();
                    return;
                }
            }
            else {
                code = p.addPlayer(storage);
            }
            switch (code) {
                case 0:
                    storage.setRequestedGameName(null); //hogy ha kilép a játszmából, ne léptesse vissza ugyan abba
                    break;
                case 1:
                    storage.setMessage("Már tagja egy játszmának.");
                    break;
                case 2:
                    storage.setMessage("A játszma megtelt.");
                    break;
                case 3:
                    storage.setMessage("Nem csatlakozhat, mert nem megfelelő jelszót adott meg.");
                    storage.setRequestedGamePassword(null);
                    break; //csak, hogy szép legyen a vége is xD
            }
        }
        if (code == 0) {
            redirectToGame();
        }
        else {
            redirectToIndex();
        }
    }
    
    private void signIn() {
        String id = request.getParameter("id");
        String password = request.getParameter("password");
        boolean secured = Boolean.parseBoolean(request.getParameter("secured"));
        if (id != null || password != null) {
            UserActions.signIn(storage, id, password, secured);
        }
    }
    
    private void signOut() {
        UserActions.signOut(storage);
    }
    
    private String getPlayName() {
        return request.getParameter("game_name");
    }
    
    private void clearPlay() {
        Play p = UserActions.getPlay(getPlayName());
        String error = "A kiválasztott játszmát nem indíthatja újra.";
        if (p != null) {
            if (!p.restart(storage)) {
                storage.setMessage(error);
            }
        }
        else {
            storage.setMessage(error);
        }
    }
    
    private void delPlay() {
        String name = getPlayName();
        String error = "A kiválasztott játszmát nem törölheti.";
        if (name != null) {
            if (!PlayRegistry.removePlay(storage, name)) {
                storage.setMessage(error);
            }
        }
        else {
            storage.setMessage(error);
        }
    }
    
}