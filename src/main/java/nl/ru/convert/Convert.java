package nl.ru.convert;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Convert {

    // Variable names according to the Old Dogs paper
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
        HashMap<String, Integer> termIdMap = new HashMap<>();

        for (int i = 0; i < reader.maxDoc(); i++) {
            Terms termsVector = reader.getTermVector(i, "contents");
            reader.document(i);
            TermsEnum termsEnum = termsVector.iterator();
            this.docs.put(i, 0);
            while (termsEnum.next() != null) {
                PostingsEnum postings = termsEnum.postings(null, PostingsEnum.ALL);

                this.docs.put(i, this.docs.get(i) + 1);
                String termString = termsEnum.term().utf8ToString();
                int termId;
                if (termIdMap.get(termString) == null) {
                    termId = termIdTemp;
                    termIdTemp++;
                } else {
                    termId = termIdMap.get(termString);
                }
                termIdMap.put(termString, termId);
                if (this.dict.get(termId) == null) {
                    this.dict.put(termId, new Pair<>(termString, 1));
                } else {
                    int value = this.dict.get(termId).getSecond();
                    this.dict.put(termId, new Pair<>(termString, value + 1));
                }
                while(postings.nextDoc() != PostingsEnum.NO_MORE_DOCS){
                    int count = postings.freq();
                    Triple<Integer, Integer, Integer> row = new Triple<>(termId, i, count);
                    terms.add(row);
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void writeDocsToFile(String filename) throws IOException {
        String filenameDocId = filename + "_docID";
        String filenameLen = filename + "_len";

        BufferedWriter docIdWriter = new BufferedWriter(new FileWriter(filenameDocId));
        BufferedWriter lenWriter = new BufferedWriter(new FileWriter(filenameLen));

        for (int key : this.docs.keySet()) {
            docIdWriter.write(key);
            lenWriter.write(this.docs.get(key));
        }

        docIdWriter.flush();
        docIdWriter.close();
        lenWriter.flush();
        lenWriter.close();
    }

    @SuppressWarnings("Duplicates")
    private void writeDictToFile(String filename) throws IOException {
        BufferedWriter termIdWriter = new BufferedWriter(new FileWriter(filename + "_termID"));
        BufferedWriter termWriter = new BufferedWriter(new FileWriter(filename + "_term"));
        BufferedWriter dfWriter = new BufferedWriter(new FileWriter(filename+ "_df"));

        for (Integer key : this.dict.keySet()) {
            Pair value = this.dict.get(key);
            termIdWriter.write(key);
            termWriter.write(value.getFirst().toString()+'\n');
            dfWriter.write((int) value.getSecond());
        }

        termIdWriter.flush();
        termIdWriter.close();
        termWriter.flush();
        termWriter.close();
        dfWriter.flush();
        dfWriter.close();
    }

    @SuppressWarnings("Duplicates")
    private void writeTermsToFile(String filename) throws IOException{
        BufferedWriter termIdWriter = new BufferedWriter(new FileWriter(filename + "_termID"));
        BufferedWriter docIDWriter = new BufferedWriter(new FileWriter(filename + "_docID"));
        BufferedWriter countWriter = new BufferedWriter(new FileWriter(filename+ "_count"));

        for (Triple value : this.terms) {
            termIdWriter.write((int) value.getFirst());
            docIDWriter.write((int) value.getSecond());
            countWriter.write((int) value.getThird());
        }

        termIdWriter.flush();
        termIdWriter.close();
        docIDWriter.flush();
        docIDWriter.close();
        countWriter.flush();
        countWriter.close();
    }

    public static void main(String[] args) throws IOException {
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
        convert.writeDocsToFile(convertArgs.docs);
        convert.writeDictToFile(convertArgs.dict);
        convert.writeTermsToFile(convertArgs.terms);
    }
}