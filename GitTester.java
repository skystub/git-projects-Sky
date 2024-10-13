import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public class GitTester {
    private static final String gitRepoPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git"; 
    private static Blob blob = new Blob(gitRepoPath);
    private static String rootTreeName = "rootTree";

    public static void main(String[] args) throws IOException {
        // GitInit git = new GitInit();
        // git.initRepo("/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky");

        try {
            setUp();

            testCommit();
            testStage();
            testCommit();

            cleanUp();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        System.out.println("\nfinished setting up");
    }

    private static void testStage() throws IOException, NoSuchAlgorithmException {
        System.out.println("\nTesting stage() method...");

        Files.writeString(Paths.get(rootTreeName, "folderA", "folderB", "file4.txt"), "hello world!");
        blob.stage(Paths.get(rootTreeName, "folderA", "folderB", "file4.txt").toString());

        Path indexPath = Paths.get(gitRepoPath, "index");
        if (Files.exists(indexPath)) {
            String indexContent = Files.readString(indexPath);
            System.out.println("\nindex file contents after staging:");
            System.out.println(indexContent);
            System.out.println("\n");
        } else {
            System.out.println("index file not found after staging");
        }
    }

    private static void testCommit() throws IOException, NoSuchAlgorithmException {
        System.out.println("testing commit...\n");

        blob.commit("sky", "commit!");

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
    }

    private static void cleanUp() throws IOException {
        deleteDirectoryRecursively(Paths.get(rootTreeName));
        clearGit(gitRepoPath);
        System.out.println("\ncleaned up!");
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
