package nl.ru.convert;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;

public class Convert {

    private Convert(Args args) throws IOException {

        Path indexPath = Paths.get(args.index);
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(args.index + " does not exist or is not a directory.");
        }

        BufferedWriter docsWriter = new BufferedWriter(new FileWriter(args.docs));
        BufferedWriter termsWriter = new BufferedWriter(new FileWriter(args.terms));
        BufferedWriter dictWriter = new BufferedWriter(new FileWriter(args.dict));

        IndexReader reader;
        if (args.inmem) {
            reader = DirectoryReader.open(MMapDirectory.open(indexPath));
        } else {
            reader = DirectoryReader.open(FSDirectory.open(indexPath));
        }

        List<LeafReaderContext> readers = reader.leaves();

        if(readers.size() != 1) {
            throw new RuntimeException("There should be only one leaf, index the collection using one writer");
        }

        HashMap<Integer, String> IDMapper = new HashMap<>();

        for (int i=0; i < reader.maxDoc(); i++){
            String collectionDocumentID = reader.document(i).getField("id").stringValue();
            IDMapper.put(i, collectionDocumentID);
            docsWriter.write(collectionDocumentID + "|" + reader.getTermVector(i, "contents").size());
            docsWriter.newLine();
        }

        for (LeafReaderContext lrc : readers) {
            int termID = 1;
            Terms terms = lrc.reader().terms("contents");
            TermsEnum termsEnum = terms.iterator();
            PostingsEnum postingsEnum = null;
            while (termsEnum.next() != null) {
                String term = termsEnum.term().utf8ToString();
                int df = termsEnum.docFreq();

                dictWriter.write(termID + "|" + term + "|" + df);
                dictWriter.newLine();

                postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);
                while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                    String collectionDocumentID = IDMapper.get(postingsEnum.docID());
                    if (args.pos) {
                        for (int i = 0; i < postingsEnum.freq(); i++) {
                            termsWriter.write(termID + "|" + collectionDocumentID + "|" + postingsEnum.nextPosition());
                            termsWriter.newLine();
                        }
                    } else {
                        termsWriter.write(termID + "|" + collectionDocumentID + "|" + postingsEnum.freq());
                        termsWriter.newLine();
                    }
                }
                termID += 1;
            }
        }

        docsWriter.flush();
        dictWriter.flush();
        termsWriter.flush();

        docsWriter.close();
        dictWriter.close();
        termsWriter.close();
    }

    public static void main(String[] args) throws IOException {
        Args convertArgs = new Args();
        CmdLineParser parser = new CmdLineParser(convertArgs, ParserProperties.defaults().withUsageWidth(90));
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.err);
            System.err.println("Example: " + Convert.class.getSimpleName() +
                    parser.printExample(OptionHandlerFilter.REQUIRED));
            return;
        }
        new Convert(convertArgs);
    }
}