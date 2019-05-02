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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
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

/**
 * File: QueryParser.java Author: Dilen Govin
 * 
 * Parses and queries for Watson project.
 */

public class QueryEngine {
    private String sourceQuestions, sourceIndexPath;
    public Analyzer analyzer;
    public Directory index;
    private boolean lemmatize, stem;
    private StanfordCoreNLP pipeline;

    public QueryEngine(String sourceQuestions, String sourceIndexPath, boolean lemmatization, boolean stem) 
    {
        this.sourceQuestions = sourceQuestions;
        this.sourceIndexPath = sourceIndexPath;
        this.lemmatize = lemmatization;
        this.stem = stem;

        if (stem || lemmatize) {
            this.analyzer = new WhitespaceAnalyzer();
        } else {
            this.analyzer = new StandardAnalyzer();
        }

        if (lemmatize) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            pipeline = new StanfordCoreNLP(props);
        }

        try {
            this.index = FSDirectory.open(new File(this.sourceIndexPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    // Extracts the questions
    public HashMap<String, String> extractQuestions() 
    {
        HashMap<String, String> questions = new HashMap<>();

        try {
            File directory = new File(this.sourceQuestions);
            File[] files = directory.listFiles();

            // Loops through the files and splits them on titles
            for (File file : files) {
                System.out.printf("Adding %s to list of questions\n", file);
                String contents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile(".*\\n.*\\n.*\\n\\n");
                Matcher matcher = pattern.matcher(contents);

                // Add documents to the questions HashMap
                while (matcher.find()) {
                    String[] parts = matcher.group(0).split("\n");
                    String category = parts[0];
                    String question = parts[1];
                    String answer = parts[2];

                    questions.put(question + " " + category, answer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return questions;
    }

    // Queries the index and returns the score
    public Double query(String question, String answer) 
    {
        Double score = 0.0;
        int hitsPerPage = 10;

        try {
            Query query = createQuery(question);

            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Part 3: Changing the scoring functions
            // searcher.setSimilarity(new BooleanSimilarity());
            // searcher.setSimilarity(new TFIDFSimilarity());

            TopDocs docs = searcher.search(query, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;

            if (hits.length != 0) {
                Document d = searcher.doc(hits[0].doc);

                if (d.get("docName").toLowerCase().contains(answer.toLowerCase()) || 
                        answer.toLowerCase().contains(d.get("docName").toLowerCase())) {
                    score = 1.0;

                } else {
                    // Computes the MRR score of the query
                    for (int i = 1; i < hits.length; i++) {
                        d = searcher.doc(hits[i].doc);

                        if (d.get("docName").toLowerCase().contains(answer.toLowerCase()) ||
                                answer.toLowerCase().contains(d.get("docName").toLowerCase())) {

                            score = (1.0 / (i + 1));
                            return score;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return score;
    }

    // Constructs query from question
    private Query createQuery(String question) 
    {
        String queryStr = "";

        // Stem query if specified
        if (stem) {
            PorterStemmer stemmer = new PorterStemmer();
            for (String word : question.split("\\s+")) {
                stemmer.setCurrent(word);
                stemmer.stem();
                queryStr += (stemmer.getCurrent() + " ");
            }

        // Lemmatize the query if specified
        } else if(lemmatize) {
            Annotation document = new Annotation(question);
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap sentence : sentences) {
                for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    queryStr += (token.get(CoreAnnotations.LemmaAnnotation.class) + " ");
                }
            }

        // Else run normally
        } else {
            queryStr = "\"";
            String[] tokens = question.split(" ");

            for (String token : tokens) {
                queryStr += (" " + token.toLowerCase());
            }

            queryStr += "\"";
        }

        try {
            return new QueryParser("content", this.analyzer).parse(QueryParser.escape(queryStr));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}