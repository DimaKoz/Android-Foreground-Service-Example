package com.example.foreground.constant

object MusicConstants {
    const val NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503
    const val DELAY_SHUTDOWN_FOREGROUND_SERVICE: Long = 20000
    const val DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE: Long = 10000

    object ACTION {
        const val MAIN_ACTION = "music.action.main"
        const val PAUSE_ACTION = "music.action.pause"
        const val PLAY_ACTION = "music.action.play"
        const val START_ACTION = "music.action.start"
        const val STOP_ACTION = "music.action.stop"
    }

    object STATE_SERVICE {
        const val PREPARE = 30
        const val PLAY = 20
        const val PAUSE = 10
        const val NOT_INIT = 0
    }
}