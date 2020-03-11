package nl.ru.convert;

import org.kohsuke.args4j.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

public class CiffToTermsBulk {

  public static class Args {
    @Option(name = "-input", metaVar = "[file]", required = true, usage = "postings file")
    public String input = "";

    @Option(name = "-termIDFile", metaVar = "[file]", required = true, usage = "Filename for termID column data")
    public String termIDFile = "";

    @Option(name = "-docIDFile", metaVar = "[file]", required = true, usage = "Filename for docID column data")
    public String docIDFile = "";

    @Option(name = "-countFile", metaVar = "[file]", required = true, usage = "Filename for count column data")
    public String countFile = "";
  }

  public static void main(String[] argv) throws Exception {
    CiffToTermsBulk.Args args = new CiffToTermsBulk.Args();
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

    FilterOutputStream termIDWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.termIDFile)));
    FilterOutputStream docIDWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.docIDFile)));
    FilterOutputStream countWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.countFile)));

    CommonIndexFileFormat.Header header = CommonIndexFileFormat.Header.parseDelimitedFrom(fileIn);
    ByteBuffer buffer;

    for (int termID=0; termID<header.getNumPostingsLists(); termID++) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl.getDf() != pl.getPostingsCount()) {
        throw new RuntimeException(String.format(
          "Unexpected number of postings! expected %d got %d", pl.getDf(), pl.getPostingsCount()));
      }
      int docID = 0;
      for (int j=0; j< pl.getDf(); j++) {
        docID += pl.getPostings(j).getDocid();

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(termID);
        termIDWriter.write(buffer.array());

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(docID);
        docIDWriter.write(buffer.array());

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(pl.getPostings(j).getTf());
        countWriter.write(buffer.array());
      }
      termIDWriter.flush();
      docIDWriter.flush();
      countWriter.flush();
    }
    termIDWriter.close();
    docIDWriter.close();
    countWriter.close();
  }
}
