package com.jcthenerd.personal.terminal.git

import cnames.structs.git_commit
import cnames.structs.git_repository
import cnames.structs.git_revwalk
import com.soywiz.klock.DateTime
import git2.*
import kotlinx.cinterop.*

class GitRepository(private val repoPath: String) {
    private val name = repoPath.split("/").last()
    private val arena = Arena()
    private val repo: CPointer<git_repository> = memScoped {
        val location = allocPointerTo<git_repository>()
        git_repository_open(location.ptr, repoPath)
        location.value!!
    }

    fun close() {
        git_repository_free(repo)
        arena.clear()
    }

    @UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
    fun getCommits(): Sequence<GitCommit> = memScoped {
        val walkPtr = allocPointerTo<git_revwalk>()
        git_revwalk_new(walkPtr.ptr, repo)
        val walk = walkPtr.value
        git_revwalk_sorting(walk, GIT_SORT_TOPOLOGICAL or GIT_SORT_TIME)
        git_revwalk_push_head(walk)

        generateSequence {
            memScoped {
                val oid = alloc<git_oid>()
                val result = git_revwalk_next(oid.ptr, walk)

                when (result) {
                    0 -> {
                        val commitPtr = allocPointerTo<git_commit>()
                        git_commit_lookup(commitPtr.ptr, repo, oid.ptr)
                        val commit = commitPtr.value!!
                        GitCommit(commit, name)
                    }
                    GIT_ITEROVER -> null
                    else -> throw Exception("Unexpected result code $result")
                }
            }
        }
    }


}