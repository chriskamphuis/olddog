package nl.ru.convert;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Convert {

    private HashMap<Integer, Integer> docs;
    private HashMap<Integer, Pair<String, Integer>> dict;
    private ArrayList<Triple<Integer, Integer, Integer>> terms;

    private Convert(Args args) throws IOException {

        Path indexPath = Paths.get(args.index);
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(args.index + " does not exist or is not a directory.");
        }

        this.docs = new HashMap<>();
        this.dict = new HashMap<>();
        this.terms = new ArrayList<>();

        IndexReader reader;
        if (args.inmem) {
            reader = DirectoryReader.open(MMapDirectory.open(indexPath));
        } else {
            reader = DirectoryReader.open(FSDirectory.open(indexPath));
        }

        int termIdTemp = 0;

        // Variable names according to the Old Dogs paper
        HashMap<String, Integer> termIdMap = new HashMap<>();


        for (int i = 0; i < reader.maxDoc(); i++) {
            Terms termsVector = reader.getTermVector(i, "contents");
            reader.document(i);
            TermsEnum termsEnum = termsVector.iterator();
            PostingsEnum postings = null;
            docs.put(i, 0);
            while (termsEnum.next() != null) {
                postings  = termsEnum.postings(postings, PostingsEnum.ALL);

                docs.put(i, docs.get(i) + 1);
                String termString = termsEnum.term().utf8ToString();
                int termId;
                if (termIdMap.get(termString) == null) {
                    termId = termIdTemp;
                    termIdTemp++;
                } else {
                    termId = termIdMap.get(termString);
                }
                termIdMap.put(termString, termId);
                if (dict.get(termId) == null) {
                    dict.put(termId, new Pair<>(termString, 1));
                } else {
                    int value = dict.get(termId).getSecond();
                    dict.put(termId, new Pair<>(termString, value + 1));
                }
                while(postings.nextDoc() != PostingsEnum.NO_MORE_DOCS){
                    int count = postings.freq();
                    Triple<Integer, Integer, Integer> row = new Triple<>(termId, i, count);
                    terms.add(row);
                }
            }
        }
    }

    private void writeToFile() {

    }

    public static void main(String[] args) throws Exception {
        Args convertArgs = new Args();
        CmdLineParser parser = new CmdLineParser(convertArgs, ParserProperties.defaults().withUsageWidth(90));
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.err);
            System.err.println("Example: "+ Convert.class.getSimpleName() +
                    parser.printExample(OptionHandlerFilter.REQUIRED));
            return;
        }
        Convert convert = new Convert(convertArgs);
        convert.writeToFile();
    }
}