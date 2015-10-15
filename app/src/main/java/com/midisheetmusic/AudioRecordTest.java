package com.midisheetmusic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jtransforms.fft.DoubleFFT_1D;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/* This class is an experiment for recording and processing audio on the fly. */

public class AudioRecordTest extends Activity {
	private static int nDuration = 1;
	private static int nSampleRate = 44100;
	private static int nNumSamples = nDuration * nSampleRate;
	private int blockSize = AudioRecord.getMinBufferSize(nSampleRate,
			AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // 2048*2
	private short[] readBuffer = new short[blockSize];
	private double[] fft = new double[blockSize*8];	// 8192*2
//	private double[] fft = new double[blockSize*2];
	private double[][] amplitude = new double[blockSize*2][1];
	private double[][] fft2chromaMatrix = new double[12][8192];
	
	private RecordButton button = null;
	boolean isRecording = false;

	AudioRecord recorder = null;
//	AudioTrack player = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Get fft2chroma matrix from file. */
		getFFT2ChromaMatrix();
		
		LinearLayout ll = new LinearLayout(this);
		button = new RecordButton(this);
		ll.addView(button, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 0));

		setContentView(ll);
	}

	class RecordThread extends Thread {
		@Override
		public void run() {
			/* Clear possible left-over recorder. */
			if (recorder != null) {
				recorder.release();
				recorder = null;
			}
			
			/* Clear possible left-over player. */
//			if (player != null) {
//				player.release();
//				player = null;
//			}

			/* Initialize recorder. */
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, nSampleRate,
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
					blockSize);
			
			/* Initialize player. */
//			player = new AudioTrack(AudioManager.STREAM_MUSIC, nSampleRate,
//					AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
//					blockSize, AudioTrack.MODE_STREAM);
			
			/* Initialize recording state. */
			recorder.startRecording();
			isRecording = true;

//			if (player.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
//				player.play();
//			}

			int readBufferResult = 0, writeBufferResult = 0;
			
			int n = 0;
			String chromaStr = "";
			String amplitudeStr = "";
			String fftStr = "";
			
			
			
			/* Start recording until interruption. */
			while (isRecording) {
				readBufferResult = recorder.read(readBuffer, 0, blockSize);

				/* Use the recorded audio raw data when record is successful. */
				if (AudioRecord.ERROR_INVALID_OPERATION != readBufferResult) {
					/* Print original data. */
//					for(int i = 0; i < 5; i++) {
//						System.out.print(readBuffer[i]);
//						System.out.print(" ");
//					}
//					System.out.print("\n");

					/* Do fft. */
					for(int i = 0; i < readBufferResult; i++) {
						fft[i] = (double) readBuffer[i];
					}
					for(int i = readBufferResult; i < fft.length; i++) {
						fft[i] = 0.0;
					}
					
					DoubleFFT_1D fftDo = new DoubleFFT_1D(readBufferResult*4);
//					DoubleFFT_1D fftDo = new DoubleFFT_1D(readBufferResult);
					fftDo.realForwardFull(fft);
					for(int i = 0; i < amplitude.length;  i++) {
						amplitude[i][0] = Math.pow(Math.pow(fft[i*2], 2) + Math.pow(fft[i*2+1], 2), 0.5);
					}
					
//					double[][] fft2chromaMatrix = FFT2ChromaMatrix.getFFT2ChromaMatrix();
					
					double[][] chroma = Matrix.multiply(fft2chromaMatrix, amplitude);
					
					n++;
					if(n == 20) {
						for(int i = 0; i < chroma.length; i++) {
							chromaStr += Double.toString(chroma[i][0]) + "\n";
						}
						
//						for(int k = 0; k < fft.length; k++) {
//							fftStr += Double.toString(fft[k]) + "\n";
//						}
						
						for(int k = 0; k < amplitude.length; k++) {
							amplitudeStr += Double.toString(amplitude[k][0]) + "\n";
						}
					}
					

					/* Inverse fft to get the raw audio data */
//					fftDo.complexInverse(fft, true);
					

					
					/* Play recorded audio on the fly. */					
//					writeBufferResult += player.write(readBuffer, 0, readBufferResult);
				}
			}
		
			/* Stop and clear the recorder. */
			recorder.stop();
			recorder.release();
			recorder = null;
			
			/* Stop and clear the player. */
//			player.stop();
//			player.release();
//			player = null;
			
			
			FileWriter.writeFile("chroma.txt", chromaStr);
//			FileWriter.writeFile("fft.txt", fftStr);
			FileWriter.writeFile("amplitude.txt", amplitudeStr);
		}
	}
	
	private void getFFT2ChromaMatrix() {
		BufferedReader reader = null;
		try {
		    reader = new BufferedReader(
		        new InputStreamReader(getAssets().open("fft2chromamatrix.txt")));

		    // do reading, usually loop until end of file reading  
		    String mLine = reader.readLine();
		    int i = 0;
		    while (mLine != null) {
		       //process line
		       String[] strArr = mLine.split(",");
		       for(int j = 0; j < strArr.length; j++) {
		      	 fft2chromaMatrix[i][j] = Double.parseDouble(strArr[j]);
		       }
		       mLine = reader.readLine();
		       i++;
		    }
		} catch (IOException e) {
		    //log the exception
		} finally {
		    if (reader != null) {
		         try {
		             reader.close();
		         } catch (IOException e) {
		             //log the exception
		         }
		    }
		}
	}

	class RecordButton extends Button {
		OnClickListener clicker = new OnClickListener() {
			public void onClick(View v) {
				if (!isRecording) {
					setText("Stop Recording");
					RecordThread recordThread = new RecordThread();
					recordThread.start();
				} else {
					setText("Start Recording");
					isRecording = false;
				}
			}
		};

		public RecordButton(Context ctx) {
			super(ctx);
			setText("Start recording");
			setOnClickListener(clicker);
		}
	}
}