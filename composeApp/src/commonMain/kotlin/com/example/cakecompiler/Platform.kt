package com.example.cakecompiler

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform