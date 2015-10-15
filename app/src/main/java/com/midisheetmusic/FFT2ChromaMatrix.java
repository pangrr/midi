package com.midisheetmusic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.res.AssetManager;

public class FFT2ChromaMatrix {

	private final static int N_BIN = 12;
	private final static int FS = 44100;
	private final static int A440 = 440;
	private final static int FFT_LEN = 8192;
	
	public static void main(String[] args) {
		double[][] matrix = getFFT2ChromaMatrix();
		
		/* Write result to file. */
		try {
			File file = new File("/Users/rpang/Desktop/fft2chromamatrix.txt");
	
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
	
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int i = 0; i < N_BIN; i++) {
				for(int j = 0; j < FFT_LEN; j++) {
					bw.write(Double.toString(matrix[i][j]));
					if(j < FFT_LEN-1) {
						bw.write(",");
					}
				}
				if(i < N_BIN-1) {
					bw.newLine();
				}
			}
			bw.close();
			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* There is bug. */
	public static double[][] getFFT2ChromaMatrix() {
		double[] fftFrqBins = getFFTFrqBins();
		double[][] D = getDeviation(fftFrqBins);
		double[] binWidths = getChromaBinWidths(fftFrqBins);
		double[][] weights = getWeights(D, binWidths);
		return weights;
	}
	
	
	
	
	
	
	private static double[] getFFTFrqBins() {
		double tmp = FS / FFT_LEN;
		double[] freq = new double[FFT_LEN-1];
		for(int i = 1; i <= FFT_LEN-1; i++) {
			freq[i-1] = i * tmp;
		}
		double[] octs = hz2octs(freq);
		double[] fftFrqBins = new double[FFT_LEN];
		fftFrqBins[0] = octs[0] * N_BIN - 1.5 * N_BIN;
		for(int i = 0; i < FFT_LEN-1; i++) {
			fftFrqBins[i+1] = octs[i] * N_BIN;
		}
		return fftFrqBins;
	}
	
	private static double[][] getDeviation(double[] fftFrqBins) {
		double[][] D = new double[N_BIN][FFT_LEN];
		int tmp = (int) N_BIN / 2;
		int tmp2 = 10 * N_BIN + tmp;
		for(int i = 0; i < N_BIN; i++) {
			for(int j = 0; j < FFT_LEN; j++) {
				D[i][j] = (fftFrqBins[j] - i + tmp2) % N_BIN - tmp;
			}
		}
		return D;
	}
	
	private static double[] getChromaBinWidths(double[] fftFrqBins) {
		double[] binWidths = new double[FFT_LEN];
		for(int i = 0; i < FFT_LEN-1; i++) {
			binWidths[i] = Math.max(1, fftFrqBins[i+1] - fftFrqBins[i]);
		}
		binWidths[FFT_LEN-1] = 1;
		return binWidths;
	}
	
	private static double[][] getWeights(double[][] D, double[] binWidths) {
		double[][] weights = new double[N_BIN][FFT_LEN];
		for(int i = 0; i < N_BIN; i++) {
			for(int j = 0; j < FFT_LEN; j++) {
				weights[i][j] = Math.exp(-0.5 * Math.pow(2*D[i][j]/binWidths[j], 2));
			}
		}
		/* Normalize each column. */
		double[] sums = new double[FFT_LEN];
		for(int i = 0; i < FFT_LEN; i++) {
			sums[i] = 0.0;
			for(int j = 0; j < N_BIN; j++) {
				sums[i] += weights[j][i];
			}
		}
		for(int i = 0; i < N_BIN; i++) {
			for(int j = 0; j < FFT_LEN; j++) {
				weights[i][j] = weights[i][j] / sums[j];
			}
		}
		/* Remove aliasing column. */
		for(int i = 0; i < N_BIN; i++) {
			for(int j = FFT_LEN/2+1; j < FFT_LEN; j++) {
				weights[i][j] = 0;
			}
		}
		return weights;
	}
	
	private static double[] hz2octs(double[] freq) {
		double tmp = A440 / 16.0;
		double log2 = Math.log(2);
		double[] octs = new double[FFT_LEN-1];
		for(int i = 0; i < FFT_LEN-1; i++) {
			octs[i] = Math.log(freq[i]/tmp) / log2;
		}
		return octs;
	}

}
