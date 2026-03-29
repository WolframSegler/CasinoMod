package data.scripts.casino;

import java.util.Map;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import data.scripts.casino.interaction.PokerHandler;
import data.scripts.casino.shared.BaseGameDelegate;

public class PokerDialogDelegate extends BaseGameDelegate {

    protected PokerPanelUI pokerPanel;
    protected PokerGame game;
    protected PokerHandler handler;

    protected PokerPanelUI.PokerActionCallback actionCallback;
    protected boolean gameEnded = false;

    protected PokerGame.Action pendingAction = null;
    protected int pendingRaiseAmount = 0;
    protected boolean pendingNextHand = false;
    protected boolean pendingSuspend = false;
    protected boolean pendingHowToPlay = false;
    protected boolean pendingFlipTable = false;
    protected boolean pendingCleanLeave = false;
    protected String lastOpponentAction = "";
    protected String lastPlayerAction = "";
    protected String returnMessage = "";

    public PokerDialogDelegate(PokerGame game, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap, Runnable onDismissCallback) {
        this(game, dialog, memoryMap, onDismissCallback, null);
    }

    public PokerDialogDelegate(PokerGame game, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap, Runnable onDismissCallback, PokerHandler handler) {
        super(dialog, memoryMap, onDismissCallback);
        this.game = game;
        this.handler = handler;

        actionCallback = new PokerPanelUI.PokerActionCallback() {
            @Override
            public void onPlayerAction(PokerGame.Action action, int raiseAmount) {
                handlePlayerAction(action, raiseAmount);
            }

            @Override
            public void onBackToMenu() {
                if (onDismissCallback != null) {
                    onDismissCallback.run();
                }
            }

            @Override
            public void onNextHand() {
                handleNextHand();
            }

            @Override
            public void onSuspend() {
                handleSuspend();
            }

            @Override
            public void onHowToPlay() {
                handleHowToPlay();
            }

            @Override
            public void onFlipTable() {
                handleFlipTable();
            }
        };

        pokerPanel = new PokerPanelUI(game, actionCallback);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return pokerPanel;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        super.init(panel, callbacks);

        if (pokerPanel != null) {
            pokerPanel.init(panel, callbacks);
            pokerPanel.updateGameState(game);

            if (lastOpponentAction != null && !lastOpponentAction.isEmpty()) {
                pokerPanel.showOpponentAction(lastOpponentAction);
            }

            if (lastPlayerAction != null && !lastPlayerAction.isEmpty()) {
                pokerPanel.showPlayerAction(lastPlayerAction);
            }

            if (returnMessage != null && !returnMessage.isEmpty()) {
                pokerPanel.showReturnMessage(returnMessage);
            }
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        PokerGame.PokerState state = game.getState();
        boolean someoneIsBust = state.playerStack < state.bigBlind || state.opponentStack < state.bigBlind;

        if (someoneIsBust && !gameEnded) {
            gameEnded = true;
        }
    }

    @Override
    protected String getCompletionEventName() {
        return "PokerGameCompleted";
    }

    public void updateGame(PokerGame newGame) {
        this.game = newGame;
        this.lastOpponentAction = "";
        this.lastPlayerAction = "";
        if (pokerPanel != null) {
            pokerPanel.updateGameState(newGame);
            pokerPanel.hideOpponentAction();
            pokerPanel.hidePlayerAction();
        }
        gameEnded = false;
        resetState();
    }

    public void refreshAfterStateChange(PokerGame updatedGame) {
        this.game = updatedGame;
        if (pokerPanel != null) {
            pokerPanel.updateGameState(updatedGame);
        }
    }

    public void startOpponentTurn() {
        if (pokerPanel != null) {
            pokerPanel.startOpponentTurn();
        }
    }

    protected void handlePlayerAction(PokerGame.Action action, int raiseAmount) {
        if (handler != null) {
            handler.processPlayerActionInPlace(action, raiseAmount, this);
            return;
        }

        pendingAction = action;
        pendingRaiseAmount = raiseAmount;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    public void setLastOpponentAction(String action) {
        this.lastOpponentAction = action;

        if (pokerPanel != null && action != null && !action.isEmpty()) {
            pokerPanel.showOpponentAction(action);
        }
    }

    public void setLastPlayerAction(String action) {
        this.lastPlayerAction = action;
    }

    public PokerGame.Action getPendingAction() {
        return pendingAction;
    }

    public int getPendingRaiseAmount() {
        return pendingRaiseAmount;
    }

    public boolean getPendingNextHand() {
        return pendingNextHand;
    }

    protected void handleNextHand() {
        if (handler != null) {
            handler.startNextHandInPlace(this);
            return;
        }

        pendingNextHand = true;
        pendingAction = null;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleSuspend() {
        pendingSuspend = true;
        pendingAction = null;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleHowToPlay() {
        pendingHowToPlay = true;
        pendingAction = null;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    protected void handleFlipTable() {
        boolean isShowdown = game != null && game.getState() != null &&
            game.getState().round == PokerGame.Round.SHOWDOWN;

        if (handler != null && isShowdown) {
            handler.handleCleanLeaveInPlace(this);
            return;
        }

        if (isShowdown) {
            pendingCleanLeave = true;
        } else {
            pendingFlipTable = true;
        }
        pendingAction = null;

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    public boolean getPendingSuspend() {
        return pendingSuspend;
    }

    public boolean getPendingHowToPlay() {
        return pendingHowToPlay;
    }

    public boolean getPendingFlipTable() {
        return pendingFlipTable;
    }

    public boolean getPendingCleanLeave() {
        return pendingCleanLeave;
    }
}