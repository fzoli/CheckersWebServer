//eseménykezeléshez készített szervlet

package mill.servlet;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.Timer;
import mill.Storage;
import mill.event.CommonEvent;
import mill.event.Eventable;

public class EventRequestHandler extends AjaxRequestHandler {
    
    @Override
    public String getServletInfo() {
        return "Event request processor.";
    }

    @Override
    protected void process(HttpServletRequest request, HttpServletResponse response) {
        Storage storage = getStorage(request);
        setContentTypeToXml(response);
        PrintWriter out = getWriter(response);
        out.print(getEmptyXmlResponse());
        if (isPosted(request, "action")) {
            if (isAction(request, "waitListEvent")) {
                waitEvent(new CommonEvent());
            }
            else if (isAction(request, "waitGameEvent")) {
                waitEvent(storage.getGameEvent());
            }
            else if (isAction(request, "ImHere")) {
                storage.updateLastConnectSign();
                waitEvent(storage.getDisconnectEvent());
            }
        }
        out.close();
    }
    
    //vár, amíg bekövetkezik az esemény, de ha ez tovább tart, mint 1 perc, akkor
    //kiváltja az eseményt, nehogy memory-leak keletkezzen
    private void waitEvent(Eventable event) {
        Timer t = new Timer(getTimeout(), new TimeoutListener(event));
        t.start();
        boolean value = event.getEventValue();
        while (true) {
            try {
                if (event.isFired(value)) {
                    t.stop();
                    break;
                }
                Thread.sleep(1); //processzor nyúzás kivédése
            }
            catch(Exception ex) {}
        }
    }
    
}