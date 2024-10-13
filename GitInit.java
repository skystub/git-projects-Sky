import java.io.File;
import java.io.IOException;

public class GitInit {
    
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
            System.out.println("git already exists!");
        }
    }
}