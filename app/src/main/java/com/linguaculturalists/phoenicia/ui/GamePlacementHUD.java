package com.linguaculturalists.phoenicia.ui;

import android.graphics.Typeface;

import com.linguaculturalists.phoenicia.GameActivity;
import com.linguaculturalists.phoenicia.PhoeniciaGame;
import com.linguaculturalists.phoenicia.components.Dialog;
import com.linguaculturalists.phoenicia.components.Scrollable;
import com.linguaculturalists.phoenicia.locale.Level;
import com.linguaculturalists.phoenicia.locale.Game;
import com.linguaculturalists.phoenicia.models.Bank;
import com.linguaculturalists.phoenicia.models.GameTile;
import com.linguaculturalists.phoenicia.models.GameTileBuilder;
import com.linguaculturalists.phoenicia.models.WordTile;
import com.linguaculturalists.phoenicia.models.WordTileBuilder;
import com.linguaculturalists.phoenicia.util.GameFonts;
import com.linguaculturalists.phoenicia.util.PhoeniciaContext;

import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.text.AutoWrap;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.util.adt.align.HorizontalAlign;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.ease.EaseBackOut;

import java.util.List;

/**
 * HUD for selecting \link Game Games \endlink to be placed as tiles onto the map.
 */
public class GamePlacementHUD extends PhoeniciaHUD implements Bank.BankUpdateListener {
    private PhoeniciaGame phoeniciaGame; /**< Reference to the current PhoeniciaGame */

    private Rectangle whiteRect; /**< Background of this HUD */
    private Scrollable blockPanel; /**< Scrollpane containing the game icons */

    /**
     * A HUD which allows the selection of new phoeniciaGame blocks to be placed on the map
     *
     * @param phoeniciaGame Refernece to the current PhoeniciaGame the HUD is running in
     */
    public GamePlacementHUD(final PhoeniciaGame phoeniciaGame) {
        super(phoeniciaGame.camera);
        this.setBackgroundEnabled(false);
        this.setOnAreaTouchTraversalFrontToBack();
        Bank.getInstance().addUpdateListener(this);
        this.phoeniciaGame = phoeniciaGame;

        this.whiteRect = new Rectangle(GameActivity.CAMERA_WIDTH/2, 64, 600, 96, PhoeniciaContext.vboManager);
        whiteRect.setColor(Color.WHITE);
        this.attachChild(whiteRect);

        this.blockPanel = new Scrollable(GameActivity.CAMERA_WIDTH/2, 64, 600, 96, Scrollable.SCROLL_HORIZONTAL);
        this.blockPanel.setPadding(16);

        this.registerTouchArea(blockPanel);
        this.registerTouchArea(blockPanel.contents);
        this.attachChild(blockPanel);

        final Font inventoryCountFont = FontFactory.create(PhoeniciaContext.fontManager, PhoeniciaContext.textureManager, 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 16, Color.RED_ARGB_PACKED_INT);
        inventoryCountFont.load();
        final List<Game> games = phoeniciaGame.locale.games;
        final int tile_start = 130;
        final int startX = (int)(blockPanel.getWidth()/2);
        for (int i = 0; i < games.size(); i++) {
            final Game currentGame = games.get(i);
            Debug.d("Adding HUD phoeniciaGame: " + currentGame.name);
            ITiledTextureRegion blockRegion = phoeniciaGame.gameSprites.get(currentGame);
            ButtonSprite block = new ButtonSprite((64 * ((i * 2)+1)), 48, blockRegion, PhoeniciaContext.vboManager);
            block.setOnClickListener(new ButtonSprite.OnClickListener() {
                @Override
                public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                    float[] cameraCenter = getCamera().getSceneCoordinatesFromCameraSceneCoordinates(GameActivity.CAMERA_WIDTH / 2, GameActivity.CAMERA_HEIGHT / 2);
                    TMXTile mapTile = phoeniciaGame.getTileAt(cameraCenter[0], cameraCenter[1]);
                    addGameTile(currentGame, mapTile);
                }
            });
            if (phoeniciaGame.session.points.get() < currentGame.points) {
                block.setEnabled(false);
            }
            this.registerTouchArea(block);
            blockPanel.attachChild(block);

            final Text purchaseCost = new Text((64 * ((i * 2)+1))+24, 20, inventoryCountFont, ""+currentGame.buy, 4, PhoeniciaContext.vboManager);
            blockPanel.attachChild(purchaseCost);
        }
        Debug.d("Finished loading HUD letters");

