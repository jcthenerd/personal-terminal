package com.jcthenerd.personal.terminal

import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import ncurses.*
import kotlinx.cinterop.*
import com.jcthenerd.personal.terminal.display.*
import com.jcthenerd.personal.terminal.git.getRepositories
import com.jcthenerd.personal.terminal.git.Git
import platform.posix.perror
import com.jcthenerd.personal.terminal.config.GitProperties
import com.jcthenerd.personal.terminal.config.readConfigFile

lateinit var configuration: Map<String, String>

@KtorExperimentalAPI
@InternalAPI
fun main(args: Array<String>) = memScoped {

    if (args.isNotEmpty()) {
        perror("Configuration Not Defined")
    }

    configuration = readConfigFile(args[0])

    val windows = initialize()
    var char: Char
    try {
        do {
            runUpdate(windows)
            char = wgetch(windows["commitWindow"]).toChar()
        } while (char != 'q')
    } finally {
        close(windows)
        Git.close()
    }
}

@KtorExperimentalAPI
@InternalAPI
fun runUpdate(window: Map<String, CPointer<WINDOW>>) = memScoped {
    val repositoriesConfig = configuration[GitProperties.REPOS]?.split(",")
    val reposDirs = getRepositories(repositoriesConfig!!)
    val repos = reposDirs.map { Git.repository(it) }
    val commits = repos.flatMap { it.getCommits().toList() }.sortedByDescending { it.date }

    window["commitWindow"]?.drawCommitWindow(commits)
    window["countWindow"]?.drawTodayCounts(commits)
    window["weekCountWindow"]?.drawWeekCounts(commits)
    window["monthCountWindow"]?.drawMonthCounts(commits)
    window["weatherWindow"]?.drawWeatherWindow()

    repos.forEach { it.close() }

    wgetch(window["commitWindow"])
}