package br.grassinimoraes.divasteroides;

import android.graphics.Color;
import android.util.Log;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Segunda geracao do jogo. Mantem a arte e o espirito do projeto original,
 * mas separa regras, inventario e ondas e nao depende do Box2D para a partida.
 */
public class GameActivityV2 extends SimpleBaseGameActivity {
    static final int CAMERA_WIDTH = 800;
    static final int CAMERA_HEIGHT = 480;
    private static final float CITY_IMPACT_Y = 118f;
    private static final float METEOR_START_Y = 354f;

    private static final int ID_SUBTRACTOR = 100;
    private static final int ID_SUB_MINUS = 101;
    private static final int ID_SUB_PLUS = 102;
    private static final int ID_BOMB_ZERO = 110;
    private static final int ID_REPAIR = 120;
    private static final int ID_MENU = 130;
    private static final int ID_RESTART = 900;
    private static final int ID_GAMEOVER_MENU = 901;
    private static final int ID_QUIZ_BASE = 3000;

    private Camera camera;
    private Scene rootScene;
    private Scene menuScene;
    private Scene gameScene;
    private Scene quizScene;

    private final Random random = new Random();
    private final PlayerInventoryV2 inventory = new PlayerInventoryV2();
    private final WaveManagerV2 waves = new WaveManagerV2();
    private final AudioBankV2 audio = new AudioBankV2();
    private final List<MeteorActor> meteors = new ArrayList<MeteorActor>();
    private final Map<Integer, HudButton> ammoButtons = new HashMap<Integer, HudButton>();
    private final List<Sprite> fireSprites = new ArrayList<Sprite>();

    private Font fontSmall;
    private Font fontMedium;
    private Font fontLarge;

    private TextureRegion menuBackgroundRegion;
    private TextureRegion laneRegion;
    private TextureRegion cityRegion;
    private TextureRegion tailRegion;
    private TextureRegion moneyRegion;
    private TextureRegion subtractorRegion;
    private TextureRegion commercialPlaneRegion;
    private TextureRegion militaryPlaneRegion;
    private TextureRegion bombZeroRegion;
    private TextureRegion quizTargetRegion;
    private TextureRegion fireRegion;
    private TiledTextureRegion meteorRegion;
    private TiledTextureRegion cannonRegion;
    private TiledTextureRegion towerRegion;

    private AnimatedSprite cannon;
    private AnimatedSprite tower;
    private Sprite citySprite;
    private CommercialPlaneActor commercialPlane;

    private Text scoreText;
    private Text waveText;
    private Text healthText;
    private Text subStatusText;
    private Text selectedWeaponText;
    private Text shieldText;
    private Rectangle healthBarFill;
    private Rectangle waveBarFill;
    private HudButton subButton;
    private HudButton bombButton;
    private HudButton repairButton;

    private float cityHealth = 100f;
    private int score;
    private boolean warningActive;
    private boolean gameplayActive;
    private boolean strikeInProgress;
    private boolean waveTransitionPending;
    private float planeCountdown;

    private MeteorActor quizMeteor;
    private GameRulesV2.QuizData quizData;
    private int quizAttemptsLeft;
    private Text quizFeedback;

