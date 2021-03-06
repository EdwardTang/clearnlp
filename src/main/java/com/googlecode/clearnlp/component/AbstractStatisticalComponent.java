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
package com.googlecode.clearnlp.component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.utils.IOUtils;

import com.googlecode.clearnlp.classification.feature.FtrTemplate;
import com.googlecode.clearnlp.classification.feature.FtrToken;
import com.googlecode.clearnlp.classification.feature.JointFtrXml;
import com.googlecode.clearnlp.classification.model.ONStringModel;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractStatisticalComponent extends AbstractComponent
{
	protected StringTrainSpace[] s_spaces;
	protected StringModel[]      s_models;
	protected JointFtrXml[]      f_xmls;

	protected DEPTree	d_tree;
	protected int		t_size;	// size of d_tree

	protected DEPNode[]	lm_deps, rm_deps;
	protected DEPNode[]	ln_sibs, rn_sibs;
	
//	====================================== CONSTRUCTORS ======================================
	
	public AbstractStatisticalComponent() {}
	
	/** Constructs a component for collecting lexica. */
	public AbstractStatisticalComponent(JointFtrXml[] xmls)
	{
		i_flag = FLAG_LEXICA;
		f_xmls = xmls;
	}
	
	/** Constructs a component for training. */
	public AbstractStatisticalComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica)
	{
		i_flag   = FLAG_TRAIN;
		f_xmls   = xmls;
		s_spaces = spaces;
		
		initLexia(lexica);
	}
	
	/** Constructs a component for developing. */
	public AbstractStatisticalComponent(JointFtrXml[] xmls, StringModel[] models, Object[] lexica)
	{
		i_flag   = FLAG_DEVELOP;
		f_xmls   = xmls;
		s_models = models;

		initLexia(lexica);
	}
	
	/** Constructs a component for decoding. */
	public AbstractStatisticalComponent(ZipInputStream zin)
	{
		i_flag = FLAG_DECODE;
		
		loadModels(zin);
	}
	
	/** Constructs a component for bootstrapping. */
	public AbstractStatisticalComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica)
	{
		i_flag   = FLAG_BOOTSTRAP;
		f_xmls   = xmls;
		s_spaces = spaces;
		s_models = models;
		
		initLexia(lexica);
	}

	/** Initializes lexica used for this component. */
	abstract protected void initLexia(Object[] lexica);

