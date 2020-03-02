package com.example.foreground

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.foreground.constant.MusicConstants
import com.example.foreground.service.SoundServiceK
import com.example.foreground.util.NetworkHelper.isInternetAvailable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener(View.OnClickListener { v ->
            val lState = SoundServiceK.state
            if (lState == MusicConstants.STATE_SERVICE.NOT_INIT) {
                if (!isInternetAvailable(v.context)) {
                    showError(v)
                    return@OnClickListener
                }
                val startIntent = Intent(v.context, SoundServiceK::class.java)
                startIntent.action = MusicConstants.ACTION.START_ACTION
                startService(startIntent)
            } else if (lState == MusicConstants.STATE_SERVICE.PREPARE ||
                    lState == MusicConstants.STATE_SERVICE.PLAY) {
                val lPauseIntent = Intent(v.context, SoundServiceK::class.java)
                lPauseIntent.action = MusicConstants.ACTION.PAUSE_ACTION
                val lPendingPauseIntent = PendingIntent.getService(v.context, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                try {
                    lPendingPauseIntent.send()
                } catch (e: CanceledException) {
                    e.printStackTrace()
                }
            } else if (lState == MusicConstants.STATE_SERVICE.PAUSE) {
                if (!isInternetAvailable(v.context)) {
                    showError(v)
                    return@OnClickListener
                }
                val lPauseIntent = Intent(v.context, SoundServiceK::class.java)
                lPauseIntent.action = MusicConstants.ACTION.PLAY_ACTION
                val lPendingPauseIntent = PendingIntent.getService(v.context, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                try {
                    lPendingPauseIntent.send()
                } catch (e: CanceledException) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun showError(v: View) {
        Snackbar.make(v, "No internet", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}