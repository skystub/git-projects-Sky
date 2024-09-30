import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.Deflater;

public class Blob {
    public static boolean compressionEnabled;
    // This is for Bonus Goal threee: to decide whether to include a hidden file 
    private static final String objectsDir = "objects";
    private static final String indexFile = "index";
    private static final boolean includeHiddenFiles = true;
    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        compressionEnabled = true;
        String gitRepoPath = "/Users/skystubbeman/Documents/HTCS_Projects/git-projects-Sky/git";
        createNewBlob("/Users/skystubbeman/Desktop/tester.txt", gitRepoPath);
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


    //Add directory method
    // what this does is formats index file and handles cyclic directories and hidden files
    public static void addDirectory(String directoryPath) throws IOException, NoSuchAlgorithmException {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + directoryPath);
        }
        Set<Path> visitedPaths = new HashSet<>();
        String treeHash = createTree(dir, "", new HashSet<>());
        updateIndex(gitRepoPath, "tree", treeHash, dir.getFileName().toString());
    }
    // This is the method for creating a new tree recursively
    // For the Bonus Section: I added Set<String> which is to keep track of visited directories.
    // Cyclic Directories - to ensure my code can handle symbolic links or shortcuts that may create cycles
    // Review this link later: https://stackoverflow.com/questions/12100299/whats-a-canonical-path
    private static String createTree(Path dir, String relativePath, Set<Path> visitedPaths) throws IOException, NoSuchAlgorithmException {
        StringBuilder treeContent = new StringBuilder();
        
        // Handle cyclic directories
        Path canonicalPath = dir.toRealPath();
        if (visitedPaths.contains(canonicalPath)) {
            System.out.println("Cyclic directory detected: " + canonicalPath);
            return "";
        }
        visitedPaths.add(canonicalPath);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                
                // Skip hidden files if not included
                if (!includeHiddenFiles && Files.isHidden(path)) {
                    System.out.println("Skipping hidden file/directory: " + path);
                    continue;
                }

                // Check for read permissions
                if (!Files.isReadable(path)) {
                    System.out.println("Permission denied: Cannot read " + path);
                    continue;
                }

                String fullPath = relativePath.isEmpty() ? name : relativePath + "/" + name;
                if (Files.isRegularFile(path)) {
                    String blobHash = createBlob(path);
                    treeContent.append(String.format("blob %s %s\n", blobHash, name));
                    updateIndex("blob", blobHash, fullPath);
                } else if (Files.isDirectory(path)) {
                    String subTreeHash = createTree(path, fullPath, new HashSet<>(visitedPaths));
                    if (!subTreeHash.isEmpty()) {
                        treeContent.append(String.format("tree %s %s\n", subTreeHash, name));
                        updateIndex("tree", subTreeHash, fullPath);
                    }
                }
            }
        } catch (AccessDeniedException e) {
            System.out.println("Access denied to directory: " + dir);
        }

        return saveObject(treeContent.toString());
    }
    private static String createBlob(Path file) throws IOException, NoSuchAlgorithmException {
        try {
            byte[] content = Files.readAllBytes(file);
            return saveObject(new String(content));
        } catch (AccessDeniedException e) {
            System.out.println("Access denied to file: " + file);
            return "";
        }
    }

    private static String saveObject(String content) throws IOException, NoSuchAlgorithmException {
        String hash = calculateSHA1(content);
        Path objectPath = Paths.get(objectsDir, hash);
        Files.write(objectPath, content.getBytes(), StandardOpenOption.CREATE);
        return hash;
    }

    private static void updateIndex(String gitRepoPath, String type, String hash, String path) throws IOException {
        String entry = String.format("%s %s %s\n", type, hash, path);
        Files.write(Paths.get(gitRepoPath, indexFile), entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }


    //Sorry Sky ~ I didn't wnat any redundancy / code duplication between calculateSHA1 and createUniqueFileName so I refactored the code to eliminate the duplication. 
    // I created a single method to handle SHA-1 calcualtion for both strings and files
    public static String calculateSHA1(File file) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = new FileInputStream(file)) {
            return calculateSHA1(is);
        }
    }

    public static String calculateSHA1(String content) throws NoSuchAlgorithmException, IOException {
        return calculateSHA1(new ByteArrayInputStream(content.getBytes()));
    }

    private static String calculateSHA1(InputStream input) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            sha1Digest.update(buffer, 0, bytesRead);
        }
        byte[] hash = sha1Digest.digest();
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String createUniqueFileName(String path) throws IOException, NoSuchAlgorithmException {
        File file = new File(path);
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] processedContent = compressionEnabled ? compressBlob(content) : content;
        return calculateSHA1(new ByteArrayInputStream(processedContent));
    }
}
