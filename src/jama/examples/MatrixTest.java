/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jama.examples;

import java.util.Arrays;
import jama.*;

/**
 *
 * @author Martin
 */
public class MatrixTest {


	// Frobeniova věta!!!

	public static void main(String[] args) {

		double[][] mData = {{1, 1}, {1, -1}, {0, 0}};
		Matrix m = new Matrix(mData);
		System.out.println("Hodnost matice: " + m.rank());

		double[][] bData = {{3}, {-1}, {0}};
		Matrix b = new Matrix(bData);

		Matrix extended = new Matrix(3, 3);
		extended.setMatrix(0, 2, 0, 1, m);
		extended.setMatrix(0, 2, 2, 2, b);
		System.out.println("Hodnost rozsirene: " + extended.rank());


		Matrix x = m.solve(b);
		x.print(5, 1);
	}

}
