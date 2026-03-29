package data.scripts.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackGame {

    public enum Action { HIT, STAND, DOUBLE_DOWN, SPLIT }

    public enum GameState { BETTING, PLAYER_TURN, DEALER_TURN, RESULT }

    public enum Suit { 
        SPADES(Strings.get("poker_suits.spades")), 
        HEARTS(Strings.get("poker_suits.hearts")), 
        DIAMONDS(Strings.get("poker_suits.diamonds")), 
        CLUBS(Strings.get("poker_suits.clubs"));
        
        public final String displayName;
        Suit(String name) { this.displayName = name; }
    }

    public enum Rank {
        TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"),
        SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), TEN(10, "10"),
        JACK(10, "J"), QUEEN(10, "Q"), KING(10, "K"), ACE(11, "A");

        public final int value;
        public final String symbol;

        Rank(int v, String s) { value = v; symbol = s; }
    }

    private static final float BLACKJACK_PAYOUT = 2.5f; // 3:2 payout for blackjack

    public record Card(Rank rank, Suit suit) {
        @Override
        public String toString() {
            return "[" + Strings.format("poker_card_format.of", suit.displayName, rank.symbol) + "]";
        }

        public int getValue() { return rank.value; }

        public boolean isAce() { return rank == Rank.ACE; }
    }

    public static class Deck {
        public final List<Card> cards = new ArrayList<>();
        private static final int NUM_DECKS = 6;

        public Deck() {
            for (int d = 0; d < NUM_DECKS; d++) {
                for(Suit s : Suit.values()) {
                    for(Rank r : Rank.values()) {
                        cards.add(new Card(r, s));
                    }
                }
            }
        }

        public void shuffle() { Collections.shuffle(cards); }

        // Auto-refresh with new shuffled deck if empty
        public Card draw() {
            if (cards.isEmpty()) {
                Deck fresh = new Deck();
                fresh.shuffle();
                cards.addAll(fresh.cards);
            }
            return cards.remove(cards.size() - 1);
        }

        public int cardsRemaining() { return cards.size(); }
    }

    public static class Hand {
        public final List<Card> cards = new ArrayList<>();
        public int betAmount;

        public Hand() {}

        public Hand(int bet) {
            this.betAmount = bet;
        }

        public void addCard(Card card) {
            cards.add(card);
        }

        public int getValue() {
            int total = 0;
            int aces = 0;
            for (Card c : cards) {
                total += c.rank().value;
                if (c.isAce()) aces++;
            }
            while (total > 21 && aces > 0) {
                total -= 10;
                aces--;
            }
            return total;
        }

        public boolean isSoft() {
            int total = 0;
            int aces = 0;
            for (Card c : cards) {
                total += c.rank().value;
                if (c.isAce()) aces++;
            }
            int reducedAces = 0;
            while (total > 21 && aces > reducedAces) {
                total -= 10;
                reducedAces++;
            }
            return aces > reducedAces;
        }

        public boolean isBust() { return getValue() > 21; }

        public boolean isBlackjack() { return cards.size() == 2 && getValue() == 21; }

        // Can split if two cards of same rank
        public boolean canSplit() {
            return cards.size() == 2 &&
                   cards.get(0).rank() == cards.get(1).rank();
        }

        public boolean canDoubleDown() { return cards.size() == 2; }
    }

    public static class GameStateData {
        public Hand playerHand;
        public Hand dealerHand;
        public List<Hand> splitHands;
        public int currentSplitIndex;
        public int pot;
        public int playerStack;
        public int currentBet;
        public GameState state;
        public int lastPotWon;
        public boolean dealerHoleCardRevealed;
        public int splitBet;
        public int originalBalance;
        public int creditCeiling;
        public int creditBorrowed;
        public List<Integer> splitHandResults;
        public boolean overdraftEnabled = false;
    }

    private final GameStateData state;
    private Deck deck;
    private int handsSinceShuffle;
    private static final int RESHUFFLE_INTERVAL = 50;

    public BlackjackGame(int playerStack) {
        state = new GameStateData();
        state.playerStack = playerStack;
        state.playerHand = new Hand();
        state.dealerHand = new Hand();
        state.splitHands = new ArrayList<>();
        deck = new Deck();
        deck.shuffle();
        handsSinceShuffle = 0;
    }

    public void startNewHand() {
        handsSinceShuffle++;
        if (handsSinceShuffle >= RESHUFFLE_INTERVAL) {
            deck = new Deck();
            deck.shuffle();
            handsSinceShuffle = 0;
        }

        // Reset state for new hand
        state.playerHand = new Hand();
        state.dealerHand = new Hand();
        state.splitHands = new ArrayList<>();
        state.currentSplitIndex = 0;
        state.pot = 0;
        state.lastPotWon = 0;
        state.dealerHoleCardRevealed = false;
        state.state = GameState.BETTING;
        state.currentBet = 0;
    }

    // Deals initial cards and sets up game state
    public boolean placeBet(int betAmount) {
        if (state.state != GameState.BETTING) return false;
        if (state.playerStack < betAmount) return false;

        state.playerStack -= betAmount;
        state.currentBet = betAmount;
        state.playerHand = new Hand(betAmount);
        state.dealerHand = new Hand();
        state.splitHands = new ArrayList<>();
        state.currentSplitIndex = 0;
        state.pot = betAmount;
        state.lastPotWon = 0;
        state.dealerHoleCardRevealed = false;

        state.playerHand.addCard(deck.draw());
        state.playerHand.addCard(deck.draw());
        state.dealerHand.addCard(deck.draw());
        state.dealerHand.addCard(deck.draw());

        state.pot += betAmount;

        boolean isBlackjack = state.playerHand.isBlackjack();
        if (isBlackjack) {
            revealDealerHoleCard();
            evaluateDealerHand();
        } else {
            state.state = GameState.PLAYER_TURN;
        }

        return true;
    }

    public void playerHit() {
        if (state.state != GameState.PLAYER_TURN) return;

        Hand currentHand = getCurrentHand();
        if (currentHand == null) return;

        currentHand.addCard(deck.draw());

        if (currentHand.isBust()) {
            handleBust(currentHand);
        }
    }

    // Advances to next split hand or triggers dealer turn
    public void playerStand() {
        if (state.state != GameState.PLAYER_TURN) return;

        if (!state.splitHands.isEmpty()) {
            state.currentSplitIndex++;
            if (state.currentSplitIndex < state.splitHands.size()) {
                state.playerHand = getCurrentHand();
                state.dealerHoleCardRevealed = false;
                return;
            }
        }

        revealDealerHoleCard();
        state.state = GameState.DEALER_TURN;
        evaluateDealerHand();
    }

    public void playerDoubleDown() {
        if (state.state != GameState.PLAYER_TURN) return;

        Hand currentHand = getCurrentHand();
        if (currentHand == null || !currentHand.canDoubleDown()) return;

        if (state.playerStack < currentHand.betAmount) return;

        state.playerStack -= currentHand.betAmount;
        state.pot += currentHand.betAmount;
        currentHand.betAmount *= 2;

        currentHand.addCard(deck.draw());

        if (currentHand.isBust()) {
            handleBust(currentHand);
        } else {
            playerStand();
        }
    }

    // Creates two hands from paired cards, doubles pot
    public void playerSplit() {
        if (state.state != GameState.PLAYER_TURN) return;

        Hand currentHand = getCurrentHand();
        if (currentHand == null || !currentHand.canSplit()) return;

        if (state.playerStack < currentHand.betAmount) return;

        if (!state.splitHands.isEmpty()) return;

        Card first = currentHand.cards.get(0);
        Card second = currentHand.cards.get(1);

        Hand hand1 = new Hand(currentHand.betAmount);
        hand1.addCard(first);
        hand1.addCard(deck.draw());

        Hand hand2 = new Hand(currentHand.betAmount);
        hand2.addCard(second);
        hand2.addCard(deck.draw());

        state.playerStack -= currentHand.betAmount;
        state.pot += currentHand.betAmount;

        state.splitHands.add(hand1);
        state.splitHands.add(hand2);
        state.playerHand = hand1;
        state.currentSplitIndex = 0;
    }

    // Handles bust: advance to next split hand or end game
    private void handleBust(Hand hand) {
        state.pot -= hand.betAmount;
        if (!state.splitHands.isEmpty() && state.currentSplitIndex < state.splitHands.size() - 1) {
            state.currentSplitIndex++;
            state.playerHand = state.splitHands.get(state.currentSplitIndex);
            state.dealerHoleCardRevealed = false;
        } else {
            revealDealerHoleCard();
            state.state = GameState.RESULT;
            determineWinners();
        }
    }

    // Dealer draws until 17 or higher
    private void evaluateDealerHand() {
        while (state.dealerHand.getValue() < 17) {
            state.dealerHand.addCard(deck.draw());
        }
        determineWinners();
    }

    // Calculates winnings for each hand against dealer
    private void determineWinners() {
        int dealerValue = state.dealerHand.getValue();
        boolean dealerBust = state.dealerHand.isBust();
        boolean dealerBlackjack = state.dealerHand.isBlackjack();
        boolean isSplitGame = !state.splitHands.isEmpty();

        int totalWinnings = 0;
        int netProfit = 0;

        state.splitHandResults = new ArrayList<>();

        List<Hand> handsToEvaluate = !state.splitHands.isEmpty() 
            ? state.splitHands 
            : List.of(state.playerHand);

        for (Hand hand : handsToEvaluate) {
            int handProfit = 0;
            int handValue = hand.getValue();

            if (hand.isBust()) {
                handProfit = -hand.betAmount;
                netProfit -= hand.betAmount;
            } else if (hand.isBlackjack() && !dealerBlackjack) {
                int win = isSplitGame ? hand.betAmount * 2 : (int)(hand.betAmount * BLACKJACK_PAYOUT);
                totalWinnings += win;
                handProfit = win - hand.betAmount;
                netProfit += handProfit;
            } else if (dealerBust || handValue > dealerValue) {
                totalWinnings += hand.betAmount * 2;
                handProfit = hand.betAmount;
                netProfit += handProfit;
            } else if (handValue < dealerValue) {
                handProfit = -hand.betAmount;
                netProfit -= hand.betAmount;
            } else {
                totalWinnings += hand.betAmount;
            }

            state.splitHandResults.add(handProfit);
        }

        state.lastPotWon = netProfit;
        state.playerStack += totalWinnings;
        state.pot = 0;
        state.state = GameState.RESULT;
    }

    private void revealDealerHoleCard() {
        state.dealerHoleCardRevealed = true;
    }

    public GameStateData getState() {
        return state;
    }

    public Hand getPlayerHand() {
        return state.playerHand;
    }

    public Hand getDealerHand() {
        return state.dealerHand;
    }

    public GameState getGameState() {
        return state.state;
    }

    public boolean isDealerHoleCardRevealed() {
        return state.dealerHoleCardRevealed;
    }

    public Hand getCurrentHand() {
        if (!state.splitHands.isEmpty() && state.currentSplitIndex < state.splitHands.size()) {
            return state.splitHands.get(state.currentSplitIndex);
        }
        return state.playerHand;
    }

    public int getCardsRemaining() {
        return deck.cardsRemaining();
    }

    public int getHandsUntilReshuffle() {
        return RESHUFFLE_INTERVAL - handsSinceShuffle;
    }
}