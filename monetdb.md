# MonetDB

OldDog works fine with MonetDB installed from the standard repositories.

For efficiency testing however, building MonetDB from source is the preferred route, for more control over specific settings.
Thanks to Spinque's Roberto Cornacchia for some insightful information that resulted in this guide.

## Preliminaries

First, review the documentation by the MonetDB team on [compilation from source](https://www.monetdb.org/Developers/SourceCompile).

Download [the latest source release](https://www.monetdb.org/downloads/sources/Latest) and unpack the archive:

    wget https://www.monetdb.org/downloads/sources/Apr2019-SP1/MonetDB-11.33.11.tar.xz
    tar xvf MonetDB-11.33.11.tar.xz

Install dependencies - only the first (`gettext`) is mandatory:

    sudo dnf install gettext-devel
    sudo dnf install readline-devel
    sudo dnf install unixODBC-devel
    sudo dnf install xz-devel lz4-devel snappy-devel
    sudo dnf install libcurl-devel libxml2-devel 
    sudo dnf install python3-devel python3-numpy R-devel
    sudo dnf install proj-devel geos-devel liblas-devel cfitsio-devel samtools-devel

Compile the release from source (add `--prefix=/path/to/install` for installing locally):

    ./bootstrap
    ./configure --disable-debug --disable-developer --disable-assert --enable-optimize
    make -j
    make install

## Performance Tuning

### Indices

MonetDB uses: `hashes`, `imprints`, `orderidx`. 

Do not forget the "implicit" index that always works: 
store tables sorted on the column you expect to select the most on.
For example, Spinque stores `TD` sorted on `T`. 

If a table is not sorted (or MonetDB does not know about its sort order) and no other index has been created, then MonetDB will highly likely create a hash upon the first selection.
Hopefully, this will be persisted. If it is persisted, all subsequent selections (normally obtained as a join with query terms in a different table) will be blazing fast.
Hashes are created and made persistent on demand; unfortunately, there is no obvious way to control this process.

Access structures `orderidx` or `imprints` may not help selections from `TD` because these are optimized for inequality or range queries, not for equality queries.
These indices can be created using SQL statements:

```
sql>\h CREATE INDEX
command  : CREATE INDEX
syntax   : CREATE [ UNIQUE | ORDERED | IMPRINTS ] INDEX ident ON qname '(' ident_list ')'
```

Mind that even if you are allowed to create these indices, MonetDB will still decide on its own whether to use them and in some cases even whether to create them at all.

More info about [indices in MonetDB](https://www.monetdb.org/Documentation/Manuals/SQLreference/Indices).

### Optimizers

_TODO:_

See the [optimizer pipelines](https://www.monetdb.org/Documentation/Cookbooks/SQLrecipes/OptimizerPipelines).

Spinque uses the optimizer pipeline without parallelism, because we have many concurrent sessions on one server.
For 1-session experiments, the parallelism created by the default optimizer is expected to help.

### UDFs

See the [Mercurial repo for extending MonetDB](https://www.monetdb.org/hg/MonetDB-extend/) (navigate to [code](https://www.monetdb.org/hg/MonetDB-extend/file/tip)).


