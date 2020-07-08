package com.vdurmont.emoji

/**
 *
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]<br></br>
 * Creator: Vincent DURMONT [vdurmont@gmail.com]
 */
class EmojiTrie(emojis: Collection<Emoji>) {
	private val root = Node()
	val maxDepth: Int

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
		return isEmoji(sequence, 0, sequence.size)
	}

	/**
	 * Checks if the sequence of chars within the given bound indices contain an emoji.
	 *
	 * @see .isEmoji
	 */
	fun isEmoji(sequence: CharArray, start: Int, end: Int): Matches {
		if (start < 0 || start > end || end > sequence.size) {
			throw ArrayIndexOutOfBoundsException("start " + start + ", end " + end + ", length " + sequence.size)
		}
		var tree: Node = root
		for (i in start until end) {
			if (!tree.hasChild(sequence[i])) {
				return Matches.IMPOSSIBLE
			}
			tree = tree.getChild(sequence[i])?: return Matches.IMPOSSIBLE

		}
		return if (tree.isEndOfEmoji) Matches.EXACTLY else Matches.POSSIBLY
	}

	fun getBestEmoji(sequence: CharArray, start: Int): Emoji? {
		if (start < 0) {
			throw ArrayIndexOutOfBoundsException("start " + start + ", length " + sequence.size)
		}
		val end = sequence.size
		var tree: Node = root
		for (i in start until end) {
			if (!tree.hasChild(sequence[i])) {
				return if (tree.isEndOfEmoji) tree.emoji else null
			}
			tree = tree.getChild(sequence[i])?: return null
		}
		return if (tree.isEndOfEmoji) tree.emoji else null
	}

	/**
	 * Finds Emoji instance from emoji unicode
	 *
	 * @param unicode unicode of emoji to get
	 * @return Emoji instance if unicode matches and emoji, null otherwise.
	 */
	fun getEmoji(unicode: String): Emoji? {
		return getEmoji(unicode.toCharArray(), 0, unicode.length)
	}

	fun getEmoji(sequence: CharArray, start: Int, end: Int): Emoji? {
		if (start < 0 || start > end || end > sequence.size) {
			throw ArrayIndexOutOfBoundsException(
				"start " + start + ", end " + end + ", length " + sequence.size
			)
		}
		var tree: Node = root
		for (i in 0 until end) {
			if (!tree.hasChild(sequence[i])) {
				return null
			}
			tree = tree.getChild(sequence[i])?: return null
		}
		return tree.emoji
	}

	enum class Matches {
		EXACTLY, POSSIBLY, IMPOSSIBLE;

		fun exactMatch(): Boolean {
			return this == EXACTLY
		}

		fun impossibleMatch(): Boolean {
			return this == IMPOSSIBLE
		}
	}

	private class Node {
		private val children: MutableMap<Char, Node> = HashMap()
		var emoji: Emoji? = null
		set(emoji) {
			field = emoji
		}

		fun hasChild(child: Char): Boolean {
			return children.containsKey(child)
		}

		fun addChild(child: Char) {
			children[child] = Node()
		}

		fun getChild(child: Char): Node? {
			return children[child]
		}

		val isEndOfEmoji: Boolean
			get() = emoji != null
	}

	init {
		var maxDepth = 0
		for (emoji in emojis) {
			var tree: Node = root
			val chars = emoji.unicode.toCharArray()
			maxDepth = Math.max(maxDepth, chars.size)
			for (c in chars) {
				if (!tree.hasChild(c)) {
					tree.addChild(c)
				}
				tree = tree.getChild(c)?: break
			}
			tree.emoji = emoji
		}
		this.maxDepth = maxDepth
	}
}