package com.rapli.magivant.ui.util

internal class NavbarScrollState(
    private val showThreshold: Float,
    private val hideThreshold: Float
) {
    var isHidden: Boolean = false
        private set
    private var accumulatedScroll = 0f

    init {
        require(showThreshold > 0f && hideThreshold > 0f)
    }

    fun onScroll(delta: Float): Boolean {
        if (!delta.isFinite() || delta == 0f) return isHidden
        if (delta * accumulatedScroll < 0f) accumulatedScroll = 0f
        accumulatedScroll = (accumulatedScroll + delta).coerceIn(-hideThreshold, showThreshold)
        if (accumulatedScroll >= showThreshold) {
            isHidden = false
            accumulatedScroll = 0f
        } else if (accumulatedScroll <= -hideThreshold) {
            isHidden = true
            accumulatedScroll = 0f
        }
        return isHidden
    }
}
