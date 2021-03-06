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
package com.googlecode.clearnlp.generation;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTInput;

/** @author Jinho D. Choi ({@code jdchoi77@gmail.com}) */
public class LGAskTest
{
//	@Ignore
	@Test
	public void testGenerateQuestionFromAsk() throws Exception
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 7);
		reader.open(UTInput.createBufferedFileReader("src/test/resources/generation/ask.txt"));
		LGAsk ask = new LGAsk(new ZipInputStream(new BufferedInputStream(new FileInputStream("/Users/jdchoi/Documents/Workspace/lib/dictionary-1.4.0.zip"))));
		DEPTree tree;
		DEPNode root;
		int i;

		String[] questions = {
				"When was the last time that you were able to log into Remedy?",
				"How many users are getting this error message?",
				"Do you need to learn how to read?",
				"Do you want to reset your password?",
				"Do you want to reset your password?",
				"Do you want to reset your password?",
				"Do I want to reset my password?",
				"Should I place an order for you?",
				"Do you want me to place an order for you?",
				"Did you want me to place an order for you?",
				"Is your account locked?",
				"Is your account being locked?",
				"Was your account locked yesterday?",
				"Were you playing basketball?",
				"Are you an existing customer?",
				"Have you registered your account?",
				"Where were you yesterday?",
				"What do you do for a living?",
				"What do you want to buy?",
				"How long have you been waiting for?",
				"How soon do you want the product to be shipped?",
				"What kind of books do you like to buy?",
				"When did your account get locked?",
				"What can I do for you?",
				"What can you do for you?",
				"Who helped you last time?",
				"Which of your accounts is locked?",
				"When should I reset your password?",
				"What is your username?",
				"Please enter your password.",
				"Please enter your password.",
				"Please be patient."};
		
		String[] asks = {
				"Ask when the last time that the user were able to log into Remedy was.",
				"Ask how many users are getting this error message.",
				"Ask whether the user needs to learn how to read.",
				"Ask whether the user wants to reset the user's password.",
				"Ask whether the user wants to reset the user's password.",
				"Ask whether the user wants to reset the user's password.",
				"Ask whether I want to reset my password.",
				"Ask whether I should place an order for the user.",
				"Ask whether the user wants me to place an order for the user.",
				"Ask whether the user wanted me to place an order for the user.",
				"Ask whether the user's account is locked.",
				"Ask whether the user's account is being locked.",
				"Ask whether the user's account was locked yesterday.",
				"Ask whether the user was playing basketball.",
				"Ask whether the user is an existing customer.",
				"Ask whether the user has registered the user's account.",
				"Ask where the user was yesterday.",
				"Ask what the user does for a living.",
				"Ask what the user wants to buy.",
				"Ask how long the user has been waiting for.",
				"Ask how soon the user wants the product to be shipped.",
				"Ask what kind of books the user likes to buy.",
				"Ask when the user's account got locked.",
				"Ask what I can do for the user.",
				"Ask what the user can do for the user.",
				"Ask who helped the user last time.",
				"Ask which of the user's accounts is locked.",
				"Ask when I should reset the user's password.",
				"Ask what the user's username is."};
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			tree = ask.generateQuestionFromAsk(tree);
			assertEquals(questions[i], LGLibEn.getForms(tree, false, " "));
			
			if (!tree.get(1).isLemma("please"))
			{
				tree = ask.generateAskFromQuestion(tree);
				assertEquals(asks[i], LGLibEn.getForms(tree, false, " "));
				
				root = tree.getFirstRoot().getDependents().get(0).getNode();
				tree = ask.generateQuestionFromDeclarative(root, false);
				assertEquals(questions[i], LGLibEn.getForms(tree, false, " "));				
			}
		}
		
		testGenerateAskFromQuestion(ask);
	}
	
	void testGenerateAskFromQuestion(LGAsk ask)
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 7);
		reader.open(UTInput.createBufferedFileReader("src/test/resources/generation/ask4.txt"));
		DEPTree tree;
		
		tree = ask.generateAskFromQuestion(reader.next());
		assertEquals("Ask what the user's name is.", LGLibEn.getForms(tree, false, " "));
		
		tree = ask.generateAskFromQuestion(reader.next());
		assertEquals("Ask whether the user can describe what the user is seeing.", LGLibEn.getForms(tree, false, " "));
	}
	
	@Test
	public void testGenerateQuestionFromDeclarative()
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 7);
		reader.open(UTInput.createBufferedFileReader("src/test/resources/generation/ask3.txt"));
		LGAsk ask = new LGAsk();
		DEPTree tree;
		int i;

		String[] questions = {
				"Is the cat's name Doug?",
				"Are you a Ninja?",
				"Do you want to reset your password?",
				"Do you want to reset your password?",
				"Do you want to reset your password?",
				"Do I want to reset my password?",
				"Should I place an order for you?",
				"Did you want me to place an order for you?",
				"Is your account locked?",
				"Is your account being locked?",
				"Was your account locked yesterday?",
				"Are you an existing customer?",
				"Have you registered your account?",
				"Where were you yesterday?",
				"What do you do for a living?",
				"What do you want to buy?",
				"How long have you been waiting for?",
				"How soon do you want the product to be shipped?",
				"What kind of books do you like to buy?",
				"When did your account get locked?",
				"What can I do for you?",
				"Who helped you last time?",
				"Which of your accounts is locked?"};
		
		for (i=0; (tree = reader.next()) != null; i++)
			assertEquals(questions[i], LGLibEn.getForms(ask.generateQuestionFromDeclarative(tree, i==1),false," "));
	}
}
