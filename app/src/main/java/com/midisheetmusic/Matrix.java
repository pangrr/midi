package com.midisheetmusic;

public class Matrix {
	public static double[][] multiply(double[][] A, double[][] B) {
    int rowsInA = A.length;
    int columnsInA = A[0].length;
    int rowsInB = B.length;
    int columnsInB = B[0].length;
    if(columnsInA != rowsInB) {
    	System.out.println("Unmatching matrix size: A columns " + Integer.toString(columnsInA) + " while B rows " + Integer.toString(rowsInB));
    	return null;
    }
    double[][] C = new double[rowsInA][columnsInB];
    for (int i = 0; i < rowsInA; i++) {
        for (int j = 0; j < columnsInB; j++) {
            C[i][j] = 0.0;
        }
    }
    for (int i = 0; i < rowsInA; i++) {
        for (int j = 0; j < columnsInB; j++) {
            for (int k = 0; k < columnsInA; k++) {
                C[i][j] = C[i][j] + A[i][k] * B[k][j];
            }
        }
    }
    return C;
	}
}
