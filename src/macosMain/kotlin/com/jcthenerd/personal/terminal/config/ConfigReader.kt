package com.jcthenerd.personal.terminal.config

import kotlinx.cinterop.*
import platform.posix.*

fun readConfigFile(path: String): Map<String, String> {
    val file = fopen(path, "r")

    if (file == null) {
        perror("cannot open config file")
    }

    val keyValue = mutableMapOf<String, String>()

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)

            generateSequence {
                fgets(buffer, bufferLength, file)?.toKString()
            }.forEach {configString ->
                val configPair = configString.split("=")
                val key = configPair[0].replace("\"", "").trim()
                val value = configPair[1].replace("\"", "").trim()
                keyValue[key] = value
            }
        }
    } finally {
        fclose(file)
    }

    return keyValue
}