package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

public class EmptyMove implements IMonsterMove {
    @Override
    public String getName() {
        return "Loaf around";
    }

    @Override
    public String getSoundPath() {
        return null;
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
