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
package com.googlecode.clearnlp.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jregex.MatchResult;
import jregex.Substitution;
import jregex.TextBuffer;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDeque;
import com.googlecode.clearnlp.component.dep.EnglishDEPParser;
import com.googlecode.clearnlp.constant.universal.STPunct;
import com.googlecode.clearnlp.constituent.CTLibEn;
import com.googlecode.clearnlp.constituent.CTNode;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.conversion.AbstractC2DConverter;
import com.googlecode.clearnlp.conversion.KaistC2DConverter;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.generation.LGVerbEn;
import com.googlecode.clearnlp.headrule.HeadRuleMap;
import com.googlecode.clearnlp.io.FileExtFilter;
import com.googlecode.clearnlp.morphology.MPLib;
import com.googlecode.clearnlp.morphology.MPLibEn;
import com.googlecode.clearnlp.propbank.PBArg;
import com.googlecode.clearnlp.propbank.PBInstance;
import com.googlecode.clearnlp.propbank.PBReader;
import com.googlecode.clearnlp.propbank.frameset.MultiFrames;
import com.googlecode.clearnlp.propbank.frameset.PBRoleset;
import com.googlecode.clearnlp.propbank.frameset.PBType;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.reader.TOKReader;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTRegex;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.map.Prob2DMap;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.StringDoublePair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


public class Tmp
{
//	static Logger log = Logger.getLogger(Tmp.class.getName());

	public Tmp(String[] args) throws Exception
	{
	}
	
	void checkMisalignedArgs(String[] args)
	{
		MultiFrames frames = new MultiFrames(args[0]);
		PBReader reader = new PBReader(UTInput.createBufferedFileReader(args[1]));
		int invalid = 0, total = 0;
		PBInstance instance;
		PBRoleset  roleset;
		String type;
		
		while ((instance = reader.nextInstance()) != null)
		{
			type = instance.type;
			
			if (type.endsWith(PBType.VERB.getValue()))
			{
				roleset = frames.getRoleset(PBType.VERB, type.substring(0,type.length()-2), instance.roleset);
				total++;
				
				if (roleset != null)
				{
					for (PBArg arg : instance.getArgs())
					{
						if (!roleset.isValidArgument(arg.label))
						{
							System.out.println(instance.toString());
							invalid++;
							break;
						}
					}
				}
			}
		}
		
		System.out.printf("%5.2f (%d/%d)\n", (double)invalid/total, invalid, total);
	}
	
