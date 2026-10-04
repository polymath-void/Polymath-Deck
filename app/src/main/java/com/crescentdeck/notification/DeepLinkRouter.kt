package com.crescentdeck.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.crescentdeck.ui.screen.MainActivity

/**
 * Deep Link Router creating and parsing standard URIs:
 * - crescent://card/{cardId}
 * - crescent://deck/{deckId}
 */
object DeepLinkRouter {

    const val SCHEME = "crescent"
    const val HOST_CARD = "card"
    const val HOST_DECK = "deck"

    fun createCardDeepLink(cardId: String): Uri {
        return Uri.parse("$SCHEME://$HOST_CARD/$cardId")
    }

    fun createDeckDeepLink(deckId: String): Uri {
        return Uri.parse("$SCHEME://$HOST_DECK/$deckId")
    }

    fun createIntentForCard(context: Context, cardId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            data = createCardDeepLink(cardId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun parseCardId(uri: Uri?): String? {
        if (uri == null || uri.scheme != SCHEME || uri.host != HOST_CARD) return null
        return uri.lastPathSegment
    }
}
