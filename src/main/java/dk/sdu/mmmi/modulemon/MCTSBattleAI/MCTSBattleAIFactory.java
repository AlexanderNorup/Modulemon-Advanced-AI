package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class MCTSBattleAIFactory implements IBattleAIFactory {

    private IGameSettings settings = null;

    public MCTSBattleAIFactory(){}

    @Override
    public IBattleAI getBattleAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl) {
        return new MCTSBattleAI(battleSimulation, participantToControl, this.settings);
    }

    public void setSettingsService(IGameSettings settings){
        this.settings = settings;
        if(settings.getSetting(SettingsRegistry.getInstance().getAIProcessingTimeSetting())==null){
            settings.setSetting(SettingsRegistry.getInstance().getAIProcessingTimeSetting(), 1000);
        }
    }

    public void removeSettingsService(IGameSettings settings) {
        this.settings = null;
    }

    @Override
    public String toString() {
        return "MCTS";
    }
}
