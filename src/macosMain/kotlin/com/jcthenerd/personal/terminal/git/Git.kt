package com.jcthenerd.personal.terminal.git

import git2.git_libgit2_init
import git2.git_libgit2_shutdown

object Git {
    init {
        git_libgit2_init()
    }

    fun close() {
        git_libgit2_shutdown()
    }

    fun repository(path: String): GitRepository {
        return GitRepository(path)
    }
}