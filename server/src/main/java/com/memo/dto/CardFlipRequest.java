package com.memo.dto;

public class CardFlipRequest {

    //Keeps the ID of the card, that should be flipped on request to server
    private int cardIndex;

    /**
     * Initializes a new card flip request for the card without defining card ID
     */
    public CardFlipRequest() {}

    /**
     * Initializes a new card flip request for the card with defined card ID
     *
     * @param cardIndex      ID of the card to be flipped on request to server.
     */
    public CardFlipRequest(int cardIndex) {
        this.cardIndex = cardIndex;
    }

    /**
     * Fetches the ID of the card (integer) to be flipped.
     */
    public int getCardIndex() {
        return this.cardIndex;
    }

    /**
     * Sets the ID of the card (integer) to be flipped.
     */
    public void setCardIndex(int cardIndex) {
        this.cardIndex = cardIndex;
    }
}
