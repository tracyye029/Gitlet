# Gitlet Design Document

**Name**: Yuxin Ye

## Classes and Data Structures
1. Gitlet class: implements several command methods to be called by Main.
   1. Fields:
      3. File GITLET_DIC: a working directory
      4. File BLOB_FOLDER, COMMITS_FOLDER, STAGING_FOLDER, BRANCH_FOLDER, HEADBRANCH_FOLDER
2. Commit class:  
   1. Fields: 
   2. private String _logMessage;
   3. 
      private String _timeStamp;
   4. 
      private String _mySHA;
   5. 
      private String _parentSHA;
   6. 
      private LinkedHashMap <String, String> _blobs; //<fileName, SHA>

   
4. StagingArea: place to add/remove files
   1. Fields: 
      1. LinkedHashmap<String, String> added: stores files added
      2. LinkedHashmap<String, String> removed: stores files removed.
      

## Algorithms
Gitlet class: implements serializable.
1. GitLet(): the class constructor. Set up directories
2. void init(): Initialize git. Make the first commit.
3. add: Add a copy of a file to the staging area.
   
-if current commit's blobMap has the same file name but different blob ID(different contents of the file):
      
-don't add to the staging area. If staging removal contains the file name, remove it. Update and store the staging area

-if same file name/different file name, you want to add it.

4. commit: Creates a new commit object and adds it to commit hashmap

-get the parent SHA, find the corresponding commit file in the commit folder, copy the parent's blobMap and then put the files in the staging area into the blobMap. 

5. rm: Removes a blob to added

-if the current commit contains the file, delete the file, add the file to the staging area removal, remove the file from staging area addition if it is there.

-if the current commit doesn't contain the file, but the staging area addition contains the file, remove it from the addition area.

-if neither the current commit doesn't contain the file nor the staging area addition contains the file, print error message.

6. log: Prints out all the commits from most recent to initial commit
7. global log: Prints out commit id, date, and message for all commits
8. find: Prints out all commit ids that match the commit message, one per line
9. status: Marks current branch *, displays current existing branches, display all files(staged files, unstaged files)
10. checkout: Three overloaded checkout methods:
    1. File name: takes the version of the file as it exists in the head commit and puts it in the working directory.
       1. If the current commit's blobMap doesn't the file name, print out the error message "File does not exist in that commit." and exists.
       2. else, if the file already exist in the working directory, find the blob hashvalue, read the contents, and write the contents to the file.
       3. 
    2. Commit id, File name: takes the file that matches the commit id and overwrites the contents of the file if it has existed in the working directory.
    3. Branch name: takes all files in the commit at the head of the input branch, put them in the working directory, overwrites the files if they are already exist.
11. branch: Create a new branch
12. rm-branch: removes a branch from the branchesMap
13. merge: Merge files from the current branch to the given branch
14. reset: Checkout back to the initial commit

Commit class: implements serializable.
1. Commit(): the class constructor. Initialize instance variables.
2. getMySHA(), getParentSHA(), getMessage(), getTime(), getBlobs()


## Persistence
Create several folder to store files: BLOB_FOLDER, COMMIT_FOLDER, STAGING_FOLDER, BRANCH_FOLDER
We can use object serialization to perform persistence. After a serialized object has been written into a file, it can be read from the file and deserialized, that is, the bytes can be used to recreate the object.
Class ObjectInputStream and ObjectOutputStream mat be useful here.
