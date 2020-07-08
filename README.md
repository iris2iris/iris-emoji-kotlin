# iris:emoji-kotlin

[![License Info](http://img.shields.io/badge/license-The%20MIT%20License-brightgreen.svg)](https://github.com/vdurmont/emoji-java/blob/master/LICENSE.md)

_The missing emoji library for Kotlin._

**[iris:emoji-kotlin](https://github.com/iris2iris/iris-emoji-kotlin)** is a lightweight Kotlin library that helps you use Emojis in your java applications.

It is copy of my Java repo **[iris:emoji-java](https://github.com/iris2iris/iris-emoji-java)**.

And it in its turn was forked from **[emoji-java](https://github.com/vdurmont/emoji-java)**

#### Reasons I forked it:

- ❗️ The most important reason I forked it was I have found how to improve its __speed up to 5x__ times!
- My repo supports complex emojis of combination `{basic_emoji}{skin_color}?{gender}?`
- It supports of bugged VK.COM web-version emojis, where ending char `\uFE0F` is absent

And of course, extend useful methods.  

## How to get it?

##### Via Direct Download:

- Use [releases](https://github.com/iris2iris/iris-emoji-kotlin/releases) tab to download the jar directly.
- Download JSON-java dependency from http://mvnrepository.com/artifact/org.json/json.

## How to use it?

### EmojiManager

The `EmojiManager` provides several static methods to search through the emojis database:

- `getForTag` returns all the emojis for a given tag
- `getForAlias` returns the emoji for an alias
- `getAll` returns all the emojis
- `isEmoji` checks if a string is an emoji
- `containsEmoji` checks if a string contains any emoji

You can also query the metadata:

- `getAllTags` returns the available tags

Or get everything:

- `getAll` returns all the emojis

### Emoji model

An `Emoji` is a POJO (plain old java object), which provides the following methods:

- `getUnicode` returns the unicode representation of the emoji
- `getUnicode(Fitzpatrick)` returns the unicode representation of the emoji with the provided Fitzpatrick modifier. If the emoji doesn't support the Fitzpatrick modifiers, this method will throw an `UnsupportedOperationException`. If the provided Fitzpatrick is null, this method will return the unicode of the emoji.
- `getDescription` returns the (optional) description of the emoji
- `getAliases` returns a list of aliases for this emoji
- `getTags` returns a list of tags for this emoji
- `getHtmlDecimal` returns an html decimal representation of the emoji
- `getHtmlHexadecimal` returns an html decimal representation of the emoji
- `supportsFitzpatrick` returns true if the emoji supports the Fitzpatrick modifiers, else false

### Fitzpatrick modifiers

Some emojis now support the use of Fitzpatrick modifiers that gives the choice between 5 shades of skin tones:

| Modifier | Type     |
| :------: | -------- |
|    🏻    | type_1_2 |
|    🏼    | type_3   |
|    🏽    | type_4   |
|    🏾    | type_5   |
|    🏿    | type_6   |

We defined the format of the aliases including a Fitzpatrick modifier as:

```
:ALIAS|TYPE:
```

A few examples:

```
:boy|type_1_2:
:swimmer|type_4:
:santa|type_6:
```

### EmojiParser

#### To unicode

To replace all the aliases and the html representations found in a string by their unicode, use `EmojiParser#parseToUnicode(String)`.

For example:

```kotlin
val str = "An :grinning:awesome :smiley:string &#128516;with a few :wink:emojis!"
val result = EmojiParser.parseToUnicode(str)
println(result)
// Prints:
// "An 😀awesome 😃string 😄with a few 😉emojis!"
```

#### To aliases

To replace all the emoji's unicodes found in a string by their aliases, use `EmojiParser#parseToAliases(String)`.

For example:

```kotlin
val str = "An 😀awesome 😃string with a few 😉emojis!"
val result = EmojiParser.parseToAliases(str)
println(result)
// Prints:
// "An :grinning:awesome :smiley:string with a few :wink:emojis!"
```

By default, the aliases will parse and include any Fitzpatrick modifier that would be provided. If you want to remove or ignore the Fitzpatrick modifiers, use `EmojiParser#parseToAliases(String, FitzpatrickAction)`. Examples:

```kotlin
val str = "Here is a boy: \uD83D\uDC66\uD83C\uDFFF!"
println(EmojiParser.parseToAliases(str))
println(EmojiParser.parseToAliases(str, FitzpatrickAction.PARSE))
// Prints twice: "Here is a boy: :boy|type_6:!"
println(EmojiParser.parseToAliases(str, FitzpatrickAction.REMOVE))
// Prints: "Here is a boy: :boy:!"
println(EmojiParser.parseToAliases(str, FitzpatrickAction.IGNORE))
// Prints: "Here is a boy: :boy:🏿!"
```

#### To html

To replace all the emoji's unicodes found in a string by their html representation, use `EmojiParser#parseToHtmlDecimal(String)` or `EmojiParser#parseToHtmlHexadecimal(String)`.

For example:

```kotlin
val str = "An 😀awesome 😃string with a few 😉emojis!"

val resultDecimal = EmojiParser.parseToHtmlDecimal(str)
println(resultDecimal)
// Prints:
// "An &#128512;awesome &#128515;string with a few &#128521;emojis!"

val resultHexadecimal = EmojiParser.parseToHtmlHexadecimal(str)
println(resultHexadecimal)
// Prints:
// "An &#x1f600;awesome &#x1f603;string with a few &#x1f609;emojis!"
```

By default, any Fitzpatrick modifier will be removed. If you want to ignore the Fitzpatrick modifiers, use `EmojiParser#parseToAliases(String, FitzpatrickAction)`. Examples:

```kotlin
val str = "Here is a boy: \uD83D\uDC66\uD83C\uDFFF!"
println(EmojiParser.parseToHtmlDecimal(str))
println(EmojiParser.parseToHtmlDecimal(str, FitzpatrickAction.PARSE))
println(EmojiParser.parseToHtmlDecimal(str, FitzpatrickAction.REMOVE))
// Print 3 times: "Here is a boy: &#128102;!"
println(EmojiParser.parseToHtmlDecimal(str, FitzpatrickAction.IGNORE))
// Prints: "Here is a boy: &#128102;🏿!"
```

The same applies for the methods `EmojiParser#parseToHtmlHexadecimal(String)` and `EmojiParser#parseToHtmlHexadecimal(String, FitzpatrickAction)`.

#### Remove emojis

You can easily remove emojis from a string using one of the following methods:

- `EmojiParser#removeAllEmojis(String)`: removes all the emojis from the String
- `EmojiParser#removeAllEmojisExcept(String, Collection<Emoji>)`: removes all the emojis from the String, except the ones in the Collection
- `EmojiParser#removeEmojis(String, Collection<Emoji>)`: removes the emojis in the Collection from the String

For example:

```kotlin
val str = "An 😀awesome 😃string with a few 😉emojis!"
val collection = ArrayList<Emoji>()
collection.add(EmojiManager.getForAlias("wink")) // This is 😉

println(EmojiParser.removeAllEmojis(str))
println(EmojiParser.removeAllEmojisExcept(str, collection))
println(EmojiParser.removeEmojis(str, collection))

// Prints:
// "An awesome string with a few emojis!"
// "An awesome string with a few 😉emojis!"
// "An 😀awesome 😃string with a few emojis!"
```

#### Extract Emojis from a string

You can search a string of mixed emoji/non-emoji characters and have all of the emoji characters returned as a Collection.

- `EmojiParser#extractEmojis(String)`: returns all emojis as a Collection. This will include duplicates if emojis are present more than once.

## Credits

**iris-emoji-java** is based on [github/vdurmont/emoji-java](https://github.com/vdurmont/emoji-java).

And in its turn **emoji-java** originally used the data provided by the [github/gemoji project](https://github.com/github/gemoji). It is still based on it but has evolved since.

## Available Emojis

See a table of the available emojis and their aliases [HERE](./EMOJIS.md).
