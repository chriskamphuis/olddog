# MonetDB

OldDog works fine with MonetDB installed from the standard repositories.

For proper efficiency testing however, tuning the server is necessary. Many thanks to [Spinque](http://www.spinque.com) 
and specifically Roberto Cornacchia for the insightful information that resulted in this guide.

## Building from source

Building MonetDB from source is the preferred route, for more control over specific settings.

Especially on a system with `SELinux` enabled, this is non-trivial; best to follow [these instructions](monetdb-from-source.html).

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


