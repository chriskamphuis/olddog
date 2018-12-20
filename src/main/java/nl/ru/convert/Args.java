package nl.ru.convert;

import org.kohsuke.args4j.Option;

public class Args {
    // required arguments
    @Option(name = "-index", metaVar = "[Path]", required = true, usage="Index path")
    public String index;

    // optional arguments
    @Option(name = "-inmem", usage = "Boolean switch to read index in memory")
    public Boolean inmem = false;
}
