/**
* Copyright (c) 2011, Regents of the University of Colorado
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
package com.goolgecode.clearnlp.experiment;

import java.io.FileInputStream;
import java.util.Arrays;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.goolgecode.clearnlp.feature.xml.POSFtrXml;
import com.goolgecode.clearnlp.pos.POSLib;
import com.goolgecode.clearnlp.pos.POSNode;
import com.goolgecode.clearnlp.pos.POSTagger;
import com.goolgecode.clearnlp.reader.POSReader;
import com.goolgecode.clearnlp.run.POSTrain;
import com.goolgecode.clearnlp.util.UTFile;
import com.goolgecode.clearnlp.util.UTInput;
import com.goolgecode.clearnlp.util.UTXml;


/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSDevelop2 extends POSTrain
{
	@Option(name="-d", usage="the directory containing development files (input; required)", required=true, metaVar="<directory>")
	private String s_devDir;
	
	public POSDevelop2() {}
	
	public POSDevelop2(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_devDir, d_threshold);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String featureXml, String trnDir, String devDir, double threshold) throws Exception
	{
		Element    eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader   reader = (POSReader)getReader(eConfig);
		POSFtrXml      xml = new POSFtrXml(new FileInputStream(featureXml));
		String[]  trnFiles = UTFile.getSortedFileList(trnDir);
		String[]  devFiles = UTFile.getSortedFileList(devDir);
		
		if (threshold < 0)	threshold = crossValidate(trnFiles, reader, xml, eConfig);
		POSTagger[] taggers = getTrainedTaggers(eConfig, reader, xml, trnFiles, null);
		int[] lCounts, gCounts;	int i;
		
		System.out.println("Threshold: "+threshold);
		gCounts = new int[4];
		
		for (String devFile : devFiles)
		{
			lCounts = predict(devFile, reader, taggers);
			for (i=0; i<gCounts.length; i++)	gCounts[i] += lCounts[i];
		}
		
		System.out.println("Overall");
		printAccuracy(gCounts);
		
		gCounts = new int[2];
		for (double th = 0.01; th<=0.03; th += 0.001)
		{
			System.out.println("Threshold: "+th);
			Arrays.fill(gCounts, 0);
			
			for (String devFile : devFiles)
			{
				lCounts = predict(devFile, reader, taggers, th);
				for (i=0; i<gCounts.length; i++)	gCounts[i] += lCounts[i];
			}
			
			System.out.println("Overall");
			printAccuracy(gCounts);
		}
	}
	
	protected int[] predict(String devFile, POSReader reader, POSTagger[] taggers, double threshold) 
	{
		int[] counts = {0,0};
		POSNode[] nodes;
		String[]  gold;
		
		System.out.println("Predicting: "+devFile);
		reader.open(UTInput.createBufferedFileReader(devFile));
		
		while ((nodes = reader.next()) != null)
		{
			gold = POSLib.getLabels(nodes);
			POSLib.normalizeForms(nodes);
			
			if (threshold < taggers[0].getCosineSimilarity(nodes))
				taggers[0].tag(nodes);
			else
				taggers[1].tag(nodes);

			counts[0] += countCorrect(nodes, gold);
			counts[1] += gold.length;
		}
		
		reader.close();
		printAccuracy(counts);
		
		return counts;
	}

	protected int[] predict(String devFile, POSReader reader, POSTagger[] taggers) 
	{
		int[] counts = {0,0,0,0}, correct = {0,0};
		POSNode[] nodes;
		String[]  gold;
		int i;
		
		System.out.println("Predicting: "+devFile);
		reader.open(UTInput.createBufferedFileReader(devFile));
		
		while ((nodes = reader.next()) != null)
		{
			gold = POSLib.getLabels(nodes);
			POSLib.normalizeForms(nodes);
			Arrays.fill(correct, 0);
			
			for (i=0; i<2; i++)
			{
				taggers[i].tag(nodes);
				correct[i] = countCorrect(nodes, gold);
				counts[i] += correct[i];
			}
			
			counts[2] += (correct[0] < correct[1]) ? correct[1] : correct[0];
			counts[3] += gold.length;
		}
		
		reader.close();
		printAccuracy(counts);
		
		return counts;
	}
	
	private void printAccuracy(int[] counts)
	{
		double accuracy;	int i, last = counts.length-1;
		
		for (i=0; i<last; i++)
		{
			accuracy = 100d * counts[i] / counts[last];
			System.out.printf("- accuracy %d: %7.5f (%d/%d)\n", i, accuracy, counts[i], counts[last]);
		}
	}
	
	private int countCorrect(POSNode[] nodes, String[] gold)
	{
		int i, correct = 0, n = nodes.length;
		
		for (i=0; i<n; i++)
		{
			if (gold[i].equals(nodes[i].pos))
				correct++;
		}
		
		return correct;
	}
	
	static public void main(String[] args)
	{
		new POSDevelop2(args);
	}
}