	void toQuestion(String[] args)
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 7);
		reader.open(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[1]);
		Pattern p = UTRegex.getORPatternExact(DEPLibEn.DEP_COMPLM, DEPLibEn.DEP_MARK);
		DEPNode root, head;
		DEPTree tree;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			root = tree.getFirstRoot();
			head = root.getDependents().get(0).getNode();
			
			tree = new DEPTree();
			head.removeDependentsByLabels(p);
			
			for (DEPNode node : head.getSubNodeSortedList())
			{
				tree.add(node);
				if (node.isDependentOf(root))
					node.setHead(tree.get(0), DEPLibEn.DEP_ROOT);
			}
			
			tree.resetIDs();
			fout.println(tree.toStringSRL()+"\n");
		}
		
		fout.close();
		reader.close();	
	}
	
	void getVerbForms(String[] args)
	{
		DEPReader fin = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		fin.open(UTInput.createBufferedFileReader(args[0]));
		Set<String> keys = new TreeSet<String>();
		Prob2DMap mVBD = new Prob2DMap();
		Prob2DMap mVBN = new Prob2DMap();
		String form, base, past, part;
		int i, size, cutoff = 1;
		DEPTree tree;
		DEPNode node;
		
		while ((tree = fin.next()) != null)
		{
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node = tree.get(i);
				base = node.lemma;
				form = node.form.toLowerCase();
				past = LGVerbEn.getPastRegularForm(base);
				
				if (node.isPos(CTLibEn.POS_VBD))
				{
					if (!form.equals(past))
						mVBD.add(base, form);
				}
				else if (node.isPos(CTLibEn.POS_VBN))
				{
					if (!form.equals(past))
						mVBN.add(base, form);
				}
			}
		}
		
		keys.addAll(mVBD.keySet());
		keys.addAll(mVBN.keySet());
		
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[1]);
		
		for (String key : keys)
		{
			past = (mVBD.getTotal1D(key) > cutoff) ? mVBD.getBestProb1D(key).s : STPunct.UNDERSCORE;
			part = (mVBN.getTotal1D(key) > cutoff) ? mVBN.getBestProb1D(key).s : STPunct.UNDERSCORE;
			
			if (!past.equals(STPunct.UNDERSCORE) || !part.equals(STPunct.UNDERSCORE))
				fout.println(key+"\t"+past+"\t"+part);
		}
		
		fout.close();
	}
	
	void testParseLabel(String[] args) throws Exception
	{
		SRLReader fin = new SRLReader(0, 1, 2, 3, 4, 5, 6, 8);
		fin.open(UTInput.createBufferedFileReader(args[0]));
		PrintStream fold = UTOutput.createPrintBufferedFileStream(args[0]+".o");
		PrintStream fnew = UTOutput.createPrintBufferedFileStream(args[0]+".n");
	//	ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(args[1])));
	//	PBFrameset p = (PBFrameset)oin.readObject();
		DEPTree tree;
		
		while ((tree = fin.next()) != null)
		{
			fold.println(tree.toStringSRL()+"\n");
			DEPLibEn.postLabel(tree);
			fnew.println(tree.toStringSRL()+"\n");
		}
	}
	
	void testException(int i) throws Exception
	{
		if (i == 0)
			throw new Exception("NO");
		
		System.out.println("BINGO");
	}
	
	void printProb1DMap(Prob1DMap map)
	{
		for (StringIntPair p : map.toSortedList())
			System.out.println(p.s+"\t"+p.i);
	}
	
	void printProb2DMap(Prob2DMap map, double threshold)
	{
		List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);
		StringDoublePair[] ps;
		StringBuilder build;
		double sum;
		int count;
		
		for (String key : keys)
		{
			count = map.getTotal1D(key);
			if (count < 2)	continue;
			
			ps = map.getProb1D(key);
			Arrays.sort(ps);

			build = new StringBuilder();
			build.append(key);
			build.append("\t");
			build.append(count);
			sum = 0;
				
			for (StringDoublePair p : ps)
			{
				build.append("\t");
				build.append(p.s);
				build.append("\t");
				build.append(100d * p.d);

				sum += p.d;
				if (sum >= threshold)	break;
			}
			
			System.out.println(build.toString());
		}
	}
	
	void checkPosDeprel(DEPTree tree, Prob2DMap mc, Prob2DMap mh)
	{
		int i, size = tree.size();
		DEPNode node, head;
		String deprel;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			head = node.getHead();
			deprel = node.getLabel();

			mc.add(deprel, MPLibEn.toCPOSTag(node.pos));
			mh.add(deprel, MPLibEn.toCPOSTag(head.pos));
		}
	}
	
	void classifySentenceType(DEPTree tree, IntIntPair count)
	{
		int i, size = tree.size();
		DEPNode node;
		String  feat;
		
		tree.setDependents();
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			feat = node.getFeat(DEPLib.FEAT_SNT);
			
			if (node.pos.startsWith("VB"))
			{
				if (isImperative(node))
				{
					count.i1++;
					
					if (feat == null || !feat.equals("IMP"))
					{
						if (node.isLemma("be"))
							System.out.println(node.id+" "+tree.toStringDEP()+"\n");
						count.i2++;
					}
				}
			}
		}
	}
	
	boolean isImperative(DEPNode verb)
	{
		Pattern P_SBJ = Pattern.compile("^[nc]subj.*");
		Pattern P_AUX = Pattern.compile("^aux.*");
		
		if (verb.isLemma("let") || verb.isLemma("thank") || verb.isLemma("welcome"))
			return false;
		
		if (!verb.isPos(CTLibEn.POS_VB) && !verb.isPos(CTLibEn.POS_VBP))
			return false;
		
		if (verb.isLabel(DEPLibEn.DEP_AUX) || verb.isLabel(DEPLibEn.DEP_AUXPASS) || verb.isLabel(DEPLibEn.DEP_XCOMP) || verb.isLabel(DEPLibEn.DEP_PARTMOD) || verb.isLabel(DEPLibEn.DEP_RCMOD) || verb.isLabel(DEPLibEn.DEP_CONJ) || verb.isLabel(DEPLibEn.DEP_HMOD))
			return false;

		List<DEPArc> deps = verb.getDependents();
		int i, size = deps.size();
		DEPNode node;
		DEPArc  dep;
		
		for (i=0; i<size; i++)
		{
			dep  = deps.get(i);
			node = dep.getNode();
			
			if (node.id < verb.id)
			{
				if (dep.isLabel(DEPLibEn.DEP_COMPLM) || dep.isLabel(DEPLibEn.DEP_MARK))
					return false;
				
				if (dep.isLabel(P_AUX) && !node.isLemma("do"))
					return false;
				
				if (node.isPos(CTLibEn.POS_TO) || node.isPos(CTLibEn.POS_MD) || node.pos.startsWith("W"))
					return false;	
			}
			
			if (dep.isLabel(P_SBJ) || dep.isLabel(DEPLibEn.DEP_EXPL))
				return false;
		}
		
		return true;
	}
	
	void classifySentenceTypeINT(DEPTree tree, DEPNode verb, IntIntPair count)
	{
		Pattern pPeriod = Pattern.compile("^[\\.\\!]+$");
		Pattern pSbj = Pattern.compile("^[nc]subj.*");
		Pattern pAux = Pattern.compile("^aux.*");
		List<DEPArc> deps = verb.getDependents();
		int i, size = deps.size();
		boolean hasAux = false;
		DEPArc  curr, prev;
		String  label;
		DEPNode node;
		
		for (i=size-1; i>=0; i--)
		{
			curr  = deps.get(i);
			node  = curr.getNode();
			label = curr.getLabel();
			
			if (curr.isLabel(DEPLibEn.DEP_PUNCT))
			{
				if (pPeriod.matcher(node.lemma).find())
					return;
			}
		}
		
		for (i=0; i<size; i++)
		{
			curr  = deps.get(i);
			node  = curr.getNode();
			label = curr.getLabel();
			
			if (node.id > verb.id)
				break;
			
			if (pAux.matcher(label).find())
			{
				if (i > 0)
				{
					prev = deps.get(i-1);
					
					if (prev.isLabel(DEPLibEn.DEP_PRECONJ))
						return;
				}
				
				hasAux = true;
			}
			else if (pSbj.matcher(label).find())
			{
				if (hasAux)
				{
					String snt = verb.getFeat(DEPLib.FEAT_SNT);
					count.i1++;
					
					if (snt == null || !snt.equals("INT"))
					{
						count.i2++;
						System.out.println(verb.id+" "+tree.toStringDEP()+"\n");
					}
				}
			}
		}
	}
	
	void checkConstituentTags(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		Set<String> phrases = new TreeSet<String>();
		Set<String> tokens  = new TreeSet<String>();
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
			checkConstituents(tree.getRoot(), phrases, tokens);
		
		reader.close();
		
		for (String s : phrases)	System.out.println(s);
		System.out.println();
		for (String s : tokens)		System.out.println(s);
	}
	
	void checkConstituents(CTNode node, Set<String> phrases, Set<String> tokens)
	{
		if (node.isPhrase())
		{
			phrases.add(node.pTag);
			
			for (CTNode child : node.getChildren())
				checkConstituents(child, phrases, tokens);
		}
		else
		{
			tokens.add(node.pTag);
		}
	}
	
	void parseBeam(String[] args)
	{
		EnglishDEPParser parser = new EnglishDEPParser(UTInput.createZipFileInputStream(args[0]));
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, -1, -1);
		int[] beams = {1,2,4,8,16,32,64};
		PrintStream fout;
		DEPTree tree;
		
		for (int beam : beams)
		{
			fout = UTOutput.createPrintBufferedFileStream(args[1]+"."+beam);
			reader.open(UTInput.createBufferedFileReader(args[1]));
			parser.setBeams(beam);

			while ((tree = reader.next()) != null)
			{
				parser.process(tree);
				fout.println(tree.toStringDEP()+"\n");
			}
			
			fout.close();
			reader.close();
		}
	}
	
