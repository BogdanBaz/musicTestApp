package com.robotemplates.webviewapp.fragment;

import static com.robotemplates.webviewapp.service.MusicService.NEXT_SONG;
import static com.robotemplates.webviewapp.service.MusicService.PAUSE;
import static com.robotemplates.webviewapp.service.MusicService.PLAY;
import static com.robotemplates.webviewapp.service.MusicService.STOP;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.robotemplates.webviewapp.R;
import com.robotemplates.webviewapp.service.MusicService;

public class MusicFragment extends Fragment {

    private View view;
    private CountDownTimer countDownTimer;
    ToggleButton playPauseBtn;
    Button nextCompositionBtn;
    FloatingActionButton homeBtn;
    TextView currentSongTxt, countDownTxt;

    private Messenger messenger, replyMessenger;

    public static final int RECONNECT = 0;
    public static final int END_TRACK = 1;
    public static final int CURRENT_SONG = 3;

    final String TAG = "FRMT_TAG";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_music, container, false);
        }

        playPauseBtn = view.findViewById(R.id.play_pause_btn);
        nextCompositionBtn = view.findViewById(R.id.next_composition_btn);
        homeBtn = view.findViewById(R.id.home_btn);
        currentSongTxt = view.findViewById(R.id.composition_txt);
        countDownTxt = view.findViewById(R.id.countdown_txt);

        playPauseBtn.setOnClickListener(view1 -> playPause());
        nextCompositionBtn.setOnClickListener(view1 -> nextSong());
        homeBtn.setOnClickListener(view1 -> goToHomePage());

        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                countDownRestart();
            }
            return true;
        });

        return view;
    }

    private void countDownRestart() {
        Log.d(TAG, "in  countDownRestart  ");

        countDownTimer.cancel();
        countDownTimer.start();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "in  " + "  onServiceConnected");
            messenger = new Messenger(service);
            Message message = Message.obtain(null, MusicService.SET_REPLY, 0, 0);
            message.replyTo = replyMessenger;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messenger = null;
        }
    };

    private void stopPlay() {
    }

    public void playPause() {
        countDownRestart();
        Message message;
        if (playPauseBtn.isChecked()) {
            message = Message.obtain(null, PAUSE, 0, 0);
        } else {
            message = Message.obtain(null, PLAY, 0, 0);
        }

        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void nextSong() {
        countDownRestart();
        Message message = Message.obtain(null, NEXT_SONG, 0, 0);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class MusicFragmentHandler extends Handler {
        public MusicFragmentHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case END_TRACK:
                    playPauseBtn.setChecked(true);
                    break;

                case RECONNECT:
                    int playerCondition = msg.getData().getInt("isPlayingNow");

                    if (playerCondition == PLAY) {
                        playPauseBtn.setChecked(false);
                        Log.d(TAG, "in Reconnect " + "PLAY");
                    }
                    if (playerCondition == PAUSE) {
                        playPauseBtn.setChecked(true);
                        Log.d(TAG, "in Reconnect " + "PAUSE");
                    }
                    if (playerCondition == STOP) {
                        countDownRestart();
                        Log.d(TAG, "in Reconnect " + "STOP");
                    }
                    break;

                case CURRENT_SONG:
                    currentSongTxt.setText(msg.getData().getString("SONG"));
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "   ON START   ");

        countDownTimer = new CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {

                Log.d(TAG, " TIC   " + millisUntilFinished / 1000);

                int sec = (int) (millisUntilFinished / 1000);
                countDownTxt.setText(sec + " s.");
                if (sec == 10) showWarning();
            }

            public void onFinish() {
            }
        }.start();

        super.onStart();
    }

    private void showWarning() {
        String message = "Hej, jesteś tam jeszcze? za %d sekund powrócę do ekranu startowego";

        @SuppressLint("DefaultLocale") AlertDialog diag = new AlertDialog.Builder(requireContext())
                .setMessage(String.format(message, 10))
                .setPositiveButton("Jestem:)", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create();
        diag.show();

        diag.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);

        final boolean[] isGoHome = new boolean[1];

        CountDownTimer timer = new CountDownTimer(10000, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                diag.setMessage(String.format(message, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                diag.cancel();
                goToHomePage();
                isGoHome[0] = true;
            }
        }.start();

        diag.setOnDismissListener(dialogInterface -> {
            timer.cancel();
            if (!isGoHome[0])
                countDownRestart();
        });
    }

    private void goToHomePage() {
        getParentFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onPause() {
        super.onPause();
        countDownTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().bindService(new Intent(getContext(), MusicService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        replyMessenger = new Messenger(new MusicFragmentHandler(Looper.getMainLooper()));
    }
}