//ha időtúllépés, akkor esemény kiváltása akkor is, ha nem keletkezett esemény
//arra kell, hogy le lehessen állítani a szervert, mivel ha ez nem lenne, örökké futna a while(true) ciklus

package checkers.servlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import checkers.event.Eventable;

class TimeoutListener implements ActionListener {

        private Eventable event;
        
        public TimeoutListener(Eventable event) {
            this.event = event;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            event.fire();
        }
        
}