/*	void wordnet()
	{
		System.setProperty("wordnet.database.dir", "/Users/jdchoi/Downloads/WordNet-3.0/dict/");

		WordNetDatabase database = WordNetDatabase.getFileInstance(); 
		VerbSynset verbSynset; 
				
		Synset[] synsets = database.getSynsets("enable", SynsetType.VERB, false);
		
		for (int i = 0; i < synsets.length; i++)
		{ 
		    verbSynset = (VerbSynset)(synsets[i]); 
		    System.out.println((i+1)+": "+Arrays.toString(verbSynset.getWordForms()));
		}
	}*/
	
	void cleanSejong(String[] args)
	{
		String[] ptbFiles = UTFile.getSortedFileList(args[0], "ptb");
		String[] rawFiles = UTFile.getSortedFileList(args[1], "raw");
		
		CTReader  pin = new CTReader();
		TOKReader tin = new TOKReader(0);
		
		int i, size = ptbFiles.length;
		List<String> tokens;
		CTTree tree;
		
		for (i=0; i<size; i++)
		{
			pin.open(UTInput.createBufferedFileReader(ptbFiles[i]));
			tin.open(UTInput.createBufferedFileReader(rawFiles[i]));
			System.out.println(rawFiles[i]);
			
			while ((tree = pin.nextTree()) != null)
			{
				tokens = tin.next();
				
				if (tree.getTokens().size() != tokens.size())
					System.out.println(UTArray.join(tokens, " "));
			}
		}
	}
	
	void printTreebank(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[1]);
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
			fout.println(tree.toString()+"\n");
		
		reader.close();
		fout.close();
	}
	
	void convertKaist(String[] args)
	{
		AbstractC2DConverter converter = new KaistC2DConverter(new HeadRuleMap(UTInput.createBufferedFileReader(args[0])));
		String[] inputFiles = UTFile.getSortedFileList(args[1], "ptb");
		String outputFile;
		PrintStream fout;
		CTReader reader;
		DEPTree dTree;
		CTTree cTree;
		
		for (String inputFile : inputFiles)
		{
			outputFile = UTFile.replaceExtension(inputFile, "dep");
			reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
			fout = UTOutput.createPrintBufferedFileStream(outputFile);
			System.out.println(outputFile);
			
			while ((cTree = reader.nextTree()) != null)
			{
				dTree = converter.toDEPTree(cTree);
				fout.println(dTree.toStringDEP()+"\n");
			}
			
			reader.close();
			fout.close();
		}
	}
	
	void extractDEP(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		Set<String> set = new HashSet<String>();
		Pattern delim = Pattern.compile("\\+");
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
		{
			extractDEPAux(tree.getRoot(), set, delim);
		}

		List<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		System.out.println(list);
	}
	
	void extractDEPAux(CTNode node, Set<String> set, Pattern delim)
	{
		Set<String> s = new HashSet<String>();
		boolean skip = false;
		char c;
		
		for (CTNode child : node.getChildren())
		{
			if (node.isPTag("NP"))
			{
				for (String pos : delim.split(child.pTag))
				{
					c = pos.charAt(0);
					
					if (c == 'n' || pos.equals("etn") || c == 'f')// || c == 'p')
						skip = true;
					else if (c != 's')
					{
						s.add(pos);
					}
				}
			}
			
			extractDEPAux(child, set, delim);
		}
		
		if (!skip)
		{
			set.addAll(s);
			if (s.contains("paa"))	System.out.println(node.toString());
		}
	}
	
	void extractPos(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		Set<String> set = new HashSet<String>();
		Pattern delim = Pattern.compile("\\+");
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
		{
			for (CTNode node : tree.getTokens())
				for (String pos : delim.split(node.pTag))
					set.add(pos);
		}
		
		List<String> l = new ArrayList<String>(set);
		Collections.sort(l);
		
		for (String pos : l)
			System.out.println(pos);
	}
	
	void countLR(String inputFile)
	{
	//	DEPReader reader = new DEPReader(0, 1, 2, 3, 5, 6, 7);
		DEPReader reader = new DEPReader(0, 1, 2, 4, 6, 8, 10);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		int i, size, left = 0, right = 0, l, r, prevId, depId;
		DEPTree tree;
		DEPNode node;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node = tree.get(i);
				prevId = -1;
				l = r = 0;
				
				for (DEPArc arc : node.getDependents())
				{
					depId = arc.getNode().id;
					
					if (depId - prevId == 1)
					{
						if (depId < node.id)	l++;
						else 					r++;
					}
					
					prevId = depId;
				}
				
				if      (l > 1)	left++;
				else if (r > 1)	right++;
			}
		}
		
		reader.close();
		System.out.printf("Left: %d, Right: %d\n", left, right);
	}
	
	void measureTime()
	{
		int i, j, len = 10, size = 1000000;
		IntArrayList list;
		IntDeque deque;
		long st, et;
		
		st = System.currentTimeMillis();
		
		for (i=0; i<size; i++)
		{
			list = new IntArrayList();
			
			for (j=0; j<len; j++)
				list.add(j);
			
			list.remove(list.size()-1);
		}
		
		et = System.currentTimeMillis();
		System.out.println(et-st);
		
		st = System.currentTimeMillis();
		
		for (i=0; i<size; i++)
		{
			deque = new IntArrayDeque();
			
			for (j=0; j<len; j++)
				deque.addLast(j);
			
			deque.removeLast();
		}
		
		et = System.currentTimeMillis();
		System.out.println(et-st);
	}
	
	void evalSubPOS(String inputFile) throws Exception
	{
		BufferedReader reader = UTInput.createBufferedFileReader(inputFile);
		Pattern delim = Pattern.compile("\t");
		int correct = 0, total = 0;
		DEPFeat p, g;
		String[] ls;
		String line;
		
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			
			if (!line.isEmpty())
			{
				ls = delim.split(line);
				g = new DEPFeat(ls[6]);
				p = new DEPFeat(ls[7]);
				
				if (g.get("SubPOS").equals(p.get("SubPOS")))
					correct++;
						
				total++;
			}
		}
		
		System.out.printf("%5.2f (%d/%d)\n", 100d*correct/total, correct, total);
	}
	
	void projectivize(String inputFile, String outputFile)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 4, 6, 8, 10);
		DEPTree tree;
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fold = UTOutput.createPrintBufferedFileStream(outputFile+".old");
		PrintStream fnew = UTOutput.createPrintBufferedFileStream(outputFile+".new");
		int i;
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			fold.println(tree.toStringCoNLL()+"\n");
			tree.projectivize();
			fnew.println(tree.toStringCoNLL()+"\n");
			
			if (i%1000 == 0)	System.out.print(".");
		}	System.out.println();
		
		reader.close();
		fold.close();
		fnew.close();
	}
	
	void wc(String inputFile)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
		CTTree tree;
		int sc, wc;
		
		for (sc=0,wc=0; (tree = reader.nextTree()) != null; sc++)
			wc += tree.getTokens().size();
		
		System.out.println(sc+" "+wc);
	}
	
	void stripTrees(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[0]+".strip");
		Set<String> set = new HashSet<String>();
		String forms;
		CTTree tree;
		int i;
		
		for (i=0; (tree = reader.nextTree()) != null; i++)
		{
			forms = tree.toForms();
			
			if (!set.contains(forms))
			{
				set.add(forms);
				fout.println(tree+"\n");
			}
		}
		
		fout.close();
		System.out.println(i+" -> "+set.size());
	}
	
	void splitTrees(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream[] fout = new PrintStream[4];
		CTTree tree;
		int i, j;
		
		fout[0] = UTOutput.createPrintBufferedFileStream(args[0]+".trn.parse");
		fout[1] = UTOutput.createPrintBufferedFileStream(args[0]+".trn.raw");
		fout[2] = UTOutput.createPrintBufferedFileStream(args[0]+".tst.parse");
		fout[3] = UTOutput.createPrintBufferedFileStream(args[0]+".tst.raw");
		
		for (i=0; (tree = reader.nextTree()) != null; i++)
		{
			j = (i%6 == 0) ? 2 : 0;
			
			fout[j]  .println(tree.toString()+"\n");
			fout[j+1].println(tree.toForms());
		}
		
		for (PrintStream f : fout)	f.close();
	}
	
	void printTreesForCKY(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[1]);
		CTTree tree;
		CTNode root;
		int count;
		
		while ((tree = reader.nextTree()) != null)
		{
			root = tree.getRoot();
			
			if (root.getChildrenSize() == 1)
			{
				count = stripPunct(tree);
				
				if (root.getChildrenSize() > 0 && tree.getTokens().size()-count >= 4 && !containsEmptyCategories(tree) && isCKYTree(root.getChild(0)))
					fout.println(tree+"\n");
			}
		}
		
		reader.close();
		fout.close();
	}
	
	boolean containsEmptyCategories(CTTree tree)
	{
		for (CTNode node : tree.getTerminals())
		{
			if (node.isEmptyCategory())
				return true;
		}
		
		return false;
	}
	
	int stripPunct(CTTree tree)
	{
		int count = 0;
		
		for (CTNode node : tree.getTokens())
		{
			if (MPLib.containsOnlyPunctuation(node.form))
			{
				node.getParent().removeChild(node);
				count++;
			}
		}
		
		return count;
	}
	
	boolean isCKYTree(CTNode node)
	{
		if (!node.isPhrase())
			return true;
		
		int size = node.getChildrenSize();
		
	/*	if (size == 1)
		{
			if (!node.getChild(0).isPhrase())
				return true;
		}*/
		
		if (size != 2)
			return false;
		
		for (CTNode child : node.getChildren())
		{
			if (!isCKYTree(child))
				return false;
		}
		
		return true;
	}
	
	void traverse(String inputFile)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
			traverseAux(tree.getRoot());
	}
	
	void traverseAux(CTNode node)
	{
		if (node.isPTag("SBAR") && node.containsTags("+IN|TO") && node.containsTags("DT"))
			System.out.println(node);
		
		for (CTNode child : node.getChildren())
			traverseAux(child);
	}
	
	void print(String outputFile, Prob1DMap map)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		
		for (StringIntPair p : map.toSortedList())
			fout.printf("%s\t%d\n", p.s, p.i);
				
		fout.close();
	}
	
	void mapPropBankToDependency(String inputFile, String outputFile)
	{
		final String NONE = "NONE"; 

		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 8);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		Prob2DMap map = new Prob2DMap();
		DEPNode node, head;
		String deprel, label, ftags;
		DEPTree tree;
		int i, size;
		
		while ((tree = reader.next()) != null)
		{
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node   = tree.get(i);
				head   = node.getHead();
				deprel = node.getLabel();
				if ((ftags = node.getFeat(DEPLib.FEAT_SEM)) != null)
					deprel = ftags;
				
				for (DEPArc arc : node.getSHeads())
				{
					label = arc.getLabel();
				//	if (label.startsWith("R-AM"))
				//		label = label.substring(2);
					
					if (arc.getNode() == head)
						map.add(label, deprel);
					else
						map.add(label, NONE);
				}
			}
		}
		
		List<String> keys = new ArrayList<String>(map.keySet());
		DecimalFormat format = new DecimalFormat("##.##"); 
		Collections.sort(keys);
		StringDoublePair[] ps;
		StringBuilder build;
		double none;
		String tmp;
		
		for (String key : keys)
		{
			build = new StringBuilder();
			ps = map.getProb1D(key);
			Arrays.sort(ps);
			none = 0;
			
			for (StringDoublePair p : ps)
			{
				if (p.s.equals(NONE))
					none = p.d;
				else if (p.d >= 0.2)
				{
					build.append("\\d"+p.s.toUpperCase());
					build.append(":");
					build.append(format.format(100d*p.d));
					build.append(", ");
				}
			}
			
			tmp = build.length() == 0 ? "" : build.substring(0, build.length()-2);
			fout.printf("%s\t%s\t%f\t%d\t%d\n", key, tmp, 100d*none, map.get(key).get(NONE), map.getTotal1D(key));
		}
		
		fout.close();
	}
	
	public List<String[]> getFilenames(String inputPath, String inputExt, String outputExt)
	{
		List<String[]> filenames = new ArrayList<String[]>();
		File f = new File(inputPath);
		String outputFile;
		
		if (f.isDirectory())
		{
			for (String inputFile : f.list(new FileExtFilter(inputExt)))
			{
				inputFile  = inputPath + File.separator + inputFile;
				outputFile = inputFile + "." + outputExt;
				filenames.add(new String[]{inputFile, outputFile});
			}
		}
		else
			filenames.add(new String[]{inputPath, inputPath+"."+outputExt});
		
		return filenames;
	}
	
	public void converNonASC(String[] args) throws Exception
	{
		Pattern asc1 = Pattern.compile("[^\\p{ASCII}]");
		Pattern asc2 = Pattern.compile("\\p{ASCII}");
		Pattern tab  = Pattern.compile("\t");
		BufferedReader fin;
		PrintStream fout;
		String[] tmp;
		String line, str;
		int i;
		
		for (String[] io : getFilenames(args[0], args[1], args[2]))
		{
			System.out.println(io[1]);
			fin  = UTInput.createBufferedFileReader(io[0]);
			fout = UTOutput.createPrintBufferedFileStream(io[1]);
			
			while ((line = fin.readLine()) != null)
			{
				line = line.trim();
				
				if (line.isEmpty())
				{
					fout.println();
					continue;
				}
				
				tmp = tab.split(line);
				
				for (i=0; i<tmp.length; i++)
				{
					str = tmp[i];
					
					if (asc2.matcher(str).find())
						tmp[i] = asc1.matcher(str).replaceAll("");
					else
						tmp[i] = "^ASCII";
				}
				
				fout.println(UTArray.join(tmp, "\t"));
			}
			
			fout.close();
		}
	}
	
	public void countSemanticDependents(String[] args)
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 8);
		reader.open(UTInput.createBufferedFileReader(args[0]));
		DEPTree tree;
		DEPNode node, dHead, sHead;
		int i;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			
			for (i=1; i<tree.size(); i++)
			{
				node  = tree.get(i);
				dHead = node.getHead();
				
				for (DEPArc sArc : node.getSHeads())
				{
					sHead = sArc.getNode();
				//	sHead = sArc.getNode().getHead();
					
					if (sHead != dHead && sHead != dHead.getHead() && node.isDescendentOf(sHead))
					{
						System.out.println(node.id+" "+sArc.getNode().id+" "+tree.toStringSRL());
						try {System.in.read();} catch (IOException e) {e.printStackTrace();}
					}
				}
			}
		}
	}
	
	static class SubstitutionOne implements Substitution
	{
		@Override
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			dest.append(match.group(0).toUpperCase());
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			new Tmp(args);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
