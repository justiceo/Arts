The entire program runs on basically three files: fragment, svngit, and merge

## Fragment
  First run fragment to break up a log into fragments
  so, to fragment avro.log into 3 files do: fragment avro.log 3
  this produces file with .1, .2, .3 extensions

## svngit
  Then run svngit with the filename, url (optional, please check generated script first), and an instanceID (for multi-threading purposes)
  so, to get the git version of the fragments generated above do: svngit avro http://svn.org/.../avro... {1,2,3}
  this produces files with .git extension
  
## Merge
  Final run merge to put the files together. 
  sample run is: merge file1 file2 file3
  this produces a file with the first filename and .merge extension

##  ************************
  To check number of commits in avro.log.git (the final file)
  egrep '?commit' < avro.log.git | wc -l

  To check number of revisions (equivalent of commit) in avro.log ( the original file)
  egrep "revision=" < avro.log | wc -l

svnGet is bash file that processes diff in commits

svnlog.file is a sample commit diff file I use for debugging

svn.bash is the bash version of the java program (incomplete)
