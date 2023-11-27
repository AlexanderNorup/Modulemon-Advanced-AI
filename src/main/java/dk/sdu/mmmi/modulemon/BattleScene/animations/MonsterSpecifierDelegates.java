package dk.sdu.mmmi.modulemon.BattleScene.animations;

import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleSceneDefaults;
import dk.sdu.mmmi.modulemon.common.drawing.Position;

public class MonsterSpecifierDelegates {
    public static IMonsterSpecifierDelegate player = new IMonsterSpecifierDelegate() {
        @Override
        public Position getDefaultPosition() {
            return BattleSceneDefaults.playerMonsterPosition();
        }

        @Override
        public void setX(BattleScene scene, float x) {
            scene.setPlayerMonsterPosition(new Position(x, scene.getPlayerMonsterPosition().getY()));
        }

        @Override
        public void setY(BattleScene scene, float y) {
            scene.setPlayerMonsterPosition(new Position(scene.getPlayerMonsterPosition().getX(), y));
        }

        @Override
        public void setRotation(BattleScene scene, float rotation) {
            scene.setPlayerMonsterRotation(rotation);
        }
    };

    public static IMonsterSpecifierDelegate enemy = new IMonsterSpecifierDelegate() {
        @Override
        public Position getDefaultPosition() {
            return BattleSceneDefaults.enemyMonsterPosition();
        }

        @Override
        public void setX(BattleScene scene, float x) {
            scene.setEnemyMonsterPosition(new Position(x, scene.getEnemyMonsterPosition().getY()));
        }

        @Override
        public void setY(BattleScene scene, float y) {
            scene.setEnemyMonsterPosition(new Position(scene.getEnemyMonsterPosition().getX(), y));
        }

        @Override
        public void setRotation(BattleScene scene, float rotation) {
            scene.setEnemyMonsterRotation(rotation);
        }
    };

}
