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
package com.googlecode.clearnlp.run;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.LineReader;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.reader.RawReader;
import com.googlecode.clearnlp.reader.TOKReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class POSPredict extends AbstractRun
{
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	@Option(name="-oe", usage="output file extension (default: tagged)", required=false, metaVar="<string>")
	private String s_outputExt = "tagged";
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="model file (required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	
	public POSPredict() {}
	
	public POSPredict(String[] args)
	{
		initArgs(args);
		
		try
		{
			Element eConfig = UTXml.getDocumentElement(new FileInputStream(s_configXml));
			
			List<String[]> filenames = getFilenames(s_inputPath, s_inputExt, s_outputExt);
			Pair<AbstractReader<?>, String> reader = getReader(eConfig);
			
			AbstractSegmenter segmenter = reader.o2.equals(AbstractReader.TYPE_RAW ) ? getSegmenter(eConfig) : null;
			AbstractTokenizer tokenizer = reader.o2.equals(AbstractReader.TYPE_LINE) ? getTokenizer(eConfig) : null;
			Pair<POSTagger[],Double> taggers = EngineGetter.getPOSTaggers(s_modelFile);
			
			for (String[] io : filenames)
			{
				if      (reader.o2.equals(AbstractReader.TYPE_RAW))
					predict(segmenter, taggers, (RawReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_LINE))
					predict(tokenizer, taggers, (LineReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_TOK))
					predict(taggers, (TOKReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_POS))
					predict(taggers, (POSReader)reader.o1, io[0], io[1]);
				else
				{
					new Exception("Invalid reader type: "+reader.o2);
					break;
				}
			}
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void predict(AbstractSegmenter segmenter, Pair<POSTagger[],Double> taggers, RawReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		POSNode[] nodes;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		for (List<String> tokens : segmenter.getSentences(fin.getBufferedReader()))
		{
			nodes = EngineProcess.getPOSNodes(taggers, tokens);
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(AbstractTokenizer tokenizer, Pair<POSTagger[],Double> taggers, LineReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		POSNode[] nodes;
		String line;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((line = fin.next()) != null)
		{
			nodes = EngineProcess.getPOSNodes(tokenizer, taggers, line);
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(Pair<POSTagger[],Double> taggers, TOKReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		List<String> tokens;
		POSNode[] nodes;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((tokens = fin.next()) != null)
		{
			nodes = EngineProcess.getPOSNodes(taggers, tokens);
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(Pair<POSTagger[],Double> taggers, POSReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		POSNode[] nodes;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((nodes = fin.next()) != null)
		{
			EngineProcess.predictPOS(taggers, nodes);
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}

	static public void main(String[] args)
	{
		new POSPredict(args);
	}
}
