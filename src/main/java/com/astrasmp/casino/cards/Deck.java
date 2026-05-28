package com.astrasmp.casino.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    public enum Suit {
        HEARTS("♥", "&c"), DIAMONDS("♦", "&c"), CLUBS("♣", "&8"), SPADES("♠", "&8");
        public final String symbol;
        public final String color;
        Suit(String symbol, String color) { this.symbol = symbol; this.color = color; }
    }

    public enum Rank {
        TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"),
        SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"),
        TEN(10, "10"), JACK(10, "J"), QUEEN(10, "Q"), KING(10, "K"), ACE(11, "A");
        public final int value;
        public final String display;
        Rank(int value, String display) { this.value = value; this.display = display; }
    }

    public record Card(Suit suit, Rank rank) {
        public String getName() {
            return suit.color + rank.display + " " + suit.symbol;
        }
    }

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) return null;
        return cards.removeFirst();
    }

    public static int calculateScore(List<Card> hand) {
        int score = 0;
        int aces = 0;
        for (Card card : hand) {
            score += card.rank().value;
            if (card.rank() == Rank.ACE) aces++;
        }
        while (score > 21 && aces > 0) {
            score -= 10;
            aces--;
        }
        return score;
    }
}