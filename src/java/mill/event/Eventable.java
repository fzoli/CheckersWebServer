//Egy tároló lényegében, amiből meg lehet állapítani azt, hogy van-e esemény

package mill.event;

public interface Eventable {
    
    public boolean getEventValue();
    
    public boolean isFired(boolean eventValue);
    
    public void fire();
    
}