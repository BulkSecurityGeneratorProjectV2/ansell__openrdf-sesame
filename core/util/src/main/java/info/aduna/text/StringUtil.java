/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package info.aduna.text;

import java.util.ArrayList;

public class StringUtil {

	/**
	 * The minimum length of initial text.
	 */
	private static final int MIN_INITIAL_TEXT_LENGTH = 3;

	/**
	 * The maximum length of derived initial text.
	 */
	private static final int MAX_INITIAL_TEXT_LENGTH = 250;

	/**
	 * Substitute String "old" by String "new" in String "text" everywhere. This
	 * is static util function that I could not place anywhere more appropriate.
	 * The name of this function is from the good-old awk time.
	 * 
	 * @param olds
	 *        The String to be substituted.
	 * @param news
	 *        The String is the new content.
	 * @param text
	 *        The String in which the substitution is done.
	 * @return The result String containing the substitutions; if no
	 *         substitutions were made, the result is 'text'.
	 */
	public static String gsub(String olds, String news, String text) {
		if (olds == null || olds.length() == 0) {
			// Nothing to substitute.
			return text;
		}
		if (text == null) {
			return null;
		}

		// Search for any occurences of 'olds'.
		int oldsIndex = text.indexOf(olds);
		if (oldsIndex == -1) {
			// Nothing to substitute.
			return text;
		}

		// We're going to do some substitutions.
		StringBuilder buf = new StringBuilder(text.length());
		int prevIndex = 0;

		while (oldsIndex >= 0) {
			// First, add the text between the previous and the current
			// occurence.
			buf.append(text.substring(prevIndex, oldsIndex));

			// Then add the substition pattern
			buf.append(news);

			// Remember the index for the next loop.
			prevIndex = oldsIndex + olds.length();

			// Search for the next occurence.
			oldsIndex = text.indexOf(olds, prevIndex);
		}

		// Add the part after the last occurence.
		buf.append(text.substring(prevIndex));

		return buf.toString();
	}

	/**
	 * Returns all text occurring after the specified separator character, or the
	 * entire string when the seperator char does not occur.
	 * 
	 * @param string
	 *        The string of which the substring needs to be determined.
	 * @param separatorChar
	 *        The character to look for.
	 * @return All text occurring after the separator character, or the entire
	 *         string when the character does not occur.
	 */
	public static String getAllAfter(String string, char separatorChar) {
		int index = string.indexOf(separatorChar);
		if (index < 0 || index == string.length() - 1) {
			return string;
		}
		else {
			return string.substring(index + 1);
		}
	}

	/**
	 * Returns all text occurring before the specified separator character, or
	 * the entire string when the seperator char does not occur.
	 * 
	 * @param string
	 *        The string of which the substring needs to be determined.
	 * @param separatorChar
	 *        The character to look for.
	 * @return All text occurring before the separator character, or the entire
	 *         string when the character does not occur.
	 */
	public static String getAllBefore(String string, char separatorChar) {
		int index = string.indexOf(separatorChar);
		return index <= 0 ? string : string.substring(0, index - 1);
	}

	/**
	 * Encodes an array of Strings into a single String than can be decoded to
	 * the original array using the corresponding decode method. Useful for e.g.
	 * storing an array of Strings as a single entry in a Preferences node.
	 */
	public static String encodeArray(String[] array) {
		StringBuilder buffer = new StringBuilder();
		int nrItems = array.length;

		for (int i = 0; i < nrItems; i++) {
			String item = array[i];
			item = StringUtil.gsub("_", "__", item);
			buffer.append(item);

			if (i < nrItems - 1) {
				buffer.append("_.");
			}
		}

		return buffer.toString();
	}

