package htl.steyr.slots.assets;

import java.util.List;

public class Player {

    private final String name;
    private final Slotmachine slotmachine;

    private boolean alive = true;
    private boolean usedRespin = false;
    private boolean submitted = false;
    private int claimedHearts = 0;
    private List<String> lastSpin = List.of();

    public Player(String name, Slotmachine slotmachine) {
        this.name = name;
        this.slotmachine = slotmachine;
    }

    public List<String> spin() {
        lastSpin = slotmachine.spin();
        return lastSpin;
    }

    public List<String> deadSpin() {
        lastSpin = slotmachine.spin(1);
        return lastSpin;
    }

    public int countHearts() {
        return slotmachine.countHearts(lastSpin);
    }

    public void resetForRound() {
        submitted = false;
        claimedHearts = 0;
        lastSpin = List.of();
    }

    public void useRespin() {
        usedRespin = true;
    }

    public void eliminate() {
        alive = false;
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean hasUsedRespin() {
        return usedRespin;
    }

    public boolean hasSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public int getClaimedHearts() {
        return claimedHearts;
    }

    public void setClaimedHearts(int claimedHearts) {
        this.claimedHearts = claimedHearts;
    }

    public List<String> getLastSpin() {
        return lastSpin;
    }
}