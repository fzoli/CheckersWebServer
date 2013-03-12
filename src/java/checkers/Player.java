package checkers;

//a bíró játékos nyílvántartásához
public class Player {
    
    private boolean accepted, connected, hit;
    private String name;

    public Player() {
        accepted = false;
        connected = false;
        hit = false;
        name = null;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isAccepted() {
        return accepted;
    }

    public boolean getHit() {
        return hit;
    }
    
    public void setHit(boolean hit) {
        this.hit = hit;
    }
    
    public void accept() {
        accepted = true;
    }

    public boolean isConnected() {
        return connected;
    }
    
    public void connect() {
        connected = true;
    }
    
    public void disconnect() {
        connected = false;
    }
    
}