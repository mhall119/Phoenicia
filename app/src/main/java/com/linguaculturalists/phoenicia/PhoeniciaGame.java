package com.linguaculturalists.phoenicia;

import android.content.res.AssetManager;
import android.view.MotionEvent;

import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.audio.sound.SoundManager;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.TextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.bitmap.AssetBitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.color.Color;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.util.debug.Debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.linguaculturalists.phoenicia.models.PlacedBlock;
import com.linguaculturalists.phoenicia.ui.BlockPlacementHUD;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;

/**
 * Created by mhall on 3/22/15.
 */
public class PhoeniciaGame {
    public GameActivity activity;
    private TextureManager textureManager;
    private AssetManager assetManager;
    private VertexBufferObjectManager vboManager;
    private SoundManager soundManager;
    public Scene scene;
    public Camera camera;

    private float mPinchZoomStartedCameraZoomFactor;
    private PinchZoomDetector mPinchZoomDetector;

    public AssetBitmapTexture terrainTexture;
    public ITiledTextureRegion terrainTiles;

    public Sprite[][] placedSprites;

    private TMXTiledMap mTMXTiledMap;

    private Sound[] blockSounds;

    public PhoeniciaGame(GameActivity activity, final ZoomCamera camera) {
        this.activity = activity;
        this.textureManager = activity.getTextureManager();
        this.assetManager = activity.getAssets();
        this.vboManager = activity.getVertexBufferObjectManager();
        this.soundManager = activity.getSoundManager();
        this.camera = camera;
        scene = new Scene();
        scene.setBackground(new Background(new Color(0, 0, 0)));

        mPinchZoomDetector = new PinchZoomDetector(new PinchZoomDetector.IPinchZoomDetectorListener() {
            @Override
            public void onPinchZoomStarted(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent) {
                mPinchZoomStartedCameraZoomFactor = camera.getZoomFactor();
            }

            @Override
            public void onPinchZoom(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent, final float pZoomFactor) {
                camera.setZoomFactor(mPinchZoomStartedCameraZoomFactor * pZoomFactor);
            }

            @Override
            public void onPinchZoomFinished(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent, final float pZoomFactor) {
                camera.setZoomFactor(mPinchZoomStartedCameraZoomFactor * pZoomFactor);
            }
        });
        scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            @Override
            public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
                mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);

