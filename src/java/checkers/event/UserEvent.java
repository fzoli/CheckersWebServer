//egyetlen egy munkamenetnek szóló eseményjelzés

package checkers.event;

public class UserEvent implements Eventable {

    private boolean eventValue = true;
    
    @Override
    public boolean getEventValue() {
        return eventValue;
    }
    
    @Override
    public boolean isFired(boolean eventValue) {
        return this.eventValue != eventValue;
    }
    
    @Override
    public void fire() {
        eventValue = !eventValue;
    }
    
}