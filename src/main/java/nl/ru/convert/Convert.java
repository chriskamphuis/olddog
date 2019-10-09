package nl.ru.convert;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.SmallFloat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Convert {

    private Convert(Args args) throws IOException {

        final double b = 0.4;

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

        long totalLength = 0;
        long numDocs = 0;
        for (int i=0; i < reader.maxDoc(); i++){
            String collectionDocumentID = reader.document(i).getField("id").stringValue();
            if(reader.getTermVector(i, "contents") == null){
                docsWriter.write(collectionDocumentID + "|" + i + "|" + 0 + "|" + 0);
                docsWriter.newLine();
                continue;
            }
            long length = reader.getTermVector(i, "contents").getSumTotalTermFreq();
            numDocs += 1;
            totalLength += length;
            docsWriter.write(collectionDocumentID + "|" + i + "|" + length + "|" + SmallFloat.longToInt4(length));
            docsWriter.newLine();
        }
        docsWriter.flush();

        float avgdl = totalLength / (float) numDocs;

        LeafReaderContext lrc = readers.get(0);
        int termID = 0;
        Terms terms = lrc.reader().terms("contents");
        TermsEnum termsEnum = terms.iterator();
        PostingsEnum postingsEnum = null;

        while (termsEnum.next() != null) {
            String term = termsEnum.term().utf8ToString();
            int df = termsEnum.docFreq();

            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);
            PriorityQueue<Double> freqs = new PriorityQueue<>();

            while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                int luceneID = postingsEnum.docID();
                freqs.add((postingsEnum.freq()) / (1 - b + b * (reader.getTermVector(luceneID, "contents").getSumTotalTermFreq()) / (avgdl)));
                if (args.pos) {
                    for (int i = 0; i < postingsEnum.freq(); i++) {
                        termsWriter.write(termID + "|" + luceneID + "|" + postingsEnum.nextPosition());
                        termsWriter.newLine();
                    }
                } else {
                    termsWriter.write(termID + "|" + luceneID + "|" + postingsEnum.freq());
                    termsWriter.newLine();
                }
            }

            if(args.adpt) {
                ArrayList<Long> dfs = new ArrayList<>();
                dfs.add(numDocs);
                dfs.add((long) termsEnum.docFreq());
                int t = 2;
                while (freqs.size() > 0) {
                    while (freqs.peek() < t - 0.5) {
                        freqs.poll();
                        if (freqs.peek() == null) {
                            break;
                        }
                    }
                    dfs.add((long) freqs.size());
                    t++;
                }

                ArrayList<Double> ig = new ArrayList<>();
                Double base = -Math.log((dfs.get(1) + 0.5) / (dfs.get(0) + 1));
                t = 1;
                ig.add(0D);
                while (true) {
                    if (dfs.size() == t + 1) {
                        break;
                    }
                    Double gain = Math.log((dfs.get(t + 1) + 0.5) / (dfs.get(t) + 1));
                    if (ig.get(t - 1) >= base + gain) {
                        ig.add(base + gain);
                        break;
                    }
                    ig.add(base + gain);
                    t++;
                }

                double end = 10.0;
                double best_score = Double.MAX_VALUE;
                double jump = 1.0;
                double max_error = 0.001;
                double where = max_error;

                while (Math.abs(jump) > max_error) {
                    if(where > Math.abs(jump) + end || ig.size()-1 <= 2) {
                        break;
                    }
                    double score = 0.0;
                    for (int i = 2; i < ig.size()-1; i++) {
                        double a = ig.get(i) / ig.get(1);
                        score += Math.pow((a - ((where + 1) * i / (where + i))), 2);
                    }
                    if(score > best_score) {
                        jump /= 2;
                        if(where + jump < 0) {
                            jump = -jump;
                        }
                        double new_score = 0.0;
                        for (int i = 2; i < ig.size()-1; i++) {
                            double a = ig.get(i) / ig.get(1);
                            new_score += Math.pow((a - ((where + jump + 1) * i / (where + jump + i))), 2);
                        }
                        if(new_score > score) {
                            jump = -jump;
                        } else {
                            where += jump;
                            best_score = new_score;
                        }
                    } else {
                        where  += jump;
                        best_score = score;
                    }
                }
                double k_hat = where;

                dictWriter.write(termID + "|" + term + "|" + df + "|" + String.format("%.3f", k_hat) + "|" + String.format("%.3f", ig.get(1)));
            } else {
                dictWriter.write(termID + "|" + term + "|" + df);
            }
            dictWriter.newLine();
            termID += 1;
        }

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