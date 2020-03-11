package nl.ru.convert;

import org.kohsuke.args4j.*;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class CiffToCsv {

  public static class Args {
    @Option(name = "-input", metaVar = "[file]", required = true, usage = "postings file")
    public String input = "";

    @Option(name = "-dict", metaVar = "[file]", required = true, usage = "output dict file")
    public String dict = "";

    @Option(name = "-terms", metaVar = "[file]", required = true, usage = "output terms file")
    public String terms = "";

    @Option(name = "-docs", metaVar = "[file]", required = true, usage = "output docs file")
    public String docs = "";
  }

  public static void main(String[] argv) throws Exception {
    CiffToCsv.Args args = new CiffToCsv.Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ExportToOldDog " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    InputStream fileIn;
    if (args.input.endsWith(".gz")) {
      fileIn = new GZIPInputStream(new FileInputStream(args.input));
    } else {
      fileIn = new FileInputStream(args.input);
    }

    BufferedWriter dictWriter = new BufferedWriter(new FileWriter(args.dict));
    BufferedWriter termsWriter = new BufferedWriter(new FileWriter(args.terms));
    BufferedWriter docsWriter = new BufferedWriter(new FileWriter(args.docs));

    CommonIndexFileFormat.Header header = CommonIndexFileFormat.Header.parseDelimitedFrom(fileIn);

    // Create terms + dict files
    for (int termID=0; termID<header.getNumPostingsLists(); termID++) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl.getDf() != pl.getPostingsCount()) {
        throw new RuntimeException(String.format(
          "Unexpected number of postings! expected %d got %d", pl.getDf(), pl.getPostingsCount()));
      }
      dictWriter.write(Integer.toString(termID) + '|' + pl.getTerm() + '|' + pl.getDf());
      dictWriter.newLine();
      int docID = 0;
      for (int j=0; j< pl.getDf(); j++) {
        docID += pl.getPostings(j).getDocid();
        termsWriter.write(Long.toString(termID) + '|' + docID + '|' + Long.toString(pl.getPostings(j).getTf()));
        termsWriter.newLine();
      }
    }
    dictWriter.close();
    termsWriter.close();

    // Create docs file
    for (int i=0; i<header.getNumDocs(); i++) {
      CommonIndexFileFormat.DocRecord docRecord = CommonIndexFileFormat.DocRecord.parseDelimitedFrom(fileIn);
      docsWriter.write(docRecord.getCollectionDocid() + '|' + docRecord.getDocid() + '|' + docRecord.getDoclength());
      docsWriter.newLine();
    }
    docsWriter.close();
    fileIn.close();
  }
}
