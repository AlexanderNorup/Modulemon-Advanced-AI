package dk.sdu.mmmi.modulemon.CommonMonster;

public class EmptyMove implements IMonsterMove {

    private static EmptyMove instance = new EmptyMove();
    public static EmptyMove getInstance(){
        return instance;
    }

    private EmptyMove(){

    }

    @Override
    public String getName() {
        return "Loaf around";
    }

    @Override
    public String getSoundPath() {
        return "/sounds/loafAround.ogg";
    }

    @Override
    public String getBattleDescription() {
        return "Your monster loafs around. It does nothing.";
    }

    @Override
    public String getSummaryScreenDescription() {
        return getBattleDescription();
    }
}