//	====================================== LOAD/SAVE MODELS ======================================

	/** Loads all models of this joint-component. */
	abstract public void loadModels(ZipInputStream zin);
	
	protected void loadDefaultConfiguration(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		int i, mSize = Integer.parseInt(fin.readLine());
		
		LOG.info("Loading configuration.\n");
		s_models = new StringModel[mSize];
		
		for (i=0; i<mSize; i++)
			s_models[i] = new StringModel();
	}

	/** Called by {@link AbstractStatisticalComponent#loadModels(ZipInputStream)}}. */
	protected ByteArrayInputStream loadFeatureTemplates(ZipInputStream zin, int index) throws Exception
	{
		LOG.info("Loading feature templates.\n");

		BufferedReader fin = UTInput.createBufferedReader(zin);
		ByteArrayInputStream template = getFeatureTemplates(fin);
		
		f_xmls[index] = new JointFtrXml(template);
		return template;
	}
	
	protected ByteArrayInputStream getFeatureTemplates(BufferedReader fin) throws IOException
	{
		StringBuilder build = new StringBuilder();
		String line;

		while ((line = fin.readLine()) != null)
		{
			build.append(line);
			build.append("\n");
		}
		
		return new ByteArrayInputStream(build.toString().getBytes());
	}
	
	/** Called by {@link AbstractStatisticalComponent#loadModels(ZipInputStream)}}. */
	protected void loadStatisticalModels(ZipInputStream zin, int index) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		s_models[index].load(fin);
	//	s_models[index] = new StringModel(fin);
	}
	
	protected void loadWeightVector(ZipInputStream zin, int index) throws Exception
	{
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(zin));
		s_models[index].loadWeightVector(oin);
	}
	
	/** For online decoders. */
	protected void loadOnlineModels(ZipInputStream zin, int index, double alpha, double rho) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		s_models[index] = new ONStringModel(fin, alpha, rho);
	}
	
	/** Saves all models of this joint-component. */
	abstract public void saveModels(ZipOutputStream zout);
	
	protected void saveDefaultConfiguration(ZipOutputStream zout, String entryName) throws Exception
	{
		zout.putNextEntry(new ZipEntry(entryName));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		LOG.info("Saving configuration.\n");
		
		fout.println(s_models.length);
		
		fout.flush();
		zout.closeEntry();
	}
	
	/** Called by {@link AbstractStatisticalComponent#saveModels(ZipOutputStream)}}. */
	protected void saveFeatureTemplates(ZipOutputStream zout, String entryName) throws Exception
	{
		int i, size = f_xmls.length;
		PrintStream fout;
		LOG.info("Saving feature templates.\n");
		
		for (i=0; i<size; i++)
		{
			zout.putNextEntry(new ZipEntry(entryName+i));
			fout = UTOutput.createPrintBufferedStream(zout);
			IOUtils.copy(UTInput.toInputStream(f_xmls[i].toString()), fout);
			fout.flush();
			zout.closeEntry();
		}
	}
	
	/** Called by {@link AbstractStatisticalComponent#saveModels(ZipOutputStream)}}. */
	protected void saveStatisticalModels(ZipOutputStream zout, String entryName) throws Exception
	{
		int i, size = s_models.length;
		PrintStream fout;
		
		for (i=0; i<size; i++)
		{
			zout.putNextEntry(new ZipEntry(entryName+i));
			fout = UTOutput.createPrintBufferedStream(zout);
			s_models[i].save(fout);
			fout.flush();
			zout.closeEntry();			
		}
	}
	
	protected void saveWeightVector(ZipOutputStream zout, String entryName) throws Exception
	{
		int i, size = s_models.length;
		ObjectOutputStream oout;
		
		for (i=0; i<size; i++)
		{
			zout.putNextEntry(new ZipEntry(entryName+i));
			oout = new ObjectOutputStream(new BufferedOutputStream(zout));
			s_models[i].saveWeightVector(oout);
			oout.flush();
			zout.closeEntry();			
		}
	}
	
//	====================================== INITIALIZATION ======================================
	
	/** Initializes dependency arcs of all nodes. */
	protected void initArcs()
	{
		DEPNode curr, prev, next;
		List<DEPArc> deps;
		DEPArc lmd, rmd;
		int i, j, len;
		
		lm_deps = new DEPNode[t_size];
		rm_deps = new DEPNode[t_size];
		ln_sibs = new DEPNode[t_size];
		rn_sibs = new DEPNode[t_size];
		
		d_tree.setDependents();
		
		for (i=1; i<t_size; i++)
		{
			deps = d_tree.get(i).getDependents();
			if (deps.isEmpty())	continue;
			
			len = deps.size(); 
			lmd = deps.get(0);
			rmd = deps.get(len-1);
			
			if (lmd.getNode().id < i)	lm_deps[i] = lmd.getNode();
			if (rmd.getNode().id > i)	rm_deps[i] = rmd.getNode();
			
			for (j=1; j<len; j++)
			{
				curr = deps.get(j  ).getNode();
				prev = deps.get(j-1).getNode();

				if (ln_sibs[curr.id] == null || ln_sibs[curr.id].id < prev.id)
					ln_sibs[curr.id] = prev;
			}
			
			for (j=0; j<len-1; j++)
			{
				curr = deps.get(j  ).getNode();
				next = deps.get(j+1).getNode();

				if (rn_sibs[curr.id] == null || rn_sibs[curr.id].id > next.id)
					rn_sibs[curr.id] = next;
			}
		}
	}
	
//	====================================== GETTERS/SETTERS ======================================
	
	/** @return all training spaces of this joint-components. */
	public StringTrainSpace[] getTrainSpaces()
	{
		return s_spaces;
	}
	
	/** @return all models of this joint-components. */
	public StringModel[] getModels()
	{
		return s_models;
	}
	
	/** @return all objects containing lexica. */
	abstract public Object[] getLexica();
	
//	====================================== PROCESS ======================================

	/** Counts the number of correctly classified labels. */
	abstract public void countAccuracy(int[] counts);
	
