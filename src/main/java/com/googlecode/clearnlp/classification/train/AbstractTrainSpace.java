/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.classification.train;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;

import com.carrotsearch.hppc.IntArrayList;
import com.googlecode.clearnlp.classification.model.AbstractModel;
import com.googlecode.clearnlp.classification.vector.AbstractFeatureVector;
import com.googlecode.clearnlp.classification.vector.SparseFeatureVector;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.pair.IntIntPair;


/**
 * Abstract train space.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractTrainSpace
{
	/** The flag to indicate sparse vector space. */
	static public final byte VECTOR_SPARSE = 0;
	/** The flag to indicate string vector space. */
	static public final byte VECTOR_STRING = 1;

	/** The delimiter between columns ({@code " "}). */
	static public final String DELIM_COL = " ";
	
	/** The abstract model to be saved. */
	protected AbstractModel       m_model;
	/** {@code true} if features are assigned with different weights. */
	protected boolean             b_weight;
	/** The list of training labels. */
	protected IntArrayList        a_ys;
	/** The list of training feature indices. */
	protected ArrayList<int[]>    a_xs;
	/** The list of training feature weights. */
	protected ArrayList<double[]> a_vs;
	
	/**
	 * Constructs an abstract train space.
	 * @param model the model to be trained.
	 * @param hasWeight {@code true} if features are assigned with different weights.
	 */
	public AbstractTrainSpace(AbstractModel model, boolean hasWeight)
	{
		m_model  = model;
		b_weight = hasWeight;
		a_ys     = new IntArrayList();
		a_xs     = new ArrayList<int[]>();
		if (hasWeight)	a_vs = new ArrayList<double[]>();
	}
	
	/**
	 * Reads training instances from the specific reader.
	 * The reader is closed after this method is called.
	 * @param reader the reader to read training instances from.
	 */
	public void readInstances(BufferedReader reader)
	{
		LineNumberReader fin = new LineNumberReader(reader);
		String line;
	
		System.out.print("Reading: ");
		
		try
		{
			while ((line = fin.readLine()) != null)
			{
				addInstance(line);
				if (fin.getLineNumber()%10000 == 0)	System.out.print(".");
			}
			
			fin.close();
			System.out.println("\rReading: "+fin.getLineNumber());
		}
		catch (IOException e) {e.printStackTrace();}
	}
	
	/**
	 * Adds a training instance to this space.
	 * @param see the description in each sub-class.
	 */
	abstract public void addInstance(String line);
	
	/** Generates vector space given training instances. */
	abstract public void build(boolean clearInstances);
	
	/** Generates vector space given training instances. */
	abstract public void build();
	
	/**
	 * Returns the list of training labels.
	 * @return the list of training labels.
	 */
	public IntArrayList getYs()
	{
		return a_ys;
	}
	
	/**
	 * Returns the list of training feature indices.
	 * @return the list of training feature indices.
	 */
	public ArrayList<int[]>getXs()
	{
		return a_xs;
	}
	
	/**
	 * Returns the list of training feature weights.
	 * @return the list of training feature weights.
	 */
	public ArrayList<double[]> getVs()
	{
		return a_vs;
	}
	
	/**
	 * Returns {@code true} if features are assigned with different weights. 
	 * @return {@code true} if features are assigned with different weights.
	 */
	public boolean hasWeight()
	{
		return b_weight;
	}
	
	/**
	 * Returns the total number of training instances.
	 * @return the total number of training instances.
	 */
	public int getInstanceSize()
	{
		return a_ys.size();
	}
	
	/**
	 * Returns the total number of labels.
	 * @return the total number of labels.
	 */
	public int getLabelSize()
	{
		return m_model.getLabelSize();
	}
	
	/**
	 * Returns the total number of features.
	 * @return the total number of features.
	 */
	public int getFeatureSize()
	{
		return m_model.getFeatureSize();
	}
	
	/**
	 * Returns {@code true} if there are only 2 labels.
	 * @return {@code true} if there are only 2 labels.
	 */
	public boolean isBinaryLabel()
	{
		return m_model.isBinaryLabel();
	}
	
	/**
	 * Returns the trained model.
	 * @return the trained model.
	 */
	public AbstractModel getModel()
	{
		return m_model;
	}
	
	/**
	 * Returns {@code true} if features are assigned with different weights. 
	 * @param vectorType the type of vector space.
	 * @param filename the name of the file containing training instances.
	 * @see AbstractTrainSpace#VECTOR_SPARSE
	 * @see AbstractTrainSpace#VECTOR_STRING
	 * @return {@code true} if features are assigned with different weights.
	 * @throws IOException
	 */
	static public boolean hasWeight(byte vectorType, String filename) throws IOException
	{
		BufferedReader fin = UTInput.createBufferedFileReader(filename);
		String[] tmp = fin.readLine().split(AbstractTrainSpace.DELIM_COL);
		int i, idx0, idx1, size = tmp.length;
		String str;
		
		fin.close();
		
		for (i=1; i<size; i++)
		{
			str  = tmp[i];
			idx0 = str.indexOf(AbstractFeatureVector.DELIM);
			if (idx0 == -1)	return false;
			
			if (vectorType == AbstractTrainSpace.VECTOR_STRING)
			{
				idx1 = str.lastIndexOf(AbstractFeatureVector.DELIM);
				if (idx1 == -1 || idx0 == idx1)	return false;
			}
		}
		
		return true;
	}
	
	public void printInstances(PrintStream fout)
	{
		int i, j, len, size = a_ys.size();
		int[] xs; double[] vs;
		StringBuilder build;
		
		for (i=0; i<size; i++)
		{
			build = new StringBuilder();
			build.append(a_ys.get(i));
			
			xs  = a_xs.get(i);
			vs  = (b_weight) ? a_vs.get(i) : null;
			len = xs.length;
			
			for (j=0; j<len; j++)
			{
				build.append(DELIM_COL);
				build.append(xs[j]);
				
				if (b_weight)
				{
					build.append(AbstractFeatureVector.DELIM);
					build.append(vs[j]);
				}
			}
			
			fout.println(build.toString());
		}
	}
	
	public void printAccuracies()
	{
		int y, i, j, len, lSize = m_model.getLabelSize(), tSize = a_ys.size();
		IntIntPair[] counts = new IntIntPair[lSize];
		SparseFeatureVector vector;
		int[] x; double[] v = null;
		IntIntPair c;
		
		for (i=0; i<lSize; i++)
			counts[i] = new IntIntPair(0, 0);
			
		for (i=0; i<tSize; i++)
		{
			y = a_ys.get(i);
			x = a_xs.get(i);
			if (b_weight)	v = a_vs.get(i);
			
			vector = new SparseFeatureVector(b_weight);
			len = x.length;
			
			for (j=0; j<len; j++)
			{
				if (b_weight)	vector.addFeature(x[j], v[j]);
				else			vector.addFeature(x[j]);
			}
			
			if (m_model.getLabel(y).equals(m_model.predictBest(vector).label))
				counts[y].i1++;

			counts[y].i2++;
		}

		for (i=0; i<lSize; i++)
		{
			c = counts[i];
			System.out.printf("%25s: %5.2f (%d/%d)\n", m_model.getLabel(i), 100d*c.i1/c.i2, c.i1, c.i2);
		}
	}
}
