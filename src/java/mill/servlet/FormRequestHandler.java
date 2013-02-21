//form-ból küldött post feldolgozása

package mill.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mill.Play;
import mill.Storage;
import mill.core.PlayRegistry;
import mill.core.UserActions;

public class FormRequestHandler extends RequestHandler {
    
    @Override
    public String getServletInfo() {
        return "Form request processor.";
    }

    @Override
    protected void process(HttpServletRequest request, HttpServletResponse response) {
        Storage storage = getStorage(request);
        if (isPosted(request, "join_game")) {
            joinGame(request, response, storage);
            return; //nem szükséges, de hangsúlyozásra jó
        }
        else {
            if (isPosted(request, "sign_in")) {
                signIn(request, storage);
            }
            else if (isPosted(request, "sign_out")) {
                signOut(storage);
            }
            else if (isPosted(request, "remove_game")) {
                delPlay(request, storage);
            }
            else if (isPosted(request, "clear_game")) {
                clearPlay(request, storage);
            }
            redirectToIndex(response);
        }
    }
    
    private String getGameName(HttpServletRequest request, Storage storage) {
        String game = request.getParameter("game_name");
        if (game == null) game = storage.getRequestedGameName();
        else storage.setRequestedGameName(game);
        return game;
    }
    
    private String getGamePass(HttpServletRequest request, Storage storage) {
        String game = request.getParameter("game_password");
        if (game == null) game = storage.getRequestedGamePassword();
        return game;
    }
    
    private void redirectToJoinPlay(HttpServletResponse response) {
        redirect(response, "join_play.jspx");
    }
    
    private void redirectToGame(HttpServletResponse response) {
        redirect(response, "game.jspx");
    }
    
    private void joinGame(HttpServletRequest request, HttpServletResponse response, Storage storage) {
        if (!storage.getIsUserSet()) return;
        Play p = UserActions.getPlay(getGameName(request, storage));
        int code = -1;
        if (p != null) {
            if (p.isPasswordProtected() && !p.isOwner(storage)) {
                String password = getGamePass(request, storage);
                if (password != null) {
                    code = p.addPlayer(storage, password);
                    storage.setRequestedGamePassword(password);
                }
                else {
                    storage.setRequestedGameName(p.getName());
                    redirectToJoinPlay(response);
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
            redirectToGame(response);
        }
        else {
            redirectToIndex(response);
        }
    }
    
    private void signIn(HttpServletRequest request, Storage storage) {
        String id = request.getParameter("id");
        String password = request.getParameter("password");
        boolean secured = Boolean.parseBoolean(request.getParameter("secured"));
        if (id != null || password != null) {
            UserActions.signIn(storage, id, password, secured);
        }
    }
    
    private void signOut(Storage storage) {
        UserActions.signOut(storage);
    }
    
    private String getPlayName(HttpServletRequest request) {
        return request.getParameter("game_name");
    }
    
    private void clearPlay(HttpServletRequest request, Storage storage) {
        Play p = UserActions.getPlay(getPlayName(request));
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
    
    private void delPlay(HttpServletRequest request, Storage storage) {
        String name = getPlayName(request);
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