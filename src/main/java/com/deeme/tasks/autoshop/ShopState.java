package com.deeme.tasks.autoshop;

/**
 * Last purchase state shown in the task label.
 */
public enum ShopState {
    NO_CREDITS("Not enough credits"),
    NO_URI("Not enough uri"),
    PURCHASE_ERROR("There was an error while purchasing"),
    PURCHASE_SUCCESS("Purchase completed"),
    ;

    private final String message;

    ShopState(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
