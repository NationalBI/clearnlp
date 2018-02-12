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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.googlecode.clearnlp.coreference.Mention;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


/**
 * Dependency tree.
 * See <a target="_blank" href="http://code.google.com/p/clearnlp/source/browse/trunk/src/edu/colorado/clear/test/dependency/DPTreeTest.java">DPTreeTest</a> for the use of this class.
 * @see DEPNode
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPTree extends ArrayList<DEPNode>
{
	private static final long serialVersionUID = -8007954222948953695L;
	private List<Mention> l_mentions;
	
	/**
	 * Constructs a dependency tree.
	 * An artificial root node gets inserted automatically.
	 */
	public DEPTree()
	{
		DEPNode root = new DEPNode();
		
		root.initRoot();
		add(root);
	}
	
	public void initXHeads()
	{
		int i, size = size();
		
		for (i=0; i<size; i++)
			get(i).x_heads = new ArrayList<DEPArc>();
	}
	
	public void initSHeads()
	{
		int i, size = size();
		
		for (i=0; i<size; i++)
			get(i).s_heads = new ArrayList<DEPArc>();
	}
	
	/**
	 * Returns the dependency node with the specific ID.
	 * If there is no such node, returns {@code null}.
	 * @return the dependency node with the specific ID.
	 */
	public DEPNode get(int id)
	{
		try
		{
			return super.get(id);
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;
		}
	}
	
	public int getLeftValency(int id)
	{
		DEPNode node = get(id);
		int i, c = 0;
		
		for (i=node.id-1; i>0; i--)
		{
			if (get(i).getHead() == node)
				c++;
		}
		
		return c;
	}
	
	public int getRightValency(int id)
	{
		int i, c = 0, size = size();
		DEPNode node = get(id);
		
		for (i=node.id+1; i<size; i++)
		{
			if (get(i).getHead() == node)
				c++;
		}
		
		return c;
	}
	
	public DEPNode getLeftMostDependent(int id)
	{
		DEPNode node, head = get(id);
		int i;
		
		for (i=1; i<id; i++)
		{
			node = get(i);
			
			if (node.getHead() == head)
				return node;
		}
		
		return null;
	}
	
	public DEPNode getLeftMostDependent(int id, int order)
	{
		DEPNode node, head = get(id);
		int i;
		
		for (i=1; i<id; i++)
		{
			node = get(i);
			
			if (node.getHead() == head)
			{
				if (order == 0)	return node;
				order--;
			}
		}
		
		return null;
	}
	
	public DEPNode getRightMostDependent(int id)
	{
		DEPNode node, head = get(id);
		int i;
		
		for (i=size()-1; i>id; i--)
		{
			node = get(i);
			
			if (node.getHead() == head)
				return node;
		}
		
		return null;
	}
	
	public DEPNode getRightMostDependent(int id, int order)
	{
		DEPNode node, head = get(id);
		int i;
		
		for (i=size()-1; i>id; i--)
		{
			node = get(i);
			
			if (node.getHead() == head)
			{
				if (order == 0)	return node;
				order--;
			}
		}
		
		return null;
	}
	
	public DEPNode getLeftNearestSibling(int id)
	{
		DEPNode node, head = get(id).getHead();
		if (head == null)	return null;
		int i, eIdx = (head.id < id) ? head.id : 0;
		
		for (i=id-1; i>eIdx; i--)
		{
			node = get(i);
			
			if (node.getHead() == head)
				return node;
		}
		
		return null;
	}
	
	public DEPNode getRightNearestSibling(int id)
	{
		DEPNode node, head = get(id).getHead();
		if (head == null)	return null;
		int i, eIdx = (id < head.id) ? head.id : size();
		
		for (i=id+1; i<eIdx; i++)
		{
			node = get(i);
			
			if (node.getHead() == head)
				return node;
		}
		
		return null;
	}
	
	public DEPNode getNextPredicate(int prevId)
	{
		int i, size = size();
		DEPNode pred;
		
		for (i=prevId+1; i<size; i++)
		{
			pred = get(i);
			
			if (pred.getFeat(DEPLib.FEAT_PB) != null)
				return pred;
		}
		
		return null;
	}
	
	public boolean containsPredicate()
	{
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			if (get(i).getFeat(DEPLib.FEAT_PB) != null)
				return true;
		}
		
		return false;
	}
	
	public void setDependents()
	{
		int i, size = size();
		DEPNode node, head;
		
		if (get(0).l_dependents != null)
			return;
		
		for (i=0; i<size; i++)
			get(i).l_dependents = new ArrayList<DEPArc>();
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			head = node.getHead();
			
			if (head != null)
				head.addDependent(node, node.getLabel());
		}
	}
	
	public List<List<DEPArc>> getArgumentList()
	{
		int i, size = size();
		List<DEPArc> args;
		DEPNode node;
		
		List<List<DEPArc>> list = new ArrayList<List<DEPArc>>();
		for (i=0; i<size; i++)	list.add(new ArrayList<DEPArc>());
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			
			for (DEPArc arc : node.getSHeads())
			{
				args = list.get(arc.getNode().id);
				args.add(new DEPArc(node, arc.getLabel()));
			}
		}
		
		return list;
	}
	
	/**
	 * Returns {@code true} if this tree contains a cycle.
	 * @return {@code true} if this tree contains a cycle.
	 */
	public boolean containsCycle()
	{
		int i, size = size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			
			if (node.getHead().isDescendentOf(node))
				return true;
		}
		
		return false;
	}
	
	public List<Mention> getMentions()
	{
		return l_mentions;
	}
	
	public void setMentions(List<Mention> mentions)
	{
		l_mentions = mentions;
	}
	
	// --------------------------------- merge ---------------------------------
	
	public void merge(List<StringIntPair[]> lHeads)
	{
		int i, size = size();
			
		StringIntPair[]   H = new StringIntPair[size];
		List<DEPCountArc> F = new ArrayList<DEPCountArc>(); 
		IntHashSet    T = new IntHashSet();
		DEPCountArc a;
		
		StringIntPair[] t = lHeads.get(0);
			
		for (i=1; i<size; i++)
			H[i] = new StringIntPair(t[i].s, t[i].i);
			
		T.add(DEPLib.ROOT_ID);
		F.addAll(getArcs(lHeads, T));
			
		while (!F.isEmpty())
		{
			Collections.sort(F);
			a = F.get(0);
			
			H[a.depId].i = a.headId;
			H[a.depId].s = a.deprel;
		
			T.add(a.depId);
			removeArcs(F, a.depId);
			
			F.addAll(getArcs(lHeads, T));			
		}
			
		resetHeads(H);
	}
		
	private List<DEPCountArc> getArcs(List<StringIntPair[]> lHeads, IntHashSet T)
	{
		Map<String,DEPCountArc> map = new HashMap<String,DEPCountArc>();
		int i, depId, len = size(), size = lHeads.size();
		DEPCountArc val;
		StringIntPair[] heads;
		StringIntPair head;
		String key;
		
		for (i=0; i<size; i++)
		{
			heads = lHeads.get(i);
			
			for (depId=1; depId<len; depId++)
			{
				head = heads[depId];
				
				if (head != null && T.contains(head.i) && !T.contains(depId))
				{
					key = depId+"_"+head.i+"_"+head.s;
					val = map.get(key);
						
					if (val == null)
					{
						val = new DEPCountArc(1, i, depId, head.i, head.s);
						map.put(key, val);
					}
					else
						val.count++;
					
					heads[depId] = null;	
				}
			}
		}
		
		return new ArrayList<DEPCountArc>(map.values());
	}
	
	private void removeArcs(List<DEPCountArc> F, int depId)
	{
		List<DEPCountArc> remove = new ArrayList<DEPCountArc>();
			
		for (DEPCountArc p : F)
		{
			if (p.depId == depId)
				remove.add(p);
		}
		
		F.removeAll(remove);
	}
	
	// --------------------------------- projectivize ---------------------------------
	
	public void projectivize()
	{
		IntArrayList ids = new IntArrayList();
		int i, size = size();
		DEPNode nonProj;
		
		for (i=1; i<size; i++)
			ids.add(i);
		
		while ((nonProj = getSmallestNonProjectiveArc(ids)) != null)
			nonProj.setHead(nonProj.getHead().getHead(), DEPLib.DEP_NON_PROJ);
	}
	
	/** Called by {@link DEPTree#projectivize()}. */
	private DEPNode getSmallestNonProjectiveArc(IntArrayList ids)
	{
		IntHashSet remove = new IntHashSet();
		DEPNode wk, nonProj = null;
		int np, max = 0;
		
		for (IntCursor cur : ids)
		{
			wk = get(cur.value);
			np = isNonProjective(wk);
			
			if (np == 0)
			{
				remove.add(cur.value);
			}
			else if (np > max)
			{
				nonProj = wk;
				max = np;
			}
		}
		
		ids.removeAll(remove);
		return nonProj;
	}
	
	/** @return > 0 if w_k is non-projective. */
	public int isNonProjective(DEPNode wk)
	{
		DEPNode wi = wk.getHead();
		if (wi == null) return 0;
		DEPNode wj;
		
		int bId, eId, j;

		if (wk.id < wi.id)
		{
			bId = wk.id;
			eId = wi.id;
		}
		else
		{
			bId = wi.id;
			eId = wk.id;
		}
		
		for (j=bId+1; j<eId; j++)
		{
			wj = get(j);
			
			if (!wj.isDescendentOf(wi))
				return Math.abs(wi.id - wk.id);
		}

		return 0;
	}
	
	// --------------------------------- clearGoldTags ---------------------------------
	
	public void clearPOSTags()
	{
		for (DEPNode node : this)
			node.pos = null;
	}
	
	public void clearHeads()
	{
		for (DEPNode node : this)
			node.d_head.clear();
	}
	
	public void clearXHeads()
	{
		for (DEPNode node : this)
			node.x_heads.clear();
	}
	
	public void clearSHeads()
	{
		for (DEPNode node : this)
			node.s_heads.clear();
	}
	
	public void clearPredicates()
	{
		for (DEPNode node : this)
			node.removeFeat(DEPLib.FEAT_PB);
	}
	
	// --------------------------------- reset ---------------------------------
	
	public void resetPOSTags(String[] tags)
	{
		int i, size = size();
		
		for (i=1; i<size; i++)
			get(i).pos = tags[i];
	}
	
	public void resetHeads(StringIntPair[] heads)
	{
		int i, size = size();
		StringIntPair head;
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			head = heads[i];
			
			if (head.i == DEPLib.NULL_ID)
				node.clearHead();
			else
				node.setHead(get(head.i), head.s);
		}
	}
	
	public StringIntPair[] getDiff(StringIntPair[] heads)
	{
		int i, size = size();
		DEPNode node, head;
		
		StringIntPair[] diff = new StringIntPair[size];
		StringIntPair p;
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			head = node.getHead();
			p    = heads[i];
			
			if (head != null && head.id != p.i && !node.isLabel(p.s))
				diff[i] = new StringIntPair(node.getLabel(), head.id);
			else
				diff[i] = new StringIntPair(null, DEPLib.NULL_ID);
		}
		
		return diff;
	}
	
	public void appendHeads(StringIntPair[] heads)
	{
		int i, size = size();
		StringIntPair p;
		
		for (i=1; i<size; i++)
		{
			p = heads[i];
			
			if (p.i != DEPLib.NULL_ID)
				get(i).setHead(get(p.i), p.s);
		}
	}
	
	// --------------------------------- getGoldTags ---------------------------------
	
	public String[] getPOSTags()
	{
		int i, size = size();
		String[] tags = new String[size];
		
		for (i=1; i<size; i++)
			tags[i] = get(i).pos;
		
		return tags;
	}
	
	public StringIntPair[] getHeads()
	{
		int i, size = size();
		DEPArc head;
		
		StringIntPair[] heads = new StringIntPair[size];
		heads[0] = new StringIntPair(DEPLib.ROOT_TAG, DEPLib.NULL_ID);
		
		for (i=1; i<size; i++)
		{
			head = get(i).d_head;
			heads[i] = (head.node != null) ? new StringIntPair(head.label, head.getNode().id) : new StringIntPair(null, DEPLib.NULL_ID);
		}
		
		return heads;
	}
	
	public Boolean[] getPredicates()
	{
		int i, size = size();
		Boolean[] rolesets = new Boolean[size];
		
		for (i=1; i<size; i++)
			rolesets[i] = get(i).getFeat(DEPLib.FEAT_PB) != null;
		
		return rolesets;
	}
	
	public String[] getSenses(String key)
	{
		int i, size = size();
		String[] senses = new String[size];
		
		for (i=1; i<size; i++)
			senses[i] = get(i).getFeat(key);
		
		return senses;
	}
	
	public String[] getRolesetIDs()
	{
		int i, size = size();
		String[] rolesets = new String[size];
		
		for (i=1; i<size; i++)
			rolesets[i] = get(i).getFeat(DEPLib.FEAT_PB);
		
		return rolesets;
	}
	
	public StringIntPair[][] getXHeads()
	{
		return getHeadsAux(true);
	}
	
	public StringIntPair[][] getSHeads()
	{
		return getHeadsAux(false);
	}
	
	private StringIntPair[][] getHeadsAux(boolean isXhead)
	{
		int i, j, len, size = size();
		StringIntPair[] heads;
		List<DEPArc> arcs;
		DEPArc arc;
		
		StringIntPair[][] xHeads = new StringIntPair[size][];
		xHeads[0] = new StringIntPair[0];
		
		for (i=1; i<size; i++)
		{
			arcs  = isXhead ? get(i).getXHeads() : get(i).getSHeads();
			len   = arcs.size();
			heads = new StringIntPair[len];
			
			for (j=0; j<len; j++)
			{
				arc = arcs.get(j);
				if (arc.getNode() == null)
				{
					System.err.println(i+"\n"+toStringDEP());
				}
				heads[j] = new StringIntPair(arc.label, arc.getNode().id);
			}
			
			xHeads[i] = heads;
		}
		
		return xHeads;
	}
	
	// --------------------------------- toString ---------------------------------
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i));
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringRaw()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(" ");
			build.append(get(i).form);
		}
		
		return build.substring(1);
	}
	
	public String toStringPOS()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringPOS());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringMorph()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringMorph());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringDEP()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringDEP());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringDAG()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringDAG());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringCoNLL()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringCoNLL());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringSRL()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringSRL());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// --------------------------------- depredicated ---------------------------------
	
	@Deprecated
	public IntHashSet getNonProjectiveSet()
	{
		IntObjectHashMap<IntHashSet> map = new IntObjectHashMap<IntHashSet>();
		int i, j, bIdx, eIdx, size = size();
		DEPNode curr, head, dep;
		
		for (i=1; i<size; i++)
		{
			curr = get(i);
			head = curr.getHead();
			
			if (curr.id < head.id)
			{
				bIdx = curr.id;
				eIdx = head.id;
			}
			else
			{
				bIdx = head.id;
				eIdx = curr.id;
			}
			
			for (j=bIdx+1; j<eIdx; j++)
			{
				curr = get(j);
				head = curr.getHead();
				
				if (head.id < bIdx || head.id > eIdx)
				{
					addNonProjectiveMap(map, i, j);
					addNonProjectiveMap(map, j, i);
				}

				for (DEPArc arc : curr.getDependents())
				{
					dep = arc.getNode();
					
					if (dep.id < bIdx || dep.id > eIdx)
					{
						addNonProjectiveMap(map, i, dep.id);
						addNonProjectiveMap(map, dep.id, i);						
					}
				}
			}
		}
		
		return getNonProjectiveMapAux(map);
	}
	
	@Deprecated
	private void addNonProjectiveMap(IntObjectHashMap<IntHashSet> map, int cIdx, int nIdx)
	{
		IntHashSet set;
		
		if (map.containsKey(cIdx))
			set = map.get(cIdx);
		else
		{
			set = new IntHashSet();
			map.put(cIdx, set);
		}
		
		set.add(nIdx);
	}
	
	@Deprecated
	private IntHashSet getNonProjectiveMapAux(IntObjectHashMap<IntHashSet> map)
	{
		IntIntPair max = new IntIntPair(-1, -1);
		IntHashSet set, remove;
		boolean removed;
		int[] keys;
		
		do
		{
			max.set(-1, -1);
			keys = map.keys().toArray();
			Arrays.sort(keys);
			
			for (int key : keys)
			{
				set = map.get(key);
				
				if (set.size() > max.i2)
					max.set(key, set.size());
			}
			
			removed = false;
			
			if (max.i2 > 0)
			{
				remove = new IntHashSet();
				
				for (IntCursor cur : map.get(max.i1))
				{
					if (map.containsKey(cur.value))
					{
						set = map.get(cur.value);
						
						if (set.contains(max.i1))
						{
							removed = true;
							set.remove(max.i1);
							if (set.isEmpty())	remove.add(cur.value);
						}
					}
				}
				
				for (IntCursor cur : remove)
					map.remove(cur.value);
			}
		}
		while (removed);
						
		return new IntHashSet(map.keys());
	}
}