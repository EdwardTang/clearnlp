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
package com.googlecode.clearnlp.nlp;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class NLPLib
{
	static public final String ENTRY_CONFIGURATION	= "_CONFIGURATION";
	static public final String ENTRY_FEATURE		= "_FEATURE";
	static public final String ENTRY_LEXICA			= "_LEXICA";
	static public final String ENTRY_MODEL			= "_MODEL";
	static public final String ENTRY_WEIGHTS		= "_WEIGHTS";
	
	static public final String MODE_TOK		= "tok";
	static public final String MODE_SEG		= "seg";
	static public final String MODE_POS		= "pos";
	static public final String MODE_MORPH	= "morph";
	static public final String MODE_DEP		= "dep";
	static public final String MODE_PRED	= "pred";
	static public final String MODE_ROLE	= "role";
	static public final String MODE_SRL		= "srl";
	static public final String MODE_SENSE	= "sense";
	static public final String MODE_POS_SB	= "pos_sb";
	
	static public final String MODE_PD_ALIGN = "pd_align";
	static public final String FORMAT_EN_CLEAR = "en_clear";
}
