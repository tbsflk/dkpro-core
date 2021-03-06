/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.dkpro.core.testing.harness;

import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertSentence;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertToken;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.internal.AssumptionViolatedException;

import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceObjectProviderBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.SegmenterBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations;

public final class SegmenterHarness
{
	public static final TestData[] DATA = new TestData[] {
		new TestData("de.1", "de", "Herr Frank M. Meier hat einen Hund.",
			new String[] { "Herr", "Frank", "M.", "Meier", "hat", "einen",
				"Hund", "."},
			new String[] { "Herr Frank M. Meier hat einen Hund." }),
		new TestData("de.2", "de", "Ich bin ein blöder Hund.",
			new String[] { "Ich", "bin", "ein", "blöder", "Hund", "." },
			new String[] { "Ich bin ein blöder Hund." }),
		new TestData("de.3", "de", "Mein Name ist Hans.",
			new String[] { "Mein", "Name", "ist", "Hans", "." },
			new String[] { "Mein Name ist Hans." }),
		// DKPRO-CORE-ASL-98: BreakIteratorSegmenter turns hypens to separate tokens
		new TestData("de.4", "de", "ihre Negativbei- spiele immer",
				new String[] { "ihre", "Negativbei-", "spiele", "immer" },
				new String[] { "ihre Negativbei- spiele immer" }),

		new TestData("en.1", "en", "Sadler, A.L. Cha-No-Yu: The Japanese Tea Ceremony.",
			new String[] { "Sadler", ",", "A.L.", "Cha-No-Yu", ":", "The",
				"Japanese", "Tea", "Ceremony", "."},
			new String[] { "Sadler, A.L. Cha-No-Yu: The Japanese Tea Ceremony." } ),
		new TestData("en.2", "en", "I love the UIMA toolkit. 1989 is the year in which the Berlin wall fell.",
			new String[] { "I", "love", "the", "UIMA", "toolkit", ".",
			"1989", "is", "the", "year", "in", "which", "the", "Berlin",
			"wall", "fell", "." },
			new String[] { "I love the UIMA toolkit.",
				"1989 is the year in which the Berlin wall fell." }),
		new TestData("en.3", "en", "I'm not a girl.",
			new String[] { "I", "'m", "not", "a", "girl", "." },
			new String[] { "I'm not a girl." }),
		new TestData("en.4", "en", "I am a stupid dog.",
			new String[] { "I", "am", "a", "stupid", "dog", "." },
			new String[] { "I am a stupid dog." }),
		new TestData("en.5", "en", "Georg \"Bullseye\" Logal is a though guy.",
			new String[] { "Georg", "\"", "Bullseye", "\"", "Logal",
			"is", "a", "though", "guy", "." },
			new String[] { "Georg \"Bullseye\" Logal is a though guy." }),
		new TestData("en.6", "en", "This doesn't compute.",
			new String[] { "This", "does", "n't", "compute", "." },
			new String[] { "This doesn't compute." }),
		new TestData("en.7", "en", "based on\n 'Carnival of Souls', written by [...] and directed by [...].",
			new String[] { "based", "on", "'", "Carnival", "of", "Souls",
				"'", ",", "written", "by", "[", "...", "]", "and", "directed",
				"by", "[", "...", "]", "." },
			new String[] { "based on\n 'Carnival of Souls', written by [...] and directed by [...]." }),
		new TestData("en.8", "en", ", , ,",
			new String[] { ",", ",", "," },
			new String[] { ", , ," }),
		new TestData("en.9", "en", "How to tokenize smileys? This is a good example. >^,,^< :0 3:[",
			new String[] { "How", "to", "tokenize", "smileys", "?", "This", "is", "a", "good", "example.", ">^,,^<", ":0", "3:[" },
			new String[] { "How to tokenize smileys?", "This is a good example.", ">^,,^< :0 3:[" }),

	// Sombody who can read arabic, please check this
    	// Covering the following sub-Saharan countries with vast areas very
		new TestData("ar.1", "ar", "تغطي الصحراء الكبرى الدول التالية بمساحات شاسعة جدا",
			new String[] { "تغطي", "الصحراء", "الكبرى", "الدول", "التالية",
					"مساحات", "شاسعة", "جدا" },
			new String[] { "تغطي الصحراء الكبرى الدول التالية بمساحات شاسعة جدا" }),

	// While the stanford parser should come with a proper tokenizer
	// for Chinese (because it can parse chinese text), this does not
	// seem to be the right one or I am using it wrong. The associated
	// test cases do not work. Maybe debugging the command below
	// would help to find out how to use it.
	// They use command to parse it: java -mx1g -cp "stanford-parser.jar"
	// edu.stanford.nlp.parser.lexparser.LexicalizedParser -tLPP
	// edu.stanford.nlp.parser.lexparser.ChineseTreebankParserParams -sentences
	// newline -escaper
	// edu.stanford.nlp.trees.international.pennchinese.ChineseEscaper
	// -outputFormat "penn,typedDependencies" -outputFormatOptions
	// "removeTopBracket" xinhuaFactoredSegmenting.ser.gz sampleInput.txt.
		new TestData("zh.1", "zh", "服务业成为广东经济转型升级的重要引擎。",
			new String[] {"服务业", "成为", "广东", "经济", "转型", "升级", "的",
				"重要", "引擎", "。"},
			new String[] {"服务业成为广东经济转型升级的重要引擎。"}),
		new TestData("zh.2", "zh", "中国离世界技术品牌有多远?",
			new String[] {"中国", "离", "世界", "技术", "品牌", "有", "多远",
				"？" },
			new String[] { "中国离世界技术品牌有多远?" })
	};

