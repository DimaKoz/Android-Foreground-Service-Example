package com.example.foreground.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.foreground.MainActivity
import com.example.foreground.R
import com.example.foreground.constant.MusicConstants

class SoundServiceK : Service(), MediaPlayer.OnErrorListener, OnPreparedListener, OnBufferingUpdateListener {
    private val mUriRadioDefault = Uri.parse("https://nfw.ria.ru/flv/audio.aspx?ID=75651129&type=mp3")
    private val mLock = Any()
    private val mHandler = Handler()
    private var mPlayer: MediaPlayer? = null
    private var mUriRadio: Uri? = null
    private var mNotificationManager: NotificationManager? = null
    private var mWiFiLock: WifiLock? = null
    private var mWakeLock: WakeLock? = null
    private val mTimerUpdateHandler = Handler()
    private val mTimerUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            mNotificationManager?.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
            mTimerUpdateHandler.postDelayed(this, MusicConstants.DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE)
        }
    }

    private val mDelayedShutdown = object : Runnable {
        override fun run() {

            unlockWiFi()
            unlockCPU()
            stopForeground(true)
            stopSelf()
        }

    }

    override fun onBind(arg0: Intent): IBinder? {
         return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(SoundServiceK::class.java.simpleName, "onCreate()")
        state = MusicConstants.STATE_SERVICE.NOT_INIT
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mUriRadio = mUriRadioDefault
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == null) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            MusicConstants.ACTION.START_ACTION -> {
                Log.i(TAG, "Received start Intent ")
                state = MusicConstants.STATE_SERVICE.PREPARE
                startForeground(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                destroyPlayer()
                initPlayer()
                play()
            }
            MusicConstants.ACTION.PAUSE_ACTION -> {
                state = MusicConstants.STATE_SERVICE.PAUSE
                mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                Log.i(TAG, "Clicked Pause")
                destroyPlayer()
                mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE)
            }
            MusicConstants.ACTION.PLAY_ACTION -> {
                state = MusicConstants.STATE_SERVICE.PREPARE
                mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                Log.i(TAG, "Clicked Play")
                destroyPlayer()
                initPlayer()
                play()
            }
            MusicConstants.ACTION.STOP_ACTION -> {
                Log.i(TAG, "Received Stop Intent")
                destroyPlayer()
                stopForeground(true)
                stopSelf()
            }
            else -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        destroyPlayer()
        state = MusicConstants.STATE_SERVICE.NOT_INIT
        try {
            mTimerUpdateHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun destroyPlayer() {
        if (mPlayer != null) {
            try {
                mPlayer!!.reset()
                mPlayer!!.release()
                Log.d(TAG, "Player destroyed")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mPlayer = null
            }
        }
        unlockWiFi()
        unlockCPU()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.d(TAG, "Player onError() what:$what")
        destroyPlayer()
        mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE)
        mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
        state = MusicConstants.STATE_SERVICE.PAUSE
        return false
    }

    private fun initPlayer() {
        mPlayer = MediaPlayer()
        mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mPlayer!!.setOnErrorListener(this)
        mPlayer!!.setOnPreparedListener(this)
        mPlayer!!.setOnBufferingUpdateListener(this)
        mPlayer!!.setOnInfoListener { mp, what, extra ->
            Log.d(TAG, "Player onInfo(), what:$what, extra:$extra")
            false
        }
        lockWiFi()
        lockCPU()
    }

    private fun play() {
        try {
            mHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        synchronized(mLock) {
            try {
                if (mPlayer == null) {
                    initPlayer()
                }
                mPlayer!!.reset()
                mPlayer!!.setVolume(1.0f, 1.0f)
                mPlayer!!.setDataSource(this, mUriRadio!!)
                mPlayer!!.prepareAsync()
            } catch (e: Exception) {
                destroyPlayer()
                e.printStackTrace()
            }
        }
    }

    private fun prepareNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                mNotificationManager!!.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) { // The user-visible name of the channel.
            val name: CharSequence = getString(R.string.text_value_radio_notification)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance)
            mChannel.setSound(null, null)
            mChannel.enableVibration(false)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = MusicConstants.ACTION.MAIN_ACTION
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val lPauseIntent = Intent(this, SoundServiceK::class.java)
        lPauseIntent.action = MusicConstants.ACTION.PAUSE_ACTION
        val lPendingPauseIntent = PendingIntent.getService(this, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val playIntent = Intent(this, SoundServiceK::class.java)
        playIntent.action = MusicConstants.ACTION.PLAY_ACTION
        val lPendingPlayIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val lStopIntent = Intent(this, SoundServiceK::class.java)
        lStopIntent.action = MusicConstants.ACTION.STOP_ACTION
        val lPendingStopIntent = PendingIntent.getService(this, 0, lStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val lRemoteViews = RemoteViews(packageName, R.layout.radio_notification)
        lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_close_button, lPendingStopIntent)
        when (state) {
            MusicConstants.STATE_SERVICE.PAUSE -> {
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE)
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPlayIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_play_arrow_white)
            }
            MusicConstants.STATE_SERVICE.PLAY -> {
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE)
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_white)
            }
            MusicConstants.STATE_SERVICE.PREPARE -> {
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.VISIBLE)
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_white)
            }
        }
        val lNotificationBuilder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }
        lNotificationBuilder
                .setContent(lRemoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        return lNotificationBuilder.build()
    }

    override fun onPrepared(mp: MediaPlayer) {
        Log.d(TAG, "Player onPrepared()")
        state = MusicConstants.STATE_SERVICE.PLAY
        mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
        try {
            mPlayer!!.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mPlayer!!.start()
        mTimerUpdateHandler.postDelayed(mTimerUpdateRunnable, 0)
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        Log.d(TAG, "Player onBufferingUpdate():$percent")
    }

    private fun lockCPU() {
        val mgr = getSystemService(Context.POWER_SERVICE) as PowerManager ?: return
        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.javaClass.simpleName)
        mWakeLock?.acquire()
        Log.d(TAG, "Player lockCPU()")
    }

    private fun unlockCPU() {
        mWakeLock?.let {
            if (it.isHeld) {
                it.release()

                Log.d(TAG, "Player unlockCPU()")
            }
        }
        mWakeLock = null
    }

    private fun lockWiFi() {
        val connManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return
        val lWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (lWifi != null && lWifi.isConnected) {
            val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            manager.let {
                mWiFiLock = manager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL, SoundServiceK::class.java.simpleName)
                mWiFiLock?.acquire()
            }

            Log.d(TAG, "Player lockWiFi()")
        }
    }

    private fun unlockWiFi() {
        if (mWiFiLock != null && mWiFiLock!!.isHeld) {
            mWiFiLock!!.release()
            mWiFiLock = null
            Log.d(TAG, "Player unlockWiFi()")
        }
    }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
        private val TAG = SoundServiceK::class.java.simpleName
        var state = MusicConstants.STATE_SERVICE.NOT_INIT
            private set
    }
}