	/**
	 * Decodes a String generated by encodeArray.
	 */
	public static String[] decodeArray(String encodedArray) {
		String[] items = encodedArray.split("_\\.");
		ArrayList<String> list = new ArrayList<String>();

		for (int i = 0; i < items.length; i++) {
			String item = items[i];
			item = gsub("__", "_", item);
			if (!item.equals("")) {
				list.add(item);
			}
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Derives the initial text from the supplied text. The returned text
	 * excludes whitespace and other special characters and is useful for display
	 * purposes (e.g. previews).
	 */
	public static String deriveInitialText(String text) {
		String result = null;

		int startIdx = 0; // index of the first text character
		int endIdx = 0; // index of the first char after the end of the text
		int textLength = text.length();

		while (startIdx < textLength && result == null) {
			startIdx = endIdx;

			// skip until first/next text character
			while (startIdx < textLength && !isInitialTextStartChar(text.charAt(startIdx))) {
				startIdx++;
			}

			// try to find an initial text of a sufficient length
			endIdx = startIdx + 1;
			while (endIdx < textLength && ((endIdx - startIdx) < MAX_INITIAL_TEXT_LENGTH)
					&& isInitialTextChar(text.charAt(endIdx)))
			{
				endIdx++;
			}

			if (endIdx - startIdx >= MIN_INITIAL_TEXT_LENGTH) {
				// get candidate text. The text is trimmed to remove any spaces
				// at the end. This will prevent texts like "A " to be accepted.
				String candidateText = text.substring(startIdx, endIdx).trim();
				if (!isGarbageText(candidateText)) {
					result = candidateText;
				}
			}
		}

		return result;
	}

	/**
	 * Titles shorter than MIN_TITLE_LENGTH and long titles that don't contain a
	 * single space character are considered to be garbage.
	 */
	public static boolean isGarbageText(String text) {
		boolean result = false;

		if (text.trim().length() < MIN_INITIAL_TEXT_LENGTH) {
			result = true;
		}
		else if (text.length() > 30) {
			result = true;

			for (int i = 0; i < text.length(); i++) {
				if (Character.getType(text.charAt(i)) == Character.SPACE_SEPARATOR) {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Appends the specified character <tt>n</tt> times to the supplied
	 * StringBuilder.
	 * 
	 * @param c
	 *        The character to append.
	 * @param n
	 *        The number of times the character should be appended.
	 * @param sb
	 *        The StringBuilder to append the character(s) to.
	 */
	public static void appendN(char c, int n, StringBuilder sb) {
		for (int i = n; i > 0; i--) {
			sb.append(c);
		}
	}

	/**
	 * Removes the double quote from the start and end of the supplied string if
	 * it starts and ends with this character. This method does not create a new
	 * string if <tt>text</tt> doesn't start and end with double quotes, the
	 * <tt>text</tt> object itself is returned in that case.
	 * 
	 * @param text
	 *        The string to remove the double quotes from.
	 * @return The trimmed string, or a reference to <tt>text</tt> if it did
	 *         not start and end with double quotes.
	 */
	public static String trimDoubleQuotes(String text) {
		int textLength = text.length();

		if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
			return text.substring(1, textLength - 1);
		}

		return text;
	}

	// A nice overview of Unicode character categories can be found at:
	// http://oss.software.ibm.com/cgi-bin/icu/ub

	private static boolean isInitialTextStartChar(char c) {
		int charType = Character.getType(c);

		return charType == Character.UPPERCASE_LETTER || charType == Character.LOWERCASE_LETTER
				|| charType == Character.TITLECASE_LETTER || charType == Character.MODIFIER_LETTER
				|| charType == Character.OTHER_LETTER || charType == Character.DECIMAL_DIGIT_NUMBER
				|| charType == Character.START_PUNCTUATION || charType == Character.INITIAL_QUOTE_PUNCTUATION;
	}

	private static boolean isInitialTextChar(char c) {
		int charType = Character.getType(c);

		return charType == Character.UPPERCASE_LETTER || charType == Character.LOWERCASE_LETTER
				|| charType == Character.TITLECASE_LETTER || charType == Character.MODIFIER_LETTER
				|| charType == Character.OTHER_LETTER || charType == Character.DECIMAL_DIGIT_NUMBER
				|| charType == Character.SPACE_SEPARATOR || charType == Character.CONNECTOR_PUNCTUATION
				|| charType == Character.DASH_PUNCTUATION || charType == Character.START_PUNCTUATION
				|| charType == Character.END_PUNCTUATION || charType == Character.INITIAL_QUOTE_PUNCTUATION
				|| charType == Character.FINAL_QUOTE_PUNCTUATION || charType == Character.OTHER_PUNCTUATION;
	}

	/**
	 * Concatenate a number of Strings. This implementation uses a StringBuilder.
	 * 
	 * @param strings
	 *        the String to concatenate
	 * @return a String that is the results of concatenating the input strings.
	 */
	public static String concat(String... strings) {
		// Determine total length of concatenated string to prevent expensive char
		// array copies for growing StringBuilder's internal array
		int totalLength = 0;
		for (String s : strings) {
			totalLength += s.length();
		}

		StringBuilder result = new StringBuilder(totalLength);
		for (String string : strings) {
			result.append(string);
		}

		return result.toString();
	}
}
