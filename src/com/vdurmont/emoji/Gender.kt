package com.vdurmont.emoji

/**
 * Создано 07.07.2020
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]
 */
enum class Gender(val unicode: String) {
	MALE("♂️"), FEMALE("♀️");

	companion object {
		fun genderFromUnicode(unicode: String): Gender? {
			for (v in values()) {
				if (v.unicode == unicode) {
					return v
				}
			}
			return null
		}

		fun genderFromType(type: String): Gender? {
			return try {
				valueOf(type.toUpperCase())
			} catch (e: IllegalArgumentException) {
				null
			}
		}

		fun find(chars: CharArray, startPos: Int): Gender? {
			if (startPos >= chars.size) return null
			val ch = chars[startPos]
			when (ch) {
				'♂' -> return MALE
				'♀' -> return FEMALE
			}
			return null
		}
	}

}