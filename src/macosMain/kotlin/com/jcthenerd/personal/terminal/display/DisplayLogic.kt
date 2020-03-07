package com.jcthenerd.personal.terminal.display

import com.soywiz.klock.DateTime
import com.soywiz.klock.hours
import com.soywiz.klock.months
import com.soywiz.klock.weeks
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.cinterop.CPointer
import ncurses.*
import com.jcthenerd.personal.terminal.config.WeatherProperties
import com.jcthenerd.personal.terminal.configuration
import com.jcthenerd.personal.terminal.git.GitCommit
import com.jcthenerd.personal.terminal.rest.getWeather
import kotlin.math.min
import platform.posix.perror

fun initialize(): Map<String, CPointer<WINDOW>> {
    initscr()

    noecho()
    curs_set(0)
    start_color()
    halfdelay(2)

    initColorPairs()

    val commitWindow = newwin(22, 79, 0, 0)!!
    val countWindow = newwin(22, 11, 0, 79)!!
    val weekCountWindow = newwin(22, 30, 22, 0)!!
    val monthCountWindow = newwin(22, 30, 22, 30)!!
    val weatherWindow = newwin(22, 30, 22, 60)!!

    return mapOf(
        "commitWindow" to commitWindow,
        "countWindow" to countWindow,
        "weekCountWindow" to weekCountWindow,
        "monthCountWindow" to monthCountWindow,
        "weatherWindow" to weatherWindow
    )
}

fun close(windows: Map<String, CPointer<WINDOW>>) {
    windows.values.forEach { delwin(it) }
    endwin()
}

fun initColorPairs() {
    init_pair(1, COLOR_RED.toShort(), COLOR_BLACK.toShort())
    init_pair(2, COLOR_WHITE.toShort(), COLOR_BLACK.toShort())
    init_pair(3, COLOR_GREEN.toShort(), COLOR_BLACK.toShort())
    init_pair(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort())
    init_pair(5, COLOR_WHITE.toShort(), COLOR_BLACK.toShort())
    init_pair(6, COLOR_WHITE.toShort(), COLOR_BLUE.toShort())
    init_pair(7, COLOR_WHITE.toShort(), COLOR_CYAN.toShort())
}

@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun CPointer<WINDOW>.drawCommitWindow(commits: List<GitCommit>) {
    wclear(this)

    wattron(this, COLOR_PAIR(4))
    box(this, 0u, 0u)
    wattroff(this, COLOR_PAIR(4))

    mvwprintw(this, 0, 2, " COMMITS ")

    var y = 0
    commits.take(20).forEach {
        y++
        val hash = it.hash?.take(5)
        val date = DateTime.fromUnix(it.date * 1000)
        val message = it.message?.trim()?.replace("\n", "")?.replace("\r", "")

        wattron(this, COLOR_PAIR(1))
        mvwprintw(this, y, 1, hash)

        wattron(this, COLOR_PAIR(2))
        mvwprintw(this, y, 7, message?.take(40))

        wattron(this, COLOR_PAIR(3))
        mvwprintw(this, y, 48, date.toString("MM/dd/yyyy hh:mm a"))

        wattron(this, COLOR_PAIR(4))
        mvwprintw(this, y, 68, it.repoName.take(10))

    }
    wrefresh(this)
}

@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun CPointer<WINDOW>.drawTodayCounts(commits: List<GitCommit>) {
    wclear(this)

    wattron(this, COLOR_PAIR(4))
    box(this, 0u, 0u)
    wattroff(this, COLOR_PAIR(4))

    mvwprintw(this, 0, 2, " COUNT ")

    val oneDayBack = (DateTime.now() - 24.hours).unixMillis / 1000

    val totalNew = commits.filter { it.date >= oneDayBack }.count()

    val columnHeight = min(totalNew, 20)

    if (columnHeight > 0) {
        wattron(this, COLOR_PAIR(6))

        for (i in 1..columnHeight) {
            val y = 21 - i
            mvwprintw(this, y, 2, " ".repeat(7))
        }

        mvwprintw(this, 20, 3, totalNew.toString())
    }

    wrefresh(this)
}

@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun CPointer<WINDOW>.drawWeekCounts(commits: List<GitCommit>) {
    wclear(this)

    wattron(this, COLOR_PAIR(4))
    box(this, 0u, 0u)
    wattroff(this, COLOR_PAIR(4))

    mvwprintw(this, 0, 2, " WEEK ")

    val oneWeekBack = (DateTime.now() - 1.weeks).unixMillis / 1000

    val totals = commits.filter {
        it.date >= oneWeekBack
    }.groupBy {
        DateTime.fromUnix(it.date * 1000).dayOfYear
    }

    totals.entries.sortedByDescending { it.key }.take(7).forEach { (key, value) ->
        val totalNew = value.count()
        val index = DateTime.now().dayOfYear - key

        val columnHeight = min(totalNew, 20)

        if (columnHeight > 0) {
            wattron(this, COLOR_PAIR(6))

            for (i in 1..columnHeight) {
                val y = 21 - i
                mvwprintw(this, y, (index * 4 + 1), " ".repeat(4))
            }

            mvwprintw(this, 20, (index * 4 + 2), totalNew.toString())
        }
    }

    wrefresh(this)
}

@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun CPointer<WINDOW>.drawMonthCounts(commits: List<GitCommit>) {
    wclear(this)

    wattron(this, COLOR_PAIR(4))
    box(this, 0u, 0u)
    wattroff(this, COLOR_PAIR(4))

    mvwprintw(this, 0, 2, " MONTH ")

    val oneMonthBack = (DateTime.now() - 1.months).unixMillis / 1000

    val totals = commits.filter {
        it.date >= oneMonthBack
    }.groupBy {
        DateTime.fromUnix(it.date * 1000).month
    }

    totals.entries.minBy { it.key }?.value?.groupBy {
        DateTime.fromUnix(it.date * 1000).dayOfYear
    }?.entries?.sortedByDescending {it.key}?.take (31)?.forEach { (key, value) ->
        val totalNew = value.count()
        val index = DateTime.now().dayOfYear - key
        val column = index / 7
        val row = index % 7

        when {
            totalNew > 10 -> wattron(this, COLOR_PAIR(6))
            totalNew > 0 -> wattron(this, COLOR_PAIR(7))
        }

        mvwprintw(this, (column * 5 + 1), (row * 4 + 1), " ".repeat(4))
        mvwprintw(this, (column * 5 + 2), (row * 4 + 1), " ".repeat(4))
    }

    wrefresh(this)
}

@KtorExperimentalAPI
@InternalAPI
@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun CPointer<WINDOW>.drawWeatherWindow() {
    wclear(this)

    wattron(this, COLOR_PAIR(4))
    box(this, 0u, 0u)
    wattroff(this, COLOR_PAIR(4))

    mvwprintw(this, 0, 2, " WEATHER ")

    val city = configuration[WeatherProperties.CITY]
    val appid = configuration[WeatherProperties.APP_ID]
    val units = configuration[WeatherProperties.UNITS]

    if (city == null || appid == null || units == null) {
        perror("Weather configuration missing")
    } else {
        val response = getWeather(city, units, appid)

        response.entries.forEachIndexed { index, (key, value) ->
            mvwprintw(this, (index * 3) + 2, 2, key)
            mvwprintw(this, (index * 3) + 3, 2, value)
        }
    }

    wrefresh(this)
}
