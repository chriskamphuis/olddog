# MonetDB from Source

Instructions for building MonetDB from source on a Fedora Core 30 Linux system with SELinux enabled.

## Preliminaries

First, review the documentation by the MonetDB team on [compilation from source](https://www.monetdb.org/Developers/SourceCompile).

## Source and dependencies

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

Handling SELinux specifics:

    sudo dnf install selinux-policy-devel checkpolicy rpm-build

For installing your locally build version of MonetDB globally, it may be wise to first remove the installed version (if any):

     sudo dnf rm MonetDB-SQL-server5 MonetDB-SQL-server5-hugeint MonetDB-client MonetDB-stream

## Compiling the codebase

Compile the release from source (add `--prefix=/path/to/install` for installing locally):

    ./bootstrap
    ./configure --disable-debug --disable-developer --disable-assert --enable-optimize
    make -j

Want to compile using `CLANG` instead of ` gcc`? Prepend prepend `CC=clang` to the `configure` command.
Want to include python and exclude R: append `--disable-rintegration --enable-pyintegration`.

Skip the usual `make install` and build the `rpm`-s instead - this includes an `rpm` that applies all the SELinux policies MonetDB needs.

    make rpm

## Installing MonetDB

Install those in two steps, such that all directories that need policies have indeed been created (SELinux `rpm` is under `noarch`):

    find rpmbuild/RPMS/x86_64 -name \*.rpm | xargs sudo dnf --disablerepo="*" install --skip-broken -y
    find rpmbuild/RPMS/noarch -name \*.rpm | xargs sudo dnf --disablerepo="*" install --skip-broken -y

Start the server:

    sudo systemctl status monetdbd

You can uninstall the `rpm`-s with some more shell magic:

    find rpmbuild/RPMS -name \*.rpm | sed -e 's/.*\/\(MonetDB.\+fedora30\)\(\.x86.64\|\.noarch\)\?\.rpm/\1/g' | xargs sudo dnf --disablerepo="*" -y rm

## SELinux tips

In case you are struggling with SELinux, e.g. when setting your `dbfarm` on a different disk, the following basics may help.

You can check assigned filecontexts using:

    sudo ls -alRZ /var/monetdb5

Info for the contexts you need is found in `monetdb.fc`:

    grep monetdb5 /usr/share/doc/MonetDB-selinux/monetdb.fc

Subsequently install those `fcontext`-s and apply them as follows, e.g., for `.merovingian_properties` you would do:

    sudo semanage fcontext -a -t monetdbd_etc_t /var/monetdb5/.merovingian_properties
    sudo restorecon -v /var/monetdb5/.merovingian_properties

## Issues

I did not resolve all problems I encountered; some more issues include:

+ `make -j rpm` does not seem to work correctly.
+ `MonetDB-client-test` package does not want to install with the above command due to the `--disablerepos` option on `dnf`.
+ SELinux complained about access to resource `cpu` - fixed by using the suggested `audit` in the error message.

