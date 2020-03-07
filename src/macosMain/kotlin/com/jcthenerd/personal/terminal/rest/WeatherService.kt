package com.jcthenerd.personal.terminal.rest

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content

val client by lazy {
    HttpClient(Curl) {
        followRedirects = true

        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
}

fun getWeather(city: String, units: String, appid: String): Map<String, String?> {
    val weatherMap = mutableMapOf<String, String?>()

    runBlocking {
        val responseObject = client.get<JsonObject> {
            url{
                takeFrom("https://api.openweathermap.org")
                encodedPath = "/data/2.5/weather"

                parameters.run {
                    append("q", city)
                    append("units", units)
                    append("appid", appid)
                }
            }
        }

        val main = responseObject["main"]?.jsonObject
        val temp = main?.get("temp")?.content
        val feelsLike = main?.get("feels_like")?.content
        val weather = responseObject["weather"]?.jsonArray?.get(0)?.jsonObject
        val description = weather?.get("description")?.content

        val unitSymbol = if (units == "imperial") "F" else "C"

        weatherMap["temp"] = "$temp $unitSymbol"
        weatherMap["real feel"] = "$feelsLike $unitSymbol"
        weatherMap["description"] = description
    }

    return weatherMap
}