	private SegmenterHarness()
	{
		// No instances
	}

	@FunctionalInterface
	public static interface AssumeResourcePredicate {
        void assume(String aLanguage, String aVariant)
            throws AssumptionViolatedException, IOException;
	}

    public static void run(AnalysisEngineDescription aAed, String... aIgnoreIds)
        throws Throwable
    {
        run(aAed, null, aIgnoreIds);
    }
	
    public static void run(AnalysisEngineDescription aAed, AssumeResourcePredicate aCheck,
            String... aIgnoreIds)
                throws Throwable
    {
        // No automatic downloading from repository during testing. This makes sure we fail if
        // models are not properly added as test dependencies.
        if (offline) {
            System.setProperty(ResourceObjectProviderBase.PROP_REPO_OFFLINE, "true");
        }
        offline = true;
        
		AnalysisEngine ae = createEngine(aAed);
		JCas jCas = ae.newJCas();

		List<String> results = new ArrayList<String>();

		try {
			for (TestData td : DATA) {
				System.out.printf("== %s ==%n", td.id);
				jCas.reset();
				
				if (aCheck != null) {
				    try {
				        aCheck.assume(td.language, null);
				    }
				    catch (AssumptionViolatedException e) {
                        results.add(String.format("%s skipped", td.id));
				        continue;
				    }
				}
				
				jCas.setDocumentLanguage(td.language);
				jCas.setDocumentText(td.text);

				boolean failed = false;

				try {
					ae.process(jCas);

					AssertAnnotations.assertSentence(td.sentences, select(jCas, Sentence.class));
					AssertAnnotations.assertToken(td.tokens, select(jCas, Token.class));

					results.add(String.format("%s OK", td.id));
				}
				catch (Throwable e) {
					failed = true;
					if (!ArrayUtils.contains(aIgnoreIds, td.id)) {
						results.add(String.format("%s FAIL", td.id));
						throw e;
					}
					else {
						results.add(String.format("%s FAIL - Known, ignored", td.id));
					}
				}

				if (!failed && ArrayUtils.contains(aIgnoreIds, td.id)) {
					results.add(String.format("%s FAIL", td.id));
					Assert.fail(td.id + " passed but was expected to fail");
				}
			}
		}
		finally {
			System.out.println("=== RESULTS ===");
			for (String r : results) {
				System.out.println(r);
			}
		}
	}

    public static void testZoning(Class<? extends SegmenterBase> aSegmenter)
        throws Exception
    {
        testZoning(aSegmenter, "en");
    }

    public static void testZoning(Class<? extends SegmenterBase> aSegmenter, String aLanguage)
        throws Exception
    {
	    testLaxZoning(aSegmenter, aLanguage);
	    testStrictZoning(aSegmenter, aLanguage);
	    testOufOfBoundsZones(aSegmenter, aLanguage);
	}
	
    public static void testLaxZoning(Class<? extends SegmenterBase> aSegmenter, String aLanguage)
        throws Exception
    {
        // No automatic downloading from repository during testing. This makes sure we fail if
        // models are not properly added as test dependencies.
        if (offline) {
            System.setProperty(ResourceObjectProviderBase.PROP_REPO_OFFLINE, "true");
        }
        offline = true;
        
        String[] sentences = { "A a a a .", "A a a a -", "B b b b .", "B b b b -", "C c c c .",
                "C c c c -" };
        
        String[] tokens = { "A", "a", "a", "a", ".", "A", "a", "a", "a", "-", "B", "b", "b", "b",
                ".", "B", "b", "b", "b", "-", "C", "c", "c", "c", ".", "C", "c", "c", "c", "-" };
        
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentLanguage(aLanguage);
        //                              1    1    2    2    3    3    4    4    5    5    6
        //                    0    5    0    5    0    5    0    5    0    5    0    5    0
        //                     ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
        jcas.setDocumentText("A a a a . A a a a - B b b b . B b b b - C c c c . C c c c -");
        //                    |------------------|                    |------------------|
        new Paragraph(jcas, 0, 19).addToIndexes();
        new Paragraph(jcas, 40, 59).addToIndexes();

        AnalysisEngine ae = createEngine(aSegmenter,
                SegmenterBase.PARAM_STRICT_ZONING, false,
                SegmenterBase.PARAM_ZONE_TYPES, Paragraph.class);
        ae.process(jcas);

        assertToken(tokens, select(jcas, Token.class));
        assertSentence(sentences, select(jcas, Sentence.class));
    }

