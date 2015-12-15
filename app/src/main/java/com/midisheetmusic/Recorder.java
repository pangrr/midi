package com.midisheetmusic;

import java.util.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.os.*;
import android.media.*;


import org.jtransforms.fft.DoubleFFT_1D;

public class Recorder extends LinearLayout {
    static Bitmap recordImage;             /** The play image */
    static Bitmap stopImage;             /** The stop image */

    private ImageButton recordButton;      /** The play/pause button */
    private ImageButton stopButton;      /** The stop button */
    private TextView speedText;          /** The "Speed %" label */
    private SeekBar speedBar;    /** The seekbar for controlling the playback speed */

    int recordState;               /** The playing state of the Midi Player */
    final int STOPPED = 1;     /** Currently STOPPED */
    final int RECORDING = 2;     /** Currently playing music */
    final int PAUSED = 3;     /** Currently PAUSED */


    MidiFile midifile;          /** The midi file to play */
    MidiOptions options;        /** The sound options for playing the midi file */
    SheetMusic sheet;           /** The sheet music to shade while playing */
    long startTime;             /** Absolute time when music started playing (msec) */
    double startPulseTime;      /** Time (in pulses) when music started playing */
    double currentPulseTime;    /** Time (in pulses) music is currently at */
    double prevPulseTime;       /** Time (in pulses) music was last at */
    Activity activity;          /** The parent activity. */

    private static final int N_PARTICLE = 1000;
    private static double baseSpeed;    // pulse per microsecond

    private static final int SAMPLE_RATE = 44100;
    private static final int FRAME_SIZE = 2048; // Number of samples to process each time.
    private int bufferSize;
    private volatile short[] readBuffer;


    private double[] fft;
    private double[][] amplitude;
    private double[][] fft2chromaMatrix;

    private AudioRecord recorder;
    private ParticleFilter particleFilter;
    private MidiSegments midiSegments;

    private StringBuffer writeStringBuffer = new StringBuffer();

    private long readRecordDataTimestamp;

//    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
//
//        public void onPeriodicNotification(AudioRecord recorder) {
//           process();
//        }
//
//        public void onMarkerReached(AudioRecord recorder) {
//        }
//    };


