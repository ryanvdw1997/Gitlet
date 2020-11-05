# Gitlet Design Document

**Name**: Ryan Van de Water

## Classes and Data Structures

#Blob
##### My blob object, takes in a File and holds the contents
**Fields**
1) String contents - the contents of the file that the blob represents
2) String blobID - the sha1 ID of the blob object

#StagingArea
##### My staging area for files
**Fields**
1) HashMap<String, Blob> addMap - the String: fileName, Blob the contents of the
respective file
2) HashMap<String, Blob> removeMap - the String: fileName, Blob: the contents of the
respective file

#Commit
##### My commit object
**Fields**
1) Date timestamp - denotes when the commit happened
2) String message - the message determined by the client
3) HashMap<String, Blob> tracking - is present to aid in denoting which file contents are being tracked
4) Commit parent - denotes the latest commit before this one that was made
5) String commitID - sha1 code for this commit object

#CommitList
##### My LinkedList of Commit objects
**Fields**
1) Commit HEAD - references which commit was last made 
2) String commitListID - sha1 code for this commitList object

#Branch
##### My branch object
**Fields**
1) String name - references the name of the branch
2) CommitList cList - references the commitList object that it is following
3) Boolean head - denotes whether we are making changes along this branch or not





## Algorithms
1) `init` : <br/>a) initializes the .gitlet repository with directories,
objects, index, refs, and logs.<br/> b) We then create the files: "Stage" (index folder) 
and "logText" (logs folder), the folders:
commits (objects folder), CommitLists (objects folder) and branches (refs folder). <br/>
c) From there we create the respective objects that will be placed into these folders.
Objects include - our initial commit, a CommitList with initial commit at the HEAD, 
the master branch which references the CommitList. We also fill our "logText" file
with the appropriate log message and fill our "Stage" file with an empty StagingArea
object.

2) `add` : <br/>a) update the addMap of the StagingArea object saved in the "Stage" file
under the index folder.<br/>
b) put each String in args as well as the files contents
into the addMap.

3) `commit` : <br/>
    a) Find the branch whose head is true aka which branch we are following<br/>
    b) Get the commitList from that branch<br/>
    c) get the head of this commitList and set it equal to a Commit that is toBeAdded<br/>
    d) update the commit with the respective instance variables<br/>
    e) update the commitList by adding to it the new commit object toBeAdded<br/>
    f) update the branch by updating the commitList it references<br/>
    g) write those updated branch and commitList objects into their respective folders and files<br/>
    h) update the logText file in the logs folder<br/>
    i) add the new commit into the commits folder<br/>


## Persistence
Create a folder that accesses the files in the CWD

