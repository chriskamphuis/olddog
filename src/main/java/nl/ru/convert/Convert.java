package nl.ru.convert;

import org.apache.lucene.codecs.blocktree.FieldReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.kohsuke.args4j.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Convert {

    private final Args args;
    private final IndexReader reader;

    public Convert(Args args) throws IOException {
        this.args = args;
        Path indexPath = Paths.get(this.args.index);
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(args.index + " does not exist or is not a directory.");
        }

        if (args.inmem) {
            this.reader = DirectoryReader.open(MMapDirectory.open(indexPath));
        } else {
            this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        }

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
        new Convert(convertArgs);
    }
}