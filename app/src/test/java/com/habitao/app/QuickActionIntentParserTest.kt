package com.habitao.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickActionIntentParserTest {
    @Test
    fun `maps shortcut action strings to routes`() {
        assertEquals(QuickActionRoute.AddTask, QuickActionIntentParser.toRoute(QuickActionIntentParser.ActionAddTask))
        assertEquals(QuickActionRoute.AddHabit, QuickActionIntentParser.toRoute(QuickActionIntentParser.ActionAddHabit))
        assertEquals(QuickActionRoute.AddRoutine, QuickActionIntentParser.toRoute(QuickActionIntentParser.ActionAddRoutine))
        assertEquals(QuickActionRoute.GlobalSearch, QuickActionIntentParser.toRoute(QuickActionIntentParser.ActionGlobalSearch))
    }

    @Test
    fun `returns null for unknown action`() {
        assertNull(QuickActionIntentParser.toRoute("unknown_action"))
        assertNull(QuickActionIntentParser.toRoute(null))
    }
}

