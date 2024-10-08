import java.io.File;
import java.io.IOException;

public class Git {
    public static void main(String[] args) throws IOException {
        String testPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky";
        //initRepo(testPath);
        // GitRepoTester.testForGitRepo(testPath);
        // GitRepoTester.deleteGitRepo(testPath);
    }

    public static void initRepo(String path) throws IOException {
        File git = new File(path, "git");
        if (!git.exists()){
            git.mkdirs();

            File objects = new File(git, "objects");
            objects.mkdir();

            File index = new File(git, "index");
            index.createNewFile();

            File head = new File (git, "HEAD");
            head.createNewFile();

        } else{
            System.out.println("This already exists!");
        }
    }
}