    private void process() {
        long microSecPassedSinceLastRead = (System.nanoTime() - readRecordDataTimestamp) / 1000;
        readRecordDataTimestamp = System.nanoTime();

        // FFT
        for(int i = 0; i < FRAME_SIZE; i++) {
            fft[i] = (double) readBuffer[i];
        }
        for(int i = FRAME_SIZE; i < fft.length; i++) {
            fft[i] = 0.0;
        }
        DoubleFFT_1D fftDo = new DoubleFFT_1D(FRAME_SIZE*4);
        fftDo.realForwardFull(fft);



        // Amplitude
        for(int i = 0; i < amplitude.length;  i++) amplitude[i][0] = Math.sqrt(fft[i * 2]*fft[i * 2] + fft[i * 2 + 1] * fft[i*2+1]);
        // Chroma feature
        double[][] tmp = Matrix.multiply(fft2chromaMatrix, amplitude);
        double avg = 0;
        for(int i = 0; i < 12; i++) {
            avg += tmp[i][0] * tmp[i][0];
        }
        avg = Math.sqrt(avg);
        double[] chromaFeature = new double[12];
        for(int i = 0; i < 12; i++) {
            chromaFeature[i] = tmp[i][0] / avg;
        }

        // Particle filter
        prevPulseTime = currentPulseTime;
        currentPulseTime  = particleFilter.move(chromaFeature, microSecPassedSinceLastRead);

        // Shade notes
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);

//        writeStringBuffer.append((endTime - startTime) / 1000 / 1000);
//        writeStringBuffer.append("\n");
    }

    class ProcessThread extends Thread {
        @Override
        public void run() {
            while(getVolume(readBuffer, FRAME_SIZE) < 30000) {};

            StringBuffer sb = new StringBuffer();
            readRecordDataTimestamp = System.nanoTime();

            while(recordState == RECORDING) {
                process();
                writeParticleSegments();
            }
            FileWriter.writeFile("positions.txt", writeStringBuffer.toString());
        }
    }

    private void writeParticleSegments() {
        for(int i: particleFilter.getParticleSegmentIndex()) {
            writeStringBuffer.append(i);
            writeStringBuffer.append(" ");
        }
        writeStringBuffer.append("\n");
    }

    class RecordThread extends Thread {
        @Override
        public void run() {
			// Clear recorder
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            recorder.startRecording();
            recordState = RECORDING;

            for(int i = 0; i < 10; i++) {
                recorder.read(readBuffer, 0, bufferSize);
            }

            ProcessThread processThread = new ProcessThread();
            processThread.start();

            while (recordState == RECORDING) {
                recorder.read(readBuffer, 0, bufferSize);
            }

            recorder.stop();
            recorder.release();
            recorder = null;
        }
    };


    private void record() {
        if (midifile == null || sheet == null || recordState == RECORDING) {
            return;
        }

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        this.setVisibility(View.GONE);
        if(recordState == STOPPED) {
            particleFilter = new ParticleFilter(midiSegments, N_PARTICLE, baseSpeed, 0);
        }

        RecordThread recordThread = new RecordThread();
        recordThread.start();
    }

    private void setInitSpeed() {   // pulse per microsecond
        double microSecPerQuarter = midifile.getTime().getTempo();
        double pulsePerQuarter = midifile.getTime().getQuarter();
        baseSpeed = pulsePerQuarter/microSecPerQuarter;
    }

    public Recorder(Activity activity) {
        super(activity);
        LoadImages(activity);
        this.activity = activity;
        this.midifile = null;
        this.options = null;
        this.sheet = null;
        recordState = STOPPED;
        startTime = SystemClock.uptimeMillis();
        startPulseTime = 0;
        currentPulseTime = 0;
        prevPulseTime = -10;
        this.setPadding(0, 0, 0, 0);
        CreateButtons();

        int screenwidth = activity.getWindowManager().getDefaultDisplay().getWidth();
        int screenheight = activity.getWindowManager().getDefaultDisplay().getHeight();
        Point newsize = MidiPlayer.getPreferredSize(screenwidth, screenheight);
        resizeButtons(newsize.x, newsize.y);
        setBackgroundColor(Color.BLACK);

        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // 2048*2
        readBuffer = new short[bufferSize];
        fft = new double[bufferSize*8];
        amplitude = new double[bufferSize*2][1];

        recorder = null;
    }


    /** The callback for pausing playback.
     *  If we're currently playing, pause the music.
     *  The actual pause is done when the timer is invoked.
     */
    public void pause() {
        this.setVisibility(View.VISIBLE);
        LinearLayout layout = (LinearLayout)this.getParent();
        layout.requestLayout();
        this.requestLayout();
        this.invalidate();

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        recordState = PAUSED;
    }

    void stop() {
        this.setVisibility(View.VISIBLE);
        if(recordState == STOPPED) {
            return;
        }
        recordState = STOPPED;
        moveTop();
        clearShade();
    }

    private void clearShade() {
        sheet.ShadeNotes(-10, (int) currentPulseTime, SheetMusic.DontScroll);
    }

    private void moveTop() {
        sheet.ShadeNotes(0, (int) currentPulseTime, SheetMusic.ImmediateScroll);
        currentPulseTime = 0;
        prevPulseTime = 0;
    }

    /** Move the current position to the location clicked.
     *  The music must be in the PAUSED/STOPPED state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So, set the currentPulseTime to the position clicked.
     */
    public void MoveToClicked(int x, int y) {
        if (recordState == RECORDING) return;
        recordState = PAUSED;

        clearShade();

        // Get position
        currentPulseTime = sheet.PulseTimeForPoint(new Point(x, y));
        prevPulseTime = currentPulseTime - midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) currentPulseTime -= midifile.getTime().getMeasure();

        // Create a new particle filter at the position.
        particleFilter = new ParticleFilter(midiSegments, N_PARTICLE, baseSpeed, (int)currentPulseTime);

        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.DontScroll);
    }

    private double getVolume(short[] readBuffer, int length) {
        double squareSum = 0.0;
        for(int i = 0; i < length; i++) {
            squareSum += readBuffer[i] * readBuffer[i];
        }
        return Math.sqrt(squareSum);
    }


    /** Get the preferred width/height given the screen width/height */
    public static Point getPreferredSize(int screenwidth, int screenheight) {
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3 ;
        Point result = new Point(screenwidth, height);
        return result;
    }

    /** Determine the measured width and height.
     *  Resize the individual buttons according to the new width/height.
     */
    @Override
    protected void onMeasure(int widthspec, int heightspec) {
        super.onMeasure(widthspec, heightspec);
        int screenwidth = MeasureSpec.getSize(widthspec);
        int screenheight = MeasureSpec.getSize(heightspec);

        /* Make the button height 2/3 the piano WhiteKeyHeight */
        int width = screenwidth;
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3;
        setMeasuredDimension(width, height);
    }

    /** When this view is resized, adjust the button sizes */
    @Override
    protected void
    onSizeChanged(int newwidth, int newheight, int oldwidth, int oldheight) {
        resizeButtons(newwidth, newheight);
        super.onSizeChanged(newwidth, newheight, oldwidth, oldheight);
    }


    /** Create the rewind, play, stop, and fast forward buttons */
    void CreateButtons() {
        this.setOrientation(LinearLayout.HORIZONTAL);

        /* Create the stop button */
        stopButton = new ImageButton(activity);
        stopButton.setBackgroundColor(Color.BLACK);
        stopButton.setImageBitmap(stopImage);
        stopButton.setScaleType(ImageView.ScaleType.FIT_XY);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
            }
        });
        this.addView(stopButton);


        /* Create the play button */
        recordButton = new ImageButton(activity);
        recordButton.setBackgroundColor(Color.BLACK);
        recordButton.setImageBitmap(recordImage);
        recordButton.setScaleType(ImageView.ScaleType.FIT_XY);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                record();
            }
        });
        this.addView(recordButton);

        /* Create the Speed bar */
        speedText = new TextView(activity);
        speedText.setText("   Speed: 100%   ");
        speedText.setGravity(Gravity.CENTER);
        this.addView(speedText);

        speedBar = new SeekBar(activity);
        speedBar.setMax(150);
        speedBar.setProgress(100);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                speedText.setText("   Speed: " + String.format(Locale.US, "%03d", progress) + "%   ");
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        this.addView(speedBar);
    }


    /** Load the play/pause/stop button images */
    public static void LoadImages(Context context) {
        Resources res = context.getResources();
        recordImage = BitmapFactory.decodeResource(res, R.drawable.play);
        stopImage = BitmapFactory.decodeResource(res, R.drawable.stop);
    }





    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {
        midifile = file;
        midiSegments = new MidiSegments(midifile);
        setInitSpeed();
        options = opt;
        sheet = s;
    }

    void resizeButtons(int newwidth, int newheight) {
        int buttonheight = newheight;
        int pad = buttonheight/6;
        stopButton.setPadding(pad, pad, pad, pad);
        recordButton.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams params;

        params = new LinearLayout.LayoutParams(buttonheight, buttonheight);
        params.bottomMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        params.leftMargin = 0;

        recordButton.setLayoutParams(params);
        stopButton.setLayoutParams(params);

        params = (LinearLayout.LayoutParams) speedText.getLayoutParams();
        params.height = buttonheight;
        speedText.setLayoutParams(params);

        params = new LinearLayout.LayoutParams(buttonheight * 5, buttonheight);
        params.width = buttonheight * 5;
        params.bottomMargin = 0;
        params.leftMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        speedBar.setLayoutParams(params);
        speedBar.setPadding(pad, pad, pad, pad);
    }

    public void setFft2chromaMatrix(double[][] fft2chromaMatrix) {
        this.fft2chromaMatrix = fft2chromaMatrix;
    }
}


