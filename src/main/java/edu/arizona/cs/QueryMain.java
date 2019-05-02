package edu.arizona.cs;

import java.util.*;
import java.io.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Query;

/**
 * File: QueryMain.java
 * Author: Dilen Govin
 * 
 * Queries the index based on the questions
 */

public class QueryMain 
{

    public static void main(String[] args)
    {
        if (args.length < 2) {
            System.err.println("ERROR: Incorrect amount of arguments");
            System.exit(1);
        }

        String sourceQuestions = args[0];
        String sourceIndexPath = args[1];
        boolean lemmatize = Boolean.parseBoolean(args[2]);
        boolean stem = Boolean.parseBoolean(args[3]);

        System.out.printf("Retrieving questions from %s\n", sourceQuestions);
        QueryEngine queryEngine = new QueryEngine(sourceQuestions, sourceIndexPath, lemmatize, stem);
        HashMap<String, String> questions = queryEngine.extractQuestions();

        double asked = 0;
        double correct = 0;
        double mrr = 0.0;
        for (String question : questions.keySet()) {
            Double score = queryEngine.query(question, questions.get(question));
            asked++;

            if (score < 1.0)
                mrr += score;
            else
                correct++;
        }

        System.out.printf("Questions Asked: %f\n", asked);
        System.out.printf("Questions Correct: %f\n", correct);
        System.out.printf("Percent of questions correct: %.2f%%\n", correct / asked * 100);
        System.out.printf("Mean Reciporcal Rank for index: %f\n", mrr / asked);

    }
}