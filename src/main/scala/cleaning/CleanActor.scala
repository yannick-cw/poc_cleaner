package cleaning

import akka.actor.{Actor, Props}
import rest_connection.{CleanedText, RawText}
import java.lang.StringBuilder
import java.io._
import scala._
import scala.io.Source
/**
  * Created by yannick on 14.05.16.
  */
object CleanActor {
  val props = Props(new CleanActor())
  val name = "clean-actor"
}

class CleanActor extends Actor {

  def receive: Receive = {
    case RawText(text) => sender ! CleanedText(stem(text))
  }

  def stem(sentence: String): String = {
    //toDO: remove stopwords
    var tmpArr= sentence.replaceAll("[^A-Za-z0-9 ]","").toLowerCase().trim().split(" +").toList

    val result: List[String] = tmpArr.map(word => step_5(step_4(step_3(step_2(step_1(word))))))
    result.reduce(_ + " " + _)
  }

  def step_1(str: String): String = step_1_c(step_1_b(step_1_a(str)))

  def step_1_a(str: String): String = replacePatterns(str, List(("sses", "ss"), ("ies", "i"), ("ss", "ss"), ("s", "")), _ >= 0)

  def step_1_b(str: String): String = {
    // (m > 0) EED -> EE
    if (str.endsWith("eed")) {
      if (stringMeasure(str.substring(0, str.length - 3)) > 0)
        return str.substring(0, str.length() - 1)
      // (*v*) ED ->
    } else if ((str.endsWith("ed")) &&
      (containsVowel(str.substring(0, str.length - 2)))) {
      return step_1_b_2(str.substring(0, str.length - 2))
      // (*v*) ING ->
    } else if ((str.endsWith("ing")) &&
      (containsVowel(str.substring(0, str.length - 3)))) {
      return step_1_b_2(str.substring(0, str.length - 3))
    } // end if
    str
  } // end step1b

  def step_1_b_2(str: String): String = {

    if (str.endsWith("at") ||
      str.endsWith("bl") ||
      str.endsWith("iz")) {
      return str + "e";
    }
    else if ((str.length() > 1) && (endsWithDoubleConsonent(str)) &&
      (!(str.endsWith("l") || str.endsWith("s") || str.endsWith("z")))) {
      return str.substring(0, str.length() - 1);
    }
    else if ((stringMeasure(str) == 1) &&
      (endsWithCVC(str))) {
      return str + "e"
    }
    str
  }

  /*
   *     (*v*) Y -> I                    happy        ->  happi
   *                                     sky          ->  sky
   */
  def step_1_c(str: String): String = {
    if (str.endsWith("y") && containsVowel(str.substring(0, str.length() - 1)))
      return str.substring(0, str.length() - 1) + "i"
    str
  } // end step1c

  def step_2(str: String): String = replacePatterns(str, List(("ational", "ate"), ("tional", "tion"), ("enci", "ence"), ("anci", "ance"),
    ("izer", "ize"), ("bli", "ble"), ("alli", "al"), ("entli", "ent"), ("eli", "e"),
    ("ousli", "ous"), ("ization", "ize"), ("ation", "ate"), ("ator", "ate"), ("alism", "al"),
    ("iveness", "ive"), ("fulness", "ful"), ("ousness", "ous"), ("aliti", "al"), ("iviti", "ive"),
    ("biliti", "ble"), ("logi", "log")))

  def step_3(str: String): String = replacePatterns(str, List(("icate", "ic"), ("ative", ""), ("alize", "al"), ("iciti", "ic"), ("ical", "ic"), ("ful", ""), ("ness", "")))

  def step_4(str: String): String = {
    val res: String = replacePatterns(str, List(("al", ""), ("ance", ""), ("ence", ""), ("er", ""), ("ic", ""), ("able", ""), ("ible", ""), ("ant", ""), ("ement", ""),
      ("ment", ""), ("ent", ""), ("ou", ""), ("ism", ""), ("ate", ""), ("iti", ""), ("ous", ""),
      ("ive", ""), ("ize", "")), _ > 1)
    if (str == res) {
      if ((str.endsWith("sion") || str.endsWith("tion")) && stringMeasure(str.substring(0, str.length() - 3)) > 1)
        return str.substring(0, str.length() - 3)
      else
        return str
    }
    else {
      return res
    }
  }

