package choirhelper.silihasah.org.ui.sing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

import choirhelper.silihasah.org.R;
import choirhelper.silihasah.org.data.Song;
import choirhelper.silihasah.org.ui.sing.post_sing.SplashDoneSinging;
import choirhelper.silihasah.org.ui.sing.post_sing.score.score_history.DatabaseHelper;

/**
 * Created by BrosnanUhYeah on 03/04/2018.
 */

public class SingActivity extends AppCompatActivity {

    Runnable updateTimerThread = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            timeinMilliseconds = SystemClock.uptimeMillis() - startTime;
            updateTime = timeSwapBuff+timeinMilliseconds;
            secs = (int)(updateTime/1000);
            priceTrigg = (int)(updateTime/1000);
            mins = secs/60;
            count++;

            secs%=60;
            int milliseconds = (int)(updateTime%1000);
            tv_timer.setText(String.format("%2d",mins)+":"+String.format("%2d",secs));

            //Seekbar handler
            seekBar.setMax(45);
            seekBar.setProgress(0);
            seekBar.setProgress(secs);

            handler.postDelayed(this,1000);

            if(secs == 45){
                handler.removeCallbacks(updateTimerThread);
            }
        }
    };

    int i = 0;
    Runnable updateBandingThread = new Runnable() {
        @Override
        public void run() {
            i++;
            mDbBanding.child(voiceType+i).child("frequency").addValueEventListener(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    frequency_banding = Float.valueOf(dataSnapshot.getValue().toString());
                    tuning();
                    freqbanding.setText(frequency_banding + " Hz");


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            handler.postDelayed(this,1000);

            if(secs == 45){
                handler.removeCallbacks(updateBandingThread);

                Intent intent = new Intent(getApplicationContext(), SplashDoneSinging.class);
                Bundle bundle = new Bundle();
                bundle.putString("songTitle",songTitle);
                bundle.putString("voicetype",voiceType);
                bundle.putString("uid",uid);
                intent.putExtras(bundle);

                postDataToSQLite();
                startActivity(intent);
            }
        }
    };

    private TextView tv_timer;
    private TextView tv_freq;
    private DatabaseReference mDb;

    Handler handler = new Handler();
    long startTime = 0L, timeinMilliseconds=0L, timeSwapBuff=0L, updateTime=0L;

    int secs = 0;
    int priceTrigg = 0;
    int mins = 0;
    int count = 0;

    private SeekBar seekBar;
    private TextView ketepatannada;

    private float frequency_suara;

    final Context context = this;

    private ImageView tunegood;
    private ImageView tunehigh;
    private ImageView tunelow;
    private TextView freqbanding;
    private String songTitle;
    private String voiceType;
    private String uid;
    private DatabaseReference mDbBanding;
    private float frequency_banding;
    float score = 0;
    Song song;
    DatabaseHelper databaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sing);

        initView();
//        serverConnect();

        databaseHelper = new DatabaseHelper(context);
        song = new Song();
        //database frequensi dari Arduino
        mDb = FirebaseDatabase.getInstance().getReference().child("frequency");

        //database pembanding
        mDbBanding = FirebaseDatabase.getInstance().getReference().child("song_list").child(uid).child("song_data").child(voiceType);

        mDb.setValue(0);
        mDbBanding.child("score").setValue(0);

        mDb.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //setiap mengecek tambah 1
                frequency_suara = Float.valueOf(dataSnapshot.getValue().toString());
                tv_freq.setText(frequency_suara +" Hz");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //if frequency suara & frequency lagu = tunegood()

        //Time Handler
        startTime = SystemClock.uptimeMillis();
        handler.postDelayed(updateTimerThread,0);
        handler.postDelayed(updateBandingThread,0);

    }

    private void initView(){
        tv_freq = (TextView)findViewById(R.id.tv_freq_listennsingpractice);
        seekBar = (SeekBar)findViewById(R.id.sb_song);
        tv_timer = (TextView)findViewById(R.id.tv_timer);
        ketepatannada = (TextView)findViewById(R.id.tv_ketepatannada_practice);
        tunegood = (ImageView)findViewById(R.id.iv_tunegood_practice);
        tunehigh = (ImageView)findViewById(R.id.iv_tunehigh_practice);
        tunelow = (ImageView)findViewById(R.id.iv_tunelow_practice);
        freqbanding = (TextView)findViewById(R.id.tv_freqbanding_singpractice);

        songTitle = getIntent().getStringExtra("title");
        voiceType = getIntent().getStringExtra("voicetype");
        uid = getIntent().getStringExtra("uid");

        setTitle(songTitle + " - " +voiceType);
    }


    private void tuning(){
        if (frequency_suara >= frequency_banding - 25 && frequency_suara <= frequency_banding + 25){
            tunegood();
            score += 2.22;
            mDbBanding.child("score").setValue(new DecimalFormat("##.##").format(score));
        }else if(frequency_suara <= frequency_banding - 25){
            tunelow();
        }else if (frequency_suara >= frequency_banding + 25){
            tunehigh();
        }else {
            idle();
        }
    }

    private void idle(){
        ketepatannada.setText("idle");
    }

    private void tunegood(){
        ketepatannada.setText("PITCH PERFECT");

        tunehigh.setImageResource(R.color.grey);
        tunegood.setImageResource(R.color.green);
        tunelow.setImageResource(R.color.grey);
    }

    private void tunehigh(){
        ketepatannada.setText("PITCH TOO HIGH");

        tunehigh.setImageResource(R.color.red);
        tunegood.setImageResource(R.color.grey);
        tunelow.setImageResource(R.color.grey);

    }

    private void tunelow(){
        ketepatannada.setText("PITCH TOO LOW");

        tunegood.setImageResource(R.color.grey);
        tunehigh.setImageResource(R.color.grey);
        tunelow.setImageResource(R.color.red);
    }

    private int j = 0;
    public void postDataToSQLite(){
        j++;
        song.setSongId(j);
        song.setmTitle(songTitle);
        song.setVoiceType(voiceType);
        song.setScore(String.valueOf(new DecimalFormat("##.##").format(score)));

        databaseHelper.addTodo(song);
        Log.d("Data udah masuk:",song.toString());
        Log.d("Song Title:",songTitle);
        Log.d("Voice Type:",voiceType);
        Log.d("Score:", String.valueOf(score));
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}
