package com.example.foreground.constant;


public class MusicConstants {

    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;
    public static final long DELAY_SHUTDOWN_FOREGROUND_SERVICE = 20000;
    public static final long DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE = 10000;

    public static class ACTION {

        public static final String MAIN_ACTION = "music.action.main";
        public static final String PAUSE_ACTION = "music.action.pause";
        public static final String PLAY_ACTION = "music.action.play";
        public static final String START_ACTION = "music.action.start";
        public static final String STOP_ACTION = "music.action.stop";

    }

    public static class STATE_SERVICE {

        public static final int PREPARE = 30;
        public static final int PLAY = 20;
        public static final int PAUSE = 10;
        public static final int NOT_INIT = 0;


    }

}
