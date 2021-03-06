package com.github.chengpohi

import ammonite.terminal.FilterTools._
import ammonite.terminal.GUILikeFilters.{wordLeft, wordRight}
import ammonite.terminal.LazyList._
import ammonite.terminal.ReadlineFilters.CutPasteFilter
import ammonite.terminal.TermCore.Filter
import ammonite.terminal.{TermInfo, ReadlineFilters, TermCore}

/**
 * Ammonite VI Mode
 * Created by chengpohi on 12/5/15.
 */
object VIFilters {
  var VI_MODE = true
  var VISUAL_MODE = true
  lazy val cutPasteFilter = CutPasteFilter()

  val viFilter = {
    enableViFilter orElse
      viSingleKeyFilter orElse
      viNavFilter orElse
      viEditModeFilter
  }

  def enableViFilter: TermCore.Filter = {
    case TS(27 ~: 13 ~: rest, b, c) => {
      VI_MODE = !VI_MODE
      VISUAL_MODE = VI_MODE
      TS(rest, b, c)
    }
    case TS(27 ~: 10 ~: rest, b, c) => {
      VI_MODE = !VI_MODE
      VISUAL_MODE = VI_MODE
      TS(rest, b, c)
    }
  }

  def viSingleKeyFilter: Filter = {
    case TS(27 ~: rest, b, c) if VI_MODE =>
      VISUAL_MODE = true
      TS(rest, b, c)
  }

  def viEditModeFilter: Filter = {
    case TS('i' ~: rest, b, c) if VISUAL_MODE =>
      VISUAL_MODE = false
      TS(rest, b, c)
    case TS('a' ~: rest, b, c) if VISUAL_MODE =>
      VISUAL_MODE = false
      TS(rest, b, c + 1)
    case TS('x' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b patch(from = c, patch = Nil, replaced = 1), c)
    case TS('d' ~: 'd' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b.take(0), 0)
    case TS('d' ~: 'w' ~: rest, b, c) if VISUAL_MODE =>
      val right: (Vector[Char], Int) = cutPasteFilter.cutWordRight(b, c)
      TS(rest, right._1, right._2)
    case TS('c' ~: 'w' ~: rest, b, c) if VISUAL_MODE =>
      VISUAL_MODE = false
      val right: (Vector[Char], Int) = cutPasteFilter.cutWordRight(b, c)
      TS(rest, right._1, right._2)
    case TS('D' ~: rest, b, c) if VISUAL_MODE =>
      val right: (Vector[Char], Int) = cutPasteFilter.cutAllRight(b, c)
      TS(rest, right._1, right._2)
    case TS(char ~: rest, b, c) if VIFilters.VISUAL_MODE =>
      TS(rest, b, c)
  }

  def viNavFilter: Filter = {
    case TS('h' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b, c - 1)
    case TS('l' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b, c + 1)
    case TS('0' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b, 0)
    case TS('b' ~: rest, b, c) if VISUAL_MODE =>
      val left = wordLeft(b, c)
      TS(rest, left._1, left._2)
    case TS('w' ~: rest, b, c) if VISUAL_MODE =>
      val left = wordRight(b, c)
      TS(rest, left._1, left._2)
    case TS('$' ~: rest, b, c) if VISUAL_MODE =>
      TS(rest, b, b.size)
  }

  case class VIHistoryFilter(history: () => Seq[String]) extends TermCore.DelegateFilter {
    val historyFilter = ReadlineFilters.HistoryFilter(history)
    var oldHistoryLength: Int = history().length

    def checkOldHistoryLength() = if (history().length > oldHistoryLength) {
      oldHistoryLength = history().length
      historyFilter.index = -1
    }

    override def filter: Filter = {
      case TermInfo(TS(p"j$rest", b, c), w) if VISUAL_MODE && lastRow(c, b, w) =>
        checkOldHistoryLength()
        historyFilter.nextHistory(b, rest)
      case TermInfo(TS(p"k$rest", b, c), w) if VISUAL_MODE && firstRow(c, b, w) =>
        checkOldHistoryLength()
        historyFilter.previousHistory(b, rest)
    }
  }

}
