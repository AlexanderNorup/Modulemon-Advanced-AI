package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleState;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.ArrayList;

public class Node {
    private IBattleState state;
    private ArrayList<Node> children = new ArrayList<Node>();
    private Node parent = null;
    private IMonsterMove parentMove = null;
    private IMonster parentSwitch = null;
    private float reward = 0;
    private int timesVisited = 0;
    public Node(IBattleState state) {
        this.state = state;
    }
    public Node(IBattleState state, Node parent, IMonsterMove parentMove) {
        this.state = state;
        this.parent = parent;
        this.parentMove = parentMove;
        parent.getChildren().add(this);
    }
    public Node(IBattleState state, Node parent, IMonster parentSwitch) {
        this.state = state;
        this.parent = parent;
        this.parentSwitch = parentSwitch;
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

    public int getTimesVisited() {
        return timesVisited;
    }

    public void incrementTimesVisited() {
        this.timesVisited++;
    }
}