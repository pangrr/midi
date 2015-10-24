package com.midisheetmusic;

import java.util.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
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
    double pulsesPerMsec;       /** The number of pulses per millisec */
    SheetMusic sheet;           /** The sheet music to shade while playing */
    long startTime;             /** Absolute time when music started playing (msec) */
    double startPulseTime;      /** Time (in pulses) when music started playing */
    double currentPulseTime;    /** Time (in pulses) music is currently at */
    double prevPulseTime;       /** Time (in pulses) music was last at */
    Activity activity;          /** The parent activity. */


    private static int nSampleRate = 44100;
    private int blockSize = AudioRecord.getMinBufferSize(nSampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // 2048*2
    private short[] readBuffer = new short[blockSize];
    private double[] fft = new double[blockSize*8];	// 8192*2
    private double[][] amplitude = new double[blockSize*2][1];
    private double[][] fft2chromaMatrix;

    private AudioRecord recorder = null;
    private ParticleFilter particleFilter;
    private MidiSegments midiSegments;




    class RecordThread extends Thread {
        @Override
        public void run() {
			/* Clear possible left-over recorder. */
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }

			/* Initialize recorder. */
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, nSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    blockSize);

			/* Initialize RECORDING state. */
            recorder.startRecording();
            recordState = RECORDING;

            int n = 0;
            StringBuffer sb = new StringBuffer();

            int readBufferResult = 0;
			/* Start RECORDING until interruption. */
            while (recordState == RECORDING) {
                readBufferResult = recorder.read(readBuffer, 0, blockSize);

				/* Use the recorded audio raw data when record is successful. */
                if (AudioRecord.ERROR_INVALID_OPERATION != readBufferResult) {
                    if(n == 10) {
                        /* Write test log here. */
                    }
                    double volume = getVolume(readBuffer, readBufferResult);

                    if(volume < 10000) {    // Skip this frame if volume is too low.
                        continue;
                    }
                    sb.append(Double.toString(volume) + "\n");

					/* Do fft. */
                    for(int i = 0; i < readBufferResult; i++) {
                        fft[i] = (double) readBuffer[i];
                    }
                    for(int i = readBufferResult; i < fft.length; i++) {
                        fft[i] = 0.0;
                    }
                    DoubleFFT_1D fftDo = new DoubleFFT_1D(readBufferResult*4);
                    fftDo.realForwardFull(fft);
                    for(int i = 0; i < amplitude.length;  i++) {
                        amplitude[i][0] = Math.sqrt(fft[i * 2]*fft[i * 2] + fft[i * 2 + 1] * fft[i*2+1]);
                    }
                    double[][] tmp = Matrix.multiply(fft2chromaMatrix, amplitude);
                    double[] chromaFeature = new double[12];
                    for(int i = 0; i < 12; i++) {
                        chromaFeature[i] = tmp[i][0];
                    }

                    prevPulseTime = currentPulseTime;
                    currentPulseTime  = particleFilter.move(chromaFeature);

                    sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
                }
                n++;
            }
            FileWriter.writeFile("volume.txt", sb.toString());

			/* Stop and clear the recorder. */
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    };




    /** The callback for the play button.
     *  If we're STOPPED or pause, then play the midi file.
     */
    private void record() {
        if (midifile == null || sheet == null || recordState == RECORDING) {
            return;
        }

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        this.setVisibility(View.GONE);
        if(recordState == STOPPED) {
            particleFilter = new ParticleFilter(new MidiSegments(midifile), 1000, 120);
        }

        RecordThread recordThread = new RecordThread();
        recordThread.start();
    }



    /** The callback for pausing playback.
     *  If we're currently playing, pause the music.
     *  The actual pause is done when the timer is invoked.
     */
    public void Pause() {
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
        sheet.ShadeNotes(0, (int)currentPulseTime, SheetMusic.ImmediateScroll);
    }




    /** Move the current position to the location clicked.
     *  The music must be in the PAUSED/STOPPED state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So, set the currentPulseTime to the position clicked.
     */
    public void MoveToClicked(int x, int y) {
        if (midifile == null || sheet == null) {
            return;
        }
        if (recordState != PAUSED && recordState != STOPPED) {
            return;
        }
        recordState = PAUSED;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);

        currentPulseTime = sheet.PulseTimeForPoint(new Point(x, y));
        prevPulseTime = currentPulseTime - midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
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


    /** Create a new MidiPlayer, displaying the play/stop buttons, and the
     *  speed bar.  The midifile and sheetmusic are initially null.
     */
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
    }


    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {
        midifile = file;
        midiSegments = new MidiSegments(midifile);
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


