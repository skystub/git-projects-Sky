import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class Blob {
    public static boolean compressionEnabled;
    private static final Logger logger = Logger.getLogger(Blob.class.getName());
    // This is for Bonus Goal threee: to decide whether to include a hidden file 
    private static final boolean INCLUDE_HIDDEN_FILES = true;
    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        compressionEnabled = true;
        String gitRepoPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git";
        createNewBlob("/Users/skystubbeman/Desktop/tester.txt", gitRepoPath);
    }

    public static String createUniqueFileName(String path)
            throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        File file = new File(path);
       
        FileInputStream in = new FileInputStream(file);
        BufferedInputStream br = new BufferedInputStream(in);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] inputBytes = br.readAllBytes();
        byte[] endingBytes;
        in.close();
        br.close();

        if (compressionEnabled){
            endingBytes = compressBlob(inputBytes);
        }
        else{
            endingBytes = inputBytes;
        }
       
        sha1Digest.update(endingBytes);
        byte[] hash = sha1Digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            sb.append(String.format("%02x", hash[i]));
        }
        System.out.println(sb.toString());
        return sb.toString();
    }

    public static byte[] compressBlob(byte[] inputBytes) throws IOException{
        Deflater deflater = new Deflater();
        deflater.setInput(inputBytes);
        deflater.finish();

        byte[] output = new byte[inputBytes.length * 2];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (!deflater.finished()){
            int num = deflater.deflate(output);
            outputStream.write(output, 0, num);
        }
        byte[] endingBytes;
        endingBytes = outputStream.toByteArray();
        outputStream.close();
        return endingBytes;
    }

    public static void createNewBlob(String filePath, String gitRepoPath) throws FileNotFoundException, NoSuchAlgorithmException, IOException{
        
        String name = createUniqueFileName(filePath);
        File file = new File(gitRepoPath + "/objects", name); 
        boolean check = file.createNewFile();
        if (!check){
            System.out.println("blob already exists");
        }

        else{
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));

            if(compressionEnabled){
                byte[] inputBytes = inputStream.readAllBytes();
                byte[] endingBytes;
                endingBytes = compressBlob(inputBytes);
                outputStream.write(endingBytes);
            }
            else{
                int i;
                while ((i = inputStream.read()) != -1){
                    outputStream.write(i);
                }
            }
            inputStream.close();
            outputStream.close();
        }
        
        BufferedWriter bw = new BufferedWriter (new FileWriter(gitRepoPath + "/index", true));
        File filePointer = new File(filePath);
        bw.write(name + " " + filePointer.getName() + "\n"); 
        bw.close();
    }


    // This is the method for creating a new tree recursively
    // For the Bonus Section: I added Set<String> which is to keep track of visited directories.
    // Cyclic Directories - to ensure my code can handle symbolic links or shortcuts that may create cycles
    // Review this link later: https://stackoverflow.com/questions/12100299/whats-a-canonical-path
    private static void createNewTree(String dirPath, String gitRepoPath, Set<String> visitedPaths) throws IOException, NoSuchAlgorithmException {
        Path dir = Paths.get(dirPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + dirPath);
        }
        String canonicalPath = dir.toRealPath().toString();
        if (visitedPaths.contains(canonicalPath)) {
            logger.warning("Cyclic directory detected: " + canonicalPath);
            return;
        }
        visitedPaths.add(canonicalPath);
        String treeName = createUniqueFileName(dirPath);
        updateIndex(gitRepoPath, "tree", treeName, dir.getFileName().toString());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!INCLUDE_HIDDEN_FILES && Files.isHidden(path)) {
                    logger.info("Skipping hidden file/directory: " + path);
                    continue;
                }
                if (!Files.isReadable(path)) {
                    logger.warning("Permission denied: Cannot read " + path);
                    continue;
                }
                if (Files.isRegularFile(path)) {
                    createNewBlob(path.toString(), gitRepoPath);
                } else if (Files.isDirectory(path)) {
                    createNewTree(path.toString(), gitRepoPath, new HashSet<>(visitedPaths));
                }
            }
        }
    }

    // Created for file updating process
    private static void updateIndex(String gitRepoPath, String type, String hash, String name) throws IOException {
        Path indexPath = Paths.get(gitRepoPath, "index");
        String entry = type + " " + hash + " " + name + "\n";
        Files.write(indexPath, entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        logger.info("Updated index: " + entry.trim());
    }
}