//	====================================== FEATURE EXTRACTION ======================================

	/** @return a field of the specific feature token (e.g., lemma, pos-tag). */
	abstract protected String getField(FtrToken token);
	
	/** @return multiple fields of the specific feature token (e.g., lemma, pos-tag). */
	abstract protected String[] getFields(FtrToken token);
	
	/** @param the dependency node that is not {@code null}. */
	protected String getDefaultField(FtrToken token, DEPNode node)
	{
		Matcher m;
		
		if (token.isField(JointFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(JointFtrXml.F_SIMPLIFIED_FORM))
		{
			return node.simplifiedForm;
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
		else if ((m = JointFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	protected String[] getDefaultFields(FtrToken token, DEPNode node)
	{
		if (token.isField(JointFtrXml.F_DEPREL_SET))
		{
			return getDeprelSet(node.getDependents());
		}
		
		return null;
	}
	
	/** @return a feature vector using the specific feature template. */
	protected StringFeatureVector getFeatureVector(JointFtrXml xml)
	{
		StringFeatureVector vector = new StringFeatureVector();
		
		for (FtrTemplate template : xml.getFtrTemplates())
			addFeatures(vector, template);
		
		return vector;
	}

	/** Called by {@link AbstractStatisticalComponent#getFeatureVector(JointFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, FtrTemplate template)
	{
		FtrToken[] tokens = template.tokens;
		int i, size = tokens.length;
		
		if (template.isSetFeature())
		{
			String[][] fields = new String[size][];
			String[]   tmp;
			
			for (i=0; i<size; i++)
			{
				tmp = getFields(tokens[i]);
				if (tmp == null)	return;
				fields[i] = tmp;
			}
			
			addFeatures(vector, template.type, fields, 0, "");
		}
		else
		{
			StringBuilder build = new StringBuilder();
			String field;
			
			for (i=0; i<size; i++)
			{
				field = getField(tokens[i]);
				if (field == null)	return;
				
				if (i > 0)	build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(field);
			}
			
			vector.addFeature(template.type, build.toString());			
		}
    }
	
	/** Called by {@link AbstractStatisticalComponent#getFeatureVector(JointFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, String type, String[][] fields, int index, String prev)
	{
		if (index < fields.length)
		{
			for (String field : fields[index])
			{
				if (prev.isEmpty())
					addFeatures(vector, type, fields, index+1, field);
				else
					addFeatures(vector, type, fields, index+1, prev + AbstractColumnReader.BLANK_COLUMN + field);
			}
		}
		else
			vector.addFeature(type, prev);
	}
	
	protected List<Pair<String,StringFeatureVector>> getTrimmedInstances(List<Pair<String,StringFeatureVector>> insts)
	{
		List<Pair<String,StringFeatureVector>> nInsts = new ArrayList<Pair<String,StringFeatureVector>>();
		Set<String> set = new HashSet<String>();
		String key;
		
		for (Pair<String,StringFeatureVector> p : insts)
		{
			key = p.o1+" "+p.o2.toString();
			
			if (!set.contains(key))
			{
				nInsts.add(p);
				set.add(key);
			}
		}
		
		return nInsts;
	}
	
//	====================================== RULES ======================================
	
	protected List<Map<String,String[]>> getRules(BufferedReader fin)
	{
		Pattern space = Pattern.compile(" "), tab = Pattern.compile("\t");
		List<Map<String,String[]>> rules = null;
		String[] tmp, val;
		String line;
		int i, ngram;
		
		try
		{
			ngram = Integer.parseInt(fin.readLine());
			rules = new ArrayList<Map<String,String[]>>(ngram);
			
			for (i=0; i<ngram; i++)
				rules.add(new HashMap<String,String[]>());
			
			while ((line = fin.readLine()) != null)
			{
				tmp = tab.split(line);
				val = space.split(tmp[1]);
				
				if (val.length <= ngram)
					rules.get(val.length-1).put(tmp[0].trim(), val);
			}
		}
		catch (IOException e) {e.printStackTrace();}
		
		return rules;
	}
	
	protected String[] getRules(List<Map<String,String[]>> list, int currId)
	{
		StringBuilder build = new StringBuilder();
		int i, j, ngram = list.size();
		String[] tmp, rules = null;
		
		for (i=currId,j=0; i<t_size && j<ngram; i++,j++)
		{
			if (j > 0)	build.append(" ");
			build.append(d_tree.get(i).form);

			tmp = list.get(j).get(build.toString());
			if (tmp != null)	rules = tmp;
		}
		
		return rules;
	}
}
