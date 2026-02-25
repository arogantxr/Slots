package htl.steyr.slots.assets;
import java.util.List;

public class Player {

    private final String name;
    private int lives;
    private final Slotmachine slotmachine;

    private List<String> lastSpin = List.of();

    public Player(String name, int lives, Slotmachine slotmachine) {
        this.name = name;
        this.lives = lives;
        this.slotmachine = slotmachine;
    }

    public List<String> spin() {
        lastSpin = slotmachine.spin();
        return lastSpin;
    }

    public List<String> getLastSpin() {
        return lastSpin;
    }

    public void loseLife() {
        if (lives > 0) lives--;
    }

    public boolean isAlive() {
        return lives > 0;
    }

    public int getLives() {
        return lives;
    }

    public String getName() {
        return name;
    }
}