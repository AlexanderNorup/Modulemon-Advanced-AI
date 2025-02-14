package dk.sdu.mmmi.modulemon.Interaction;

import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.InteracteePart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.TextDisplayPart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.MonsterTeamPart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityType;
import dk.sdu.mmmi.modulemon.CommonMap.IMapEvent;
import dk.sdu.mmmi.modulemon.CommonMap.IMapView;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.TextMapEvent;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.InteractPart;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IPostEntityProcessingService;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class InteractProcessing implements IPostEntityProcessingService {
    private IMapView mapView;

    private InteractPair previousInteractPair = new InteractPair();

    private IGameSettings settings;

    @Override
    public void process(GameData gameData, World world) {
        boolean wasAbleToInteract = false;
        for (Entity entity : world.getEntities()) {
            InteractPart interactPart = entity.getPart(InteractPart.class);
            if (interactPart != null) {
                if (interactPart.canInteract()) {
                    if(entity.getType() == EntityType.PLAYER){
                        if(!gameData.getKeys().isDown(GameKeys.E) && !gameData.getKeys().isDown(GameKeys.ACTION)) {
                            //If the interactor is the Player, don't interact if not holding either E or ACTION.
                            continue;
                        }
                        gameData.getKeys().setKey(GameKeys.E, false);
                        gameData.getKeys().setKey(GameKeys.ACTION, false);
                    }

                    wasAbleToInteract = true;
                    //Checking that we only interact with the same pair once.
                    if(this.previousInteractPair.isSamePair(entity, interactPart.getInteractWith())){
                        continue;
                    }
                    this.previousInteractPair.setNewPair(entity, interactPart.getInteractWith());

                    //If there is an interactee part, we just that.
                    InteracteePart interacteePart = interactPart.getInteractWith().getPart(InteracteePart.class);
                    if(interacteePart != null){
                        IMapEvent event = interacteePart.getEvent(entity);
                        this.mapView.addMapEvent(event);
                        break;
                    }

                    MonsterTeamPart monsterTeam1 = entity.getPart(MonsterTeamPart.class);
                    MonsterTeamPart monsterTeam2 = interactPart.getInteractWith().getPart(MonsterTeamPart.class);

                    // Check if one of the entities does not have a monster team
                    if (monsterTeam1 == null || monsterTeam2 == null) {
                        continue;
                    }

                    if(monsterTeam1.getMonsterTeam().size() == 0 || monsterTeam2.getMonsterTeam().size() == 0){
                        TextMapEvent event = new TextMapEvent(new LinkedList<>(Collections.singletonList("You have no monsters. Go get some from a vending machine")));
                        mapView.addMapEvent(event);
                        continue;
                    }

                    if (!monsterTeam1.hasAliveMonsters() || !monsterTeam2.hasAliveMonsters()) {
                        Entity deadEntity = !monsterTeam1.hasAliveMonsters() ? entity : interactPart.getInteractWith();
                        if(deadEntity.getType() == EntityType.GENERIC){ //Enemies monsters are dead
                            TextMapEvent textMapEvent = new TextMapEvent(new LinkedList<>(Collections.singletonList("Aww man, I lost..")));
                            mapView.addMapEvent(textMapEvent);
                        }else{
                            //Must be the player who has dead monsters
                            TextMapEvent textMapEvent = new TextMapEvent(new LinkedList<>(Collections.singletonList("I wanna battle, but your monsters are dead. Go heal them up!")));
                            mapView.addMapEvent(textMapEvent);
                        }
                        continue;
                    }


                    // This isn't great. Currently this relies on the NPC always initiating the battle.
                    // Could be nice to somehow check who is player, who is npc, and put them in the right spots.
                    Queue<String> lines;
                    if (entity.getPart(TextDisplayPart.class) != null) {
                        lines = ((TextDisplayPart) entity.getPart(TextDisplayPart.class)).getLines();
                    } else {
                        // Default text on battle
                        lines = new LinkedList<>(Collections.singletonList("..."));
                    }

                    BattleEvent battle;
                    if(entity.getType() == EntityType.PLAYER){
                        battle = new BattleEvent(lines, entity, interactPart.getInteractWith(), mapView, settings);
                    }else{
                        battle = new BattleEvent(lines, interactPart.getInteractWith(), entity, mapView, settings);
                    }
                    mapView.addMapEvent(battle);
                }
            }
        }
        if(!wasAbleToInteract){
            this.previousInteractPair.setNewPair(null, null);
        }
    }

    public void setMapView(IMapView mapView) {
        this.mapView = mapView;
    }

    public void removeMapView(IMapView mapView) {
        this.mapView = null;
    }

    private class InteractPair{
        private Entity entity1;
        private Entity entity2;

        public boolean isSamePair(Entity entity1, Entity entity2){
            return entity1 == this.entity1 && entity2 == this.entity2
                    || entity2 == this.entity1 && entity1 == this.entity2;
        }

        public void setNewPair(Entity entity1, Entity entity2){
            this.entity1 = entity1;
            this.entity2 = entity2;
        }
    }
}
