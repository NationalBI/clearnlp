/**
* Copyright 2012 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.dependency;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntHashSet;
import com.googlecode.clearnlp.engine.AbstractEngine;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.util.pair.StringIntPair;
import com.googlecode.clearnlp.util.triple.Triple;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractDEPParser extends AbstractEngine
{ 
	protected final String LB_LEFT		= "L";
	protected final String LB_RIGHT		= "R";
	protected final String LB_NO		= "N";
	protected final String LB_SHIFT		= "S";
	protected final String LB_REDUCE	= "R";
	protected final String LB_PASS		= "P";
	protected final String LB_DELIM		= "_";
	protected final Pattern P_LABELS 	= Pattern.compile(LB_DELIM);
	
	protected final int    IDX_ARC		= 0;
	protected final int    IDX_LIST		= 1;
	protected final int    IDX_DEPREL	= 2;
	
	protected DEPTree			d_tree;
	protected IntHashSet    s_reduce;
	protected Set<String>		s_punc;
	protected StringIntPair[]	g_heads;
	protected DEPNode[]			lm_deps;
	protected DEPNode[]			rm_deps;
	protected int				i_lambda;
	protected int				i_beta;
	protected int				n_trans;
	protected PrintStream		f_trans;
	
	/** Initializes the flag of this parser. */
	public AbstractDEPParser(byte flag)
	{
		super(flag);
	}
	
	/** Loads collections and a dependency parsing model from the specific input reader. */
	abstract public void loadModel(BufferedReader fin);
	/** Saves collections and a dependency parsing model to the specific output stream. */
	abstract public void saveModel(PrintStream fout);
	/** @return an array of {arc-label, list-label, dependency-label}. */
	abstract protected String[] getLabels();
	/** Called by {@link AbstractDEPParser#postProcess()}. */
	abstract protected void postProcessAux(DEPNode node, int dir, Triple<DEPNode,String,Double> max);
	
	/**
	 * Returns the number of transitions used for parsing the current dependency tree.
	 * @return the number of transitions used for parsing the current dependency tree.
	 */
	public int getNumTransitions()
	{
		return n_trans;
	}
	
	/** Parses the dependency tree. */
	public void parse(DEPTree tree)
	{
		init(tree);
		parseAux();

		if (i_flag == FLAG_PREDICT)
			postProcess();
		else if (i_flag == FLAG_DEMO)
			f_trans.println();
	}
	
	/** Initializes the dependency parser given the specific dependency tree. */
	protected void init(DEPTree tree)
	{
		d_tree   = tree;
		n_trans  = 0;
		i_lambda = 0;
		i_beta   = 1;
		s_reduce = new IntHashSet();
		
		if (i_flag != FLAG_PREDICT)
			g_heads = d_tree.getHeads();

		initArcs();
		tree.clearHeads();
	}
	
	/** Initializes dependency arcs of all nodes. */
	private void initArcs()
	{
		int size = d_tree.size();
		
		lm_deps = new DEPNode[size];
		rm_deps = new DEPNode[size];
	}
	
	/** Called by {@link AbstractDEPParser#parse(DEPTree)}. */
	protected void parseAux()
	{
		int size = d_tree.size();
		DEPNode  lambda, beta;
		String[] labels;
		
		while (i_beta < size)
		{
			if (i_lambda < 0)
			{
				noShift();
				continue;
			}
			
			lambda = d_tree.get(i_lambda);
			beta   = d_tree.get(i_beta); 
			labels = getLabels();
			n_trans++;
			
			if (labels[IDX_ARC].equals(LB_LEFT))
			{
				if (i_lambda == DEPLib.ROOT_ID)
					noShift();
				else if (beta.isDescendentOf(lambda))
					noPass();
				else if (labels[IDX_LIST].equals(LB_REDUCE))
					leftReduce(lambda, beta, labels[IDX_DEPREL]);
				else
					leftPass(lambda, beta, labels[IDX_DEPREL]);
			}
			else if (labels[IDX_ARC].equals(LB_RIGHT))
			{
				if (lambda.isDescendentOf(beta))
					noPass();
				else if (labels[IDX_LIST].equals(LB_SHIFT))
					rightShift(lambda, beta, labels[IDX_DEPREL]);
				else
					rightPass(lambda, beta, labels[IDX_DEPREL]);
			}
			else
			{
				if (labels[IDX_LIST].equals(LB_SHIFT))
					noShift();
				else if (labels[IDX_LIST].equals(LB_REDUCE) && lambda.hasHead())
					noReduce();
				else
					noPass();
			}
		}
	}

	/** @return [arc-label, list-label, dependency-label]. */
	protected String[] getGoldLabels()
	{
		String[] labels = getGoldLabelArc();
		
		if (labels[IDX_ARC].equals(LB_LEFT))
		{
			labels[IDX_LIST] = isGoldReduce(true) ? LB_REDUCE : LB_PASS;
		}
		else if (labels[IDX_ARC].equals(LB_RIGHT))
		{
			labels[IDX_LIST] = isGoldShift() ? LB_SHIFT : LB_PASS;
		}
		else
		{
			if (isGoldShift())
				labels[IDX_LIST] = LB_SHIFT;
			else if (isGoldReduce(false))
				labels[IDX_LIST] = LB_REDUCE;
			else
				labels[IDX_LIST] = LB_PASS;
		}
		
		return labels;
	}
	
	/** Called by {@link AbstractDEPParser#getGoldLabels()}. */
	private String[] getGoldLabelArc()
	{
		StringIntPair head = g_heads[i_lambda];
		String[] labels = new String[3];
		
		if (head.i == i_beta)
		{
			labels[IDX_ARC]    = LB_LEFT;
			labels[IDX_DEPREL] = head.s;
			return labels;
		}
		
		head = g_heads[i_beta];
		
		if (head.i == i_lambda)
		{
			labels[IDX_ARC]    = LB_RIGHT;
			labels[IDX_DEPREL] = head.s;
			return labels;
		}
		
		labels[IDX_ARC]    = LB_NO;
		labels[IDX_DEPREL] = "";
		
		return labels;
	}
	
	/** Called by {@link AbstractDEPParser#getGoldLabels()}. */
	private boolean isGoldShift()
	{
		if (g_heads[i_beta].i < i_lambda)
			return false;
		
		int i;
		
		for (i=i_lambda-1; i>0; i--)
		{
			if (s_reduce.contains(i))
				continue;
			
			if (g_heads[i].i == i_beta)
				return false;
		}
		
		return true;
	}
	
	/** Called by {@link AbstractDEPParser#getGoldLabels()}. */
	private boolean isGoldReduce(boolean hasHead)
	{
		if (!hasHead && !d_tree.get(i_lambda).hasHead())
			return false;
		
		int i, size = d_tree.size();
		
		for (i=i_beta+1; i<size; i++)
		{
			if (g_heads[i].i == i_lambda)
				return false;
		}
		
		return true;
	}
	
	protected void leftReduce(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Left-Reduce", i_lambda+" <-"+deprel+"- "+i_beta);
		
		leftArc(lambda, beta, deprel);
		reduce();
	}
	
	protected void leftPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Left-Pass", i_lambda+" <-"+deprel+"- "+i_beta);
		
		leftArc(lambda, beta, deprel);
		pass();
	}
	
	protected void rightShift(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Right-Shift", i_lambda+" -"+deprel+"-> "+i_beta);
		
		rightArc(lambda, beta, deprel);
		shift();
	}
	
	protected void rightPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Right-Pass", i_lambda+" -"+deprel+"-> "+i_beta);
		
		rightArc(lambda, beta, deprel);
		pass();
	}
	
	protected void noShift()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Shift", null);
		
		shift();
	}
	
	protected void noReduce()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Reduce", null);
		
		reduce();
	}
	
	protected void noPass()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Pass", null);
		
		pass();
	}
	
	private void leftArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		lambda.setHead(beta, deprel);
		lm_deps[i_beta] = lambda;
	}
	
	private void rightArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		beta.setHead(lambda, deprel);
		rm_deps[i_lambda] = beta;
	}
	
	private void shift()
	{
		i_lambda = i_beta++;
	}
	
	private void reduce()
	{
		s_reduce.add(i_lambda);
		passAux();
	}
	
	private void pass()
	{
		passAux();
	}
	
	private void passAux()
	{
		int i;
		
		for (i=i_lambda-1; i>=0; i--)
		{
			if (!s_reduce.contains(i))
			{
				i_lambda = i;
				return;
			}
		}
		
		i_lambda = i;
	}
	
	protected void postProcess()
	{
		Triple<DEPNode,String,Double> max = new Triple<DEPNode,String,Double>(null, null, -1d);
		DEPNode root = d_tree.get(DEPLib.ROOT_ID);
		int i, size = d_tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = d_tree.get(i);
			
			if (!node.hasHead())
			{
				max.set(root, DEPLibEn.DEP_ROOT, -1d);
				
				postProcessAux(node, -1, max);
				postProcessAux(node, +1, max);
				
				node.setHead(max.o1, max.o2);
			}
		}
	}
	
	protected void printState(String trans, String deprel)
	{
		StringBuilder build = new StringBuilder();

		// arc-transition
		build.append(trans);
		build.append("\t");
		
		// lambda_1
		build.append("[");
		
		if (i_lambda >= 0)
		{
			if (i_lambda > 0)
				build.append("L1|");
			
			build.append(i_lambda);
		}
		
		build.append("]");
		build.append("\t");
		
		// lambda_2
		build.append("[");
		int lambda2 = getFirstLambda2();
		
		if (i_beta - lambda2 > 0)
		{
			build.append(lambda2);
			
			if (i_beta - lambda2 > 1)
				build.append("|L2");
		}
		
		build.append("]");
		build.append("\t");
		
		// beta
		build.append("[");
		if (i_beta < d_tree.size())
		{
			build.append(i_beta);
			
			if (i_beta+1 < d_tree.size())
				build.append("|B");
		}
		
		build.append("]");
		build.append("\t");
		
		// relation
		if (deprel != null)
			build.append(deprel);
		
		f_trans.println(build.toString());
	}
	
	/** Called by {@link AbstractDEPParser#printState(String, String)}. */
	private int getFirstLambda2()
	{
		int i;
		
		for (i=i_lambda+1; i<i_beta; i++)
		{
			if (!s_reduce.contains(i))
				return i;
		}
		
		return d_tree.size();
	}
	
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(DEPFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(DEPFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(DEPFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(DEPFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if (token.isField(DEPFtrXml.F_LNPL))
		{
			return getLeftNearestPunctuation (0, i_lambda);
		}
		else if (token.isField(DEPFtrXml.F_RNPL))
		{
			return getRightNearestPunctuation(i_lambda, i_beta);
		}
		else if (token.isField(DEPFtrXml.F_LNPB))
		{
			return getLeftNearestPunctuation (i_lambda, i_beta);
		}
		else if (token.isField(DEPFtrXml.F_RNPB))
		{
			return getRightNearestPunctuation(i_beta, d_tree.size());
		}
		else if ((m = DEPFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		else if ((m = DEPFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			int field = Integer.parseInt(m.group(1));
			int size  = d_tree.size();
			
			switch (field)
			{
			case 0: return (i_lambda == 1) ? token.field : null;
			case 1: return (i_beta == size-1) ? token.field : null;
			case 2: return (i_lambda+1 == i_beta) ? token.field : null;
		//	case 3: return (i_lambda == 2) ? token.field : null;
		//	case 4: return (i_beta == size-2) ? token.field : null;
		//	case 5: return (i_lambda+2 == i_beta) ? token.field : null;
		//	case 6: return d_tree.get(i_lambda).hasHead() ? token.field : null;
		//	case 7: return d_tree.get(i_beta).hasHead() ? token.field : null;
			}
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		return null;
	}
	
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = null;
		
		switch (token.source)
		{
		case DEPFtrXml.S_STACK : node = getNodeStack(token);	break;
		case DEPFtrXml.S_LAMBDA: node = getNodeLambda(token);	break;
		case DEPFtrXml.S_BETA  : node = getNodeBeta(token);		break;
		}
		
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(DEPFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(DEPFtrXml.R_LMD))	node = lm_deps[node.id];
			else if (token.isRelation(DEPFtrXml.R_RMD))	node = rm_deps[node.id];			
		}
		
		return node;
	}
	
	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeStack(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int offset = Math.abs(token.offset), i;
		int dir    = (token.offset < 0) ? -1 : 1;
					
		for (i=i_lambda+dir; 0<i && i<i_beta; i+=dir)
		{
			if (!s_reduce.contains(i) && --offset == 0)
				return d_tree.get(i);
		}
		
		return null;
	}

	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeLambda(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int cIndex = i_lambda + token.offset;
		
		if (0 < cIndex && cIndex < i_beta)
			return d_tree.get(cIndex);
		
		return null;
	}
	
	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeBeta(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_beta);
		
		int cIndex = i_beta + token.offset;
		
		if (i_lambda < cIndex && cIndex < d_tree.size())
			return d_tree.get(cIndex);
		
		return null;
	}
	
	private String getLeftNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=rIdx-1; i>lIdx; i--)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
	
	private String getRightNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=lIdx+1; i<rIdx; i++)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
}
