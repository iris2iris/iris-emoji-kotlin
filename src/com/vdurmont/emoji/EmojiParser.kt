package com.vdurmont.emoji;

import com.vdurmont.emoji.EmojiManager.isEmoji
import com.vdurmont.emoji.EmojiParser.FitzpatrickAction.*
import com.vdurmont.emoji.EmojiParser.extractEmojiStrings
import java.util.*

/**
 * Provides methods to parse strings with emojis.
 *
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]<br>
 * Creator: Vincent DURMONT [vdurmont@gmail.com]
 */
object EmojiParser {

    /**
     * See {@link #parseToAliases(String, FitzpatrickAction)} with the action
     * "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their alias.
     */
    fun parseToAliases(input: String): String {
        return parseToAliases(input, PARSE);
    }

    /**
     * Replaces the emoji's unicode occurrences by one of their alias
     * (between 2 ':').<br>
     * Example: <code>😄</code> will be replaced by <code>:smile:</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE action, a "|" will be
     * appendend to the alias, with the fitzpatrick type.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>:boy|type_6:</code><br>
     * The fitzpatrick types are: type_1_2, type_3, type_4, type_5, type_6<br>
     * <br>
     * When a fitzpatrick modifier is present with a REMOVE action, the modifier
     * will be deleted.<br>
     * Example: <code>👦🏿</code> will be replaced by <code>:boy:</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored.<br>
     * Example: <code>👦🏿</code> will be replaced by <code>:boy:🏿</code><br>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their alias.
     */
    fun parseToAliases(input: String, fitzpatrickAction: FitzpatrickAction): String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                when (fitzpatrickAction) {
                    REMOVE ->
                        return ":" + emoji.emoji.aliases.get(0) + ":";
                    IGNORE ->
                        return ":" + emoji.emoji.aliases.get(0) + ":" + emoji.fitzpatrickUnicode;

                    /*FitzpatrickAction.PARSE*/ else ->
                        if (emoji.hasFitzpatrick()) {
                            return ":" + emoji.emoji.aliases.get(0) + "|" + emoji.fitzpatrickType + ":";
                        } else {
                            return ":" + emoji.emoji.aliases.get(0) + ":";
                        }
                }
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * Replace all emojis with character
     *
     * @param str               the string to process
     * @param replacementString replacement the string that will replace all the emojis
     * @return the string with replaced character
     */
    fun replaceAllEmojis(str: String, replacementString: String): String {
        val emojiTransformer: EmojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                return replacementString;
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }


    /**
     * Replaces the emoji's aliases (between 2 ':') occurrences and the html
     * representations by their unicode.<br>
     * Examples:<br>
     * <code>:smile:</code> will be replaced by <code>😄</code><br>
     * <code>&amp;#128516;</code> will be replaced by <code>😄</code><br>
     * <code>:boy|type_6:</code> will be replaced by <code>👦🏿</code>
     *
     * @param input the string to parse
     * @return the string with the aliases and html representations replaced by
     * their unicode.
     */
    fun parseToUnicode(input: String): String? {
        val sb = StringBuilder(input.length)
        var last = 0
        while (last < input.length) {
            var alias: AliasCandidate? = getAliasAt(input, last)
            if (alias == null) {
                alias = getHtmlEncodedEmojiAt(input, last)
            }
            if (alias != null) {
                sb.append(alias.emoji.unicode)
                last = alias.endIndex
                if (alias.fitzpatrick != null) {
                    sb.append(alias.fitzpatrick!!.unicode)
                }
            } else {
                sb.append(input[last])
            }
            last++
        }
        return sb.toString()
    }

    /**
     * Finds the alias in the given string starting at the given point, null otherwise
     */
    private fun getAliasAt(input: String, start: Int): AliasCandidate? {
        if (input.length < start + 2 || input[start] != ':') return null // Aliases start with :
        val aliasEnd = input.indexOf(':', start + 2) // Alias must be at least 1 char in length
        if (aliasEnd == -1) return null // No alias end found
        val fitzpatrickStart = input.indexOf('|', start + 2)
        if (fitzpatrickStart != -1 && fitzpatrickStart < aliasEnd) {
            val emoji = EmojiManager.getForAlias(input.substring(start, fitzpatrickStart)) ?: return null
            // Not a valid alias
            if (!emoji.supportsFitzpatrick()) return null // Fitzpatrick was specified, but the emoji does not support it
            val fitzpatrick =
                Fitzpatrick.fitzpatrickFromType(input.substring(fitzpatrickStart + 1, aliasEnd))
            return AliasCandidate(emoji, fitzpatrick, start, aliasEnd)
        }
        val emoji = EmojiManager.getForAlias(input.substring(start, aliasEnd)) ?: return null
        // Not a valid alias
        return AliasCandidate(emoji, null, start, aliasEnd)
    }

