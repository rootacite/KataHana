package com.acite.katahana.engine

import io.ktor.client.HttpClient

internal expect fun createEngineHttpClient(): HttpClient
