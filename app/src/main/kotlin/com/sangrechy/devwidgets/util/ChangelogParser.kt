package com.sangrechy.devwidgets.util

data class ChangelogSection(
    val title: String,
    val items: List<String>
)

object ChangelogParser {
    fun parse(rawMarkdown: String?): List<ChangelogSection> {
        if (rawMarkdown.isNullOrBlank()) {
            return getFallbackChangelog()
        }

        val lines = rawMarkdown.lines()
        val sections = mutableListOf<ChangelogSection>()
        var currentSectionTitle = "What's New"
        val currentItems = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // Filter out noise lines like "Full Changelog"
            if (trimmed.startsWith("**Full Changelog**", ignoreCase = true) ||
                trimmed.startsWith("Full Changelog:", ignoreCase = true) ||
                trimmed.startsWith("https://github.com", ignoreCase = true)) {
                continue
            }

            // Section headers: e.g. "## What's Changed", "### Features"
            if (trimmed.startsWith("#")) {
                if (currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
                    currentItems.clear()
                }
                currentSectionTitle = trimmed.trimStart('#').trim()
                continue
            }

            // Bullet points (*, -, •) or numbered items
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                val cleanItem = cleanLine(trimmed.substring(2))
                if (cleanItem.isNotBlank()) {
                    currentItems.add(cleanItem)
                }
            } else if (trimmed.matches(Regex("""^\d+\.\s+.*"""))) {
                val cleanItem = cleanLine(trimmed.replaceFirst(Regex("""^\d+\.\s+"""), ""))
                if (cleanItem.isNotBlank()) {
                    currentItems.add(cleanItem)
                }
            } else {
                val cleanItem = cleanLine(trimmed)
                if (cleanItem.isNotBlank()) {
                    currentItems.add(cleanItem)
                }
            }
        }

        if (currentItems.isNotEmpty()) {
            sections.add(ChangelogSection(currentSectionTitle, currentItems.toList()))
        }

        return if (sections.isEmpty()) getFallbackChangelog() else sections
    }

    private fun cleanLine(line: String): String {
        var result = line
        // Remove "by @user in https://github.com/..."
        result = result.replace(Regex("""\s+by\s+@[\w-]+(?:\s+in\s+https?://\S+)?""", RegexOption.IGNORE_CASE), "")
        // Remove trailing "in https://github.com/..."
        result = result.replace(Regex("""\s+in\s+https?://\S+""", RegexOption.IGNORE_CASE), "")
        // Clean markdown links [Text](url) -> Text
        result = result.replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1")
        // Remove backticks
        result = result.replace("`", "")
        // Remove bold / italic markdown stars
        result = result.replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
        result = result.replace(Regex("""\*([^*]+)\*"""), "$1")
        // Remove PR hashes like (#123)
        result = result.replace(Regex("""\s*\(#\d+\)"""), "")
        return result.trim()
    }

    fun getFallbackChangelog(): List<ChangelogSection> {
        return listOf(
            ChangelogSection(
                title = "What's New in this Release",
                items = listOf(
                    "Frosted transparent styling with dynamic wallpaper support",
                    "Offline LeetCode problems viewer & multi-note manager",
                    "Customizable launcher home screen widgets (transparent, frosted & solid)",
                    "Performance optimizations and background sync stability"
                )
            )
        )
    }
}
