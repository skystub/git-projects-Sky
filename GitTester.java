import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public class GitTester {
    private static final String gitRepoPath = Paths.get("git").toString();
    private static Blob blob = new Blob(gitRepoPath, "rootTree");
    private static String rootTreeName = "rootTree";

    public static void main(String[] args) throws IOException {
        GitInit git = new GitInit();
        git.initRepo(".");

        try {
            String firstCommitHash;
            String secondCommitHash;
            setUp();

            firstCommitHash = testCommit();
            testStage();
            secondCommitHash = testCommit();

            testCheckout(firstCommitHash);

            System.out.println("\ndeleting rootTree");
            deleteDirectoryRecursively(Paths.get(rootTreeName));

            testCheckout(secondCommitHash);

            cleanUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void testCheckout(String commitHash){
        System.out.println("\nrestoring this commit: " + commitHash);
        System.out.println("\nthe following changes were made to restore the commit: ");

        blob.checkout(commitHash);
    }

    private static void setUp() throws IOException, NoSuchAlgorithmException {
        Files.createDirectories(Paths.get("./", rootTreeName, "folderA", "folderB", "folderC"));

        Files.writeString(Paths.get("./", rootTreeName, "file1.txt"), "hello world!");
        Files.writeString(Paths.get("./", rootTreeName, "folderA", "folderB", "file3.txt"), "hello world!!");
        Files.writeString(Paths.get("./", rootTreeName, "folderA", "folderB", "folderC", "file2.txt"), "hello world!!!");

        Path currentDirectory = Paths.get(System.getProperty("user.dir"));
        blob.addDirectory(Paths.get(currentDirectory.toString(), rootTreeName).toString());

        Path indexPath = Paths.get(gitRepoPath, "index");
        if (Files.exists(indexPath)) {
            String indexContent = Files.readString(indexPath);
            System.out.println("\nindex file contents after adding original directory:");
            System.out.println(indexContent);

        } else {
            System.out.println("index file not found after staging");
        }

        System.out.println("\nfinished setting up\n");
    }

    private static void testStage() throws IOException, NoSuchAlgorithmException {
        System.out.println("\nTesting stage() method:");
        System.out.println("\nthe following changes to rootTree were made: ");
        Files.writeString(Paths.get(rootTreeName, "folderA", "folderB", "file4.txt"), "hello world!");

        System.out.println("\nadded rootTree/folderA/folderB/file4.txt");

        blob.stage(Paths.get(rootTreeName, "folderA", "folderB", "file4.txt").toString());

        Files.createDirectory(Paths.get(rootTreeName, "folderA", "newFolder"));

        System.out.println("added rootTree/folderA/newFolder");

        Files.writeString(Paths.get(rootTreeName, "folderA", "newFolder","testFile.txt"), "this is a test!");

        System.out.println("added rootTree/folderA/newFolder/testFile.txt");

        Files.delete(Paths.get(rootTreeName, "folderA", "folderB", "folderC", "file2.txt"));
        System.out.println("deleted rootTree/folderA/folderC/file2.txt");

        Files.delete(Paths.get(rootTreeName, "folderA", "folderB", "folderC"));
        
        System.out.println("deleted rootTree/folderA/folderC");

        blob.stage(Paths.get(rootTreeName, "folderA", "newFolder", "testFile.txt").toString());


        Path indexPath = Paths.get(gitRepoPath, "index");
        if (Files.exists(indexPath)) {
            String indexContent = Files.readString(indexPath);
            System.out.println("\nindex file contents after changes:");
            System.out.println(indexContent);
            System.out.println("\n");
        } else {
            System.out.println("index file not found after staging");
        }
    }

    private static String testCommit() throws IOException, NoSuchAlgorithmException {
        System.out.println("testing commit: \n");

        String hash = blob.commit("sky", "commit!");

        Path headPath = Paths.get(gitRepoPath, "HEAD");
        if (Files.exists(headPath)) {
            String commitHash = Files.readString(headPath).trim();

            Path commitFilePath = Paths.get(gitRepoPath, "objects", commitHash);
            if (Files.exists(commitFilePath)) {
                String commitContent = Files.readString(commitFilePath);
                System.out.println("commit file info:\n");
                System.out.println(commitContent);
            } else {
                System.out.println("commit file not found.");
            }
            System.out.println("\nupdated HEAD hash: " + commitHash);

        } else {
            System.out.println("\nHEAD file not found after commit");
        }
        return hash;
    }

    private static void cleanUp() throws IOException {
        deleteDirectoryRecursively(Paths.get(rootTreeName));
        clearGit(gitRepoPath);
        System.out.println("\ncleaned up contents of git and deleted rootTree!");
    }

    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            System.out.println("this directory doesn't exist or is already deleted: " + path);
            return;
        }

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
        File indexFile = new File(Paths.get(gitRepoPath, "index").toString());
        FileWriter indexWriter = new FileWriter(indexFile, false);
        indexWriter.write("");
        indexWriter.close();

        File headFile = new File(Paths.get(gitRepoPath, "HEAD").toString());
        FileWriter headWriter = new FileWriter(headFile, false);
        headWriter.write("");
        headWriter.close();
    }
}