                switch(pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        addBlock((int) pSceneTouchEvent.getX(), (int) pSceneTouchEvent.getY());
                        break;
                    case TouchEvent.ACTION_UP:
                        //MainActivity.this.mSmoothCamera.setZoomFactor(1.0f);
                        break;
                    case TouchEvent.ACTION_MOVE:
                        MotionEvent motion = pSceneTouchEvent.getMotionEvent();
                        if(motion.getHistorySize() > 0){
                            for(int i = 1, n = motion.getHistorySize(); i < n; i++){
                                int calcX = (int) motion.getHistoricalX(i) - (int) motion.getHistoricalX(i-1);
                                int calcY = (int) motion.getHistoricalY(i) - (int) motion.getHistoricalY(i-1);
                                //System.out.println("diffX: "+calcX+", diffY: "+calcY);

                                camera.setCenter(camera.getCenterX() - calcX, camera.getCenterY() + calcY);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

    }

    public void load() throws IOException {
        terrainTexture = new AssetBitmapTexture(textureManager, assetManager, "textures/terrain.png");
        terrainTexture.load();
        terrainTiles = TextureRegionFactory.extractTiledFromTexture(terrainTexture, 0, 0, 640, 1024, 10, 16);

        try {
            final TMXLoader tmxLoader = new TMXLoader(assetManager, textureManager, TextureOptions.BILINEAR_PREMULTIPLYALPHA, vboManager);
            this.mTMXTiledMap = tmxLoader.loadFromAsset("textures/map.tmx");
            for (TMXLayer tmxLayer : this.mTMXTiledMap.getTMXLayers()){
                scene.attachChild(tmxLayer);
            }
            placedSprites = new Sprite[this.mTMXTiledMap.getTileColumns()][this.mTMXTiledMap.getTileRows()];
        } catch (final TMXLoadException e) {
            Debug.e(e);
        }

        try {
            blockSounds = new Sound[26];
            char letters[] = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
            for (int i = 0; i < 26; i++) {
                System.out.println("Loading sound file "+i+": sounds/"+letters[i]+".ogg");
                blockSounds[i] = SoundFactory.createSoundFromAsset(this.soundManager, this.activity, "sounds/"+letters[i]+".ogg");
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        this.syncDB();

        System.out.println("Loading saved blocks");
        List<PlacedBlock> savedBlocks = PlacedBlock.objects(this.activity.getApplicationContext()).all().toList();
        for (int i = 0; i < savedBlocks.size(); i++) {
            PlacedBlock block = savedBlocks.get(i);
            System.out.println("Restoring tile "+block.tileId.get()+" at "+block.isoX.get()+"x"+block.isoY.get());
            this.setBlockAtIso(block.isoX.get(), block.isoY.get(), block.tileId.get());
        }
    }
    private void syncDB() {
        List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
        models.add(PlacedBlock.class);

        DatabaseAdapter.setDatabaseName("game_db");
        DatabaseAdapter adapter = new DatabaseAdapter(this.activity.getApplicationContext());
        adapter.setModels(models);
    }

    public void reset() {
        // Detach sprites
        for (int c = 0; c < placedSprites.length; c++) {
            for (int r = 0; r < placedSprites[c].length; r++) {
                if (placedSprites[c][r] != null) {
                    scene.detachChild(placedSprites[c][r]);
                    scene.unregisterTouchArea(placedSprites[c][r]);
                    placedSprites[c][r] = null;
                }
            }
        }
        // Delete DB records
        List<PlacedBlock> savedBlocks = PlacedBlock.objects(this.activity.getApplicationContext()).all().toList();
        for (int i = 0; i < savedBlocks.size(); i++) {
            savedBlocks.get(i).delete(this.activity.getApplicationContext());
        }
    }
    public void start(Camera camera) {
        camera.setCenter(400, -400);
        camera.setHUD(this.getBlockPlacementHUD());
    }
    public HUD getBlockPlacementHUD() {
        BlockPlacementHUD.init(this);
        return BlockPlacementHUD.getInstance();
    }

    public void addBlock(int x, int y) {
        TMXTile blockTile = this.placeBlock(x, y, BlockPlacementHUD.getPlaceBlock());
        if (blockTile != null) {
            PlacedBlock newBlock = new PlacedBlock();
            newBlock.isoX.set(blockTile.getTileColumn());
            newBlock.isoY.set(blockTile.getTileRow());
            newBlock.tileId.set(BlockPlacementHUD.getPlaceBlock());
            System.out.println("Saving block "+newBlock.tileId.get()+" at "+newBlock.isoX.get()+"x"+newBlock.isoY.get());
            if (newBlock.save(this.activity.getApplicationContext())) {
                System.out.println("Save successful");
            }
        }
    }
    public TMXTile setBlockAtIso(int tileColumn, int tileRow, final int placeBlock) {
        final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(1);
        final TMXTile tmxTile = tmxLayer.getTMXTile(tileColumn, tileRow);
        if (tmxTile == null) {
            System.out.println("Can't place blocks outside of map");
            return null;
        }
        int tileX = (int)tmxTile.getTileX();// tiles are 64px wide, assume the touch is targeting the middle
        int tileY = (int)tmxTile.getTileY();// tiles are 64px wide, assume the touch is targeting the middle
        int tileZ = tmxTile.getTileZ();

        ITextureRegion blockRegion = terrainTiles.getTextureRegion(placeBlock);
        ButtonSprite block = new ButtonSprite(tileX, tileY, blockRegion, vboManager);
        block.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                System.out.println("Clicked block: "+placeBlock);
                playBlockSound(placeBlock);
            }
        });
        block.setZIndex(tileZ);
        placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] = block;
        scene.registerTouchArea(block);
        scene.attachChild(block);

        scene.sortChildren();
        return tmxTile;
    }
    public TMXTile placeBlock(int x, int y, final int placeBlock) {
        if (placeBlock < 0) {
            System.out.println("No active block to place");
            return null;
        }
        final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(1);
        final TMXTile tmxTile = tmxLayer.getTMXTileAt(x, y);
        if (tmxTile == null) {
            System.out.println("Can't place blocks outside of map");
            return null;
        }
        int tileX = (int)tmxTile.getTileX();// tiles are 64px wide, assume the touch is targeting the middle
        int tileY = (int)tmxTile.getTileY();// tiles are 64px wide, assume the touch is targeting the middle
        int tileZ = tmxTile.getTileZ();

        if (placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] != null) {
            scene.detachChild(placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()]);
            scene.unregisterTouchArea(placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()]);
            placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] = null;
        }

        System.out.println("  px-x: "+x);
        System.out.println("  px-y: "+y);
        System.out.println("tile-x: "+tmxTile.getTileColumn());
        System.out.println("tile-y: "+tmxTile.getTileRow());
        System.out.println("tile-z: "+tileZ);

        ITextureRegion blockRegion = terrainTiles.getTextureRegion(placeBlock);
        ButtonSprite block = new ButtonSprite(tileX, tileY, blockRegion, vboManager);
        block.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                System.out.println("Clicked block: "+placeBlock);
                playBlockSound(placeBlock);
            }
        });
        block.setZIndex(tileZ);
        placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] = block;
        scene.registerTouchArea(block);
        scene.attachChild(block);

        scene.sortChildren();
        return tmxTile;
    }

    void playBlockSound(int blockId) {

        System.out.println("Playing sound: "+blockId);
        int soundId = blockId-130;//Letters start at tile 130
        if (soundId < 0 || soundId > 25) {
            System.out.println("Sound out of range: "+soundId);
            return;
        }

        if (this.blockSounds[soundId] != null) {
            this.blockSounds[soundId].play();
        } else {
            System.out.println("No blockSounds["+soundId+"]!");
        }
    }
}
