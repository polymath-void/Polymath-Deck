package com.polymathdeck.data.db.entity

/**
 * Supported card types in the Polymath Deck rendering engine.
 */
enum class CardType(val typeId: Int) {
    NATIVE(0),
    WEB(1),
    VIDEO(2),
    AUDIO(3),
    ARTICLE(4),
    IMAGE(5),
    PDF(6);

    companion object {
        fun fromId(id: Int): CardType = entries.find { it.typeId == id } ?: NATIVE
    }
}
