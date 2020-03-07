package com.jcthenerd.personal.terminal.git

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.*

const val MAX_DEPTH = 2

fun getRepositories(paths: List<String>): List<String> = paths.flatMap {
    getRepositories(
        it,
        0
    )
}

fun getRepositories(path: String, currentDepth: Int = 0): List<String> {
    val mutableList = mutableListOf<String>()

    if (checkIfIsGitRepo(path)) {
        mutableList.add(path)
    } else {
        val directory = opendir(path)
        var dir: CPointer<dirent>? = readdir(directory)
        while (dir != null) {
            val newDir = dir.pointed
            val newDirName = newDir.d_name.toKString()
            if (newDirName != "." && newDirName != ".." && newDir.d_type == DT_DIR.toUByte()) {
                val fullPath = "$path/$newDirName"
                if (checkIfIsGitRepo(fullPath)) {
                    mutableList.add(fullPath)
                } else {
                    if (currentDepth < MAX_DEPTH) {
                        mutableList.addAll(
                            getRepositories(
                                fullPath,
                                currentDepth + 1
                            )
                        )
                    }
                }
            }

            dir = readdir(directory)
        }
        closedir(directory)
    }

    return mutableList
}

fun checkIfIsGitRepo(path: String): Boolean {
    val directory = opendir("$path/.git")

    val found = (directory != null)

    if (found) {
        closedir(directory)
    }

    return found
}