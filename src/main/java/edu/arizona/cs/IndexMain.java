package edu.arizona.cs;

/**
 * File: WatsonMain.java
 * Author: Dilen Govin
 * 
 * Main class to run the Watson simulator from
 * TODO: Update this
 */

public class IndexMain
{
    public static void main(String[] args)
    {
        if (args.length < 2) {
            System.err.println("ERROR: Incorrect amount of arguments");
            System.exit(1);
        }

        String sourceDirectory = args[0];
        String sourceIndexPath = args[1];
        boolean lemmatization = Boolean.parseBoolean(args[2]);
        boolean stem = Boolean.parseBoolean(args[3]);

        System.out.printf("Building Index from file(s) in %s\n", sourceDirectory);
        
        // Index the documents
        IndexBuilder indexBuilder = new IndexBuilder(sourceDirectory, sourceIndexPath, lemmatization, stem);
        indexBuilder.build();
    }
}
