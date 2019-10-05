package com.example.foreground;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.foreground.constant.MusicConstants;
import com.example.foreground.service.SoundService;
import com.example.foreground.util.NetworkHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int lState = SoundService.getState();
                if (lState == MusicConstants.STATE_SERVICE.NOT_INIT) {
                    if (!NetworkHelper.isInternetAvailable(v.getContext())) {
                        showError(v);
                        return;
                    }
                    Intent startIntent = new Intent(v.getContext(), SoundService.class);
                    startIntent.setAction(MusicConstants.ACTION.START_ACTION);
                    startService(startIntent);
                } else if (lState == MusicConstants.STATE_SERVICE.PREPARE ||
                        lState == MusicConstants.STATE_SERVICE.PLAY) {
                    Intent lPauseIntent = new Intent(v.getContext(), SoundService.class);
                    lPauseIntent.setAction(MusicConstants.ACTION.PAUSE_ACTION);
                    PendingIntent lPendingPauseIntent = PendingIntent.getService(v.getContext(), 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    try {
                        lPendingPauseIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                } else if (lState == MusicConstants.STATE_SERVICE.PAUSE) {
                    if (!NetworkHelper.isInternetAvailable(v.getContext())) {
                        showError(v);
                        return;
                    }
                    Intent lPauseIntent = new Intent(v.getContext(), SoundService.class);
                    lPauseIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
                    PendingIntent lPendingPauseIntent = PendingIntent.getService(v.getContext(), 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    try {
                        lPendingPauseIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }

                }


            }
        });
    }

    private void showError(View v) {
        Snackbar.make(v, "No internet", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
