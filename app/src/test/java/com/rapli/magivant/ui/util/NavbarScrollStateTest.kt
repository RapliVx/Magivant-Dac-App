package com.rapli.magivant.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavbarScrollStateTest {
    @Test
    fun navbarHidesOnlyAfterEnoughContentHasScrolled() {
        val state = NavbarScrollState(8f, 32f)
        assertFalse(state.onScroll(-12f))
        assertFalse(state.onScroll(-12f))
        assertTrue(state.onScroll(-8f))
    }

    @Test
    fun smallDirectionChangesDoNotFlashTheNavbar() {
        val state = NavbarScrollState(8f, 32f)
        assertTrue(state.onScroll(-32f))
        assertTrue(state.onScroll(3f))
        assertTrue(state.onScroll(-1f))
        assertTrue(state.onScroll(7f))
        assertFalse(state.onScroll(1f))
    }

    @Test
    fun aNewTabStartsWithVisibleNavigation() {
        val firstTab = NavbarScrollState(8f, 32f)
        assertTrue(firstTab.onScroll(-32f))
        val secondTab = NavbarScrollState(8f, 32f)
        assertFalse(secondTab.isHidden)
        assertFalse(secondTab.onScroll(-1f))
    }

    @Test
    fun invalidOrZeroMovementDoesNotChangeVisibility() {
        val state = NavbarScrollState(8f, 32f)
        assertTrue(state.onScroll(-32f))
        assertTrue(state.onScroll(Float.NaN))
        assertTrue(state.onScroll(Float.POSITIVE_INFINITY))
        assertTrue(state.onScroll(0f))
    }
}