    public static void testOufOfBoundsZones(Class<? extends SegmenterBase> aSegmenter,
            String aLanguage)
        throws Exception
    {
        // No automatic downloading from repository during testing. This makes sure we fail if
        // models are not properly added as test dependencies.
        if (offline) {
            System.setProperty(ResourceObjectProviderBase.PROP_REPO_OFFLINE, "true");
        }
        offline = true;
        
        //                       1    1    2    2    3    3    4    4    5    5    6
        //             0    5    0    5    0    5    0    5    0    5    0    5    0
        //              ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
        String text = "A a a a . A a a a - B b b b . B b b b - C c c c . C c c c -";
        //             |------------------|                    |------------------|
        
        // non-strict zoning
        {
            String[] sentences = { "A a a a .", "A a a a -", "B b b b .", "B b b b -", "C c c c .",
                    "C c c c -" };

            String[] tokens = { "A", "a", "a", "a", ".", "A", "a", "a", "a", "-", "B", "b", "b",
                    "b", ".", "B", "b", "b", "b", "-", "C", "c", "c", "c", ".", "C", "c", "c", "c",
                    "-" };
            
            JCas jcas = JCasFactory.createJCas();
            jcas.setDocumentLanguage(aLanguage);
            jcas.setDocumentText(text);
            new Paragraph(jcas, 0, 19).addToIndexes();
            new Paragraph(jcas, 40, 65).addToIndexes();
    
            AnalysisEngine ae = createEngine(aSegmenter,
                    SegmenterBase.PARAM_STRICT_ZONING, false,
                    SegmenterBase.PARAM_ZONE_TYPES, Paragraph.class);
            ae.process(jcas);
    
            assertToken(tokens, select(jcas, Token.class));
            assertSentence(sentences, select(jcas, Sentence.class));
        }
        
        // strict zoning
        {
            String[] sentences = { "A a a a .", "A a a a -", "C c c c .", "C c c c -" };

            String[] tokens = { "A", "a", "a", "a", ".", "A", "a", "a", "a", "-", "C", "c", "c",
                    "c", ".", "C", "c", "c", "c", "-" };
            
            JCas jcas = JCasFactory.createJCas();
            jcas.setDocumentLanguage(aLanguage);
            jcas.setDocumentText(text);
            new Paragraph(jcas, 0, 19).addToIndexes();
            new Paragraph(jcas, 40, 65).addToIndexes();
            
            AnalysisEngine ae = createEngine(aSegmenter,
                    SegmenterBase.PARAM_STRICT_ZONING, true,
                    SegmenterBase.PARAM_ZONE_TYPES, Paragraph.class);
            ae.process(jcas);
    
            assertToken(tokens, select(jcas, Token.class));
            assertSentence(sentences, select(jcas, Sentence.class));
        }
    }
    
    public static void testStrictZoning(Class<? extends SegmenterBase> aSegmenter, String aLanguage)
        throws Exception
    {
        // No automatic downloading from repository during testing. This makes sure we fail if
        // models are not properly added as test dependencies.
        if (offline) {
            System.setProperty(ResourceObjectProviderBase.PROP_REPO_OFFLINE, "true");
        }
        offline = true;
        
        String[] sentences = { "A a a a .", "A a a a -", "C c c c .", "C c c c -" };
        
        String[] tokens = { 
                "A", "a", "a", "a", ".", 
                "A", "a", "a", "a", "-",
                "C", "c", "c", "c", ".", 
                "C", "c", "c", "c", "-" };
        
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentLanguage(aLanguage);
        //                              1    1    2    2    3    3    4    4    5    5    6
        //                    0    5    0    5    0    5    0    5    0    5    0    5    0
        //                     ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
        jcas.setDocumentText("A a a a . A a a a - B b b b . B b b b - C c c c . C c c c -");
        //                    |------------------|                    |------------------|
        new Paragraph(jcas, 0, 19).addToIndexes();
        new Paragraph(jcas, 40, 59).addToIndexes();

        AnalysisEngine ae = createEngine(aSegmenter,
                SegmenterBase.PARAM_STRICT_ZONING, true,
                SegmenterBase.PARAM_ZONE_TYPES, Paragraph.class);
        ae.process(jcas);

        assertToken(tokens, select(jcas, Token.class));
        assertSentence(sentences, select(jcas, Sentence.class));
    }
	
	static class TestData
	{
		final String id;
		final String language;
		final String text;
		final String[] sentences;
		final String[] tokens;

		public TestData(String aId, String aLanguage, String aText, String[] aTokens, String[] aSentences)
		{
			id = aId;
			language = aLanguage;
			text = aText;
			sentences = aSentences;
			tokens = aTokens;
		}
	}
	
    private static boolean offline = true;
    
    public static void autoloadModelsOnNextTestRun()
    {
        offline = false;
    }
}
