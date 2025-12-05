package com.bitflaker.lucidsourcekit.utils.export

import kotlin.text.iterator

class MarkDownEscaper {
     companion object {
          private val escapeMapFull: HashMap<Char, String> = hashMapOf(
               Pair('<', "&lt;"),
               Pair('>', "&gt;"),
               Pair('*', "&ast;"),
               Pair('#', "\u2317"),
               Pair('|', "&vert;"),
               Pair('`', "&grave;"),
               Pair('[', "&lbrack;"),
               Pair(']', "&rbrack;"),
               Pair('-', "&minus;"),
               Pair('_', "&lowbar;"),
               Pair('~', "&tilde;"),
               Pair('.', "&period;"),
               Pair('(', "&lpar;"),
               Pair(')', "&rpar;"),
               Pair('+', "&plus;"),
               Pair('!', "&excl;"),
               Pair('{', "&lbrace;"),
               Pair('}', "&rbrace;"),
               Pair('\\', "&bsol;")
          )

          private val escapeMapBlock: HashMap<Char, String> = hashMapOf(
               Pair('#', "\u2317")
          )

          fun escape(text: String?): String {
               if (text == null) return ""
               return escapeImpl(text, escapeMapFull)
          }

          fun blockEscape(text: String?): String {
               if (text == null) return ""
               return escapeImpl(text, escapeMapBlock)
          }

          fun finalizeEscapedHtml(text: String): String {
               return text.replace("\u00B6", "<br />")
                    .replace("href", "nhref")
          }

          private fun escapeImpl(text: String, map: java.util.HashMap<Char, String>): String {
               val escaped = StringBuilder()
               val newLinesEscaped = text.replace("\r\n", "\u00B6")
                    .replace("\r", "\u00B6")
                    .replace("\n", "\u00B6")
               for (c in newLinesEscaped) {
                    escaped.append(map.getOrDefault(c, c))
               }
               return escaped.toString()
          }
     }
}