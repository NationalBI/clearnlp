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
package com.googlecode.clearnlp.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.carrotsearch.hppc.IntHashSet;
import com.googlecode.clearnlp.util.UTHppc;


/** @author Jinho D. Choi ({@code choijd@colorado.edu}) */
public class UTHppcTest
{
	@Test
	public void test()
	{
		IntHashSet set1 = new IntHashSet();
		set1.add(3);	set1.add(1);	set1.add(2);
		
		assertEquals(3, UTHppc.max(set1));
		assertEquals(1, UTHppc.min(set1));
		
		IntHashSet set2 = new IntHashSet();
		assertEquals(true, UTHppc.isSubset(set1, set2));
		
		set2.add(1);	set2.add(2);
		assertEquals(true, UTHppc.isSubset(set1, set2));
		
		set2.add(3);		
		assertEquals(true, UTHppc.isSubset(set1, set2));
		
		set2.add(4);
		assertEquals(false, UTHppc.isSubset(set1, set2));
	}
}