    @Override
    public EngineOptions onCreateEngineOptions() {
        camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        EngineOptions options = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new FillResolutionPolicy(), camera);
        options.getAudioOptions().setNeedsSound(true);
        options.getTouchOptions().setNeedsMultiTouch(true);
        return options;
    }

    @Override
    protected void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("graficos/");

        ITexture fontTextureSmall = new BitmapTextureAtlas(getTextureManager(), 256, 256, TextureOptions.BILINEAR);
        fontSmall = FontFactory.createStrokeFromAsset(getFontManager(), fontTextureSmall, getAssets(),
                "fnt/ALEAWB__.TTF", 18, true, Color.WHITE, 2, Color.BLACK);
        fontSmall.load();

        ITexture fontTextureMedium = new BitmapTextureAtlas(getTextureManager(), 256, 256, TextureOptions.BILINEAR);
        fontMedium = FontFactory.createStrokeFromAsset(getFontManager(), fontTextureMedium, getAssets(),
                "fnt/ALEAWB__.TTF", 27, true, Color.WHITE, 2, Color.BLACK);
        fontMedium.load();

        ITexture fontTextureLarge = new BitmapTextureAtlas(getTextureManager(), 512, 512, TextureOptions.BILINEAR);
        fontLarge = FontFactory.createStrokeFromAsset(getFontManager(), fontTextureLarge, getAssets(),
                "fnt/ALEAWB__.TTF", 46, true, Color.rgb(75, 190, 255), 3, Color.WHITE);
        fontLarge.load();

        BitmapTextureAtlas menuAtlas = new BitmapTextureAtlas(getTextureManager(), 1024, 512, TextureOptions.NEAREST);
        menuBackgroundRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuAtlas, this,
                "menu_cena.png", 0, 0);
        menuAtlas.load();

        BitmapTextureAtlas laneAtlas = new BitmapTextureAtlas(getTextureManager(), 1024, 128, TextureOptions.NEAREST);
        laneRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(laneAtlas, this,
                "lane_armas.png", 0, 0);
        laneAtlas.load();

        BitmapTextureAtlas cityAtlas = new BitmapTextureAtlas(getTextureManager(), 256, 32, TextureOptions.NEAREST);
        cityRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(cityAtlas, this,
                "cidade_grande.png", 0, 0);
        cityAtlas.load();

        BitmapTextureAtlas meteorAtlas = new BitmapTextureAtlas(getTextureManager(), 128, 16, TextureOptions.NEAREST);
        meteorRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(meteorAtlas, this,
                "meteoro e itens.png", 0, 0, 8, 1);
        meteorAtlas.load();

        BitmapTextureAtlas cannonAtlas = new BitmapTextureAtlas(getTextureManager(), 128, 32, TextureOptions.NEAREST);
        cannonRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(cannonAtlas, this,
                "canhao1.png", 0, 0, 8, 2);
        cannonAtlas.load();

        BitmapTextureAtlas towerAtlas = new BitmapTextureAtlas(getTextureManager(), 64, 32, TextureOptions.NEAREST);
        towerRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(towerAtlas, this,
                "torre.png", 0, 0, 8, 1);
        towerAtlas.load();

        tailRegion = loadSingleTexture("rastro_meteoro.png", 16, 48);
        moneyRegion = loadSingleTexture("dinheiro_bonus.png", 32, 32);
        subtractorRegion = loadSingleTexture("subtrator.png", 32, 32);
        commercialPlaneRegion = loadSingleTexture("aviao_comercial.png", 128, 64);
        militaryPlaneRegion = loadSingleTexture("aviao_militar.png", 128, 64);
        bombZeroRegion = loadSingleTexture("bomba0.png", 32, 32);
        quizTargetRegion = loadSingleTexture("alvo_quiz.png", 32, 32);
        fireRegion = loadSingleTexture("fogo.png", 32, 32);

        audio.load(getSoundManager(), this);
    }

    private TextureRegion loadSingleTexture(String asset, int atlasWidth, int atlasHeight) {
        BitmapTextureAtlas atlas = new BitmapTextureAtlas(getTextureManager(), atlasWidth, atlasHeight,
                TextureOptions.NEAREST);
        TextureRegion region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(atlas, this, asset, 0, 0);
        atlas.load();
        return region;
    }

    @Override
    public Scene onCreateScene() {
        rootScene = new Scene();
        rootScene.setBackground(new Background(0.01f, 0.03f, 0.08f));
        showMenu();
        return rootScene;
    }

    private void showMenu() {
        menuScene = new Scene();
        menuScene.setBackground(new Background(0.01f, 0.03f, 0.08f));

        Sprite background = new Sprite(CAMERA_WIDTH / 2f, CAMERA_HEIGHT / 2f,
                menuBackgroundRegion, getVertexBufferObjectManager());
        background.setWidth(CAMERA_WIDTH);
        background.setHeight(CAMERA_HEIGHT);
        menuScene.attachChild(background);

        Text title = new Text(400, 400, fontLarge, "ASTEROIDE MATEMATICO", 40,
                getVertexBufferObjectManager());
        title.setScale(0.86f);
        menuScene.attachChild(title);

        Text subtitle = new Text(400, 352, fontSmall,
                "DEFENDA A CIDADE USANDO MATEMATICA", 64, getVertexBufferObjectManager());
        subtitle.setColor(0.75f, 0.92f, 1f);
        menuScene.attachChild(subtitle);

        HudButton start = new HudButton(400, 270, 235, 58, "INICIAR", 700, menuScene);
        start.setColor(0.05f, 0.26f, 0.38f);
        menuScene.attachChild(start);
        menuScene.registerTouchArea(start);

        Text help = new Text(400, 190, fontSmall,
                "Comece com 2 e 3. Desbloqueie primos, use o subtrator\ne resolva desafios de adicao e multiplicacao.",
                180, getVertexBufferObjectManager());
        help.setScale(0.88f);
        menuScene.attachChild(help);

        Text difficulty = new Text(400, 130, fontSmall,
                "DIFICULDADE PROGRESSIVA - METEOROS ATE 200 - MUNICAO ATE 31",
                110, getVertexBufferObjectManager());
        difficulty.setColor(1f, 0.82f, 0.25f);
        difficulty.setScale(0.83f);
        menuScene.attachChild(difficulty);

        rootScene.setChildScene(menuScene);
    }

    private void startNewGame() {
        inventory.reset();
        waves.reset();
        meteors.clear();
        ammoButtons.clear();
        fireSprites.clear();
        cityHealth = 100f;
        score = 0;
        warningActive = false;
        gameplayActive = false;
        strikeInProgress = false;
        waveTransitionPending = false;
        commercialPlane = null;
        quizMeteor = null;
        quizData = null;
        planeCountdown = 13f;

        buildGameScene();
        rootScene.setChildScene(gameScene);
        beginWaveWarning();
    }

    private void buildGameScene() {
        gameScene = new Scene();
        gameScene.setBackground(new Background(0.035f, 0.10f, 0.19f));
        gameScene.setTouchAreaBindingOnActionDownEnabled(true);

        citySprite = new Sprite(400, 50, cityRegion, getVertexBufferObjectManager());
        citySprite.setWidth(800);
        citySprite.setHeight(100);
        gameScene.attachChild(citySprite);

        createCityFire(115, 69, 0.65f);
        createCityFire(355, 62, 0.85f);
        createCityFire(655, 68, 0.70f);

        tower = new AnimatedSprite(400, 118, towerRegion, getVertexBufferObjectManager());
        tower.setCurrentTileIndex(0);
        tower.setScale(3.7f);
        gameScene.attachChild(tower);

        cannon = new AnimatedSprite(400, 145, cannonRegion, getVertexBufferObjectManager());
        cannon.setCurrentTileIndex(8);
        cannon.setScale(3.2f);
        gameScene.attachChild(cannon);

        Sprite lane = new Sprite(400, 432, laneRegion, getVertexBufferObjectManager());
        lane.setWidth(800);
        lane.setHeight(96);
        lane.setAlpha(0.94f);
        gameScene.attachChild(lane);

        createHud();
        createGameTimers();
        updateHud();
    }

    private void createCityFire(float x, float y, float scale) {
        Sprite fire = new Sprite(x, y, fireRegion, getVertexBufferObjectManager());
        fire.setScale(scale);
        fire.setVisible(false);
        fire.registerEntityModifier(new LoopEntityModifier(new SequenceEntityModifier(
                new AlphaModifier(0.20f, 0.45f, 1f),
                new AlphaModifier(0.22f, 1f, 0.45f),
                new ScaleModifier(0.18f, scale, scale * 1.12f),
                new ScaleModifier(0.18f, scale * 1.12f, scale))));
        fireSprites.add(fire);
        gameScene.attachChild(fire);
    }

    private void createHud() {
        Text dividerLabel = new Text(235, 466, fontSmall, "DIVISORES", 20, getVertexBufferObjectManager());
        dividerLabel.setScale(0.70f);
        dividerLabel.setColor(0.55f, 0.90f, 1f);
        gameScene.attachChild(dividerLabel);

        for (int i = 0; i < GameRulesV2.PRIME_AMMO.length; i++) {
            int prime = GameRulesV2.PRIME_AMMO[i];
            float x = 22 + i * 40;
            HudButton button = new HudButton(x, 430, 34, 38, String.valueOf(prime), i, gameScene);
            button.setColor(0.05f, 0.22f, 0.30f);
            ammoButtons.put(prime, button);
            gameScene.attachChild(button);
            gameScene.registerTouchArea(button);
        }

        HudButton subMinus = new HudButton(505, 430, 28, 32, "-", ID_SUB_MINUS, gameScene);
        HudButton subPlus = new HudButton(625, 430, 28, 32, "+", ID_SUB_PLUS, gameScene);
        subButton = new HudButton(565, 430, 82, 38, "SUB", ID_SUBTRACTOR, gameScene);
        subMinus.setColor(0.25f, 0.13f, 0.36f);
        subPlus.setColor(0.25f, 0.13f, 0.36f);
        subButton.setColor(0.25f, 0.13f, 0.36f);
        gameScene.attachChild(subMinus);
        gameScene.attachChild(subButton);
        gameScene.attachChild(subPlus);
        gameScene.registerTouchArea(subMinus);
        gameScene.registerTouchArea(subButton);
        gameScene.registerTouchArea(subPlus);

        subStatusText = new Text(565, 462, fontSmall, "SUB 0", 30, getVertexBufferObjectManager());
        subStatusText.setScale(0.68f);
        gameScene.attachChild(subStatusText);

        bombButton = new HudButton(690, 430, 64, 40, "0 x0", ID_BOMB_ZERO, gameScene);
        bombButton.setColor(0.35f, 0.25f, 0.04f);
        gameScene.attachChild(bombButton);
        gameScene.registerTouchArea(bombButton);

        repairButton = new HudButton(765, 430, 58, 40, "$0", ID_REPAIR, gameScene);
        repairButton.setColor(0.10f, 0.32f, 0.12f);
        gameScene.attachChild(repairButton);
        gameScene.registerTouchArea(repairButton);

        HudButton menu = new HudButton(765, 386, 58, 25, "MENU", ID_MENU, gameScene);
        menu.setColor(0.14f, 0.16f, 0.20f);
        menu.setTextScale(0.62f);
        gameScene.attachChild(menu);
        gameScene.registerTouchArea(menu);

        scoreText = new Text(500, 382, fontSmall, "PONTOS 0", 50, getVertexBufferObjectManager());
        scoreText.setScale(0.82f);
        gameScene.attachChild(scoreText);

        waveText = new Text(305, 382, fontSmall, "ONDA 1", 80, getVertexBufferObjectManager());
        waveText.setScale(0.82f);
        gameScene.attachChild(waveText);

        selectedWeaponText = new Text(630, 382, fontSmall, "ARMA /2", 50, getVertexBufferObjectManager());
        selectedWeaponText.setScale(0.72f);
        selectedWeaponText.setColor(0.65f, 0.92f, 1f);
        gameScene.attachChild(selectedWeaponText);

        shieldText = new Text(675, 354, fontSmall, "ESCUDO 0", 30, getVertexBufferObjectManager());
        shieldText.setScale(0.72f);
        shieldText.setColor(0.48f, 0.80f, 1f);
        gameScene.attachChild(shieldText);

        Rectangle healthBarBg = new Rectangle(112, 382, 180, 14, getVertexBufferObjectManager());
        healthBarBg.setColor(0.25f, 0.03f, 0.03f);
        healthBarBg.setAlpha(0.85f);
        gameScene.attachChild(healthBarBg);

        healthBarFill = new Rectangle(22, 382, 180, 10, getVertexBufferObjectManager());
        healthBarFill.setAnchorCenter(0f, 0.5f);
        healthBarFill.setColor(0.15f, 0.95f, 0.22f);
        gameScene.attachChild(healthBarFill);

        healthText = new Text(112, 354, fontSmall, "CIDADE 100%", 30, getVertexBufferObjectManager());
        healthText.setScale(0.75f);
        gameScene.attachChild(healthText);

        Rectangle waveBarBg = new Rectangle(305, 355, 155, 8, getVertexBufferObjectManager());
        waveBarBg.setColor(0.03f, 0.09f, 0.14f);
        gameScene.attachChild(waveBarBg);

        waveBarFill = new Rectangle(227.5f, 355, 155, 6, getVertexBufferObjectManager());
        waveBarFill.setAnchorCenter(0f, 0.5f);
        waveBarFill.setColor(0.20f, 0.70f, 1f);
        gameScene.attachChild(waveBarFill);
    }

    private void createGameTimers() {
        gameScene.registerUpdateHandler(new TimerHandler(1.0f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                if (gameplayActive && !isWorldPaused() && waves.canSpawnMore()) {
                    spawnScheduledMeteor();
                    handler.setTimerSeconds(waves.getSpawnInterval());
                } else {
                    handler.setTimerSeconds(0.25f);
                }
            }
        }));

        gameScene.registerUpdateHandler(new TimerHandler(0.10f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                if (gameplayActive && !isWorldPaused()) {
                    planeCountdown -= 0.10f;
                    if (planeCountdown <= 0f && commercialPlane == null && waves.getWave() >= 2) {
                        spawnCommercialPlane();
                        planeCountdown = 18f + random.nextFloat() * 15f;
                    }
                }
                checkWaveCompletion();
            }
        }));
    }

    private boolean isWorldPaused() {
        return warningActive || quizScene != null || strikeInProgress || !gameplayActive;
    }

    private void beginWaveWarning() {
        warningActive = true;
        gameplayActive = false;
        waveTransitionPending = false;
        updateHud();

        final Rectangle redOverlay = new Rectangle(400, 240, 800, 480, getVertexBufferObjectManager());
        redOverlay.setColor(0.75f, 0.02f, 0.02f);
        redOverlay.setAlpha(0.22f);
        redOverlay.registerEntityModifier(new LoopEntityModifier(new SequenceEntityModifier(
                new AlphaModifier(0.34f, 0.13f, 0.30f),
                new AlphaModifier(0.34f, 0.30f, 0.13f))));
        gameScene.attachChild(redOverlay);

        final Text warningText = new Text(400, 260, fontLarge,
                "ALERTA - ONDA " + waves.getWave(), 60, getVertexBufferObjectManager());
        warningText.setColor(1f, 0.22f, 0.15f);
        warningText.setScale(0.82f);
        gameScene.attachChild(warningText);

        final Text rangeText = new Text(400, 210, fontMedium,
                "Meteoros ate " + waves.getMaxMeteorValue(), 50, getVertexBufferObjectManager());
        rangeText.setColor(1f, 0.82f, 0.40f);
        rangeText.setScale(0.80f);
        gameScene.attachChild(rangeText);

        audio.play(AudioBankV2.SIREN, 0.72f);

        gameScene.registerUpdateHandler(new TimerHandler(7.55f, false, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                redOverlay.clearEntityModifiers();
                redOverlay.detachSelf();
                warningText.detachSelf();
                rangeText.detachSelf();
                warningActive = false;
                gameplayActive = true;
                audio.play(AudioBankV2.RAIN_START, 0.70f);
                showFloatingText("CHUVA DE METEOROS!", 400, 320, 0.45f, 0.85f, 1f);
                updateHud();
            }
        }));
    }

    private void spawnScheduledMeteor() {
        waves.markSpawned();
        int max = waves.getMaxMeteorValue();
        float quizChance = GameRulesV2.specialQuizChance(waves.getWave());
        float roll = random.nextFloat();

        if (roll < quizChance) {
            if (random.nextBoolean()) {
                GameRulesV2.QuizData data = GameRulesV2.createMultiplicationQuiz(random, max);
                spawnMeteor(MeteorType.MULTIPLICATION, data.result, data, 0, randomMeteorX(), METEOR_START_Y);
                audio.play(AudioBankV2.MULT_METEOR, 0.45f);
            } else {
                GameRulesV2.QuizData data = GameRulesV2.createAdditionQuiz(random, max);
                spawnMeteor(MeteorType.ADDITION, data.result, data, 0, randomMeteorX(), METEOR_START_Y);
                audio.play(AudioBankV2.ADD_METEOR, 0.45f);
            }
            return;
        }

        int value = GameRulesV2.generateMeteorValue(random, waves.getWave(), inventory);
        spawnMeteor(MeteorType.NORMAL, value, null, 0, randomMeteorX(), METEOR_START_Y);
        if (random.nextFloat() < 0.25f) {
            audio.play(AudioBankV2.METEOR_IN, 0.20f);
        }
    }

    private float randomMeteorX() {
        return 52f + random.nextFloat() * 696f;
    }

    private MeteorActor spawnMeteor(MeteorType type, int value, GameRulesV2.QuizData data,
                                    int bonusValue, float x, float y) {
        MeteorActor meteor = new MeteorActor(x, y, type, value, data, bonusValue,
                GameRulesV2.meteorSpeed(waves.getWave(), random));
        meteors.add(meteor);
        gameScene.attachChild(meteor);
        gameScene.registerTouchArea(meteor);
        return meteor;
    }

    private void spawnRandomBonus(float x, float y, boolean guaranteed) {
        int lockedPrime = GameRulesV2.nextEligibleLockedPrime(inventory, waves.getWave());
        if (lockedPrime > 0 && (guaranteed || waves.getTotalDestroyed() % 5 == 0 || random.nextFloat() < 0.45f)) {
            spawnMeteor(MeteorType.BONUS_AMMO, lockedPrime, null, lockedPrime, x, Math.max(y, 300));
            return;
        }

        int roll = random.nextInt(100);
        MeteorType type;
        int value = 0;
        if (cityHealth < 45 && roll < 32) {
            type = MeteorType.BONUS_MONEY;
            value = 25;
        } else if (roll < 24) {
            type = MeteorType.BONUS_SUBTRACTOR;
            value = 10;
        } else if (roll < 46) {
            type = MeteorType.BONUS_MONEY;
            value = 25;
        } else if (roll < 62) {
            type = MeteorType.BONUS_HEALTH;
            value = 10;
        } else if (roll < 78) {
            type = MeteorType.BONUS_SHIELD;
            value = 1;
        } else if (roll < 90) {
            type = MeteorType.BONUS_BOMB_ZERO;
            value = 1;
        } else if (lockedPrime > 0) {
            type = MeteorType.BONUS_AMMO;
            value = lockedPrime;
        } else {
            type = MeteorType.BONUS_MONEY;
            value = 25;
        }
        spawnMeteor(type, value, null, value, x, Math.max(y, 300));
    }

    private void onMeteorTapped(MeteorActor meteor) {
        if (!gameplayActive || warningActive || strikeInProgress || meteor == null || !meteor.active) {
            return;
        }

        if (meteor.isBonus()) {
            collectBonus(meteor);
            return;
        }

        if (meteor.isQuizMeteor()) {
            openQuiz(meteor);
            return;
        }

        if (inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.BOMB_ZERO) {
            useBombZero();
            return;
        }

        fireBeamAt(meteor);

        if (inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.SUBTRACTOR) {
            useSubtractorOn(meteor);
        } else {
            useDivisorOn(meteor);
        }
    }

    private void fireBeamAt(MeteorActor meteor) {
        audio.play(AudioBankV2.SHOT, 0.54f);
        final Line beam = new Line(400, 150, meteor.getX(), meteor.getY(), 3f,
                getVertexBufferObjectManager());
        if (inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.SUBTRACTOR) {
            beam.setColor(0.72f, 0.40f, 1f);
        } else if (inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.BOMB_ZERO) {
            beam.setColor(1f, 0.78f, 0.10f);
        } else {
            beam.setColor(0.22f, 0.90f, 1f);
        }
        gameScene.attachChild(beam);
        AlphaModifier alpha = new AlphaModifier(0.16f, 1f, 0f) {
            @Override
            public void onModifierFinished(IEntity item) {
                item.detachSelf();
                super.onModifierFinished(item);
            }
        };
        alpha.setAutoUnregisterWhenFinished(true);
        beam.registerEntityModifier(alpha);
    }

    private void useDivisorOn(MeteorActor meteor) {
        int divisor = inventory.getSelectedDivisor();
        int before = meteor.value;
        if (divisor > 1 && before % divisor == 0) {
            int after = before / divisor;
            score += GameRulesV2.scoreForDivision(before, divisor);
            audio.play(AudioBankV2.DIV_OK, 0.60f);
            spawnHitParticles(meteor.getX(), meteor.getY(), 0.25f, 1f, 0.35f, 7);
            showFloatingText(before + " / " + divisor + " = " + after,
                    meteor.getX(), meteor.getY() + 30, 0.35f, 1f, 0.45f);
            if (after <= 1) {
                destroyDangerousMeteor(meteor, true, false);
            } else {
                meteor.setNumericValue(after);
            }
        } else {
            score = Math.max(0, score - 2);
            audio.play(AudioBankV2.DIV_BAD, 0.55f);
            meteor.flashWrong();
            showFloatingText(divisor + " nao divide " + before,
                    meteor.getX(), meteor.getY() + 26, 1f, 0.25f, 0.20f);
        }
        updateHud();
    }

    private void useSubtractorOn(MeteorActor meteor) {
        int amount = inventory.getSubtractorValue();
        if (amount <= 0 || amount > inventory.getSubtractorCharge() || amount > meteor.value) {
            audio.play(AudioBankV2.DIV_BAD, 0.45f);
            showFloatingText("Carga insuficiente", meteor.getX(), meteor.getY() + 25,
                    1f, 0.30f, 0.25f);
            return;
        }
        if (!inventory.useSubtractor()) {
            return;
        }
        int before = meteor.value;
        int after = before - amount;
        audio.play(AudioBankV2.SUBTRACT, 0.62f);
        spawnHitParticles(meteor.getX(), meteor.getY(), 0.75f, 0.35f, 1f, 8);
        showFloatingText(before + " - " + amount + " = " + after,
                meteor.getX(), meteor.getY() + 28, 0.82f, 0.50f, 1f);
        if (after <= 1) {
            destroyDangerousMeteor(meteor, true, false);
        } else {
            meteor.setNumericValue(after);
        }
        updateHud();
    }

    private void useBombZero() {
        if (!inventory.useBombZero()) {
            return;
        }
        audio.play(AudioBankV2.BOMB_ZERO, 0.82f);
        applyCityDamage(8f, "Dano colateral da bomba 0");
        showFloatingText("MULTIPLICACAO POR ZERO!", 400, 280, 1f, 0.78f, 0.12f);

        List<MeteorActor> copy = new ArrayList<MeteorActor>(meteors);
        for (MeteorActor meteor : copy) {
            if (meteor.active && !meteor.isBonus()) {
                spawnExplosion(meteor.getX(), meteor.getY(), true);
                destroyDangerousMeteor(meteor, false, true);
            }
        }
        updateHud();
    }

    private void collectBonus(MeteorActor meteor) {
        switch (meteor.type) {
            case BONUS_AMMO:
                if (inventory.unlockPrime(meteor.bonusValue)) {
                    audio.play(AudioBankV2.AMMO_UNLOCK, 0.72f);
                    showFloatingText("MUNICAO " + meteor.bonusValue + " DESBLOQUEADA",
                            meteor.getX(), meteor.getY() + 30, 0.35f, 0.88f, 1f);
                } else {
                    inventory.addMoney(8);
                    audio.play(AudioBankV2.BONUS_AMMO, 0.55f);
                }
                break;
            case BONUS_SUBTRACTOR:
                inventory.addSubtractorCharge(Math.max(10, meteor.bonusValue));
                audio.play(AudioBankV2.BONUS_SUB, 0.65f);
                showFloatingText("SUBTRATOR +10", meteor.getX(), meteor.getY() + 25,
                        0.78f, 0.45f, 1f);
                break;
            case BONUS_MONEY:
                inventory.addMoney(Math.max(25, meteor.bonusValue));
                audio.play(AudioBankV2.BONUS_MONEY, 0.65f);
                showFloatingText("RECURSOS +25", meteor.getX(), meteor.getY() + 25,
                        1f, 0.85f, 0.20f);
                break;
            case BONUS_HEALTH:
                healCity(Math.max(10, meteor.bonusValue));
                audio.play(AudioBankV2.BONUS_HEALTH, 0.62f);
                showFloatingText("REPARO +10", meteor.getX(), meteor.getY() + 25,
                        0.30f, 1f, 0.35f);
                break;
            case BONUS_SHIELD:
                inventory.addShieldCharge();
                audio.play(AudioBankV2.BONUS_SHIELD, 0.62f);
                showFloatingText("ESCUDO +1", meteor.getX(), meteor.getY() + 25,
                        0.45f, 0.85f, 1f);
                break;
            case BONUS_BOMB_ZERO:
                inventory.addBombZero(1);
                audio.play(AudioBankV2.BONUS_AMMO, 0.62f);
                showFloatingText("BOMBA 0 +1", meteor.getX(), meteor.getY() + 25,
                        1f, 0.75f, 0.18f);
                break;
            default:
                return;
        }
        score += 5;
        removeMeteor(meteor);
        updateHud();
    }

    private void openQuiz(final MeteorActor meteor) {
        if (quizScene != null || strikeInProgress) return;

        quizMeteor = meteor;
        quizData = meteor.quizData;
        quizAttemptsLeft = 2;
        audio.play(AudioBankV2.QUIZ_OPEN, 0.60f);

        quizScene = new Scene();
        quizScene.setBackgroundEnabled(false);
        final Rectangle dim = new Rectangle(400, 240, 800, 480, getVertexBufferObjectManager());
        dim.setColor(0.01f, 0.02f, 0.04f);
        dim.setAlpha(0.76f);
        quizScene.attachChild(dim);

        Text title = new Text(400, 340, fontMedium,
                quizData.multiplication ? "DESAFIO DE MULTIPLICACAO" : "DESAFIO DE ADICAO",
                80, getVertexBufferObjectManager());
        title.setColor(quizData.multiplication ? 0.45f : 1f,
                quizData.multiplication ? 1f : 0.88f,
                quizData.multiplication ? 0.48f : 0.20f);
        quizScene.attachChild(title);

        Text expression = new Text(400, 285, fontLarge, quizData.expression() + " = ?", 50,
                getVertexBufferObjectManager());
        expression.setScale(0.90f);
        quizScene.attachChild(expression);

        for (int i = 0; i < 3; i++) {
            HudButton answer = new HudButton(245 + i * 155, 205, 120, 58,
                    String.valueOf(quizData.answers[i]), ID_QUIZ_BASE + i, quizScene);
            answer.setColor(0.08f, 0.28f, 0.39f);
            quizScene.attachChild(answer);
            quizScene.registerTouchArea(answer);
        }

        quizFeedback = new Text(400, 137, fontSmall, "2 tentativas", 80,
                getVertexBufferObjectManager());
        quizFeedback.setColor(0.75f, 0.90f, 1f);
        quizScene.attachChild(quizFeedback);

        gameScene.setChildScene(quizScene, true, true, true);
    }

    private void answerQuiz(int answerIndex) {
        if (quizScene == null || quizData == null || quizMeteor == null || !quizMeteor.active) {
            return;
        }
        int answer = quizData.answers[Math.max(0, Math.min(2, answerIndex))];
        if (answer == quizData.result) {
            audio.play(AudioBankV2.QUIZ_OK, 0.72f);
            gameScene.clearChildScene();
            quizScene = null;
            beginMilitaryStrike(quizMeteor);
            return;
        }

        quizAttemptsLeft--;
        audio.play(AudioBankV2.QUIZ_BAD, 0.65f);
        if (quizAttemptsLeft > 0) {
            quizFeedback.setText("Errado. Mais 1 tentativa.");
            quizFeedback.setColor(1f, 0.42f, 0.28f);
            return;
        }

        final MeteorActor failed = quizMeteor;
        final int result = quizData.result;
        gameScene.clearChildScene();
        quizScene = null;
        quizMeteor = null;
        quizData = null;
        failed.convertQuizToNormal(result);
        showFloatingText("Resultado: " + result + " - agora divida!",
                failed.getX(), failed.getY() + 30, 1f, 0.58f, 0.20f);
    }

    private void beginMilitaryStrike(final MeteorActor target) {
        if (target == null || !target.active) return;
        strikeInProgress = true;
        quizMeteor = null;
        quizData = null;
        audio.play(AudioBankV2.TARGET_LOCK, 0.65f);

        final Sprite targetSprite = new Sprite(target.getX(), target.getY(), quizTargetRegion,
                getVertexBufferObjectManager());
        targetSprite.setScale(1.55f);
        targetSprite.registerEntityModifier(new LoopEntityModifier(new SequenceEntityModifier(
                new AlphaModifier(0.12f, 1f, 0.12f),
                new AlphaModifier(0.12f, 0.12f, 1f))));
        gameScene.attachChild(targetSprite);

        gameScene.registerUpdateHandler(new TimerHandler(0.55f, false, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                final Sprite plane = new Sprite(-70, Math.min(330, target.getY() + 72),
                        militaryPlaneRegion, getVertexBufferObjectManager());
                plane.setScale(1.0f);
                gameScene.attachChild(plane);
                audio.play(AudioBankV2.MILITARY_FLYBY, 0.55f);
                MoveXModifier fly = new MoveXModifier(2.25f, -70, 870) {
                    @Override
                    public void onModifierFinished(IEntity item) {
                        item.detachSelf();
                        super.onModifierFinished(item);
                    }
                };
                fly.setAutoUnregisterWhenFinished(true);
                plane.registerEntityModifier(fly);
            }
        }));

        gameScene.registerUpdateHandler(new TimerHandler(1.35f, false, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                if (target.active) {
                    audio.play(AudioBankV2.MILITARY_BOMB, 0.80f);
                    spawnExplosion(target.getX(), target.getY(), true);
                    float bx = target.getX();
                    float by = target.getY();
                    destroyDangerousMeteor(target, true, true);
                    spawnRandomBonus(bx, Math.max(300, by), true);
                }
                targetSprite.clearEntityModifiers();
                targetSprite.detachSelf();
                strikeInProgress = false;
                updateHud();
            }
        }));
    }

    private void destroyDangerousMeteor(MeteorActor meteor, boolean giveScore, boolean suppressRandomBonus) {
        if (meteor == null || !meteor.active) return;
        float x = meteor.getX();
        float y = meteor.getY();
        int original = meteor.originalValue;
        removeMeteor(meteor);
        waves.markDestroyed();

        if (giveScore) {
            score += GameRulesV2.scoreForDestroyedMeteor(original, waves.getWave());
        }
        audio.play(AudioBankV2.EXPLOSION, 0.55f);
        spawnExplosion(x, y, false);

        if (!suppressRandomBonus) {
            int nextPrime = GameRulesV2.nextEligibleLockedPrime(inventory, waves.getWave());
            boolean forceAmmo = nextPrime > 0 && waves.getTotalDestroyed() % 5 == 0;
            if (forceAmmo || random.nextFloat() < GameRulesV2.randomBonusChance(cityHealth, waves.getWave())) {
                spawnRandomBonus(x, Math.max(300, y), forceAmmo);
            }
        }
        updateHud();
    }

    private void onMeteorReachedCity(MeteorActor meteor) {
        if (meteor == null || !meteor.active) return;

        if (meteor.isBonus()) {
            removeMeteor(meteor);
            audio.play(AudioBankV2.CITY_HIT, 0.38f);
            applyCityDamage(3f, "Bonus perdido: impacto na cidade");
            return;
        }

        int impactValue = meteor.getImpactValue();
        removeMeteor(meteor);
        waves.markLost();

        if (inventory.consumeShieldCharge()) {
            audio.play(AudioBankV2.BONUS_SHIELD, 0.62f);
            showFloatingText("ESCUDO ABSORVEU O IMPACTO", 400, 160, 0.45f, 0.85f, 1f);
        } else {
            audio.play(AudioBankV2.CITY_HIT, 0.72f);
            applyCityDamage(GameRulesV2.cityDamage(impactValue, waves.getWave()), "Cidade atingida");
        }
        updateHud();
    }

    private void removeMeteor(MeteorActor meteor) {
        if (meteor == null || !meteor.active) return;
        meteor.active = false;
        meteor.clearEntityModifiers();
        gameScene.unregisterTouchArea(meteor);
        meteors.remove(meteor);
        if (meteor.hasParent()) meteor.detachSelf();
    }

    private int activeDangerousMeteorCount() {
        int count = 0;
        for (MeteorActor meteor : meteors) {
            if (meteor.active && !meteor.isBonus()) count++;
        }
        return count;
    }

    private void checkWaveCompletion() {
        if (!gameplayActive || warningActive || strikeInProgress || quizScene != null || waveTransitionPending) {
            return;
        }
        if (!waves.allScheduled() || activeDangerousMeteorCount() > 0) {
            return;
        }

        waveTransitionPending = true;
        gameplayActive = false;
        audio.play(AudioBankV2.WAVE_DONE, 0.72f);
        score += 20 + waves.getWave() * 5;
        showFloatingText("ONDA " + waves.getWave() + " CONCLUIDA", 400, 285,
                0.38f, 1f, 0.55f);
        updateHud();

        gameScene.registerUpdateHandler(new TimerHandler(2.2f, false, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler handler) {
                waves.nextWave();
                inventory.adjustSubtractorValue(0, waves.getMaxMeteorValue());
                beginWaveWarning();
            }
        }));
    }

    private void spawnCommercialPlane() {
        if (commercialPlane != null) return;
        float y = 210 + random.nextFloat() * 95;
        commercialPlane = new CommercialPlaneActor(-70, y);
        gameScene.attachChild(commercialPlane);
        audio.play(AudioBankV2.COMMERCIAL_FLYBY, 0.36f);
        showFloatingText("TRAFEGO CIVIL - PROTEJA O AVIAO", 400, 330,
                0.75f, 0.90f, 1f);
    }

    private void onCommercialPlaneSafe(CommercialPlaneActor plane) {
        if (plane != commercialPlane) return;
        score += 20;
        inventory.addMoney(3);
        plane.active = false;
        if (plane.hasParent()) plane.detachSelf();
        commercialPlane = null;
        showFloatingText("AVIAO EM SEGURANCA +20", 400, 300, 0.45f, 1f, 0.65f);
        updateHud();
    }

    private void checkCommercialPlaneCollision(CommercialPlaneActor plane) {
        if (plane == null || !plane.active || isWorldPaused()) return;
        List<MeteorActor> copy = new ArrayList<MeteorActor>(meteors);
        for (MeteorActor meteor : copy) {
            if (meteor.active && !meteor.isBonus() && plane.collidesWith(meteor)) {
                audio.play(AudioBankV2.COMMERCIAL_HIT, 0.78f);
                spawnExplosion(plane.getX(), plane.getY(), true);
                plane.active = false;
                if (plane.hasParent()) plane.detachSelf();
                commercialPlane = null;
                removeMeteor(meteor);
                waves.markLost();
                score = Math.max(0, score - 40);
                applyCityDamage(4f, "Aviao civil atingido");
                showFloatingText("AVIAO ATINGIDO -40", 400, 300, 1f, 0.28f, 0.20f);
                updateHud();
                return;
            }
        }
    }

    private void applyCityDamage(float amount, String reason) {
        if (amount <= 0 || cityHealth <= 0) return;
        cityHealth = Math.max(0f, cityHealth - amount);
        Log.d("AsteroideV2", reason + ": -" + amount);
        updateCityVisuals();
        if (cityHealth <= 0) {
            gameOver();
        }
    }

    private void healCity(float amount) {
        cityHealth = Math.min(100f, cityHealth + Math.max(0, amount));
        updateCityVisuals();
    }

    private void updateCityVisuals() {
        int visibleFires = cityHealth <= 25 ? 3 : cityHealth <= 50 ? 2 : cityHealth <= 75 ? 1 : 0;
        for (int i = 0; i < fireSprites.size(); i++) {
            fireSprites.get(i).setVisible(i < visibleFires);
        }
        updateHud();
    }

    private void repairCityWithMoney() {
        if (cityHealth >= 100f) {
            showFloatingText("Cidade ja esta reparada", 680, 350, 0.65f, 0.90f, 1f);
            return;
        }
        if (!inventory.spendMoney(25)) {
            showFloatingText("Sao necessarios $25", 680, 350, 1f, 0.55f, 0.22f);
            return;
        }
        healCity(22f);
        audio.play(AudioBankV2.REPAIR, 0.68f);
        showFloatingText("RECONSTRUCAO +22", 680, 350, 0.35f, 1f, 0.45f);
        updateHud();
    }

    private void gameOver() {
        gameplayActive = false;
        warningActive = false;
        audio.play(AudioBankV2.GAME_OVER, 0.72f);

        final Scene over = new Scene();
        over.setBackgroundEnabled(false);
        Rectangle dim = new Rectangle(400, 240, 800, 480, getVertexBufferObjectManager());
        dim.setColor(0.03f, 0.01f, 0.01f);
        dim.setAlpha(0.84f);
        over.attachChild(dim);

        Text title = new Text(400, 315, fontLarge, "CIDADE DESTRUIDA", 50, getVertexBufferObjectManager());
        title.setColor(1f, 0.24f, 0.18f);
        title.setScale(0.80f);
        over.attachChild(title);

        Text summary = new Text(400, 255, fontMedium,
                "Pontos: " + score + "   Onda: " + waves.getWave(), 80, getVertexBufferObjectManager());
        summary.setScale(0.80f);
        over.attachChild(summary);

        HudButton restart = new HudButton(330, 175, 170, 52, "TENTAR DE NOVO", ID_RESTART, over);
        HudButton menu = new HudButton(510, 175, 150, 52, "MENU", ID_GAMEOVER_MENU, over);
        restart.setColor(0.07f, 0.31f, 0.22f);
        menu.setColor(0.22f, 0.20f, 0.20f);
        over.attachChild(restart);
        over.attachChild(menu);
        over.registerTouchArea(restart);
        over.registerTouchArea(menu);
        gameScene.setChildScene(over, true, true, true);
    }

    private void onHudButton(int id) {
        audio.play(AudioBankV2.UI_CLICK, 0.28f);

        if (id == 700) {
            startNewGame();
            return;
        }
        if (id >= ID_QUIZ_BASE && id < ID_QUIZ_BASE + 3) {
            answerQuiz(id - ID_QUIZ_BASE);
            return;
        }
        if (id == ID_RESTART) {
            gameScene.clearChildScene();
            startNewGame();
            return;
        }
        if (id == ID_GAMEOVER_MENU || id == ID_MENU) {
            audio.stop(AudioBankV2.SIREN);
            showMenu();
            return;
        }

        if (id >= 0 && id < GameRulesV2.PRIME_AMMO.length) {
            inventory.selectDivisor(GameRulesV2.PRIME_AMMO[id]);
            updateHud();
            return;
        }

        switch (id) {
            case ID_SUBTRACTOR:
                inventory.selectSubtractor();
                break;
            case ID_SUB_MINUS:
                inventory.adjustSubtractorValue(-1, waves.getMaxMeteorValue());
                break;
            case ID_SUB_PLUS:
                inventory.adjustSubtractorValue(1, waves.getMaxMeteorValue());
                break;
            case ID_BOMB_ZERO:
                inventory.selectBombZero();
                break;
            case ID_REPAIR:
                repairCityWithMoney();
                break;
            default:
                break;
        }
        updateHud();
    }

    private void updateHud() {
        if (gameScene == null || healthText == null) return;

        for (int prime : GameRulesV2.PRIME_AMMO) {
            HudButton button = ammoButtons.get(prime);
            if (button == null) continue;
            boolean unlocked = inventory.isPrimeUnlocked(prime);
            button.setEnabled(unlocked);
            button.setLabel(unlocked ? String.valueOf(prime) : "?");
            button.setSelected(unlocked && inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.DIVISOR
                    && inventory.getSelectedDivisor() == prime);
        }

        boolean hasSub = inventory.getSubtractorCharge() > 0;
        subButton.setEnabled(hasSub);
        subButton.setSelected(hasSub && inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.SUBTRACTOR);
        subStatusText.setText("SUB " + inventory.getSubtractorValue() + "/" + inventory.getSubtractorCharge());

        bombButton.setEnabled(inventory.getBombZeroCount() > 0);
        bombButton.setSelected(inventory.getWeaponMode() == PlayerInventoryV2.WeaponMode.BOMB_ZERO);
        bombButton.setLabel("0 x" + inventory.getBombZeroCount());

        repairButton.setLabel("$" + inventory.getMoney());
        repairButton.setEnabled(inventory.getMoney() >= 25 && cityHealth < 100f);

        scoreText.setText("PONTOS " + score);
        waveText.setText("ONDA " + waves.getWave() + "  MAX " + waves.getMaxMeteorValue());
        shieldText.setText("ESCUDO " + inventory.getShieldCharges());
        healthText.setText("CIDADE " + Math.round(cityHealth) + "%");

        healthBarFill.setWidth(180f * cityHealth / 100f);
        if (cityHealth > 60) healthBarFill.setColor(0.15f, 0.95f, 0.22f);
        else if (cityHealth > 30) healthBarFill.setColor(1f, 0.72f, 0.08f);
        else healthBarFill.setColor(1f, 0.10f, 0.06f);

        waveBarFill.setWidth(155f * waves.getProgress());

        switch (inventory.getWeaponMode()) {
            case SUBTRACTOR:
                selectedWeaponText.setText("ARMA SUB " + inventory.getSubtractorValue());
                selectedWeaponText.setColor(0.78f, 0.48f, 1f);
                break;
            case BOMB_ZERO:
                selectedWeaponText.setText("ARMA x0");
                selectedWeaponText.setColor(1f, 0.80f, 0.18f);
                break;
            default:
                selectedWeaponText.setText("ARMA /" + inventory.getSelectedDivisor());
                selectedWeaponText.setColor(0.65f, 0.92f, 1f);
                break;
        }
    }

    private void spawnHitParticles(float x, float y, float r, float g, float b, int count) {
        for (int i = 0; i < count; i++) {
            final Rectangle particle = new Rectangle(x, y, 4 + random.nextInt(4), 4 + random.nextInt(4),
                    getVertexBufferObjectManager());
            particle.setColor(r, g, b);
            gameScene.attachChild(particle);
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float dist = 18 + random.nextFloat() * 35;
            float tx = x + (float) Math.cos(angle) * dist;
            float ty = y + (float) Math.sin(angle) * dist;
            ParallelEntityModifier modifier = new ParallelEntityModifier(
                    new MoveModifier(0.38f, x, tx, y, ty),
                    new AlphaModifier(0.38f, 1f, 0f),
                    new ScaleModifier(0.38f, 1f, 0.25f)) {
                @Override
                public void onModifierFinished(IEntity item) {
                    item.detachSelf();
                    super.onModifierFinished(item);
                }
            };
            modifier.setAutoUnregisterWhenFinished(true);
            particle.registerEntityModifier(modifier);
        }
    }

    private void spawnExplosion(float x, float y, boolean big) {
        spawnHitParticles(x, y, 1f, big ? 0.35f : 0.58f, 0.08f, big ? 18 : 11);
        if (big) {
            audio.play(AudioBankV2.BIG_EXPLOSION, 0.58f);
        }
    }

    private void showFloatingText(String value, float x, float y, float r, float g, float b) {
        if (gameScene == null) return;
        final Text text = new Text(x, y, fontSmall, value, 120, getVertexBufferObjectManager());
        text.setColor(r, g, b);
        text.setScale(0.88f);
        gameScene.attachChild(text);
        ParallelEntityModifier modifier = new ParallelEntityModifier(
                new MoveYModifier(0.95f, y, y + 46),
                new AlphaModifier(0.95f, 1f, 0f),
                new ScaleModifier(0.95f, 0.85f, 1.08f)) {
            @Override
            public void onModifierFinished(IEntity item) {
                item.detachSelf();
                super.onModifierFinished(item);
            }
        };
        modifier.setAutoUnregisterWhenFinished(true);
        text.registerEntityModifier(modifier);
    }

    private enum MeteorType {
        NORMAL,
        ADDITION,
        MULTIPLICATION,
        BONUS_AMMO,
        BONUS_SUBTRACTOR,
        BONUS_MONEY,
        BONUS_HEALTH,
        BONUS_SHIELD,
        BONUS_BOMB_ZERO
    }

    private final class MeteorActor extends AnimatedSprite {
        private MeteorType type;
        private int value;
        private final int originalValue;
        private final int bonusValue;
        private GameRulesV2.QuizData quizData;
        private final float speed;
        private final Text label;
        private boolean active = true;

        MeteorActor(float x, float y, MeteorType type, int value, GameRulesV2.QuizData quizData,
                    int bonusValue, float speed) {
            super(x, y, meteorRegion.deepCopy(), getVertexBufferObjectManager());
            this.type = type;
            this.value = value;
            this.originalValue = Math.max(2, value);
            this.quizData = quizData;
            this.bonusValue = bonusValue;
            this.speed = isBonusType(type) ? speed * 0.78f : speed;

            setCurrentTileIndex(random.nextInt(4));
            configureVisualType();

            Sprite tail = new Sprite(getWidth() / 2f, getHeight() + 10f, tailRegion,
                    getVertexBufferObjectManager());
            tail.setScale(0.32f);
            tail.setAlpha(isBonus() ? 0.12f : 0.22f);
            attachChild(tail);

            label = new Text(getWidth() / 2f, getHeight() / 2f, fontSmall, "000000000", 24,
                    getVertexBufferObjectManager());
            label.setScale(0.32f);
            attachChild(label);
            updateLabel();
        }

        private boolean isBonusType(MeteorType t) {
            return t == MeteorType.BONUS_AMMO || t == MeteorType.BONUS_SUBTRACTOR
                    || t == MeteorType.BONUS_MONEY || t == MeteorType.BONUS_HEALTH
                    || t == MeteorType.BONUS_SHIELD || t == MeteorType.BONUS_BOMB_ZERO;
        }

        private void configureVisualType() {
            if (type == MeteorType.MULTIPLICATION) {
                setColor(0.48f, 1f, 0.52f);
            } else if (type == MeteorType.ADDITION) {
                setColor(1f, 0.90f, 0.30f);
            } else if (type == MeteorType.BONUS_AMMO) {
                setColor(0.42f, 0.82f, 1f);
            } else if (type == MeteorType.BONUS_HEALTH) {
                setCurrentTileIndex(4);
            } else if (type == MeteorType.BONUS_SHIELD) {
                setCurrentTileIndex(5);
            } else if (type == MeteorType.BONUS_MONEY) {
                setCurrentTileIndex(6);
            } else if (type == MeteorType.BONUS_BOMB_ZERO) {
                setCurrentTileIndex(7);
            } else if (type == MeteorType.BONUS_SUBTRACTOR) {
                setColor(0.75f, 0.52f, 1f);
            }

            float scale;
            if (isBonus()) {
                scale = 2.4f;
            } else {
                float normalized = (float) Math.sqrt(Math.max(2, value) / 200f);
                scale = 2.6f + normalized * 1.8f;
            }
            setScale(scale);

            if (type == MeteorType.BONUS_MONEY) attachIcon(moneyRegion);
            else if (type == MeteorType.BONUS_SUBTRACTOR) attachIcon(subtractorRegion);
            else if (type == MeteorType.BONUS_BOMB_ZERO) attachIcon(bombZeroRegion);
        }

        private void attachIcon(TextureRegion region) {
            Sprite icon = new Sprite(getWidth() / 2f, getHeight() / 2f, region,
                    getVertexBufferObjectManager());
            icon.setScale(0.27f);
            attachChild(icon);
        }

        private void updateLabel() {
            if (type == MeteorType.MULTIPLICATION || type == MeteorType.ADDITION) {
                label.setText(quizData == null ? String.valueOf(value) : quizData.expression());
                label.setScale(0.27f);
            } else if (type == MeteorType.BONUS_AMMO) {
                label.setText("+" + bonusValue);
            } else if (type == MeteorType.BONUS_SUBTRACTOR) {
                label.setText("SUB");
            } else if (type == MeteorType.BONUS_MONEY) {
                label.setText("$25");
            } else if (type == MeteorType.BONUS_HEALTH) {
                label.setText("+10");
            } else if (type == MeteorType.BONUS_SHIELD) {
                label.setText("ESC");
            } else if (type == MeteorType.BONUS_BOMB_ZERO) {
                label.setText("x0");
            } else {
                label.setText(String.valueOf(value));
                label.setScale(0.32f);
            }
        }

        boolean isBonus() {
            return isBonusType(type);
        }

        boolean isQuizMeteor() {
            return type == MeteorType.ADDITION || type == MeteorType.MULTIPLICATION;
        }

        int getImpactValue() {
            return isQuizMeteor() && quizData != null ? quizData.result : Math.max(2, value);
        }

        void setNumericValue(int newValue) {
            value = Math.max(1, newValue);
            updateLabel();
            float normalized = (float) Math.sqrt(Math.max(2, value) / 200f);
            setScale(2.6f + normalized * 1.8f);
        }

        void convertQuizToNormal(int resolvedValue) {
            type = MeteorType.NORMAL;
            quizData = null;
            value = Math.max(2, resolvedValue);
            setColor(1f, 1f, 1f);
            setCurrentTileIndex(random.nextInt(4));
            setNumericValue(value);
        }

        void flashWrong() {
            registerEntityModifier(new SequenceEntityModifier(
                    new AlphaModifier(0.07f, 1f, 0.35f),
                    new AlphaModifier(0.12f, 0.35f, 1f)));
        }

        @Override
        public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
            if (event.isActionUp()) {
                onMeteorTapped(this);
                return true;
            }
            return true;
        }

        @Override
        protected void onManagedUpdate(float secondsElapsed) {
            if (active && gameplayActive && !warningActive && quizScene == null && !strikeInProgress) {
                setY(getY() - speed * secondsElapsed);
                if (getY() <= CITY_IMPACT_Y) {
                    onMeteorReachedCity(this);
                }
            }
            super.onManagedUpdate(secondsElapsed);
        }
    }

    private final class CommercialPlaneActor extends Sprite {
        private boolean active = true;
        private final float speed = 92f;

        CommercialPlaneActor(float x, float y) {
            super(x, y, commercialPlaneRegion, getVertexBufferObjectManager());
        }

        @Override
        protected void onManagedUpdate(float secondsElapsed) {
            if (active && gameplayActive && !isWorldPaused()) {
                setX(getX() + speed * secondsElapsed);
                checkCommercialPlaneCollision(this);
                if (active && getX() > CAMERA_WIDTH + 80) {
                    onCommercialPlaneSafe(this);
                }
            }
            super.onManagedUpdate(secondsElapsed);
        }
    }

    private final class HudButton extends Rectangle {
        private final int id;
        private final Text label;
        private boolean enabled = true;
        private boolean selected;
        private final float baseR;
        private final float baseG;
        private final float baseB;

        HudButton(float x, float y, float width, float height, String text, int id, Scene ownerScene) {
            super(x, y, width, height, getVertexBufferObjectManager());
            this.id = id;
            setColor(0.08f, 0.20f, 0.28f);
            setAlpha(0.92f);
            baseR = getRed();
            baseG = getGreen();
            baseB = getBlue();
            label = new Text(width / 2f, height / 2f, fontSmall, text, 32,
                    getVertexBufferObjectManager());
            label.setScale(0.76f);
            attachChild(label);
        }

        void setTextScale(float scale) {
            label.setScale(scale);
        }

        void setLabel(String text) {
            label.setText(text);
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            refreshVisual();
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            refreshVisual();
        }

        private void refreshVisual() {
            if (!enabled) {
                setAlpha(0.30f);
                setColor(0.10f, 0.12f, 0.14f);
            } else if (selected) {
                setAlpha(1f);
                setColor(0.08f, 0.58f, 0.72f);
            } else {
                setAlpha(0.92f);
                setColor(Math.max(0.05f, baseR), Math.max(0.08f, baseG), Math.max(0.10f, baseB));
            }
        }

        @Override
        public void setColor(float red, float green, float blue) {
            super.setColor(red, green, blue);
        }

        @Override
        public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
            if (event.isActionUp() && enabled) {
                registerEntityModifier(new SequenceEntityModifier(
                        new ScaleModifier(0.06f, getScaleX(), getScaleX() * 0.88f),
                        new ScaleModifier(0.08f, getScaleX() * 0.88f, getScaleX())));
                onHudButton(id);
                return true;
            }
            return true;
        }
    }
}
