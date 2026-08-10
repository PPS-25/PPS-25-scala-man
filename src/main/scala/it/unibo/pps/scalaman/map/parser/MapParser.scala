package it.unibo.pps.scalaman.map.parser

import it.unibo.pps.scalaman.model.map.Cell
import it.unibo.pps.scalaman.model.map.MapParseError
import it.unibo.pps.scalaman.model.map.RawMap

object MapParser:
  /** Converts a raw textual map into the intermediate `RawMap` representation.
    *
    * The parser only checks syntax and symbol support. Gameplay constraints are deliberately
    * deferred to the validation layer.
    */
  def parse(text: String): Either[List[MapParseError], RawMap] =
    val lines = text.linesIterator.toVector
    if isEmptyMap(lines) then Left(List(MapParseError.EmptyMap))
    else
      val expectedWidth = lines.head.length
      val errors = syntaxErrors(lines, expectedWidth)

      if errors.nonEmpty then Left(errors)
      else Right(RawMap(lines.map(parseRow)))

  private def isEmptyMap(lines: Vector[String]): Boolean =
    lines.isEmpty || lines.forall(_.isEmpty)

  private def syntaxErrors(lines: Vector[String], expectedWidth: Int): List[MapParseError] =
    lines.zipWithIndex.flatMap { case (line, rowIndex) =>
      rowErrors(line, rowIndex, expectedWidth)
    }.toList

  private def rowErrors(line: String, rowIndex: Int, expectedWidth: Int): List[MapParseError] =
    val raggedRowError =
      if line.length == expectedWidth then Nil
      else List(MapParseError.RaggedRow(rowIndex, expectedWidth, line.length))

    raggedRowError ++ unsupportedSymbolErrors(line, rowIndex)

  private def unsupportedSymbolErrors(line: String, rowIndex: Int): List[MapParseError] =
    line.zipWithIndex.collect {
      case (char, colIndex) if supportedCell(char).isEmpty =>
        MapParseError.UnsupportedSymbol(char, rowIndex, colIndex)
    }.toList

  private def parseRow(line: String): Vector[Cell] =
    line.iterator.map(parseCell).toVector

  private def parseCell(char: Char): Cell =
    supportedCell(char).getOrElse(
      throw new IllegalArgumentException(s"Unsupported symbol '$char' reached parser output")
    )

  private def supportedCell(char: Char): Option[Cell] =
    char match
      case '#'                    => Some(Cell.Wall)
      case '.'                    => Some(Cell.Floor)
      case 'S'                    => Some(Cell.Spawn)
      case 'C'                    => Some(Cell.Collectible)
      case 'H'                    => Some(Cell.Hunter)
      case 'A'                    => Some(Cell.Anticipator)
      case 'I'                    => Some(Cell.InvulnerabilityBonus)
      case 'R'                    => Some(Cell.SlowdownBonus)
      case digit if digit.isDigit => Some(Cell.Teleport(digit.asDigit))
      case _                      => None
