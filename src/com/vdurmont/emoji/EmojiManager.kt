package com.vdurmont.emoji

import com.vdurmont.emoji.EmojiLoader.loadEmojis
import com.vdurmont.emoji.EmojiTrie.Matches
import java.io.IOException

/**
 * Holds the loaded emojis and provides search functions.
 *
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]<br></br>
 * Creator: Vincent DURMONT [vdurmont@gmail.com]
 */
object EmojiManager {
	private const val PATH = "/emojis.json"
	private val EMOJIS_BY_ALIAS: MutableMap<String, Emoji> = HashMap()
	private val EMOJIS_BY_TAG: MutableMap<String, Set<Emoji>> = HashMap()
	lateinit var ALL_EMOJIS: List<Emoji>
	lateinit var EMOJI_TRIE: EmojiTrie

	/**
	 * Returns all the [Emoji]s for a given tag.
	 *
	 * @param tag the tag
	 *
	 * @return the associated [Emoji]s, null if the tag
	 * is unknown
	 */
	fun getForTag(tag: String?): Set<Emoji>? {
		return if (tag == null) {
			null
		} else EMOJIS_BY_TAG[tag]
	}

	/**
	 * Returns the [Emoji] for a given alias.
	 *
	 * @param alias the alias
	 *
	 * @return the associated [Emoji], null if the alias
	 * is unknown
	 */
	fun getForAlias(alias: String?): Emoji? {
		return if (alias == null || alias.isEmpty()) {
			null
		} else EMOJIS_BY_ALIAS[trimAlias(alias)]
	}

	private fun trimAlias(alias: String): String {
		val len = alias.length
		return alias.substring(
			if (alias[0] == ':') 1 else 0,
			if (alias[len - 1] == ':') len - 1 else len
		)
	}

	/**
	 * Returns the [Emoji] for a given unicode.
	 *
	 * @param unicode the the unicode
	 *
	 * @return the associated [Emoji], null if the
	 * unicode is unknown
	 */
	fun getByUnicode(unicode: String?): Emoji? {
		if (unicode == null) {
			return null
		}
		val res = EmojiParser.getEmojiInPosition(unicode.toCharArray(), 0) ?: return null
		return res.emoji
	}

	/**
	 * Returns all the [Emoji]s
	 *
	 * @return all the [Emoji]s
	 */
	val all: Collection<Emoji>?
		get() = ALL_EMOJIS

	/**
	 * Tests if a given String is an emoji.
	 *
	 * @param string the string to test
	 * @return true if the string is an emoji's unicode, false else
	 */
	fun isEmoji(string: String?): Boolean {
		if (string == null) return false
		val chars = string.toCharArray()
		val result = EmojiParser.getEmojiInPosition(chars, 0)
		return result != null && result.emojiStartIndex == 0 && result.endIndex == chars.size
	}

	/**
	 * Tests if a given String contains an emoji.
	 *
	 * @param string the string to test
	 * @return true if the string contains an emoji's unicode, false otherwise
	 */
	fun containsEmoji(string: String?): Boolean {
		return if (string == null) false else EmojiParser.getNextEmoji(string.toCharArray(), 0) != null
	}

	/**
	 * Tests if a given String only contains emojis.
	 *
	 * @param string the string to test
	 * @return true if the string only contains emojis, false else
	 */
	fun isOnlyEmojis(string: String?): Boolean {
		return string != null && EmojiParser.removeAllEmojis(string).isEmpty()
	}

	/**
	 * Checks if sequence of chars contain an emoji.
	 *
	 * @param sequence Sequence of char that may contain emoji in full or
	 * partially.
	 * @return &lt;li&gt;
	 * Matches.EXACTLY if char sequence in its entirety is an emoji
	 * &lt;/li&gt;
	 * &lt;li&gt;
	 * Matches.POSSIBLY if char sequence matches prefix of an emoji
	 * &lt;/li&gt;
	 * &lt;li&gt;
	 * Matches.IMPOSSIBLE if char sequence matches no emoji or prefix of an
	 * emoji
	 * &lt;/li&gt;
	 */
	fun isEmoji(sequence: CharArray): Matches {
		return EMOJI_TRIE.isEmoji(sequence)
	}

	/**
	 * Returns all the tags in the database
	 *
	 * @return the tags
	 */
	val allTags: Collection<String>
		get() = EMOJIS_BY_TAG.keys

	init {
		try {
			val stream = EmojiLoader::class.java.getResourceAsStream(PATH)
			val emojis = loadEmojis(stream)

			for (emoji in emojis) {
				for (tag in emoji.tags) {
					val tagSet = EMOJIS_BY_TAG.computeIfAbsent(tag) { k: String? -> HashSet() } as HashSet
					tagSet.add(emoji)
				}
				for (alias in emoji.aliases) {
					EMOJIS_BY_ALIAS[alias] = emoji
				}
			}
			EMOJI_TRIE = EmojiTrie(emojis)
			emojis.sortWith(java.util.Comparator { e1: Emoji, e2: Emoji -> e2.unicode.length - e1.unicode.length })
			ALL_EMOJIS = emojis
			stream.close()
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}
}