package com.orudesk.mobile.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

actual fun platformEngine(): HttpClientEngine = Java.create()
