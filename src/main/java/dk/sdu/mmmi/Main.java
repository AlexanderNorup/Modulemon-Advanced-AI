package dk.sdu.mmmi;

import dk.sdu.mmmi.modulemon.BattleScene.BattleView;
import dk.sdu.mmmi.modulemon.BattleSimulation.BattleSimulation;
import dk.sdu.mmmi.modulemon.Collision.CollisionProcessing;
import dk.sdu.mmmi.modulemon.Game;
import dk.sdu.mmmi.modulemon.Interaction.InteractProcessing;
import dk.sdu.mmmi.modulemon.Map.MapView;
import dk.sdu.mmmi.modulemon.MapEntities.MapEntityPlugin;
import dk.sdu.mmmi.modulemon.Monster.BattleMonsterProcessor;
import dk.sdu.mmmi.modulemon.Monster.MonsterRegistry;
import dk.sdu.mmmi.modulemon.NPC.NPCControlSystem;
import dk.sdu.mmmi.modulemon.NPC.NPCPlugin;
import dk.sdu.mmmi.modulemon.Player.Player;
import dk.sdu.mmmi.modulemon.Player.PlayerControlSystem;
import dk.sdu.mmmi.modulemon.Player.PlayerPlugin;
import dk.sdu.mmmi.modulemon.Settings.Settings;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        // In the initial version of Modulémon, we used OSGi to orchestrate all the components.
        // In this port, we orchestrate the components manually. This unfortunately means we cannot enable and disable
        //  components on runtime.
        // And we'll also have the problem with the flat classpath!!!!! Let's hope it just works(tm).

        var settings  = new Settings();
        var monsterRegistry = new MonsterRegistry();
        var battleMonsterProcessor = new BattleMonsterProcessor();
        var battleAI = new dk.sdu.mmmi.modulemon.BattleAI.BattleAIFactory();
//        var battleAI = new dk.sdu.mmmi.modulemon.SimpleAI.BattleAIFactory(); // Uncomment for Simple AI

        var battleSimulation = new BattleSimulation();
        battleSimulation.setAIFactory(battleAI);
        battleSimulation.setMonsterProcessor(battleMonsterProcessor);

        var battle = new BattleView();
        battle.setBattleSimulation(battleSimulation);
        battle.setSettingsService(settings);
        battle.setMonsterRegistry(monsterRegistry);

        // Map stuff
        var map = new MapView();
        map.setSettingsService(settings);
        map.setBattleView(battle);

        var collision = new CollisionProcessing();
        collision.setSettingsService(settings);
        collision.setMapView(map);
        map.addPostProcessingService(collision);

        var mapEntities = new MapEntityPlugin();
        mapEntities.setMonsterRegistryService(monsterRegistry);
        map.addGamePluginService(mapEntities);

        var npc = new NPCPlugin();
        npc.setMonsterRegistryService(monsterRegistry);
        map.addGamePluginService(npc);

        var player= new PlayerPlugin();
        player.setMonsterRegistryService(monsterRegistry);
        map.addGamePluginService(player);

        var playerControl = new PlayerControlSystem();
        var npcControl = new NPCControlSystem();
        map.addEntityProcessingService(playerControl);
        map.addEntityProcessingService(npcControl);

        var interaction = new InteractProcessing();
        interaction.setMapView(map);
        map.addPostProcessingService(interaction);

        var game = new Game();
        game.setSettingsService(settings);
        game.addGameViewServiceList(battle);
        game.addGameViewServiceList(map);
    }
}