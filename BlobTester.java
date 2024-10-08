import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BlobTester {
    private static Blob git = new Blob("/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git");
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String gitPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git";
        String content = "hello world and everyone in it!";
        String correctHash = "88d9814d5c99271752f74fae7f363230a68e06b7"; // using online sha-1 hash
        // blobValidation(gitPath, content, correctHash);
        directoryValidation(gitPath);
    }

    public static void directoryValidation(String gitRepoPath) throws IOException, NoSuchAlgorithmException {
        Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        Path testDir;
        if (Files.exists(currentDir.resolve("testFolder"))) {
            System.out.println("testFolder already exists.");
        } else {
            testDir = Files.createDirectory(currentDir.resolve("testFolder"));

            Path dir1 = Files.createDirectory(testDir.resolve("dir1"));
            Path file1 = Files.createFile(dir1.resolve("file1.txt"));
            Files.write(file1, "hello world!".getBytes());

            Path dir2 = Files.createDirectory(dir1.resolve("dir2"));

            Path dir3 = Files.createDirectory(dir2.resolve("dir3"));

            Path dir4 = Files.createDirectory(dir3.resolve("dir4"));
            Path file2 = Files.createFile(dir4.resolve("file2.txt"));

            Path dir5 = Files.createDirectory(dir4.resolve("dir5"));

            Path file3 = Files.createFile(dir5.resolve("file3.txt"));
            Files.write(file3, "hello world!!".getBytes());

            git.addDirectory(testDir.toString());

            ArrayList<String> indexEntry = new ArrayList<String>();

            // file1
            String file1Hash = git.createUniqueFileName(file1.toString());
            String file1Entry = "blob " + file1Hash + " file1.txt";

            verifyBlob(file1, file1Hash, "file1.txt", gitRepoPath);

            indexEntry.add("blob " + file1Hash + " " + testDir.getFileName().toString() + File.separator
                    + dir1.getFileName().toString() + File.separator + file1.getFileName().toString());

            // file2
            String file2Hash = git.createUniqueFileName(file2.toString());
            String file2Entry = "blob " + file2Hash + " file2.txt";

            verifyBlob(file2, file2Hash, "file2.txt", gitRepoPath);

            indexEntry.add("blob " + file2Hash + " " + testDir.getFileName().toString() + File.separator
                    + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString() + File.separator
                    + dir3.getFileName().toString() + File.separator + dir4.getFileName().toString() + File.separator
                    + file2.getFileName().toString());

            // file3
            String file3Hash = git.createUniqueFileName(file3.toString());
            String file3Entry = "blob " + file3Hash + " file3.txt";

            verifyBlob(file3, file3Hash, "file3.txt", gitRepoPath);

            indexEntry.add("blob " + file3Hash + " " + testDir.getFileName().toString() + File.separator
                    + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString() + File.separator
                    + dir3.getFileName().toString() + File.separator + dir4.getFileName().toString() + File.separator
                    + dir5.getFileName().toString() + File.separator + "file3.txt");

            // dir5
            String dir5Hash = git.calculateSHA1(file3Entry);
            String dir5Entry = "tree " + dir5Hash + " dir5";

            Path dir5ObjectFile = Paths.get(gitRepoPath, "objects", dir5Hash);
            if (Files.exists(dir5ObjectFile)) {
                System.out.println("tree for dir5 exists with hash: " + dir5Hash + "\n");

                indexEntry.add("tree " + dir5Hash + " " + testDir.getFileName().toString() + File.separator
                        + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString()
                        + File.separator + dir3.getFileName().toString() + File.separator
                        + dir4.getFileName().toString() + File.separator + dir5.getFileName().toString());
            } else {
                System.out.println("tree for dir5 does not exist\n");
            }

            // dir4
            String dir4Hash = git.calculateSHA1(file2Entry + "\n" + dir5Entry);
            String dir4Entry = "tree " + dir4Hash + " dir4";

            Path dir4ObjectFile = Paths.get(gitRepoPath, "objects", dir4Hash);
            if (Files.exists(dir4ObjectFile)) {
                System.out.println("tree for dir4 exists with hash: " + dir4Hash + "\n");

                indexEntry.add("tree " + dir4Hash + " " + testDir.getFileName().toString() + File.separator
                        + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString()
                        + File.separator + dir3.getFileName().toString() + File.separator
                        + dir4.getFileName().toString());
            } else {
                System.out.println("tree for dir4 does not exist\n");
            }

            // dir3
            String dir3Hash = git.calculateSHA1(dir4Entry);
            String dir3Entry = "tree " + dir3Hash + " dir3";

            Path dir3ObjectFile = Paths.get(gitRepoPath, "objects", dir3Hash);
            if (Files.exists(dir3ObjectFile)) {
                System.out.println("tree for dir3 exists with hash: " + dir3Hash + "\n");

                indexEntry.add("tree " + dir3Hash + " " + testDir.getFileName() + File.separator
                        + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString()
                        + File.separator + dir3.getFileName().toString());
            } else {
                System.out.println("tree for dir3 does not exist\n");
            }

            // dir2
            String dir2Hash = git.calculateSHA1(dir3Entry);
            String dir2Entry = "tree " + dir2Hash + " dir2";

            Path dir2ObjectFile = Paths.get(gitRepoPath, "objects", dir2Hash);
            if (Files.exists(dir2ObjectFile)) {
                System.out.println("tree for dir2 exists with hash: " + dir2Hash + "\n");

                indexEntry.add("tree " + dir2Hash + " " + testDir.getFileName().toString() + File.separator
                        + dir1.getFileName().toString() + File.separator + dir2.getFileName().toString());
            } else {
                System.out.println("tree for dir2 does not exist\n");
            }

            // dir1
            String dir1Hash = git.calculateSHA1(file1Entry + "\n" + dir2Entry);
            String dir1Entry = "tree " + dir1Hash + " dir1";

            Path dir1ObjectFile = Paths.get(gitRepoPath, "objects", dir1Hash);
            if (Files.exists(dir1ObjectFile)) {
                System.out.println("tree for dir1 exists with hash: " + dir1Hash + "\n");

                indexEntry.add("tree " + dir1Hash + " " + testDir.getFileName().toString() + File.separator
                        + dir1.getFileName().toString());
            } else {
                System.out.println("tree for dir1 does not exist\n");
            }

            // testDir
            String testFolderHash = git.calculateSHA1(dir1Entry);
            String testFolderEntry = "tree " + testFolderHash + " testFolder";

            Path testFolderObjectFile = Paths.get(gitRepoPath, "objects", testFolderHash);
            if (Files.exists(testFolderObjectFile)) {
                System.out.println("tree for testFolder exists with hash: " + testFolderHash + "\n");

                indexEntry.add("tree " + testFolderHash + " " + testDir.getFileName().toString());
            } else {
                System.out.println("tree for testFolder does not exist\n");
            }

            BufferedReader bufr = new BufferedReader(new FileReader(Paths.get(gitRepoPath, "index").toString()));
            String indexLine;
            Set<String> tempIndexEntries = new HashSet<>(indexEntry);

            System.out.println("correct index entries:\n");
            while ((indexLine = bufr.readLine()) != null) {
                if (!indexEntry.contains(indexLine)) {
                    System.out.println("there's a line in index that shouldn't be there: " + indexLine);
                } else {
                    System.out.println(indexLine);
                    indexEntry.remove(indexLine);
                }
            }

            if (!indexEntry.isEmpty()) {
                System.out.println("\nindex file is missing the following entries: " + indexEntry);
            } else {
                System.out.println("\nindex file looks correct!");
            }
            bufr.close();
            deleteDirectoryRecursively(testDir);
            clearGit(gitRepoPath);
        }
    }

    public static void verifyBlob(Path file, String hash, String name, String gitRepoPath) throws IOException {
        Path objectFile = Paths.get(gitRepoPath, "objects", hash);
        if (Files.exists(objectFile)) {
            System.out.println("blob for " + name + " exists with hash: " + hash);

            byte[] originalContent = Files.readAllBytes(file);
            byte[] storedContent = Files.readAllBytes(objectFile);

            if (Arrays.equals(originalContent, storedContent)) {
                System.out.println("blob content matches the original file\n");
            } else {
                System.out.println("blob content does not match the original file\n");
            }

        } else {
            System.out.println("blob for " + name + " does not exist\n");
        }
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

    public static void clearGit(String gitRepoPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(gitRepoPath, "objects"))) {
            for (Path entry : stream) {
                Files.delete(entry);
            }
        }

        File objects = new File(Paths.get(gitRepoPath, "index").toString());
        FileWriter writer = new FileWriter(objects, false);
        writer.write("");
        writer.close();
    }

    public static void blobValidation(String gitPath, String content, String correctHash)
            throws IOException, NoSuchAlgorithmException {
        File file1 = new File("./", "test.txt");
        file1.createNewFile();

        FileWriter fw1 = new FileWriter(file1);
        fw1.write(content);
        fw1.close();

        git.createNewBlob(file1.getPath());

        File testFile = new File(gitPath + File.separator + "objects", correctHash);

        if (testFile.exists()) {
            System.out.println("blob was created properly!\n");
        } else {
            System.out.println("blob wasn't created!\n");
        }

        FileReader reader = new FileReader(testFile);
        BufferedReader br = new BufferedReader(reader);
        StringBuilder contents = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
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

        while ((indexLine = bufr.readLine()) != null) {
            if (indexLine.equals(correctHash + " " + file1.getName())) {
                lineFound = true;
                System.out.println("index file has correct entry for blob");
                break;
            }
        }
        if (!lineFound) {
            System.out.println("index file doesn't have the correct entry for blob");
        }
        bufr.close();
        clearGit(gitPath);
    }
}