    /**
     * Finds the HTML encoded emoji in the given string starting at the given point, null otherwise
     */
    private fun getHtmlEncodedEmojiAt(input: String, start: Int): AliasCandidate? {
        if (input.length < start + 4 || input[start] != '&' || input[start + 1] != '#') return null
        var longestEmoji: Emoji? = null
        var longestCodePointEnd = -1
        val chars = CharArray(EmojiManager.EMOJI_TRIE.maxDepth)
        var charsIndex = 0
        var codePointStart = start
        do {
            val codePointEnd =
                input.indexOf(';', codePointStart + 3) // Code point must be at least 1 char in length
            if (codePointEnd == -1) break
            charsIndex += try {
                val radix = if (input[codePointStart + 2] == 'x') 16 else 10
                val codePoint = input.substring(codePointStart + 2 + radix / 16, codePointEnd).toInt(radix)
                Character.toChars(codePoint, chars, charsIndex)
            } catch (e: IllegalArgumentException) {
                break
            }
            val foundEmoji = EmojiManager.EMOJI_TRIE.getEmoji(chars, 0, charsIndex)
            if (foundEmoji != null) {
                longestEmoji = foundEmoji
                longestCodePointEnd = codePointEnd
            }
            codePointStart = codePointEnd + 1
        } while (input.length > codePointStart + 4 && input[codePointStart] == '&' && input[codePointStart + 1] == '#' && charsIndex < chars.size &&
            !EmojiManager.EMOJI_TRIE.isEmoji(chars, 0, charsIndex).impossibleMatch()
        )
        return if (longestEmoji == null) null else AliasCandidate(longestEmoji, null, start, longestCodePointEnd)
    }

    /**
     * See [.parseToHtmlDecimal] with the action
     * "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their html decimal
     * representation.
     */
    fun parseToHtmlDecimal(input: String): String? {
        return parseToHtmlDecimal(input, PARSE)
    }

    /**
     * Replaces the emoji's unicode occurrences by their html representation.<br>
     * Example: <code>😄</code> will be replaced by <code>&amp;#128516;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE or REMOVE action, the
     * modifier will be deleted from the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#128102;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored and will remain in the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#128102;🏿</code>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their html decimal
     * representation.
     */
    fun parseToHtmlDecimal(input: String, fitzpatrickAction: FitzpatrickAction): String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                return when (fitzpatrickAction) {
                    PARSE, REMOVE -> emoji.emoji.getHtmlDecimal();
                    IGNORE -> emoji.emoji.getHtmlDecimal() +
                            emoji.fitzpatrickUnicode;
                };
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * See {@link #parseToHtmlHexadecimal(String, FitzpatrickAction)} with the
     * action "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their html hex
     * representation.
     */
    fun parseToHtmlHexadecimal(input: String): String {
        return parseToHtmlHexadecimal(input, PARSE);
    }

    /**
     * Replaces the emoji's unicode occurrences by their html hex
     * representation.<br>
     * Example: <code>👦</code> will be replaced by <code>&amp;#x1f466;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE or REMOVE action, the
     * modifier will be deleted.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#x1f466;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored and will remain in the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#x1f466;🏿</code>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their html hex
     * representation.
     */
    fun parseToHtmlHexadecimal(input: String,fitzpatrickAction: FitzpatrickAction): String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(unicodeCandidate: EmojiResult): String {
                return when (fitzpatrickAction) {
                    PARSE, REMOVE -> unicodeCandidate.emoji.getHtmlHexadecimal();
                    IGNORE -> unicodeCandidate.emoji.getHtmlHexadecimal() +
                            unicodeCandidate.fitzpatrickUnicode;
                };
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * Removes all emojis from a String
     *
     * @param str the string to process
     * @return the string without any emoji
     */
    fun removeAllEmojis(str: String): String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }


    /**
     * Removes a set of emojis from a String
     *
     * @param str            the string to process
     * @param emojisToRemove the emojis to remove from this string
     * @return the string without the emojis that were removed
     */
    fun removeEmojis(str: String, emojisToRemove: Collection<Emoji>):String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                if (!emojisToRemove.contains(emoji.emoji)) {
                    return emoji.emoji.unicode +
                            emoji.fitzpatrickUnicode;
                }
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }

