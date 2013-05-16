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
package com.googlecode.clearnlp.dependency.srl;

import java.util.regex.Pattern;

import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.reader.AbstractReader;

/**
 * Dependency arc.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SRLArc implements Comparable<SRLArc>
{
	/** The linking node. */
	protected DEPNode node;
	/** The dependency label to the linking node. */
	protected String label;
	
	/** Constructs an empty dependency arc. */
	public SRLArc()
	{
		clear();
	}
	
	/**
	 * Constructs a dependency arc.
	 * @param node the linking node.
	 * @param label the dependency label for the linking node.
	 */
	public SRLArc(DEPNode node, String label)
	{
		set(node, label);
	}
	
	/** Sets the node to {@code null} and the label to {@link AbstractReader#DUMMY_TAG}. */
	public void clear()
	{
		set(null, null);
	}
	
	/**
	 * Returns the linking node.
	 * @return the linking node
	 */
	public DEPNode getNode()
	{
		return node;
	}
	
	/**
	 * Set the linking node to the specific node.
	 * @param node the node to be set.
	 */
	public void setNode(DEPNode node)
	{
		this.node = node;
	}
	
	/**
	 * Returns the dependency label.
	 * @return the dependency label.
	 */
	public String getLabel()
	{
		return label;
	}
	
	/**
	 * Sets the dependency label to the linking node.
	 * @param label the dependency label to the linking node.
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Sets the linking node and dependency label. 
	 * @param node the linking node.
	 * @param label the dependency label to the linking node.
	 */
	public void set(DEPNode node, String label)
	{
		this.node  = node;
		this.label = label;
	}
	
	/**
	 * Returns {@code true} if the specific node is its linking node.
	 * @param node the node to be compared.
	 * @return {@code true} if the specific node is its linking node.
	 */
	public boolean isNode(DEPNode node)
	{
		return this.node == node;
	}
	
	/**
	 * Returns {@code true} if the specific label is its label.
	 * @param label the label to be compared.
	 * @return {@code true} if the specific label is its label.
	 */
	public boolean isLabel(String label)
	{
		return this.label.equals(label);
	}
	
	public boolean isLabel(Pattern regex)
	{
		return regex.matcher(label).find();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(node.id);
		build.append(DEPLib.DELIM_HEADS_KEY);
		build.append(label);
		
		return build.toString();
	}
	
	@Override
	public int compareTo(SRLArc arc)
	{
		return label.compareTo(arc.getLabel());
	}	
}
