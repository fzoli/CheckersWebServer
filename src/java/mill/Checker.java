package mill;

import java.awt.Point;

public class Checker {

    private int player; //index: 0, 1
    private Point position; //x: row, y: col
    private int type; //type: basic, queen
    private final static String[] TYPES = {"basic", "queen"};
    //a game.js és game.css használja őket! (tehát nem szabad átírni)

    public Checker(int player, int row, int col) {
        this.player = player;
        position = new Point(row, col);
        type = 1;
    }
    
    //alap, vagy dáma
    public String getType() {
        return TYPES[type - 1];
    }
    
    public int getLevel() {
        return type;
    }
    
    public int getPlayer() {
        return player;
    }
    
    public int getRow() {
        return position.x;
    }

    public int getCol() {
        return position.y;
    }
    
    public Point getPosition() {
        return position;
    }
    
    public void setPosition(int row, int col) {
        position.x = row;
        position.y = col;
    }
    
    //dámává válik az alap figura
    public void levelUp() { //ha nem érte el a max szintet, szintet lép
        if (type < TYPES.length) type++;
    }
    
}