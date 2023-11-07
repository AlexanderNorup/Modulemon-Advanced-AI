package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleState;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.ArrayList;

public class Node {
    private IBattleParticipant participant;
    private IBattleState state;
    private ArrayList<Node> children = new ArrayList<Node>();
    private Node parent = null;
    private IMonsterMove parentMove = null;
    private IMonster parentSwitch = null;
    private float reward = 0;
    private int timesVisited = 0;
    private int turnsTillWin = Integer.MAX_VALUE;
    public Node(IBattleState state, IBattleParticipant participant) {
        // Constructor used when creating root node
        this.state = state;
        this.participant = participant;
    }
    public Node(IBattleState state, Node parent, IMonsterMove parentMove) {
        this.state = state;
        this.parent = parent;
        this.parentMove = parentMove;
        this.participant = getOpposingParticipant(parent.getParticipant(), state);
        parent.getChildren().add(this);
    }
    public Node(IBattleState state, Node parent, IMonster parentSwitch) {
        this.state = state;
        this.parent = parent;
        this.parentSwitch = parentSwitch;
        this.participant = getOpposingParticipant(parent.getParticipant(), state);
        parent.getChildren().add(this);
    }

    public IBattleState getState() {
        return state;
    }

    public void setState(IBattleState state) {
        this.state = state;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<Node> children) {
        this.children = children;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public IMonsterMove getParentMove() {
        return parentMove;
    }

    public void setParentMove(IMonsterMove parentMove) {
        this.parentMove = parentMove;
    }

    public IMonster getParentSwitch() {
        return parentSwitch;
    }

    public void setParentSwitch(IMonster parentSwitch) {
        this.parentSwitch = parentSwitch;
    }

    public float getReward() {
        return reward;
    }

    public void setReward(float reward) {
        this.reward = reward;
    }

    public void setTimesVisited(int timesVisited) {
        this.timesVisited = timesVisited;
    }

    public int getTurnsTillWin() {
        return turnsTillWin;
    }

    public void setTurnsTillWin(int turnsTillWin) {
        this.turnsTillWin = turnsTillWin;
    }

    public int getTimesVisited() {
        return timesVisited;
    }

    public void incrementTimesVisited() {
        this.timesVisited++;
    }

    public IBattleParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(IBattleParticipant participant) {
        this.participant = participant;
    }

    private IBattleParticipant getOpposingParticipant(IBattleParticipant current, IBattleState state) {
        return state.getPlayer().equals(current) ? state.getEnemy() : state.getPlayer();
    }

    @Override
    public String toString() {
        if(this.parent != null){
            String action = "doing nothing";
            if(this.parentMove != null){
                action = String.format("%s using %s", this.parent.getParticipant().getActiveMonster().getName(), this.parentMove);
            }else if(this.parentSwitch != null){
                action = "Switching to " + this.parentSwitch;
            }
            return String.format("%s (Reward: %.8f; Winning in %d turns)", action, this.reward, this.getTurnsTillWin());
        }else{
            return "Root node";
        }
    }
}
