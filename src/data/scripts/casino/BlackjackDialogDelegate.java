package data.scripts.casino;

import java.util.Map;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import data.scripts.casino.interaction.BlackjackHandler;
import data.scripts.casino.shared.BaseGameDelegate;

public class BlackjackDialogDelegate extends BaseGameDelegate {

    protected BlackjackPanelUI blackjackPanel;
    protected BlackjackGame game;
    protected BlackjackHandler handler;

    protected BlackjackPanelUI.BlackjackActionCallback actionCallback;

    protected BlackjackGame.Action pendingAction = null;
    protected boolean pendingNewHand = false;
    protected boolean pendingLeave = false;

    public BlackjackDialogDelegate(BlackjackGame game, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap, Runnable onDismissCallback, BlackjackHandler handler) {
        super(dialog, memoryMap, onDismissCallback);
        this.game = game;
        this.handler = handler;

        actionCallback = new BlackjackPanelUI.BlackjackActionCallback() {
            @Override
            public void onPlayerAction(BlackjackGame.Action action) {
                handlePlayerAction(action);
            }

            @Override
            public void onNewHand() {
                handleNewHand();
            }

            @Override
            public void onLeave() {
                handleLeave();
            }

            @Override
            public void onHowToPlay() {
                handleHowToPlay();
            }

            @Override
            public void onPlaceBet(int amount) {
                handlePlaceBet(amount);
            }
        };

        blackjackPanel = new BlackjackPanelUI(game, actionCallback);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return blackjackPanel;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        super.init(panel, callbacks);

        if (blackjackPanel != null) {
            blackjackPanel.init(panel, callbacks);
            blackjackPanel.updateGameState(game);
        }
    }

    @Override
    protected String getCompletionEventName() {
        return "BlackjackGameCompleted";
    }

    public void updateGame(BlackjackGame newGame) {
        this.game = newGame;
        if (blackjackPanel != null) {
            blackjackPanel.updateGameState(newGame);
        }
        resetState();
    }

    public void refreshAfterStateChange(BlackjackGame updatedGame) {
        this.game = updatedGame;
        if (blackjackPanel != null) {
            blackjackPanel.updateGameState(updatedGame);
        }
    }

    protected void handlePlayerAction(BlackjackGame.Action action) {
        if (handler != null) {
            handler.processPlayerActionInPlace(action, this);
            return;
        }

        pendingAction = action;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleNewHand() {
        if (handler != null) {
            handler.startNewHandInPlace(this);
            return;
        }

        pendingNewHand = true;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleLeave() {
        pendingLeave = true;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleHowToPlay() {
        if (handler != null) {
            handler.showHowToPlay(this);
            return;
        }

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handlePlaceBet(int amount) {
        if (handler != null) {
            handler.placeBetInPlace(amount, this);
            return;
        }

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    public BlackjackGame.Action getPendingAction() {
        return pendingAction;
    }

    public boolean getPendingNewHand() {
        return pendingNewHand;
    }

    public boolean getPendingLeave() {
        return pendingLeave;
    }
}