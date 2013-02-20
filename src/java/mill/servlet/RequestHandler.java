//Ez az osztály tartalmazza az alap szervlet metódusokat
//Redirect (get esetén index.jspx), Storage elkérés, request elkérés, response elkérés

package mill.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import mill.Storage;

public abstract class RequestHandler extends HttpServlet {

    private Storage storage;
    private HttpServletRequest request;
    private HttpServletResponse response;  
    
    protected Storage getStorage() {
        return storage;
    }
    
    protected HttpServletRequest getRequest() {
        return request;
    }
    
    protected HttpServletResponse getResponse() {
        return response;
    }
    
    protected boolean isPosted(String str) {
        return request.getParameter(str) != null;
    }
    
    protected void redirect(String url) {
        redirect(response, url);
    }
    
    protected void redirectToIndex() {
        redirectToIndex(response);
    }
    
    private void redirectToIndex(HttpServletResponse response) {
        redirect(response, "index.jspx");
    }
    
    private void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        }
        catch(IOException ex) {}
    }
    
    //request, response, storage beállítása illetve utf-8 kódolás beállítása
    private void initVariables(HttpServletRequest request, HttpServletResponse response) {
        this.request = request; //fontos, hogy első legyen, mert a többi használja
        fixUTF8();
        this.response = response;
        setStorage();
    }
    
    //utf-8 kódolás beállítása, hogy jó legyen a post-ban kapott adatok kiolvasása
    private void fixUTF8() {
        try {
            request.setCharacterEncoding("utf-8");
        }
        catch(UnsupportedEncodingException ex) {}
    }
    
    //ha még nincs storage, létrehozza, egyébként elkéri
    private void setStorage() {
        try { //a biztonság kedvéért
            storage = (Storage)getSession().getAttribute("storage");
        }
        catch(Exception ex) {}
        finally {
            if (storage == null) { //üres süti esetén
                Storage s = new Storage();
                getSession().setAttribute("storage", s);
                storage = s;
            }
        }
    }
    
    private HttpSession getSession() {
        return request.getSession(true);
    }
    
    //get esetén vissza a kezdőoldalra...
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectToIndex(response);
    }

    //post esetén példányváltozók beállítása és feldolgozás
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        initVariables(request, response);
        process();
    }
    
    //minden szervletnek egyedi infója van
    @Override
    public abstract String getServletInfo();
    
    //minden szervlet mást-mást csinál
    protected abstract void process();
    
}