package me.steven.indrevstorage.utils

import me.steven.indrevstorage.api.ItemType
import net.minecraft.item.Item
import net.minecraft.tag.ItemTags
import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.util.Formatting
import net.minecraft.util.registry.Registry

/*inline*/ class SearchTerm(private val term: String) {

    fun matches(itemType: ItemType): Boolean {
        val tagIndex = term.indexOf('#')
        val tagPredicate = testOrTrue(tagIndex >= 0) { item ->
            val lastIndex = term.indexOf(' ', tagIndex)
            val path = term.substring(tagIndex + 1, if (lastIndex > tagIndex) lastIndex else term.length)
            if (path.any { !isPathCharacterValid(it) }) return@testOrTrue false
            ItemTags.getTagGroup().getTagsFor(item).any { it.path.startsWith(path) }
        }

        val modIdIndex = term.indexOf('@')
        val modIdPredicate = testOrTrue(modIdIndex >= 0) { item ->
            val id = Registry.ITEM.getId(item)
            val lastIndex = term.indexOf(' ', modIdIndex)
            id.namespace.startsWith(term.substring(modIdIndex + 1, if (lastIndex > modIdIndex) lastIndex else term.length))
        }

        val cleanTerm = term.replace(REGEX, "")
        val matches = cleanTerm.split('|')
        return tagPredicate(itemType.item) && modIdPredicate(itemType.item) && matches.any { Registry.ITEM.getId(itemType.item).path.startsWith(it) }
    }

    private fun testOrTrue(condition: Boolean, test: (Item) -> Boolean): (Item) -> Boolean {
        return if (condition) return test else { _ -> true }
    }

    private fun isPathCharacterValid(c: Char): Boolean {
        return c == '_' || c == '-' || c in 'a'..'z' || c in '0'..'9' || c == '/' || c == '.'
    }

    companion object {
        val REGEX = Regex("([#@].*?(\\s|\$))")

        fun applyFormatting(text: String): MutableText {
            return text.split(Regex("\\s+")).map { raw ->
                LiteralText(raw).let {
                    when (raw.firstOrNull()) {
                        '#' -> it.formatted(Formatting.LIGHT_PURPLE)
                        '@' -> it.formatted(Formatting.AQUA)
                        else -> it.formatted(Formatting.WHITE)
                    }
                }
            }.reduce { first, second -> first.append(LiteralText(" ")).append(second) }
        }
    }
}