  def step_5(str: String): String = step_5_b(step_5_a(str))

  def step_5_a(str: String): String = {
    // (m > 1) E ->
    if ((stringMeasure(str.substring(0, str.length() - 1)) > 1) &&
      str.endsWith("e"))
      return str.substring(0, str.length() - 1)
    // (m = 1 and not *0) E ->
    else if ((stringMeasure(str.substring(0, str.length() - 1)) == 1) &&
      (!endsWithCVC(str.substring(0, str.length() - 1))) &&
      (str.endsWith("e")))
      return str.substring(0, str.length() - 1)
    else
      return str
  } // end step5a

  def step_5_b(str: String): String = {
    // (m > 1 and *d and *L) ->
    if (str.endsWith("l") &&
      endsWithDoubleConsonent(str) &&
      (stringMeasure(str.substring(0, str.length() - 1)) > 1)) {
      str.substring(0, str.length() - 1)
    } else {
      str
    }
  } // end step5b

  // does string contain a vowel?
  def containsVowel(str: String): Boolean = {
    for (ch <- str toList) {
      if (isVowel(ch))
        return true
    }
    // no aeiou but there is y
    if (str.indexOf('y') > -1)
      return true
    else
      false
  } // end function

  // is char a vowel?
  def isVowel(c: Char): Boolean = {
    for (ch <- "aeiou" toList)
      if (c == ch)
        return true
    false
  } // end function

  /*
   * Special check for 'y', since it may be both vowel and consonent depending on surrounding letters
   */
  def isVowel(str: String, i: Int): Boolean = {
    for (ch <- "aeiou" toList)
      if (str(i) == ch || (str(i) == 'y' && i > 0 && i + 1 < str.length && !isVowel(str(i - 1)) && !isVowel(str(i + 1))))
        return true
    false
  } // end function

  // returns a CVC measure for the string
  def stringMeasure(str: String): Int = {
    var count = 0
    var vowelSeen: Boolean = false

    for (i <- 0 to str.length - 1) {
      if (isVowel(str, i)) {
        vowelSeen = true
      } else if (vowelSeen) {
        count += 1
        vowelSeen = false
      }
    }
    count
  } // end function

  // does stem end with CVC?
  def endsWithCVC(str: String): Boolean = {
    if (str.length() >= 3) {
      val cvc = (str(str.length - 1), str(str.length - 2), str(str.length - 3))
      val cvc_str = cvc._1.toString + cvc._2 + cvc._3

      if ((cvc._1 == 'w') || (cvc._1 == 'x') || (cvc._1 == 'y'))
        false
      else if (!isVowel(cvc._1) && isVowel(cvc_str, 1) && !isVowel(cvc._3))
        true
      else
        false
    }
    else
      false
  } // end function

  // does string end with a double consonent?
  def endsWithDoubleConsonent(str: String): Boolean = {
    val c: Char = str.charAt(str.length() - 1);
    if (c == str.charAt(str.length() - 2))
      if (!containsVowel(str.substring(str.length() - 2))) {
        return true
      }
    false
  } // end function

  def replacePatterns(str: String, patterns: List[(String, String)]): String = replacePatterns(str, patterns, _ > 0)

  def replaceLast(str: String, pattern: String, replacement: String) = new StringBuilder(str).replace(str.lastIndexOf(pattern), str.lastIndexOf(pattern) + pattern.length, replacement).toString

  def replacePatterns(str: String, patterns: List[(String, String)], comparer: Int => Boolean): String = {
    for (pattern <- patterns)
      if (str.endsWith(pattern._1)) {
        val res = replaceLast(str, pattern._1, pattern._2)
        if (comparer(stringMeasure(replaceLast(str, pattern._1, ""))))
          return res
        else
          return str
      }
    str
  }
}

