//eseménykezeléshez készített szervlet

package mill.servlet;

import java.io.PrintWriter;
import javax.swing.Timer;
import mill.Storage;
import mill.event.CommonEvent;
import mill.event.Eventable;

public class EventRequestHandler extends AjaxRequestHandler {

    private Storage storage;
    
    @Override
    public String getServletInfo() {
        return "Event request processor.";
    }

    @Override
    protected void process() {
        storage = getStorage();
        setContentTypeToXml();
        PrintWriter out = getWriter();
        out.print(getEmptyXmlResponse());
        if (isPosted("action")) {
            if (isAction("waitListEvent")) {
                waitEvent(new CommonEvent());
            }
            else if (isAction("waitGameEvent")) {
                waitEvent(storage.getGameEvent());
            }
            else if (isAction("ImHere")) {
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