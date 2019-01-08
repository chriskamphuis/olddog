package nl.ru.convert;

import org.kohsuke.args4j.Option;

class Args {
    // required arguments
    @Option(name = "-index", metaVar = "[Path]", required = true, usage="Index path")
    String index;

    // optional arguments
    @Option(name = "-inmem", usage = "Boolean switch to read index in memory")
    Boolean inmem = false;
}
