//Ez az osztály tartalmazza az alap szervlet metódusokat
//Redirect (get esetén index.jspx), Storage elkérés, request elkérés, response elkérés

package checkers.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import checkers.Storage;

public abstract class RequestHandler extends HttpServlet {

    //ha még nincs storage, létrehozza, egyébként elkéri
    protected Storage getStorage(HttpServletRequest request) {
        Storage storage = null;
        try { //a biztonság kedvéért
            storage = (Storage)request.getSession(true).getAttribute("storage");
        }
        catch(Exception ex) {}
        finally {
            if (storage == null) { //üres süti esetén
                Storage s = new Storage();
                request.getSession(true).setAttribute("storage", s);
                storage = s;
            }
        }
        return storage;
    }
    
    protected boolean isPosted(HttpServletRequest request, String str) {
        return request.getParameter(str) != null;
    }
    
    protected void redirectToIndex(HttpServletResponse response) {
        redirect(response, "index.jspx");
    }
    
    protected void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        }
        catch(IOException ex) {}
    }
    
    //utf-8 kódolás beállítása, hogy jó legyen a post-ban kapott adatok kiolvasása
    private void fixUTF8(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("utf-8");
        }
        catch(UnsupportedEncodingException ex) {}
    }
    
    //get esetén vissza a kezdőoldalra...
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectToIndex(response);
    }

    //post esetén példányváltozók beállítása és feldolgozás
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fixUTF8(request);
        process(request, response);
    }
    
    //minden szervletnek egyedi infója van
    @Override
    public abstract String getServletInfo();
    
    //minden szervlet mást-mást csinál
    protected abstract void process(HttpServletRequest request, HttpServletResponse response);
    
}