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
package com.googlecode.clearnlp.component.srl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.classification.feature.FtrToken;
import com.googlecode.clearnlp.classification.feature.JointFtrXml;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.constant.universal.STConstant;
import com.googlecode.clearnlp.constant.universal.STPunct;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLArc;
import com.googlecode.clearnlp.dependency.srl.SRLLib;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.propbank.PBLib;
import com.googlecode.clearnlp.propbank.frameset.AbstractFrames;
import com.googlecode.clearnlp.propbank.frameset.PBRoleset;
import com.googlecode.clearnlp.propbank.frameset.PBType;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.pair.ObjectDoublePair;
import com.googlecode.clearnlp.util.pair.StringIntPair;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractSRLabeler extends AbstractStatisticalComponent
{
	private final String ENTRY_CONFIGURATION = NLPLib.MODE_SRL + NLPLib.ENTRY_CONFIGURATION;
	private final String ENTRY_FEATURE		 = NLPLib.MODE_SRL + NLPLib.ENTRY_FEATURE;
	private final String ENTRY_LEXICA		 = NLPLib.MODE_SRL + NLPLib.ENTRY_LEXICA;
	private final String ENTRY_MODEL		 = NLPLib.MODE_SRL + NLPLib.ENTRY_MODEL;
	private final String ENTRY_WEIGHTS	     = NLPLib.MODE_SRL + NLPLib.ENTRY_WEIGHTS;
	private final String ENTRY_FRAMES		 = NLPLib.MODE_SRL + "_FRAMES";
	
	protected final int LEXICA_PATH_UP	 = 0;
	protected final int LEXICA_PATH_DOWN = 1;
	protected final int LEXICA_FRAMES	 = 2;
	
	protected final int PATH_ALL		 = 0;
	protected final int PATH_UP			 = 1;
	protected final int PATH_DOWN		 = 2;
	protected final int SUBCAT_ALL		 = 0;
	protected final int SUBCAT_LEFT		 = 1;
	protected final int SUBCAT_RIGHT	 = 2;
	
	protected final String LB_NO_ARG = "N";
	
	protected DEPNode			d_lca;
	protected IntOpenHashSet    s_skip;
	protected List<String>		l_argns;
	protected StringIntPair[][]	g_heads;
	protected int				i_pred, i_arg;
	protected Map<String,ObjectDoublePair<DEPNode>> m_argns;
	
	protected Prob1DMap			m_down, m_up;	// only for collecting
	protected Set<String>		s_down, s_up;
	protected PBRoleset			p_roleset;
	protected AbstractFrames	m_frames;
	
//	====================================== CONSTRUCTORS ======================================
	
	/** Constructs a semantic role labeler for collecting lexica. */
	public AbstractSRLabeler(JointFtrXml[] xmls, AbstractFrames frames)
	{
		super(xmls);
		m_down   = new Prob1DMap();
		m_up     = new Prob1DMap();
		m_frames = frames;
	}
	
	/** Constructs a semantic role labeler for training. */
	public AbstractSRLabeler(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica)
	{
		super(xmls, spaces, lexica);
	}
	
	/** Constructs a semantic role labeler for developing. */
	public AbstractSRLabeler(JointFtrXml[] xmls, StringModel[] models, Object[] lexica)
	{
		super(xmls, models, lexica);
	}
	
	/** Constructs a semantic role labeler for decoding. */
	public AbstractSRLabeler(ZipInputStream in)
	{
		super(in);
	}
	
	/** Constructs a semantic role labeler for bootstrapping. */
	public AbstractSRLabeler(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica)
	{
		super(xmls, spaces, models, lexica);
	}
	
	@Override @SuppressWarnings("unchecked")
	protected void initLexia(Object[] lexica)
	{
		s_down   = (Set<String>)   lexica[LEXICA_PATH_DOWN];
		s_up     = (Set<String>)   lexica[LEXICA_PATH_UP];
		m_frames = (AbstractFrames)lexica[LEXICA_FRAMES];
	}
	
	abstract protected String getHardLabel(DEPNode node);
	abstract protected PBType getPBType(DEPNode pred);
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	@Override
	public void loadModels(ZipInputStream zin)
	{
		f_xmls   = new JointFtrXml[1];
		s_models = null;
		ZipEntry zEntry;
		String   entry;
				
		try
		{
			while ((zEntry = zin.getNextEntry()) != null)
			{
				entry = zEntry.getName();
				
				if      (entry.equals(ENTRY_CONFIGURATION))
					loadDefaultConfiguration(zin);
				else if (entry.startsWith(ENTRY_FEATURE))
					loadFeatureTemplates(zin, Integer.parseInt(entry.substring(ENTRY_FEATURE.length())));
				else if (entry.startsWith(ENTRY_FRAMES))
					loadFrames(zin);
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
				else if (entry.startsWith(ENTRY_MODEL))
					loadStatisticalModels(zin, Integer.parseInt(entry.substring(ENTRY_MODEL.length())));
				else if (entry.startsWith(ENTRY_WEIGHTS))
					loadWeightVector(zin, Integer.parseInt(entry.substring(ENTRY_WEIGHTS.length())));
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void loadFrames(ZipInputStream zin) throws Exception
	{
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(zin));
		m_frames = (AbstractFrames)oin.readObject();
	}
	
	private void loadLexica(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		LOG.info("Loading lexica.\n");

		s_down = UTInput.getStringSet(fin);
		s_up   = UTInput.getStringSet(fin);
	}

	@Override
	public void saveModels(ZipOutputStream zout)
	{
		try
		{
			saveDefaultConfiguration(zout, ENTRY_CONFIGURATION);
			saveFeatureTemplates    (zout, ENTRY_FEATURE);
			saveFrames              (zout);
			saveLexica              (zout);
			saveStatisticalModels   (zout, ENTRY_MODEL);
			saveWeightVector        (zout, ENTRY_WEIGHTS);
			zout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void saveFrames(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new ZipEntry(ENTRY_FRAMES));
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(zout));
		oout.writeObject(m_frames);
		oout.flush();
		zout.closeEntry();			
	}
	
	private void saveLexica(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new ZipEntry(ENTRY_LEXICA));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		LOG.info("Saving lexica.\n");
		
		UTOutput.printSet(fout, s_down);	fout.flush();
		UTOutput.printSet(fout, s_up);		fout.flush();
		
		zout.closeEntry();
	}
	
//	====================================== GETTERS AND SETTERS ======================================
	
	@Override
	public Object[] getLexica()
	{
		Object[] lexica = new Object[3];
		
		lexica[LEXICA_PATH_DOWN] = (i_flag == FLAG_LEXICA) ? m_down.toSet(f_xmls[0].getPathDownCutoff()) : s_down; 
		lexica[LEXICA_PATH_UP]   = (i_flag == FLAG_LEXICA) ? m_up  .toSet(f_xmls[0].getPathUpCutoff())   : s_up;
		lexica[LEXICA_FRAMES]    = m_frames;
		
		return lexica;
	}
	
	@Override
	public void countAccuracy(int[] counts)
	{
		int i, pTotal = 0, rTotal = 0, correct = 0;
		StringIntPair[] gHeads;
		List<SRLArc>    sHeads;
		
		for (i=1; i<t_size; i++)
		{
			sHeads = d_tree.get(i).getSHeads();
			gHeads = g_heads[i];
			
			pTotal += sHeads.size();
			rTotal += gHeads.length;
			
			for (StringIntPair p : gHeads)
				for (DEPArc arc : sHeads)
					if (arc.getNode().id == p.i && arc.isLabel(p.s))
						correct++;
		}
		
		counts[0] += correct;
		counts[1] += pTotal;
		counts[2] += rTotal;
	}
	
//	====================================== INITIALIZATION ======================================
	
	/** Called by {@link AbstractSRLabeler#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	d_tree  = tree;
	 	t_size  = tree.size();
		i_pred  = getNextPredId(0);
		s_skip  = new IntOpenHashSet();
		l_argns = new ArrayList<String>();
		m_argns = new HashMap<String,ObjectDoublePair<DEPNode>>();
		
		if (i_flag != FLAG_DECODE)
		{
			g_heads = tree.getSHeads();
			tree.clearSHeads();
		}
		else
			tree.initSHeads();

		initArcs();
	}
	
	/** @return the ID of the next predicate. */
	private int getNextPredId(int prevId)
	{
		DEPNode pred = d_tree.getNextPredicate(prevId);
		return (pred != null) ? pred.id : d_tree.size();
	}
	
	private PBRoleset getRoleset()
	{
		if (m_frames != null)
		{
			DEPNode pred = d_tree.get(i_pred);
			PBType  type = getPBType(pred);
			
			if (type != null)
				return m_frames.getRoleset(type, pred.lemma, pred.getFeat(DEPLibEn.FEAT_PB));
		}
		
		return null;
	}
	
	private void addLexica(DEPTree tree)
	{
		DEPNode pred = tree.getNextPredicate(0);
		DEPNode head;
		
		tree.setDependents();
		
		while (pred != null)
		{
			for (DEPArc arc : pred.getGrandDependents())
				collectDown(pred, arc.getNode());
		
			head = pred.getHead();
			if (head != null)	collectUp(pred, head.getHead());
			pred = tree.getNextPredicate(pred.id);
		}
	}
	
	private void collectDown(DEPNode pred, DEPNode arg)
	{
		if (arg.isArgumentOf(pred))
		{
			for (String path : getDUPathList(pred, arg.getHead()))
				m_down.add(path);
		}
		
		for (DEPArc arc : arg.getDependents())
			collectDown(pred, arc.getNode());
	}
	
	private void collectUp(DEPNode pred, DEPNode head)
	{
		if (head == null)	return;
		
		for (DEPArc arc : head.getDependents())
		{
			if (arc.getNode().isArgumentOf(pred))
			{
				for (String path : getDUPathList(head, pred))
					m_up.add(path);
				
				break;
			}
		}	
		
		collectUp(pred, head.getHead());
	}
	
	private String getDUPath(DEPNode top, DEPNode bottom)
	{
		return getPathAux(top, bottom, JointFtrXml.F_DEPREL, SRLLib.DELIM_PATH_DOWN, true);
	}
	
	private List<String> getDUPathList(DEPNode top, DEPNode bottom)
	{
		List<String> paths = new ArrayList<String>();
		
		while (bottom != top)
		{
			paths.add(getDUPath(top, bottom));
			bottom = bottom.getHead();
		}
		
		return paths;
	}
	
	public Set<String> getDownSet(int cutoff)
	{
		return m_down.toSet(cutoff);
	}
	
	public Set<String> getUpSet(int cutoff)
	{
		return m_up.toSet(cutoff);
	}

//	================================ PROCESS ================================
	
	@Override
	public void process(DEPTree tree)
	{
		if (i_flag == FLAG_LEXICA)
			addLexica(tree);
		else
		{
			init(tree);
			label();
		}
	}
	
	private void label()
	{
		DEPNode pred;
		
		while (i_pred < t_size)
		{
			pred = d_tree.get(i_pred);
			
			p_roleset = getRoleset(); 
			s_skip.clear();
			s_skip.add(i_pred);
			s_skip.add(DEPLib.ROOT_ID);
			l_argns.clear();
			m_argns.clear();
			
			d_lca = pred;

			do
			{
				labelAux(pred, d_lca);
				d_lca = d_lca.getHead();
			}
			while (d_lca != null);// && (pred.isDependentOf(d_lca) || s_up.contains(getDUPath(d_lca, pred))));
			
			i_pred = getNextPredId(i_pred);
		}
	}
	
	/** Called by {@link AbstractSRLabeler#label(DEPTree)}. */
	private void labelAux(DEPNode pred, DEPNode head)
	{
		if (!s_skip.contains(head.id))
		{
			i_arg = head.id;
			addArgument(getLabel(getDirIndex()));	
		}
		
		labelDown(pred, head.getDependents());
	}
	
	/** Called by {@link AbstractSRLabeler#labelAux(DEPNode, IntOpenHashSet)}. */
	private void labelDown(DEPNode pred, List<DEPArc> arcs)
	{
		DEPNode arg;
		
		for (DEPArc arc : arcs)
		{
			arg = arc.getNode();
			
			if (!s_skip.contains(arg.id))
			{
				i_arg = arg.id;
				addArgument(getLabel(getDirIndex()));
				
				if (i_pred == d_lca.id && s_down.contains(getDUPath(pred, arg)))
					labelDown(pred, arg.getDependents());
			}
		}
	}
	
	private int getDirIndex()
	{
		return (i_arg < i_pred) ? 0 : 1;
	}
	
	private StringPrediction getLabel(int idx)
	{
		StringFeatureVector vector = getFeatureVector(f_xmls[0]);
		StringPrediction p = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			p = new StringPrediction(getGoldLabel(), 1d);
			s_spaces[idx].addInstance(p.label, vector);
		}
		else if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
		{
			p = getAutoLabel(idx, vector);
		}
		else if (i_flag == FLAG_BOOTSTRAP)
		{
			p = getAutoLabel(idx, vector);
			s_spaces[idx].addInstance(getGoldLabel(), vector);
		}

		return p;
	}
	
	/** Called by {@link AbstractSRLabeler#getGoldLabel(byte)}. */
	private String getGoldLabel()
	{
		for (StringIntPair head : g_heads[i_arg])
		{
			if (head.i == i_pred)
				return head.s;
		}
		
		return LB_NO_ARG;
	}

	/** Called by {@link AbstractSRLabeler#getLabel(byte)}. */
	private StringPrediction getAutoLabel(int idx, StringFeatureVector vector)
	{
		StringPrediction p = getBestPrediction(s_models[idx], vector);
		
		if (i_flag == FLAG_DECODE && !p.label.equals(LB_NO_ARG))
		{
			String label = getHardLabel(d_tree.get(i_arg));
			if (label != null)	p.label = label;
		}
		
		return p;
	}

	private void addArgument(StringPrediction p)
	{
		s_skip.add(i_arg);
		
		if (!p.label.equals(LB_NO_ARG))
		{
			DEPNode pred = d_tree.get(i_pred);
			DEPNode arg  = d_tree.get(i_arg);
			
			if (PBLib.isNumberedArgument(p.label))
			{
				l_argns.add(p.label);
				
				if (PBLib.isCoreNumberedArgument(p.label))
				{
					ObjectDoublePair<DEPNode> prev = m_argns.get(p.label);
					
					if (prev != null)
					{
						DEPNode node = (DEPNode)prev.o;
						node.removeSHeadsByLabel(p.label);
					}
					
					m_argns.put(p.label, new ObjectDoublePair<DEPNode>(arg, p.score));
				}
			}
			
			String fTag = STConstant.EMPTY;
			
			if (!p.label.contains(STPunct.HYPHEN) && p_roleset != null)
			{
				String n = PBLib.getNumber(p.label);
				fTag = p_roleset.getFunctionTag(n);
			}
			
			arg.addSHead(pred, p.label, fTag);
		}
	}
	
//	================================ FEATURE EXTRACTION ================================

	@Override
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(JointFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(JointFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(JointFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(JointFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if (token.isField(JointFtrXml.F_DISTANCE))
		{
			return getDistance(node);
		}
		else if ((m = JointFtrXml.P_ARGN.matcher(token.field)).find())
		{
			int idx = l_argns.size() - Integer.parseInt(m.group(1)) - 1;
			return (idx >= 0) ? l_argns.get(idx) : null;
		}
		else if ((m = JointFtrXml.P_PATH.matcher(token.field)).find())
		{
			String type = m.group(1);
			int    dir  = Integer.parseInt(m.group(2));
			
			return getPath(type, dir);
		}
		else if ((m = JointFtrXml.P_SUBCAT.matcher(token.field)).find())
		{
			String type = m.group(1);
			int    dir  = Integer.parseInt(m.group(2));
			
			return getSubcat(node, type, dir);
		}
		else if ((m = JointFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		else if ((m = JointFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			DEPNode pred = d_tree.get(i_pred);
			int    field = Integer.parseInt(m.group(1));
			
			switch (field)
			{
			case 0: return (node.isDependentOf(pred))  ? token.field : null;
			case 1: return (pred.isDependentOf(node))  ? token.field : null;
			case 2: return (pred.isDependentOf(d_lca)) ? token.field : null;
			case 3: return (pred == d_lca) ? token.field : null;
			case 4: return (node == d_lca) ? token.field : null;
			}
		}
		
		return null;
	}
	
	@Override
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		
		if (token.isField(JointFtrXml.F_DEPREL_SET))
		{
			return getDeprelSet(node.getDependents());
		}
		else if (token.isField(JointFtrXml.F_GRAND_DEPREL_SET))
		{
			return getDeprelSet(node.getGrandDependents());
		}
		
		return null;
	}
	
	private String getDistance(DEPNode node)
	{
		int dist = Math.abs(i_pred - node.id);
		
		if      (dist <=  5)	return "0";
		else if (dist <= 10)	return "1";
		else if (dist <= 15)	return "2";
		else					return "3";
	}
	
	private String getPath(String type, int dir)
	{
		DEPNode pred = d_tree.get(i_pred);
		DEPNode arg  = d_tree.get(i_arg);
		
		if (dir == PATH_UP)
		{
			if (d_lca != pred)
				return getPathAux(d_lca, pred, type, SRLLib.DELIM_PATH_UP, true);
		}
		else if (dir == PATH_DOWN)
		{
			if (d_lca != arg)
				return getPathAux(d_lca, arg, type, SRLLib.DELIM_PATH_DOWN, true);
		}
		else
		{
			if (pred == d_lca)
				return getPathAux(pred, arg, type, SRLLib.DELIM_PATH_DOWN, true);
			else if (pred.isDescendentOf(arg))
				return getPathAux(arg, pred, type, SRLLib.DELIM_PATH_UP, true);
			else
			{
				String path = getPathAux(d_lca, pred, type, SRLLib.DELIM_PATH_UP, true);
				path += getPathAux(d_lca, arg, type, SRLLib.DELIM_PATH_DOWN, false);
				
				return path;
			}			
		}
		
		return null;
	}
	
	private String getPathAux(DEPNode top, DEPNode bottom, String type, String delim, boolean includeTop)
	{
		StringBuilder build = new StringBuilder();
		DEPNode head = bottom;
		int dist = 0;
		
		do
		{
			if (type.equals(JointFtrXml.F_POS))
			{
				build.append(delim);
				build.append(head.pos);
			}
			else if (type.equals(JointFtrXml.F_DEPREL))
			{
				build.append(delim);
				build.append(head.getLabel());
			}
			else if (type.equals(JointFtrXml.F_DISTANCE))
			{
				dist++;
			}
		
			head = head.getHead();
		}
		while (head != top && head != null);
		
		if (type.equals(JointFtrXml.F_POS))
		{
			if (includeTop)
			{
				build.append(delim);
				build.append(top.pos);	
			}
		}
		else if (type.equals(JointFtrXml.F_DISTANCE))
		{
			build.append(delim);
			build.append(dist);
		}
		
		return build.length() == 0 ? null : build.toString();
	}
	
	private String getSubcat(DEPNode node, String type, int dir)
	{
		List<DEPArc>  deps  = node.getDependents();
		StringBuilder build = new StringBuilder();
		int i, size = deps.size();
		DEPNode dep;
		
		if (dir == SUBCAT_LEFT)
		{
			for (i=0; i<size; i++)
			{
				dep = deps.get(i).getNode();
				if (dep.id > node.id)	break;
				getSubcatAux(build, dep, type);
			}
		}
		else if (dir == SUBCAT_RIGHT)
		{
			for (i=size-1; i>=0; i--)
			{
				dep = deps.get(i).getNode();
				if (dep.id < node.id)	break;
				getSubcatAux(build, dep, type);
			}
		}
		else
		{
			for (i=0; i<size; i++)
			{
				dep = deps.get(i).getNode();
				getSubcatAux(build, dep, type);
			}
		}
		
		return build.length() == 0 ? null : build.substring(SRLLib.DELIM_SUBCAT.length());
	}
	
	private void getSubcatAux(StringBuilder build, DEPNode node, String type)
	{
		build.append(SRLLib.DELIM_SUBCAT);
		
		if (type.equals(JointFtrXml.F_POS))
			build.append(node.pos);
		else if (type.equals(JointFtrXml.F_DEPREL))
			build.append(node.getLabel());
	}
	
	protected DEPNode getNode(FtrToken token)
	{
		DEPNode node = null;
		
		switch (token.source)
		{
		case JointFtrXml.S_PRED: node = d_tree.get(i_pred);	break;
		case JointFtrXml.S_ARG : node = d_tree.get(i_arg);	break;
		}
		
		if (token.relation != null)
		{
			     if (token.isRelation(JointFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(JointFtrXml.R_LMD))	node = lm_deps[node.id];
			else if (token.isRelation(JointFtrXml.R_RMD))	node = rm_deps[node.id];			
			else if (token.isRelation(JointFtrXml.R_LNS))	node = ln_sibs[node.id];
			else if (token.isRelation(JointFtrXml.R_RNS))	node = rn_sibs[node.id];
		}
		
		return node;
	}

//	================================ RERANK ================================
	
	private StringPrediction getBestPrediction(StringModel model, StringFeatureVector vector)
	{
		List<StringPrediction> ps = model.predictAll(vector);
		rerankPredictions(ps);
		Collections.sort(ps);

		return ps.get(0);
	}
	
	protected void rerankPredictions(List<StringPrediction> ps)
	{
		for (StringPrediction p : ps)
		{
			if (rerankFrameMismatch(p) || rerankRedundantNumberedArgument(p))
				p.score = -1;
		}
	}
	
	protected boolean rerankFrameMismatch(StringPrediction prediction)
	{
		if (p_roleset != null && !p_roleset.isValidArgument(prediction.label))
			return true;
		
		return false;
	}
	
	protected boolean rerankRedundantNumberedArgument(StringPrediction prediction)
	{
		ObjectDoublePair<DEPNode> prev = m_argns.get(prediction.label);
		
		if (prev != null && prev.d >= prediction.score)
			return true;
		
		return false;
	}
}
