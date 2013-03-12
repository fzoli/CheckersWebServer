//arra kell, hogy a szerver detektálhassa, hogy megszakadt a kapcsolat a klienssel

package checkers.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import checkers.Storage;

public class DisconnectListener implements ActionListener {

    private Storage storage;
    private Date lastSign;
    
    public DisconnectListener(Storage storage) {
        this.storage = storage;
    }

    public void updateLastSign() {
        lastSign = new Date();
    }
    
    private boolean isUserInConnect() {
        //5 másodperc haladék csak a biztonság kedvéért... (de igazából nem kellene)
        return new Date().getTime() - lastSign.getTime() < storage.getDisconnectDelay() + 5000;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isUserInConnect()) {
            storage.userDisconnected();
        }
        storage.getDisconnectEvent().fire();
    }
    
}