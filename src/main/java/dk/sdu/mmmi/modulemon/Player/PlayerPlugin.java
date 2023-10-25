package dk.sdu.mmmi.modulemon.Player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.*;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterRegistry;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IGamePluginService;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.util.*;

public class PlayerPlugin implements IGamePluginService {
    private static final int TILE_SIZE = 64;
    private Entity player;
    private static IMonsterRegistry monsterRegistry;

    public PlayerPlugin() {
    }

    @Override
    public void start(GameData gameData, World world) {
        // Add entities to the world
        player = createPlayer(gameData);
        world.addEntity(player);
    }

    private Entity createPlayer(GameData gameData) {
        int xOffsetCorrection = 7; // Add to x when displaying
        int yOffsetCorrection = 20; // Add to y when displaying
        int tilemapXPos = 5;
        int tilemapYPos = 44;

        float x = (tilemapXPos)* TILE_SIZE ;
        float y =  (TILE_SIZE*(TILE_SIZE-1) - (tilemapYPos) * TILE_SIZE );

        Entity player = new Player();
        PositionPart positionPart = new PositionPart(x, y);
        positionPart.setVisualOffsetPos(new Vector2(xOffsetCorrection, yOffsetCorrection));
        player.add(positionPart);
        player.add(new MovingPart());
        player.add(new InteractPart(positionPart, 1));
        List<Texture> upSprite = Collections.singletonList(AssetLoader.getInstance().getTextureAsset("/assets/main-char-up5.png", Player.class));
        List<Texture> downSprites = getAnimations("down", "walking", 8);
        List<Texture> leftSprite = Collections.singletonList(AssetLoader.getInstance().getTextureAsset("/assets/main-char-left5.png", Player.class));
        List<Texture> rightSprite = Collections.singletonList(AssetLoader.getInstance().getTextureAsset("/assets/main-char-right5.png", Player.class));

        List<Texture> upIdleSprites = getAnimations("up", "idle", 3);
        List<Texture> downIdleSprites = getAnimations("down", "idle", 3);
        List<Texture> leftIdleSprites = getAnimations("left", "idle", 3);
        List<Texture> rightIdleSprites = getAnimations("right", "idle", 3);
        player.add(new SpritePart(upSprite, downSprites, leftSprite, rightSprite, upIdleSprites, downIdleSprites, leftIdleSprites, rightIdleSprites));
        Queue<String> playerLines = new LinkedList<>();
        playerLines.add("Alright, lets battle!");
        player.add(new TextDisplayPart(playerLines));
        addMonsterTeam(player, gameData);

        return player;
    }

    @NotNull
    private static List<Texture> getAnimations(String direction, String action, int frameCount) {
        List<Texture> sprites = new ArrayList<>();
        for (int i = 1; i <= frameCount; i++) {
            Texture sprite = AssetLoader.getInstance().getTextureAsset("/assets/" + action + "Animations/main-char-" + direction + "-" + action + "" + i +".png", Player.class);
            sprites.add(sprite);
        }
        return sprites;
    }

    private void addMonsterTeam(Entity entity, GameData gameData) {
        List<IMonster> monsterList = new ArrayList<>();
        if(gameData != null && gameData.getKeys().isDown(GameKeys.LEFT_CTRL))
            monsterList.add(monsterRegistry.getMonster(6 % monsterRegistry.getMonsterAmount())); //God
        entity.add(new MonsterTeamPart(monsterList));
    }

    @Override
    public void stop(GameData gameData, World world) {
        // Remove entities
        world.removeEntity(player);
    }


    public void setMonsterRegistryService (IMonsterRegistry registry){
        this.monsterRegistry = registry;
        if (player != null) {
            addMonsterTeam(player, null);
        }
    }

    public void removeMonsterRegistryService(IMonsterRegistry monsterRegistry) {
        this.monsterRegistry = null;
        if (player != null) {
            player.remove(MonsterTeamPart.class);
        }
    }

}