    /**
     * Removes all the emojis in a String except a provided set
     *
     * @param str          the string to process
     * @param emojisToKeep the emojis to keep in this string
     * @return the string without the emojis that were removed
     */
    fun removeAllEmojisExcept(str: String, emojisToKeep: Collection<Emoji>): String {
        val emojiTransformer = object : EmojiTransformer {
            override fun transform(emoji: EmojiResult): String {
                if (emojisToKeep.contains(emoji.emoji)) {
                    return emoji.emoji.unicode +
                            emoji.fitzpatrickUnicode;
                }
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }


    /**
     * Detects all unicode emojis in input string and replaces them with the
     * return value of transformer.transform()
     *
     * @param input       the string to process
     * @param transformer emoji transformer to apply to each emoji
     * @return input string with all emojis transformed
     */
    fun parseFromUnicode(input: String, transformer: EmojiTransformer): String {
        var prev = 0;
        val sb = StringBuilder(input.length);
        val replacements = getEmojies(input);
        for (candidate in replacements) {
            sb.append(input, prev, candidate.emojiStartIndex);

            sb.append(transformer.transform(candidate));
            prev = candidate.endIndex;
        }

        return sb.append(input.substring(prev)).toString();
    }

    /*fun extractEmojiStrings(input: String?): List<String?>? {
        return extractEmojiStrings(input, 0)
    }*/

    fun extractEmojiStrings(input: String, limit: Int = 0): List<String?>? {
        val items = extractEmojis(input, limit)
        val result: MutableList<String?> = ArrayList(items.size)
        for (i in items) {
            result.add(i.toString())
        }
        return result
    }

    /*fun extractEmojis(input: String): List<EmojiResult?>? {
        return getEmojies(input, 0)
    }*/

    fun extractEmojis(input: String, limit: Int = 0): List<EmojiResult> {
        return getEmojies(input, limit)
    }

    /**
     * Generates a list UnicodeCandidates found in input string. A
     * UnicodeCandidate is created for every unicode emoticon found in input
     * string, additionally if Fitzpatrick modifier follows the emoji, it is
     * included in UnicodeCandidate. Finally, it contains start and end index of
     * unicode emoji itself (WITHOUT Fitzpatrick modifier whether it is there or
     * not!).
     *
     * @param input String to find all unicode emojis in
     * @return List of UnicodeCandidates for each unicode emote in text
     */
    fun getEmojies(input: String, limit: Int): List<EmojiResult> {
        var limit = limit
        val inputCharArray = input.toCharArray()
        val candidates: MutableList<EmojiResult> = ArrayList()
        var next: EmojiResult?
        var i = 0
        while (getNextEmoji(inputCharArray, i).also { next = it } != null) {
            next!!
            candidates.add(next!!)
            if (limit != 0) {
                limit--
                if (limit <= 0) break
            }
            i = next!!.endIndex
        }
        return candidates
    }

    fun getEmojies(input: String): List<EmojiResult> {
        return getEmojies(input, 0)
    }

    /**
     * Finds the next UnicodeCandidate after a given starting index
     *
     * @param chars char array to find UnicodeCandidate in
     * @param start starting index for search
     * @return the next UnicodeCandidate or null if no UnicodeCandidate is found after start index
     */
    fun getNextEmoji(chars: CharArray, start: Int): EmojiResult? {
        for (i in start until chars.size) {
            val emoji = getEmojiInPosition(chars, i);
            if (emoji != null)
                return emoji;
        }

        return null;
    }

    fun getEmojiInPosition(chars: CharArray, start: Int): EmojiResult? {
        val emoji = getBestBaseEmoji(chars, start);
        if (emoji == null)
            return null;

        var fitzpatrick: Fitzpatrick? = null;
        var gender: Gender? = null;
        var endPos = start + emoji.unicode.length;
        if (emoji.supportsFitzpatrick) {
            fitzpatrick = Fitzpatrick.find(chars, endPos);
            if (fitzpatrick != null) {
                endPos += 2;
            }
            val gg = findGender(chars, endPos);
            if (gg != null) {
                endPos = gg.endPos + 1;
                gender = gg.gender;
            }
        }

        if (chars.size > endPos) {
            val ch = chars[endPos];
            if (ch == '\uFE0F')
                endPos++;
        }
        return EmojiResult(emoji, fitzpatrick, gender, chars, start, endPos);
    }

    private fun findGender(chars: CharArray, startPos: Int): GenderMatch? {
        val len = chars.size;
        if (len <= startPos)
            return null;
        var pos = startPos;
        val ch = chars[pos];
        if (ch != '\u200D')
            return null;
        pos++;
        val gender = Gender.find(chars, pos) ?: return null;
        return GenderMatch(gender, pos);
    }

    private class GenderMatch(val gender: Gender?, val endPos: Int)


    /**
     * Returns end index of a unicode emoji if it is found in text starting at
     * index startPos, -1 if not found.
     * This returns the longest matching emoji, for example, in
     * "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66"
     * it will find alias:family_man_woman_boy, NOT alias:man
     *
     * @param text     the current text where we are looking for an emoji
     * @param startPos the position in the text where we should start looking for
     *                 an emoji end
     * @return the end index of the unicode emoji starting at startPos. -1 if not
     * found
     */
    fun getBestBaseEmoji(text: CharArray, startPos: Int): Emoji? {
        return EmojiManager.EMOJI_TRIE.getBestEmoji(text, startPos);
    }


    class EmojiResult(
        val emoji: Emoji,
        val fitzpatrick: Fitzpatrick?,
        val gender: Gender?,
        val source: CharArray,
        val emojiStartIndex: Int,
        val endIndex: Int
    ) {

        fun hasFitzpatrick(): Boolean {
            return fitzpatrick != null
        }

        val fitzpatrickType: String
            get() = if (hasFitzpatrick()) fitzpatrick!!.name else ""

        val fitzpatrickUnicode: String
            get() = if (hasFitzpatrick()) fitzpatrick!!.unicode else ""

        val emojiEndIndex: Int
            get() = emojiStartIndex + emoji.unicode.length

        val fitzpatrickEndIndex: Int
            get() = emojiEndIndex + if (fitzpatrick != null) 2 else 0

        private var sub: String? = null

        override fun toString(): String {
            if (sub != null) return sub!!
            val len = endIndex - emojiStartIndex
            val sub = CharArray(len)
            System.arraycopy(source, emojiStartIndex, sub, 0, len)
            this.sub = String(sub)
            return this.sub!!
        }

    }


    private class AliasCandidate (
        val emoji: Emoji,
        val fitzpatrick: Fitzpatrick?,
        val startIndex: Int,
        val endIndex: Int
    )

    /**
     * Enum used to indicate what should be done when a Fitzpatrick modifier is
     * found.
     */
    enum class FitzpatrickAction {
        /**
         * Tries to match the Fitzpatrick modifier with the previous emoji
         */
        PARSE,

        /**
         * Removes the Fitzpatrick modifier from the string
         */
        REMOVE,

        /**
         * Ignores the Fitzpatrick modifier (it will stay in the string)
         */
        IGNORE
    }

    interface EmojiTransformer {
        fun transform(emoji: EmojiResult): String
    }


}

fun main() {
    val text =
        "\uD83D\uDC68\u200D\uD83D\uDCBB\uD83E\uDDB9\uD83C\uDFFE\uD83E\uDDD1\uD83C\uDFFD\u200D\uD83D\uDD2C\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83C\uDF73\uD83D\uDC70\uD83C\uDFFE\uD83E\uDDDB\uD83C\uDFFD\u200D♂️\uD83E\uDD31\uD83C\uDFFF\uD83D\uDC68\uD83C\uDFFC\u200D\uD83C\uDFEB\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83C\uDF73\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83C\uDF73\uD83D\uDC73\uD83C\uDFFB\u200D♂️"
    val items = extractEmojiStrings(text)
    println(items)
    val res = isEmoji("\uD83E\uDDDB\uD83C\uDFFD\u200D♂️ ")
    println(res)
}