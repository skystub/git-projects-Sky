import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BlobTester {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String gitPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git";
        String content = "hello world and everyone in it!";
        String correctHash = "88d9814d5c99271752f74fae7f363230a68e06b7"; //using online sha-1 hash
        //blobValidation(gitPath, content, correctHash);
        //directoryValidation(gitPath);
    }

    public static void directoryValidation(String gitRepoPath) throws IOException, NoSuchAlgorithmException{
        Path currentDir = Paths.get(".").toAbsolutePath().normalize();

        if (Files.exists(currentDir.resolve("testFolder"))) {
            System.out.println("testFolder already exists.");
        } else {
            Path testDir = Files.createDirectory(currentDir.resolve("testFolder"));
            
            Path dir1 = Files.createDirectory(testDir.resolve("dir1"));
            Path file1 = Files.createFile(dir1.resolve("file1.txt"));
            Files.write(file1, "hello world!".getBytes());

            Path dir2 = Files.createDirectory(dir1.resolve("dir2"));

            Path dir3 = Files.createDirectory(dir2.resolve("dir3"));

            Path dir4 = Files.createDirectory(dir3.resolve("dir4"));
            Path file2 = Files.createFile(dir4.resolve("file2.txt"));

            Path dir5 = Files.createDirectory(dir4.resolve("dir5"));

            Path file3 = Files.createFile(dir5.resolve("file3.txt"));
            Files.write(file1, "hello world!!".getBytes());

            Blob.addDirectory(gitRepoPath, testDir.toString());

            ArrayList<String> indexEntries = new ArrayList<String>();

            //file1
            String file1Hash = Blob.createUniqueFileName(file1.toString());
            String file1Entry = "blob" + file1Hash + "file1.txt";

            Path file1ObjectFile = Paths.get(gitRepoPath, "objects", file1Hash);
            if (File.exists(file1ObjectFile)){
                System.out.println("blob for file1.txt exists with hash: " + file1Hash);
                
                byte[] originalContent = Files.readAllBytes(file1);
                byte[] storedContent = Files.readAllBytes(file1ObjectFile);

                if (Arrays.equals(originalContent, storedContent)) {
                    System.out.println("blob content matches the original file\n");
                } else {
                    System.out.println("blob content does not match the original file\n");
                }

                indexEntry.add("blob" + file1Hash + "Testdir" + File.separator + "file1.txt");

            } else {
                System.out.println("blob for file1.txt does not exist\n");
            }

            //file2
            String file2Hash = Blob.createUniqueFileName(file2.toString());
            String file2Entry = "blob" + file2Hash + "file2.txt";

            Path file2ObjectFile = Paths.get(gitRepoPath, "objects", file2Hash);
            if (File.exists(file2ObjectFile)){
                System.out.println("blob for file2.txt exists with hash: " + file2Hash);
                
                byte[] originalContent = Files.readAllBytes(file2);
                byte[] storedContent = Files.readAllBytes(file2ObjectFile);

                if (Arrays.equals(originalContent, storedContent)) {
                    System.out.println("blob content matches the original file\n");
                } else {
                    System.out.println("blob content does not match the original file\n");
                }

                indexEntry.add("blob" + file2Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2" + File.separator + "dir3" + File.separator + "file2.txt");

            } else {
                System.out.println("blob for file1.txt does not exist\n");
            }

            //file3
            String file3Hash = Blob.createUniqueFileName(file3.toString());
            String file3Entry = "blob" + file3Hash + "file3.txt";

            Path file3ObjectFile = Paths.get(gitRepoPath, "objects", file3Hash);
            if (File.exists(file3ObjectFile)){
                System.out.println("blob for file3.txt exists with hash: " + file3Hash);
                
                byte[] originalContent = Files.readAllBytes(file3);
                byte[] storedContent = Files.readAllBytes(file3ObjectFile);

                if (Arrays.equals(originalContent, storedContent)) {
                    System.out.println("blob content matches the original file\n");
                } else {
                    System.out.println("blob content does not match the original file\n");
                }

                indexEntry.add("blob" + file3Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2" + File.separator + "dir3" + File.separator + "dir4" + File.separator + "dir5" + File.separator + "file3.txt");
            } else {
                System.out.println("blob for file3.txt does not exist\n");
            }

            //dir5
            String dir5Hash = Blob.createUniqueFileName(file3Entry);
            String dir5Entry = "tree" + dir5Hash + "dir5";

            Path dir5ObjectFile = Paths.get(gitRepoPath, "objects", dir5Hash);
            if (File.exists(dir5ObjectFile)){
                System.out.println("tree for dir5 exists with hash: " + dir5Hash);
                
                indexEntry.add("tree" + dir5Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2" + File.separator + "dir3" + File.separator + "dir4" + File.separator + "dir5");
            }
            else{
                System.out.println("tree for dir5 does not exist\n");
            }

            //dir4
            String dir4Hash = Blob.createUniqueFileName(dir5Entry);
            String dir4Entry = "tree" + dir4Hash + "dir4";

            Path dir4ObjectFile = Paths.get(gitRepoPath, "objects", dir4Hash);
            if (File.exists(dir4ObjectFile)){
                System.out.println("tree for dir4 exists with hash: " + dir4Hash);
                
                indexEntry.add("tree" + dir4Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2" + File.separator + "dir3" + File.separator + "dir4");
            }
            else{
                System.out.println("tree for dir4 does not exist\n");
            }

            //dir3
            String dir3Hash = Blob.createUniqueFileName(file2Entry + "\n" + dir4Entry);
            String dir3Entry = "tree" + dir3Hash + "dir3";

            Path dir3ObjectFile = Paths.get(gitRepoPath, "objects", dir3Hash);
            if (File.exists(dir3ObjectFile)){
                System.out.println("tree for dir3 exists with hash: " + dir3Hash);
                
                indexEntry.add("tree" + dir3Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2" + File.separator + "dir3");
            }
            else{
                System.out.println("tree for dir3 does not exist\n");
            }

            //dir2
            String dir2Hash = Blob.createUniqueFileName(dir3Entry);
            String dir2Entry = "tree" + dir2Hash + "dir2";

            Path dir2ObjectFile = Paths.get(gitRepoPath, "objects", dir2Hash);
            if (File.exists(dir2ObjectFile)){
                System.out.println("tree for dir2 exists with hash: " + dir2Hash);
                
                indexEntry.add("tree" + dir2Hash + "Testdir" + File.separator + "dir1" + File.separator + "dir2");
            }
            else{
                System.out.println("tree for dir2 does not exist\n");
            }

            //dir1
            String dir1Hash = Blob.createUniqueFileName(dir2Entry);
            String dir1Entry = "tree" + dir1Hash + "dir1";

            Path dir1ObjectFile = Paths.get(gitRepoPath, "objects", dir1Hash);
            if (File.exists(dir1ObjectFile)){
                System.out.println("tree for dir1 exists with hash: " + dir1Hash);
                
                indexEntry.add("tree" + dir1Hash + "Testdir" + File.separator + "dir1");
            }
            else{
                System.out.println("tree for dir1 does not exist\n");
            }

            //testDir
            String testFolderHash = Blob.createUniqueFileName(file1Entry + "\n" + dir1Entry);
            String testFolderEntry = "tree" + testFolderHash + "testFolder";

            Path testFolderObjectFile = Paths.get(gitRepoPath, "objects", testFolderHash);
            if (File.exists(testFolderObjectFile)){
                System.out.println("tree for testFolder exists with hash: " + testFolderHash);
                
                indexEntry.add("tree" + testFolderHash + "testFolder" + File.separator + "testFolder");
            }
            else{
                System.out.println("tree for testFolder does not exist\n");
            }

            BufferedReader bufr = new BufferedReader(new FileReader(Paths.get(gitRepoPath, "objects", "index")));
            String indexLine;
            Set<String> tempIndexEntries = new HashSet<>(indexEntries);

            while ((indexLine = bufr.readLine()) != null) {
                if (!indexEntries.contains(indexLine)) {
                    System.out.println("there's a line in index that shouldn't be there: " + indexLine);
                } else {
                    indexEntries.remove(indexLine);
                }
            }

            if (!indexEntries.isEmpty()) {
                System.out.println("index file is missing the following entries: " + indexEntries);
            } else {
                System.out.println("index file looks correct!");
            }
            bufr.close();
        } 
        deleteDirectoryRecursively(testDir);
    }

    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }

    public static void blobValidation(String gitPath, String content, String correctHash) throws IOException, NoSuchAlgorithmException{
        File file1 = new File("./", "test.txt");
        file1.createNewFile();
        
        FileWriter fw1 = new FileWriter(file1);
        fw1.write(content);
        fw1.close(); 

        Blob.createNewBlob(file1.getPath(), gitPath);

        File testFile = new File (gitPath + File.separator + "objects", correctHash);

        if (testFile.exists()){
            System.out.println("blob was created properly!\n");
        }
        else{
            System.out.println("blob wasn't created!\n");
        }

        FileReader reader = new FileReader(testFile);
        BufferedReader br = new BufferedReader(reader);
        StringBuilder contents = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null){
            contents.append(line);
        }
        br.close();

        FileInputStream fis = new FileInputStream(testFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        fis.close();
        byte[] fileBytes = byteArrayOutputStream.toByteArray();

        if (Arrays.equals(fileBytes, content.getBytes())) {
            System.out.println("content in blob is the same!\n");
        } else {
            System.out.println("content in blob is different :(\n");
        }

        File indexFile = new File(Paths.get("git") + File.separator + "index");
        BufferedReader bufr = new BufferedReader(new FileReader(indexFile));
        String indexLine;
        boolean lineFound = false;

        while ((indexLine = bufr.readLine()) != null){
            if (indexLine.equals (correctHash + " " + file1.getName())){
                lineFound = true;
                System.out.println("index file has correct entry for blob");
                break;
            }
        }
        if (!lineFound){
            System.out.println("index file doesn't have the correct entry for blob");
        }
        bufr.close();

        resetTestFiles(file1, testFile, indexFile, correctHash);
    }

    public static void resetTestFiles(File contentFile, File hashedFile, File indexFile, String correctHash) throws IOException{
        if (contentFile.delete()){
            System.out.println(contentFile.getName() + " was deleted\n");
        }
        else{
            System.out.println("failed to delete " + contentFile.getName() + "\n");
        }

        if (hashedFile.delete()){
            System.out.println(hashedFile.getName() + " was deleted\n");
        }
        else{
            System.out.println("failed to delete " + hashedFile.getName() + "\n");
        }

        File tempFile = new File("tempFile.txt");

        BufferedReader reader = new BufferedReader(new FileReader(indexFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String lineToRemove = correctHash + " " + contentFile.getName();
        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            if(!currentLine.equals(lineToRemove)){
                writer.write(currentLine + "\n");
            }
        }
        writer.close(); 
        reader.close(); 
        boolean successful = tempFile.renameTo(indexFile);
        if (successful){
            System.out.println("deleted the update in index\n");
        }
        else{
            System.out.println("failed to delete hash from index\n");
        }
    }
}
