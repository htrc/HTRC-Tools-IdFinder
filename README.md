# HTRC-Tools-IdFinder
Finds the HTRC volume ID(s) for each title/author pair in a CSV file

# Usage
```
idfinder 1.0-SNAPSHOT
edu.illinois.i3.htrc.apps.idfinder
  -a, --author  <arg>        The (zero-based) index of the column containing the
                             authors (default = 0)
  -i, --input  <arg>         The CSV file containing the title/author of the
                             volumes to look up
  -m, --max-results  <arg>   The max number of results to retrieve per query
                             (default = 1000000)
  -o, --output  <arg>        The output CSV file containing the extra ID column
  -s, --solr  <arg>          The SOLR endpoint to use
                             (default = http://chinkapin.pti.indiana.edu:9994/solr/meta)
  -t, --title  <arg>         The (zero-based) index of the column containing the
                             titles (default = 1)
      --help                 Show help message
      --version              Show version of this program
```
