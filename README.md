# OlympusKtor

OlympusKtor is a backend for the [OlympusBlog](https://github.com/sentrionic/OlympusBlog) using [Ktor](https://ktor.io/).

## Stack
 - [Exposed](https://github.com/JetBrains/Exposed) as the DB ORM
 - [HikariCP](https://github.com/brettwooldridge/HikariCP) to connect to the DB
 - [Yavi](https://github.com/making/yavi) for validation
 - [Thumbnailator](https://github.com/coobird/thumbnailator) for image resizing
 - Kotlin Version 1.5.10

## Getting started
1. Clone this repository
2. Rename `application.conf.example` in `resources` to `application.conf`
   and fill out the values. AWS is only required if you want file upload, 
   GMail if you want to send reset emails.
3. Run `Application.kt`