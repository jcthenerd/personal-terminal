package com.jcthenerd.personal.terminal.git

import git2.*
import kotlinx.cinterop.*

class GitCommit(private val commit: CPointer<git_commit>, val repoName: String) {
    val message: String?
        get() = git_commit_message(commit)?.toKString()

    val date: Long
        get() = git_commit_time(commit)

    val hash: String?
        get() {
            return git_oid_tostr_s(git_commit_id(commit))?.toKString()
        }
}