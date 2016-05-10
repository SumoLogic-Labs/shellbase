package com.sumologic.shellbase.table

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ASCIITableTest extends CommonWordSpec {
  "A ASCIITable" should {

    "print a nice table given some data" in {

      val sut = setupCityTable
      sut.bare = false

      val lines = sut.renderLines(cities)
      lines.foreach(println)
      lines.size should be(cities.size + 4)
      ensureAllCities(lines)
    }

    "print a bare table given some data" in {
      val sut = setupCityTable
      val lines = sut.renderLines(cities)
      lines.foreach(println)
      lines.size should be(cities.size + 2)
      ensureAllCities(lines)
    }

    "render an empty cell value when null values are included" in {
      val sut = new ASCIITable[City]
      sut.addColumn("City", 30, _.name)
      sut.emptyCell = "gurr"
      val lines = sut.renderLines(List(City(null, 2, 3)))
      lines(2).contains("gurr") should be(true)
    }

    "trim strings when they exceed the maximum width" when {
      "it is in the header" in {
        val sut = new ASCIITable[City]
        sut.addColumn("1234567890", 5, _.name)
        val lines = sut.renderLines(List(City("1234567890", 2, 3)))
        lines(0).contains("5") should be(true)
        lines(0).contains("6") should be(false)
      }

      "it is in a row" in {
        val sut = new ASCIITable[City]
        sut.addColumn("City", 5, _.name)
        val lines = sut.renderLines(List(City("1234567890", 2, 3)))
        lines(2).contains("5") should be(true)
        lines(2).contains("6") should be(false)
      }
    }

    "not trim strings when disabled" when {
      "it is in the header" in {
        val sut = new ASCIITable[City]
        sut.addColumn("1234567890", 5, _.name)
        sut.trimValues = false
        val lines = sut.renderLines(List(City("1234567890", 2, 3)))
        lines(0).contains("5") should be(true)
        lines(0).contains("6") should be(true)
      }

      "it is in a row" in {
        val sut = new ASCIITable[City]
        sut.addColumn("City", 5, _.name)
        sut.trimValues = false
        val lines = sut.renderLines(List(City("1234567890", 2, 3)))
        lines(2).contains("5") should be(true)
        lines(2).contains("6") should be(true)
      }
    }

    "not accept rows with negative or 0 width" in {
      val sut = new ASCIITable[City]
      the[IllegalArgumentException] thrownBy {
        sut.addColumn("City", 0, _.name)
      }

      the[IllegalArgumentException] thrownBy {
        sut.addColumn("City", -1, _.name)
      }
    }

    "throw an exception if no data is passed" in {
      val sut = new ASCIITable[City]
      sut.addColumn("City", 5, _.name)
      the[IllegalArgumentException] thrownBy {
        sut.renderLines(List())
      }
    }

    "throw an exception if no columns are defined" in {
      val sut = new ASCIITable[City]
      the[IllegalArgumentException] thrownBy {
        sut.renderLines(cities)
      }

    }
  }

  val cities = List(
    City("Villingen", 4, 7),
    City("Schwenningen", 2, 3),
    City("Karlsruhe", 17, 22),
    City("Berlin", 99, 34),
    City("Muenchen", 2, 34)
  )


  def setupCityTable: ASCIITable[City] = {
    val sut = new ASCIITable[City]

    sut.addColumn("City", 30, _.name)
    sut.addColumn("Bars", 5, _.bars.toString, true)
    sut.addColumn("Restaurants", 30, _.restaurants.toString, true)
    sut
  }


  def ensureAllCities(lines: Seq[String]) {
    cities.foreach {
      city =>
        lines.exists(_.contains(city.name)) should be(true)
    }
  }

  case class City(name: String, bars: Int, restaurants: Long)

}
