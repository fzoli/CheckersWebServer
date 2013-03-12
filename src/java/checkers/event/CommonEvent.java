//mindenkinek szóló eseményjelzés

package checkers.event;

public class CommonEvent implements Eventable {
    
    private static boolean eventValue;
    private static Object initTest;

    public CommonEvent() {
        if (initTest == null) { //ha még nem lett inicializálva
            initTest = new Object();
            eventValue = true;
        }
    }
    
    @Override
    public boolean getEventValue() {
        return eventValue;
    }
    
    @Override
    public boolean isFired(boolean eventValue) {
        return CommonEvent.eventValue != eventValue;
    }
    
    @Override
    public void fire() {
        eventValue = !eventValue;
    }

}