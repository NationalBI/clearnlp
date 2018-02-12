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
package com.googlecode.clearnlp.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntHashSet;
import com.googlecode.clearnlp.ner.NERNode;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.DEPReader;


/**
 * Dependency node.
 * See <a target="_blank" href="http://code.google.com/p/clearnlp/source/browse/trunk/src/edu/colorado/clear/test/dependency/DPNodeTest.java">DPNodeTest</a> for the use of this class.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPNode extends NERNode implements Comparable<DEPNode>
{
	/** The ID of this node (default: {@link DEPLib#NULL_ID}). */
	public int             id;
	/** The extra features of this node (default: empty). */
	protected DEPFeat      d_feats;
	/** The dependency head of this node (default: empty). */
	protected DEPArc       d_head;
	/** The list of secondary heads of this node (default: empty). */
	protected List<DEPArc> x_heads;
	/** The list of semantic heads of this node (default: empty). */
	protected List<DEPArc> s_heads;
	/** The sorted list of all dependents of this node (default: empty). */
	protected List<DEPArc> l_dependents;
	/** {@code true} if this node is a terminal. */
	public boolean         b_terminal;
	
	/**
	 * Constructs a null dependency node.
	 * Calls {@link DEPNode#initNull()}.
	 */
	public DEPNode()
	{
		super();
	}
	
	/**
	 * Constructs a dependency node using the specific values.
	 * Calls {@link DEPNode#init(int, String, String, String, String, int, String)}.
	 */
	public DEPNode(int id, String form, String lemma, String pos, DEPFeat feats)
	{
		init(id, form, lemma, pos, feats);
	}
	
	public DEPNode(int id, String form)
	{
		init(id, form, null, null, new DEPFeat());
	}
	
	public DEPNode(int id, POSNode node)
	{
		init(id, node.form, node.lemma, node.pos, new DEPFeat());
	}
	
	/** Initializes this node as an artificial root node. */
	public void initRoot()
	{
		init(DEPLib.ROOT_ID, DEPLib.ROOT_TAG, DEPLib.ROOT_TAG, DEPLib.ROOT_TAG, new DEPFeat());
	}
	
	/**
	 * Initializes the values of this node.
	 * @param id the ID of this node.
	 * @param form the word-form of this node.
	 * @param lemma the lemma of the word-form.
	 * @param pos the part-of-speech tag of this node.
	 * @param feats the extra features of this node.
	 * @param headId the ID of the head of this node.
	 * @param s the dependency label of this node to its head.
	 */
	public void init(int id, String form, String lemma, String pos, DEPFeat feats)
	{
		this.id      = id;
		this.form    = form;
		this.lemma   = lemma;
		this.pos     = pos;
		this.nament   = AbstractColumnReader.BLANK_COLUMN;
		d_feats      = feats;
		d_head       = new DEPArc();
	//	x_heads      = new ArrayList<DEPArc>();
	//	s_heads      = new ArrayList<DEPArc>();
	//	l_dependents = new ArrayList<DEPArc>();
	}
	
	/**
	 * Returns {@code true} if this node is an artificial root.
	 * @return {@code true} if this node is an artificial root.
	 */
	public boolean isRoot()
	{
		DEPNode head = getHead();
		return head != null && head.id == DEPLib.ROOT_ID;
	}
	
	/**
	 * Returns the value of the specific feature.
	 * If the feature does not exist, returns {@code null}.
	 * @param key the key of the feature.
	 * @return the value of the specific feature.
	 */
	public String getFeat(String key)
	{
		return d_feats.get(key);
	}
	
	/**
	 * Adds an extra feature to this node using the specific key and value.
	 * @param key the key of the feature.
	 * @param value the value of the feature.
	 */
	public void addFeat(String key, String value)
	{
		d_feats.put(key, value);
	}
	
	/**
	 * Removes the feature with the specific key.
	 * @param key the key of the feature to be removed.
	 */
	public void removeFeat(String key)
	{
		d_feats.remove(key);
	}
	
	public void setFeats(DEPFeat feats)
	{
		d_feats = feats;
	}
	
	/**
	 * Returns the dependency label of this node to its head. 
	 * @return the dependency label of this node to its head.
	 */
	public String getLabel()
	{
		return d_head.label;
	}
	
	/**
	 * Sets the dependency label of this node to the specific label.
	 * @param label the dependency label to be assigned.
	 */
	public void setLabel(String label)
	{
		d_head.setLabel(label);
	}
	
	/**
	 * Returns {@code true} if the dependency label of this node equals to the specific label.
	 * @param label the dependency label to be compared.
	 * @return {@code true} if the dependency label of this node equals to the specific label.
	 */
	public boolean isLabel(String label)
	{
		return d_head.label != null && d_head.isLabel(label);
	}
	
	/**
	 * Returns the dependency head of this node.
	 * If the head does not exists, returns {@code null}.
	 * @return the dependency head of this node.
	 */
	public DEPNode getHead()
	{
		return d_head.node;
	}
	
	public void setHead(DEPNode head)
	{
		d_head.setNode(head);
	}
	
	/**
	 * Sets the dependency head and label of this node to the specific node and label.
	 * If the head already exists, replaces the head with the specific node.
	 * @param headId the ID of the head.
	 * @param label the dependency label to the head.
	 */
	public void setHead(DEPNode head, String label)
	{
		d_head.set(head, label);
	}
	
	/**
	 * Returns {@code true} if this node has a dependency head.
	 * @return {@code true} if this node has a dependency head.
	 */
	public boolean hasHead()
	{
		return d_head.node != null;
	}
	
	public void clearHead()
	{
		d_head.clear();
	}
	
	public DEPNode getGrandHead()
	{
		DEPNode head = getHead();
		return (head == null) ? null : head.getHead();
	}
	
	/**
	 * Returns {@code true} if this node is a dependent of the specific node.
	 * @param node the potential head.
	 * @return {@code true} if this node is a dependent of the specific node.
	 */
	public boolean isDependentOf(DEPNode node)
	{
		return d_head.isNode(node);
	}
	
	public boolean isSiblingOf(DEPNode node)
	{
		return node.isDependentOf(getHead());
	}
	
	/**
	 * Returns {@code true} if this node is a descendant of the specific node. 
	 * @param node the potential ancestor.
	 * @return {@code true} if this node is a descendant of the specific node.
	 */
	public boolean isDescendentOf(DEPNode node)
	{
		DEPNode head = getHead();
		
		while (head != null)
		{
			if (head == node)	return true;
			head = head.getHead();
		}
		
		return false;
	}
	
	public List<DEPArc> getSHeads()
	{
		return s_heads;
	}
	
	public void setSHeads(List<DEPArc> sHeads)
	{
		s_heads = sHeads;
	}
	
	public List<DEPArc> getSHeadsByLabel(String label)
	{
		List<DEPArc> sHeads = new ArrayList<DEPArc>();
		
		for (DEPArc arc : s_heads)
		{
			if (arc.isLabel(label))
				sHeads.add(arc);
		}
		
		return sHeads;
	}
	
	public void addSHead(DEPNode head, String label)
	{
		s_heads.add(new DEPArc(head, label));
	}
	
	public void removeSHeads(Collection<DEPArc> sHeads)
	{
		s_heads.removeAll(sHeads);
	}
	
	public void removeSHeadsByLabel(String label)
	{
		s_heads.removeAll(getSHeadsByLabel(label));
	}
	
	public boolean containsSHead(DEPNode sHead)
	{
		for (DEPArc arc : s_heads)
		{
			if (arc.isNode(sHead))
				return true;
		}
		
		return false;
	}
	
	public boolean containsSHead(String label)
	{
		for (DEPArc arc : s_heads)
		{
			if (arc.isLabel(label))
				return true;
		}
		
		return false;
	}
	
	public boolean isArgumentOf(DEPNode sHead)
	{
		for (DEPArc arc : s_heads)
		{
			if (arc.isNode(sHead))
				return true;
		}
		
		return false;
	}
	
	public boolean isArgumentOf(DEPNode sHead, String label)
	{
		for (DEPArc arc : s_heads)
		{
			if (arc.isNode(sHead) && arc.isLabel(label))
				return true;
		}
		
		return false;
	}
	
	public List<DEPArc> getDependents()
	{
		return l_dependents;
	}
	
	public List<DEPNode> getLeftDependents()
	{
		List<DEPNode> list = new ArrayList<DEPNode>();
		int i, size = l_dependents.size();
		DEPArc arc;
		
		for (i=0; i<size; i++)
		{
			arc = l_dependents.get(i);
			
			if (arc.getNode().id > id)
				break;
			
			list.add(arc.getNode());
		}
		
		return list;
	}
	
	public List<DEPNode> getRightDependents()
	{
		List<DEPNode> list = new ArrayList<DEPNode>();
		int i;
		DEPArc arc;
		
		for (i=l_dependents.size()-1; i>=0; i--)
		{
			arc = l_dependents.get(i);
			
			if (arc.getNode().id < id)
				break;
			
			list.add(arc.getNode());
		}
		
		return list;
	}
	
	public List<DEPArc> getGrandDependents()
	{
		List<DEPArc> list = new ArrayList<DEPArc>();
		
		for (DEPArc arc : l_dependents)
			list.addAll(arc.getNode().getDependents());
	
		return list;
	}
	
	public DEPNode getLeftNearestDependent()
	{
		DEPArc arc;
		int i;
		
		for (i=l_dependents.size()-1; i>=0; i--)
		{
			arc = l_dependents.get(i);
			
			if (arc.getNode().id < id)
				return arc.getNode();
		}
		
		return null;
	}
	
	public DEPNode getRightNearestDependent()
	{
		int i, size = l_dependents.size();
		DEPArc arc;
		
		for (i=0; i<size; i++)
		{
			arc = l_dependents.get(i);
			
			if (arc.getNode().id > id)
				return arc.getNode();
		}
		
		return null;
	}
	
	public DEPNode getLeftMostDependent()
	{
		DEPNode dep;
		
		if (!l_dependents.isEmpty())
		{
			dep = l_dependents.get(0).getNode();
			if (dep.id < id)	return dep;
		}

		return null;
	}
	
	public DEPNode getRightMostDependent()
	{
		DEPNode dep;
		
		if (!l_dependents.isEmpty())
		{
			dep = l_dependents.get(l_dependents.size()-1).getNode();
			if (dep.id > id)	return dep;
		}

		return null;
	}
	
	/** @return a list of descendents with the specific labels. */
	public List<DEPNode> getDescendents(Pattern regex)
	{
		List<DEPNode> desc = new ArrayList<DEPNode>();
		getDescendentsAux(this, desc, regex);
		
		return desc;
	}
	
	public void getDescendentsAux(DEPNode node, List<DEPNode> desc, Pattern regex)
	{
		for (DEPArc arc : node.getDependents())
		{
			if (arc.isLabel(regex))
			{
				getDescendentsAux(arc.getNode(), desc, regex);
				desc.add(arc.getNode());
			}
		}
	}
	
	/** @param depth > 0. */
	public List<DEPArc> getDescendents(int depth)
	{
		List<DEPArc> list = new ArrayList<DEPArc>();
		
		getDescendentsAux(this, list, depth-1);
		return list;
	}
	
	public void getDescendentsAux(DEPNode curr, List<DEPArc> list, int depth)
	{
		List<DEPArc> deps = curr.getDependents();
		list.addAll(deps);
		
		if (depth == 0)	return;
		
		for (DEPArc arc : deps)
			getDescendentsAux(arc.getNode(), list, depth-1);
	}
	
	public Set<DEPNode> getArgumentCandidateSet(int depth, boolean includeSelf)
	{
		Set<DEPNode> set = new HashSet<DEPNode>();
		
		for (DEPArc arc : getDescendents(depth))
			set.add(arc.getNode());
		
		DEPNode head = getHead();
		
		while (head != null)
		{
			set.add(head);
			
			for (DEPArc arc : head.getDependents())
				set.add(arc.getNode());
						
			head = head.getHead();
		}
		
	/*	if (head != null)
		{
			for (DEPArc arc : head.getGrandDependents())
				set.add(arc.getNode());
					
			do
			{
				for (DEPArc arc : head.getDependents())
					set.add(arc.getNode());
							
				head = head.getHead();
			}
			while (head != null);
		}*/
		
		if (includeSelf)	set.add   (this);
		else				set.remove(this);
		
		return set;
	}
	
	public List<DEPNode> getDependentsByLabels(String... labels)
	{
		List<DEPNode> list = new ArrayList<DEPNode>();
		
		for (DEPArc arc : l_dependents)
		{
			for (String label : labels)
			{
				if (arc.isLabel(label))
					list.add(arc.getNode());
			}
		}
		
		return list;
	}
	
	void addDependent(DEPNode node, String label)
	{
		l_dependents.add(new DEPArc(node, label));
	}
	
	public boolean containsDependent(String label)
	{
		for (DEPArc node : l_dependents)
		{
			if (node.isLabel(label))
				return true;
		}
		
		return false;
	}
	
	public IntHashSet getSubtreeIdSet()
	{
		IntHashSet set = new IntHashSet();
		
		getSubtreeIdSetAux(set, this);
		return set;
	}

	private void getSubtreeIdSetAux(IntHashSet set, DEPNode curr)
	{
		set.add(curr.id);
		
		for (DEPArc arc : curr.getDependents())
			getSubtreeIdSetAux(set, arc.getNode());
	}
	
	public int[] getSubtreeIdArray()
	{
		IntHashSet set = getSubtreeIdSet();
		int[] list = set.toArray();
		Arrays.sort(list);
		
		return list;
	}
	
	public String toStringPOS()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(form);		build.append(DEPReader.DELIM_COLUMN);
		build.append(pos);		build.append(DEPReader.DELIM_COLUMN);
		build.append(d_feats);
		
		return build.toString();
	}
	
	public String toStringMorph()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(form);		build.append(DEPReader.DELIM_COLUMN);
		build.append(lemma);	build.append(DEPReader.DELIM_COLUMN);
		build.append(pos);
		
		return build.toString();
	}
	
	public String toStringDEP()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(id);		build.append(DEPReader.DELIM_COLUMN);
		build.append(form);		build.append(DEPReader.DELIM_COLUMN);
		build.append(lemma);	build.append(DEPReader.DELIM_COLUMN);
		build.append(pos);		build.append(DEPReader.DELIM_COLUMN);
		build.append(d_feats);	build.append(DEPReader.DELIM_COLUMN);
		
		if (hasHead())
		{
			build.append(d_head.node.id);	build.append(DEPReader.DELIM_COLUMN);
			build.append(d_head.label);
		}
		else
		{
			build.append(AbstractColumnReader.BLANK_COLUMN);	build.append(DEPReader.DELIM_COLUMN);
			build.append(AbstractColumnReader.BLANK_COLUMN);
		}
		
		return build.toString();
	}
	
	public String toStringCoNLL()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(id);						build.append(DEPReader.DELIM_COLUMN);
		build.append(form);						build.append(DEPReader.DELIM_COLUMN);
		build.append(lemma);					build.append(DEPReader.DELIM_COLUMN);
		build.append(pos);						build.append(DEPReader.DELIM_COLUMN);
		build.append(pos);						build.append(DEPReader.DELIM_COLUMN);
		build.append(DEPReader.BLANK_COLUMN);	build.append(DEPReader.DELIM_COLUMN);
		
		if (hasHead())
		{
			build.append(d_head.node.id);	build.append(DEPReader.DELIM_COLUMN);
			build.append(d_head.label);
		}
		else
		{
			build.append(AbstractColumnReader.BLANK_COLUMN);	build.append(DEPReader.DELIM_COLUMN);
			build.append(AbstractColumnReader.BLANK_COLUMN);
		}
		
		return build.toString();
	}
	
	public String toStringDAG()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(toStringDEP());		build.append(DEPReader.DELIM_COLUMN);
		build.append(toString(x_heads));
		
		return build.toString();
	}
	
	public String toStringSRL()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(toStringDEP());		build.append(DEPReader.DELIM_COLUMN);
		build.append(toString(s_heads));
		
		return build.toString();
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(toStringDEP());		build.append(DEPReader.DELIM_COLUMN);
		build.append(toString(x_heads));	build.append(DEPReader.DELIM_COLUMN);
		build.append(toString(s_heads));	build.append(DEPReader.DELIM_COLUMN);
		build.append(nament);
		
		return build.toString();
	}
	
	private String toString(List<DEPArc> heads)
	{
		StringBuilder build = new StringBuilder();
		Collections.sort(heads);
		
		for (DEPArc arc : heads)
		{
			build.append(DEPLib.DELIM_HEADS);
			build.append(arc.toString());
		}
		
		if (build.length() > 0)
			return build.substring(DEPLib.DELIM_HEADS.length());
		else
			return AbstractColumnReader.BLANK_COLUMN;
	}

	@Override
	public int compareTo(DEPNode node)
	{
		return id - node.id;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Returns secondary heads of this node.
	 * @return secondary heads of this node.
	 */
	public List<DEPArc> getXHeads()
	{
		return x_heads;
	}
	
	public Set<DEPNode> getXAncestorSet()
	{
		Set<DEPNode> set = new HashSet<DEPNode>();
		
		getXAncestorIdSetAux(this, set);
		return set;
	}
	
	private void getXAncestorIdSetAux(DEPNode node, Set<DEPNode> set)
	{
		DEPNode head;
		
		for (DEPArc arc : node.x_heads)
		{
			head = arc.getNode();
			
			set.add(head);
			getXAncestorIdSetAux(head, set);
		}
	}
	
	public DEPArc getXHead(DEPNode head)
	{
		for (DEPArc arc : x_heads)
		{
			if (arc.isNode(head))
				return arc;
		}
		
		return null;
	}
	
	public List<DEPArc> getXHeads(String label)
	{
		List<DEPArc> list = new ArrayList<DEPArc>();
		
		for (DEPArc arc : x_heads)
		{
			if (arc.isLabel(label))
				list.add(arc);
		}
		
		return list;
	}
	
	public void addXHead(DEPNode head, String label)
	{
		x_heads.add(new DEPArc(head, label));
	}
	
	public boolean hasXHead()
	{
		return !x_heads.isEmpty();
	}
	
	public boolean isXDescendentOf(DEPNode node)
	{
		return isXDescendentOfAux(this, node);
	}
	
	private boolean isXDescendentOfAux(DEPNode curr, DEPNode node)
	{
		for (DEPArc arc : curr.x_heads)
		{
			if (arc.isNode(node) || isXDescendentOfAux(arc.getNode(), node))
				return true;
		}
		
		return false;
	}
	
	public boolean containsXHead(DEPNode xHead)
	{
		for (DEPArc arc : x_heads)
		{
			if (arc.isNode(xHead))
				return true;
		}
		
		return false;
	}
	
/*	protected void addChild(DPNode child)
	{
		int idx = Collections.binarySearch(l_dependents, child);
		if (idx < 0)	l_dependents.add(-idx-1, child);
	}
	
	public String getXPath(DEPNode node, int flag)
	{
		StringIntPair path = getXPathUp(node, flag);
		if (path.s != null)	return path.s;
		
		path = getXPathDown(node, flag);
		if (path.s != null)	return path.s;
	
		getXPathAux(this, node, node.getXAncestorSet(), "", path, 0, flag);
		return path.s;
	}
	
	public StringIntPair getXPathUp(DEPNode node, int flag)
	{
		StringIntPair path = new StringIntPair(null, Integer.MAX_VALUE);

		getXPathUpAux(this, node, "", path, 0, flag, DEPLib.DELIM_PATH_UP);
		return path;
	}
	
	public StringIntPair getXPathDown(DEPNode node, int flag)
	{
		StringIntPair path = new StringIntPair(null, Integer.MAX_VALUE);

		getXPathUpAux(node, this, "", path, 0, flag, DEPLib.DELIM_PATH_DOWN);
		return path;
	}
	
	private String getXPathAux(DEPNode source, DEPNode target, Set<DEPNode> targetAncestors, String prevPath, StringIntPair path, int height, int flag)
	{
		String currPath = (flag == 0) ? prevPath + DEPLib.DELIM_PATH_DOWN + source.pos : prevPath + DEPLib.DELIM_PATH_DOWN;
		DEPNode head;
		
		for (DEPArc arc : source.x_heads)
		{
			head = arc.getNode();
			
			if (targetAncestors.contains(head))
			{
				StringIntPair sPath = target.getXPathUp(head, flag);
				
				if (height < path.i)
				{
					switch (flag)
					{
					case 0: path.set(sPath.s + currPath, sPath.i + height); break;
					case 1: path.set(sPath.s + currPath + arc.label, sPath.i + height); break;
					case 2: path.set(sPath.s + DEPLib.DELIM_PATH_DOWN + height, sPath.i + height);
					}
				}
				
				break;
			}
			else
			{
				if (flag == 0)	getXPathAux(head, target, targetAncestors, currPath, path, height+1, flag);
				else			getXPathAux(head, target, targetAncestors, currPath + arc.label, path, height+1, flag);
			}
		}
		
		return null;
	}
	
	private void getXPathUpAux(DEPNode curr, DEPNode head, String prevPath, StringIntPair path, int height, int flag, String delim)
	{
		String currPath = (flag == 0) ? prevPath + delim + curr.pos : prevPath + delim;
		
		for (DEPArc arc : curr.x_heads)
		{
			if (arc.isNode(head))
			{
				if (height < path.i)
				{
					switch (flag)
					{
					case 0: path.set(currPath + delim + head.pos, height); break;
					case 1: path.set(currPath + arc.label, height); break;
					case 2: path.set(delim+height, height);
					}
				}
				
				break;
			}
			else
			{
				if (flag == 0)	getXPathUpAux(arc.getNode(), head, currPath, path, height+1, flag,  delim);
				else			getXPathUpAux(arc.getNode(), head, currPath + arc.label, path, height+1, flag, delim);
			}
		}
	} */
}