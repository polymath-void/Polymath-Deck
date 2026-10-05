package com.polymathdeck.engine.quadtree

/**
 * Global layout normalizer that automatically generates strict flexbox and bounding CSS
 * ensuring external web content perfectly aligns within the one-frame, single-column
 * QuadTree spatial grid coordinate system.
 */
object QuadTreeLayoutNormalizer {

    fun generateNormalizerCss(): String = """
        html, body {
            overflow-x: hidden !important;
            overscroll-behavior: contain !important;
            touch-action: pan-y pinch-zoom !important;
            -webkit-overflow-scrolling: touch !important;
            max-width: 100% !important;
            box-sizing: border-box !important;
            margin: 0 !important;
            padding: 0 !important;
        }
        * {
            box-sizing: border-box !important;
        }
        ::-webkit-scrollbar {
            width: 4px;
            height: 4px;
        }
        ::-webkit-scrollbar-thumb {
            background: rgba(56, 189, 248, 0.4);
            border-radius: 4px;
        }
        ::-webkit-scrollbar-track {
            background: transparent;
        }
    """.trimIndent()
}
