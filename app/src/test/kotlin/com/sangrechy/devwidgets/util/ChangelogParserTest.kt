package com.sangrechy.devwidgets.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parse_emptyOrNullMarkdown_returnsFallbackChangelog() {
        val emptyResult = ChangelogParser.parse("")
        assertFalse(emptyResult.isEmpty())
        assertEquals("What's New in this Release", emptyResult[0].title)

        val nullResult = ChangelogParser.parse(null)
        assertFalse(nullResult.isEmpty())
    }

    @Test
    fun parse_gitHubReleaseMarkdown_cleansBoilerplateAndReturnsStructuredSections() {
        val raw = """
            ## What's Changed
            * Add wallpaper studio and chassis styles by @sangrechy in https://github.com/sangrechy/project_dev_android_widgets/pull/15
            * Fix layout issues in https://github.com/sangrechy/project_dev_android_widgets/pull/16 (#16)
            * Update `Type.kt` typography **styles**
            
            **Full Changelog**: https://github.com/sangrechy/project_dev_android_widgets/compare/v1.0.0...v1.1.0
        """.trimIndent()

        val sections = ChangelogParser.parse(raw)
        assertEquals(1, sections.size)
        assertEquals("What's Changed", sections[0].title)
        assertEquals(3, sections[0].items.size)

        // Verifies "by @sangrechy in https://..." was cleaned
        assertEquals("Add wallpaper studio and chassis styles", sections[0].items[0])
        // Verifies "in https://..." and "(#16)" was cleaned
        assertEquals("Fix layout issues", sections[0].items[1])
        // Verifies backticks and bold markdown were cleaned
        assertEquals("Update Type.kt typography styles", sections[0].items[2])
    }
}
