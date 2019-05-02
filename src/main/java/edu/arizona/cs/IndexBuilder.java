package edu.arizona.cs;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;

/**
 * File: IndexBuffer.java
 * Author: Dilen Govin
 * 
 * Indexes the wikipedia documents
 */

public class IndexBuilder 
{
    private String sourceDirectory, sourceIndexPath;
    private IndexWriter writer;
    private boolean lemmatize;
    private boolean stem;
    private StanfordCoreNLP pipeline;
    
    public IndexBuilder(String sourceDirectory, String sourceIndexPath, boolean lemmatize, boolean stem)
    {
        this.sourceDirectory = sourceDirectory;
        this.sourceIndexPath = sourceIndexPath;
        this.lemmatize = lemmatize;
        this.stem = stem;
        this.pipeline = null;
        
        try {
            FSDirectory index = FSDirectory.open(new File(this.sourceIndexPath).toPath());
            Analyzer analyzer;

            // Initializes the analyzer depending on the arguments
            if (stem || lemmatize) {
                analyzer = new WhitespaceAnalyzer();
            } else {
                analyzer = new StandardAnalyzer();
            }

            if (lemmatize) {
                Properties props = new Properties();
                props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
                pipeline = new StanfordCoreNLP(props);
            }

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            this.writer = new IndexWriter(index, config);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Builds the index
    public void build()
    {
        try {
            File directory = new File(this.sourceDirectory);
            File[] files = directory.listFiles();

            for (File file : files) {
                System.out.printf("Adding %s to the index\n", file.getName());
                addFileToIndex(file);
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Parses the file and adds the index
    public void addFileToIndex(File file)
    {
        try {
            String contents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String patternStr = "\\n+\\[\\[(.*?)\\]\\]\\n+";
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(contents);
            String[] docs = contents.split(patternStr);

            int i = 1;
            while (matcher.find()) {
                String title = matcher.group(1);
                String content = docs[i].trim().replaceAll("\\s+", " ").toLowerCase();

                if (stem) {
                    String stemmedContent = "";
                    PorterStemmer stemmer = new PorterStemmer();

                    for (String word : content.split("\\s+")) {
                        stemmer.setCurrent(word);
                        stemmer.stem();
                        stemmedContent += (stemmer.getCurrent() + " ");
                    }

                    addDoc(title, stemmedContent);

                } else if (lemmatize) {
                    String lemmatizedContent = "";
                    Annotation document = new Annotation(content);
                    pipeline.annotate(document);
                    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

                    for (CoreMap sentence : sentences) {
                        for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                            lemmatizedContent += (token.get(LemmaAnnotation.class) + " ");
                        }
                    }

                    addDoc(title, lemmatizedContent);

                } else {
                    addDoc(title, content);
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // adds the document to the index
    public void addDoc(String docName, String content) throws IOException
    {
        Document doc = new Document();
        doc.add(new StringField("docName", docName, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        this.writer.addDocument(doc);
    }

}