        Debug.d("Finished instantiating BlockPlacementHUD");

    }

    /**
     * Animate the bottom panel sliding up into view.
     */
    @Override
    public void show() {
        whiteRect.registerEntityModifier(new MoveYModifier(0.5f, -48, 64, EaseBackOut.getInstance()));
        blockPanel.registerEntityModifier(new MoveYModifier(0.5f, -48, 64, EaseBackOut.getInstance()));
    }

    /**
     * Handles changes to the player's account balance by enablind or disabling game options
     * @param new_balance The player's new account balance
     */
    @Override
    public void onBankAccountUpdated(int new_balance) {
        // TODO: Enable/disable words based on available balance
    }

    /**
     * Capture scene touch events and allow them to pass through if not handled by anything in this HUD
     * @param pSceneTouchEvent
     * @return
     */
    @Override
    public boolean onSceneTouchEvent(final TouchEvent pSceneTouchEvent) {

        final boolean handled = super.onSceneTouchEvent(pSceneTouchEvent);
        if (handled) return true;

        return false;
    }

    /**
     * Create a new WordTile (with Sprite and Builder).
     * @param game Word to create the tile for
     * @param onTile Map tile to place the new tile on
     */
    private void addGameTile(final Game game, final TMXTile onTile) {
        if (phoeniciaGame.session.account_balance.get() < game.buy) {
            Dialog lowBalanceDialog = new Dialog(500, 300, Dialog.Buttons.OK, PhoeniciaContext.vboManager, new Dialog.DialogListener() {
                @Override
                public void onDialogButtonClicked(Dialog dialog, Dialog.DialogButton dialogButton) {
                    dialog.close();
                    unregisterTouchArea(dialog);
                }
            });
            int difference = game.buy - phoeniciaGame.session.account_balance.get();
            Text confirmText = new Text(lowBalanceDialog.getWidth()/2, lowBalanceDialog.getHeight()-48, GameFonts.dialogText(), "You need "+difference+" more coins", 30,  new TextOptions(AutoWrap.WORDS, lowBalanceDialog.getWidth()*0.8f, HorizontalAlign.CENTER), PhoeniciaContext.vboManager);
            lowBalanceDialog.attachChild(confirmText);

            lowBalanceDialog.open(this);
            this.registerTouchArea(lowBalanceDialog);
            return;
        }
        Debug.d("Placing game "+game.name+" at "+onTile.getTileColumn()+"x"+onTile.getTileRow());
        final GameTile gameTile = new GameTile(this.phoeniciaGame, game);

        gameTile.isoX.set(onTile.getTileColumn());
        gameTile.isoY.set(onTile.getTileRow());

        phoeniciaGame.createGameSprite(gameTile, new PhoeniciaGame.CreateGameSpriteCallback() {
            @Override
            public void onGameSpriteCreated(GameTile tile) {
                GameTileBuilder builder = new GameTileBuilder(phoeniciaGame.session, gameTile, gameTile.item_name.get(), game.construct);
                builder.start();
                builder.save(PhoeniciaContext.context);
                phoeniciaGame.addBuilder(builder);

                gameTile.setBuilder(builder);
                gameTile.save(PhoeniciaContext.context);
                gameTile.restart(PhoeniciaContext.context);
                Bank.getInstance().debit(game.buy);
            }

            @Override
            public void onGameSpriteCreationFailed(GameTile tile) {
                Debug.d("Failed to create game sprite");
            }
        });

    }

}