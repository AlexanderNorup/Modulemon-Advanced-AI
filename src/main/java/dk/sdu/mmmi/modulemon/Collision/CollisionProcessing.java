package dk.sdu.mmmi.modulemon.Collision;

import com.badlogic.gdx.math.Vector2;
import dk.sdu.mmmi.modulemon.CommonMap.IMapView;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityType;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.PositionPart;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IPostEntityProcessingService;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class CollisionProcessing implements IPostEntityProcessingService {
    private IMapView mapView;
    private AssetLoader loader = AssetLoader.getInstance();
    private float bonkCooldown;
    private IGameSettings settings;

    @Override
    public void process(GameData gameData, World world) {
        if(mapView == null){
            return;
        }
        for (Entity entity : world.getEntities()) {
            PositionPart entityPosPart = entity.getPart(PositionPart.class);
            var targetPos = entityPosPart.getTargetPos();
            if(targetPos != null && mapView.isCellBlocked(targetPos.x, targetPos.y) && !entityPosPart.getTargetPos().equals(new Vector2(0,0))) {
                // Map cell is blocked!
                entityPosPart.setTargetPos(null);

                if(entity.getType().equals(EntityType.PLAYER)){
                    playBonkSound();
                }

                break;
            }

            for (Entity checking : world.getEntities()) {
                if(entity.equals(checking))
                    continue;

                PositionPart checkPosPart = checking.getPart(PositionPart.class);
                if(checkPosPart == null){
                    // Entity checking against has no position part. That's weird tho, maybe log that?
                    continue;
                }

                if(targetPos != null
                        && (targetPos.epsilonEquals(checkPosPart.getCurrentPos()) //Checking if moving into another entity
                        || targetPos.epsilonEquals(checkPosPart.getTargetPos()))){ // Checking if moving into the same block
                    // We're trying to move into another entity
                    entityPosPart.setTargetPos(null);

                    if(entity.getType().equals(EntityType.PLAYER)){
                        playBonkSound();
                    }
                    break;
                }
            }
        }
        if(bonkCooldown >= 0){
            bonkCooldown -= gameData.getDelta();
        }
    }

    private void playBonkSound(){
        if (bonkCooldown <= 0) {
            if(settings != null){
                loader.getSoundAsset("/sounds/bonk.ogg", this.getClass()).play( ((int) settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting()) / 100f) / 2f);
            }
            else loader.getSoundAsset("/sounds/bonk.ogg", this.getClass()).play( );
            bonkCooldown = 0.5f;
        }
    }

    public void setMapView(IMapView mapView){
        this.mapView = mapView;
    }

    public void removeMapView(IMapView mapView){
        this.mapView = null;
    }
    
    public void setSettingsService(IGameSettings settings){
        this.settings = settings;
        if (settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting())==null) {
            settings.setSetting(SettingsRegistry.getInstance().getSoundVolumeSetting(), 60);
        }

    }

    public void removeSettingsService(IGameSettings settings){
        this.settings = null;
    }
}
