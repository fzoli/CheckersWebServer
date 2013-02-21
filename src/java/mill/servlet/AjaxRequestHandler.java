//Ez a szervlet ajax kérésekre válaszoláshoz szükséges

package mill.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dom4j.dom.DOMDocument;
import org.w3c.dom.Element;

public abstract class AjaxRequestHandler extends RequestHandler {
    
    private int timeout = 60000; //1 perc
    
    protected int getTimeout() {
        return timeout;
    }
    
    protected boolean isAction(HttpServletRequest request, String str) {
        String action = request.getParameter("action");
        if (action == null) return false;
        return action.equals(str);
    }
    
    protected PrintWriter getWriter(HttpServletResponse response) {
        try {
            return response.getWriter();
        }
        catch(IOException ex) {
            return null;
        }
    }
    
    protected void setContentTypeToXml(HttpServletResponse response) {
        response.setContentType("text/xml; charset=utf-8");
    }
    
    protected String getEmptyXmlResponse() {
        DOMDocument doc = new DOMDocument();
        Element root = doc.createElement("response");
        doc.appendChild(root);
        return doc.asXML();
    }
    
}