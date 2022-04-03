package com.robotemplates.webviewapp.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.robotemplates.webviewapp.fragment.MusicFragment;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service {

    private MediaPlayer player;
    private final Messenger messenger = new Messenger(new MusicServiceHandler(Looper.getMainLooper()));
    private Messenger replyMessenger;
    private final ArrayList<String> uriList;
    private int songIndex = 0;

    public static final String URI_CONSTANT = "android.resource://com.robotemplates.webviewapp/raw/";

    public static final int NEXT_SONG = 1;
    public static final int PLAY = 2;
    public static final int PAUSE = 3;
    public static final int STOP = 4;
    public static final int SET_REPLY = 0;

    final String TAG = "SRVC_TAG";

    {
        uriList = new ArrayList<>();
        uriList.add(URI_CONSTANT + "mdb_l3_start_gb");
        uriList.add(URI_CONSTANT + "mdb_l3_start_pl");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        Uri uri = Uri.parse(uriList.get(0));

        player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            player.setDataSource(getApplicationContext(), uri);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendSongName() {
        Message reply = Message.obtain(null, MusicFragment.CURRENT_SONG, 0, 0);
        Bundle b = new Bundle();
        b.putString("SONG", uriList.get(songIndex).split(URI_CONSTANT)[1]);
        reply.setData(b);
        try {
            replyMessenger.send(reply);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        player.stop();
    }

    private class MusicServiceHandler extends Handler {
        public MusicServiceHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SET_REPLY:
                    replyMessenger = msg.replyTo;
                    Message reply = Message.obtain(null, MusicFragment.RECONNECT, 0, 0);
                    Bundle b = new Bundle();
                    if (player != null) {
                        if (player.isPlaying()) {
                            b.putInt("isPlayingNow", PLAY);
                            Log.d(TAG, "playing now");
                        } else if (player.getCurrentPosition() > 0) {
                            b.putInt("isPlayingNow", PAUSE);
                            Log.d(TAG, "paused now , pos is " + player.getCurrentPosition());
                        } else {
                            b.putInt("isPlayingNow", STOP);
                            Log.d(TAG, " Stopped now");
                        }
                        MusicService.this.sendSongName();
                    }
                    reply.setData(b);
                    try {
                        replyMessenger.send(reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                case NEXT_SONG:
                    if (player.isPlaying()) {
                        player.stop();
                    }
                    player = new MediaPlayer();
                    songIndex = (songIndex + 1 < uriList.size()) ? songIndex + 1 : 0;
                    Uri uri = Uri.parse(uriList.get(songIndex));

                    try {
                        player.setDataSource(getBaseContext(), uri);
                        player.prepare();
                        MusicService.this.sendSongName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case PLAY:
                    if (player != null && !player.isPlaying()) {
                        player.start();
                        PlayerRunnable playerRunnable = new PlayerRunnable();
                    }
                    break;

                case PAUSE:
                    if (player != null && player.isPlaying()) {
                        player.pause();
                    }
                    break;

                case STOP:
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public class PlayerRunnable extends Thread implements Runnable {

        public PlayerRunnable() {
            this.start();
        }

        public void run() {
//While is playing
            while (player.isPlaying()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    return;
                }
            }
                Message reply = Message.obtain(null, MusicFragment.END_TRACK, 0, 0);
                Bundle b = new Bundle();
                reply.setData(b);
                try {
                    replyMessenger.send(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
        }
    }
}