/**
* Copyright 2012-2013 University of Massachusetts Amherst
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
package com.googlecode.clearnlp.experiment;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.clearnlp.classification.feature.JointFtrXml;
import com.googlecode.clearnlp.component.dep.CDEPParser;
import com.googlecode.clearnlp.component.dep.ONDEPPassParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.nlp.NLPTrain;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;

public class NLPOnline extends NLPTrain
{
	public NLPOnline(String[] featureFiles, String trainDir, String devDir, double alpha, double rho, int devId) throws Exception
	{
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		String[]   devFiles = UTFile.getSortedFileListBySize(devDir, ".*", true);
		JointReader  reader = new JointReader(0, 1, 2, 4, -1, 5, 6);
		
		Object[] lexica = getLexica(new CDEPParser(xmls), reader, xmls, trainFiles, devId);
		ONDEPPassParser parser = new ONDEPPassParser(xmls, lexica, alpha, rho);
		List<DEPTree> trainTrees = getTrees(reader, trainFiles, devId);
		List<DEPTree> devTrees = getTrees(reader, devFiles, devId);
		
		parser.train(trainTrees);
		int[] counts = new int[4];
		
		for (DEPTree tree : devTrees)
		{
			parser.develop(tree);
			parser.countAccuracy(counts);
		}
		
		System.out.printf("LAS: %5.2f (%d/%d)", 100d*counts[1]/counts[0], counts[1], counts[0]);
		System.out.printf("UAS: %5.2f (%d/%d)", 100d*counts[2]/counts[0], counts[2], counts[0]);
		System.out.printf("LS : %5.2f (%d/%d)", 100d*counts[3]/counts[0], counts[3], counts[0]);
	}
	
	public List<DEPTree> getTrees(JointReader reader, String[] trainFiles, int devId)
	{
		List<DEPTree> trees = new ArrayList<DEPTree>();
		int i, size = trainFiles.length;
		DEPTree tree;
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				trees.add(tree);
			
			reader.close();
		}

		return trees;
	}

	static public void main(String[] args) 
	{
		try
		{
			new NLPOnline(args[0].split(":"), args[1], args[2], Double.parseDouble(args[3]), Double.parseDouble(args[4]), -1);
		}
		catch (Exception e) {e.printStackTrace();};
	}
}
