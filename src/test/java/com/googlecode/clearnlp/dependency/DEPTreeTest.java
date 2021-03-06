/**
 * Copyright (c) 2009/09-2012/08, Regents of the University of Colorado
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Copyright 2012/09-2013/04, University of Massachusetts Amherst
 * Copyright 2013/05-Present, IPSoft Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.googlecode.clearnlp.dependency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.googlecode.clearnlp.dependency.factory.DefaultDEPNodeDatumFactory;
import com.googlecode.clearnlp.dependency.factory.DefaultDEPTreeDatumFactory;
import com.googlecode.clearnlp.dependency.factory.IDEPTreeDatum;
import com.googlecode.clearnlp.dependency.srl.SRLArc;


public class DEPTreeTest
{
	@Test
	public void testCloneSRL()
	{
		DEPTree tree = new DEPTree();
		
		assertEquals(DEPLib.ROOT_ID, tree.get(0).id);
		assertEquals(null, tree.get(1));
		
		DEPNode sbj = new DEPNode(1, "John", "john", "NNP", new DEPFeat());
		DEPNode vbd = new DEPNode(2, "bought", "buy", "VBD", new DEPFeat());
		DEPNode nns = new DEPNode(3, "cars", "car", "NNS", new DEPFeat());
		
		vbd.addFeat(DEPLibEn.FEAT_PB, "buy.01");
		
		sbj.setHead(vbd, "NSBJ");
		vbd.setHead(tree.get(0), "ROOT");
		nns.setHead(vbd, "DOBJ");
		
		sbj.initSHeads();
		vbd.initSHeads();
		nns.initSHeads();
		
		sbj.addSHead(vbd, "A0");
		nns.addSHead(new SRLArc(vbd, "A1", "PPT"));
		
		tree.add(sbj);
		tree.add(vbd);
		tree.add(nns);
		
		testClone(tree);
		testGetDEPTreeDatum(tree);
	}
	
	public void testClone(DEPTree tree)
	{
		String s1 = tree.toStringSRL()+"\n";
		DEPTree copy = tree.clone();
		
		copy.get(1).setLabel("nsbuj");
		copy.get(2).addFeat(DEPLibEn.FEAT_PB, "01");
		copy.get(3).setHead(copy.get(0));
		
		String s2 = tree.toStringSRL()+"\n";
		assertEquals(s1, s2);
	}
	
	public void testGetDEPTreeDatum(DEPTree tree)
	{
		IDEPTreeDatum datum = tree.getDEPTreeDatum();
		DEPTree newTree = DEPTree.buildFrom(datum);
		assertEquals(tree.toStringSRL(), newTree.toStringSRL());
		
		datum = tree.getDEPTreeDatum(new DefaultDEPTreeDatumFactory(), new DefaultDEPNodeDatumFactory());
		newTree = DEPTree.buildFrom(datum);
		assertEquals(tree.toStringSRL(), newTree.toStringSRL());
	}
}
