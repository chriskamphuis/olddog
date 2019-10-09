package nl.ru.convert;

import org.kohsuke.args4j.Option;

class Args {
    // required arguments
    @Option(name = "-index", required = true, usage="Index path")
    String index;

    @Option(name = "-docs", required = true, usage="Filename for docs")
    String docs;

    @Option(name = "-dict", required = true, usage="Filename for dict")
    String dict;

    @Option(name = "-terms",required = true, usage="Filename for terms")
    String terms;

    // optional arguments
    @Option(name = "-inmem", usage = "Boolean switch to read index in memory")
    Boolean inmem = false;

    @Option(name = "-adpt", usage = "Boolean switch to also get term specific k1 values")
    Boolean adpt = false;

    @Option(name = "-pos", usage = "Return positions instead of sums")
    Boolean pos = false;
}
