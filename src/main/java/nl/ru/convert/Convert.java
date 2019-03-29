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

public class Convert {

    private Convert(Args args) throws IOException {

        Path indexPath = Paths.get(args.index);
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(args.index + " does not exist or is not a directory.");
        }

        FileOutputStream docsDocIdWriter = new FileOutputStream(args.docs + "_docID");
        FileOutputStream lenWriter = new FileOutputStream(args.docs + "_len");

        FileOutputStream termsTermIdWriter = new FileOutputStream(args.terms + "_termID");
        FileOutputStream termsDocIDWriter = new FileOutputStream(args.terms + "_docID");
        FileOutputStream termsPosCountWriter = new FileOutputStream(args.terms+ "_count");

        FileOutputStream dictTermIdWriter = new FileOutputStream(args.dict + "_termID");
        BufferedWriter dictTermWriter = new BufferedWriter(new FileWriter(args.dict + "_term"));
        FileOutputStream dictDfWriter = new FileOutputStream(args.dict+ "_df");

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

        for (int i=0; i < reader.maxDoc(); i++){
            docsDocIdWriter.write(i);
            lenWriter.write((int) reader.getTermVector(i, "contents").size());
        }

        for (LeafReaderContext lrc : readers) {
            int termID = 1;
            Terms terms = lrc.reader().terms("contents");
            TermsEnum termsEnum = terms.iterator();
            PostingsEnum postingsEnum = null;
            while (termsEnum.next() != null) {
                String term = termsEnum.term().utf8ToString();
                int df = termsEnum.docFreq();

                dictTermIdWriter.write(termID);
                dictTermWriter.write(term);
                dictTermWriter.newLine();
                dictDfWriter.write(df);

                postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);
                while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                    if (args.pos) {
                        for (int i = 0; i < postingsEnum.freq(); i++) {
                            termsTermIdWriter.write(termID);
                            termsDocIDWriter.write(postingsEnum.docID());
                            termsPosCountWriter.write(postingsEnum.nextPosition());
                        }
                    } else {
                        termsTermIdWriter.write(termID);
                        termsDocIDWriter.write(postingsEnum.docID());
                        termsPosCountWriter.write(postingsEnum.freq());
                    }
                }
                termID += 1;
            }
        }

        docsDocIdWriter.flush();
        lenWriter.flush();
        termsTermIdWriter.flush();
        termsDocIDWriter.flush();
        termsPosCountWriter.flush();
        dictTermIdWriter.flush();
        dictTermWriter.flush();
        dictDfWriter.flush();

        docsDocIdWriter.close();
        lenWriter.close();
        termsTermIdWriter.close();
        termsDocIDWriter.close();
        termsPosCountWriter.close();
        dictTermIdWriter.close();
        dictTermWriter.close();
        dictDfWriter.close();
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