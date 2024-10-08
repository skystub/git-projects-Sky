import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.Deflater;
import java.time.LocalDateTime;  
import java.time.format.DateTimeFormatter;  

public class Blob {
    public static boolean compressionEnabled = false;
    private String gitRepoPath;
    private static final String objectsDir = "objects";
    private static final String indexFile = "index";
    private static final boolean includeHiddenFiles = true;
    
    public Blob (String gitRepoPath){
        this.gitRepoPath = gitRepoPath;
    }
    
    public static void initRepo(String path) throws IOException {
        File git = new File(path, "git");
        if (!git.exists()){
            git.mkdirs();

            File objects = new File(git, objectsDir);
            objects.mkdir();

            File index = new File(git, indexFile);
            index.createNewFile();

            File head = new File (git, "HEAD");
            head.createNewFile();

        } else{
            System.out.println("This already exists!");
        }
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

    public void createCommit(String author, String message) throws NoSuchAlgorithmException, IOException{
        String rootTreeHash = createUniqueFileName(Paths.get(gitRepoPath, "index").toString());
        StringBuilder content = new StringBuilder();
        java.util.Date date = new java.util.Date(); 
        Path headPath = Paths.get(gitRepoPath, "HEAD");
        File head = headPath.toFile();
        BufferedReader br = new BufferedReader(new FileReader(head));
        String parent;
        if (Objects.equals(br.readLine(), null)){
            parent = "";
        }
        else{
            parent = br.readLine();
        }
        br.close();

        content.append("tree: " + rootTreeHash + "\n" + "parent: " + parent + "\n" + "author: " + author + "\n" +  "date: " + date + "message: " + message);

        String commitHash = calculateSHA1(content.toString());
        File commitFile = new File(Paths.get(gitRepoPath, "objects").toString(), commitHash);
        commitFile.createNewFile();
        FileWriter commitFileWriter = new FileWriter(commitFile);
        commitFileWriter.write(content.toString());
        commitFileWriter.close();
        
        FileWriter headWriter = new FileWriter(head, false);
        headWriter.write(commitHash);
        headWriter.close();

        Path indexPath = Paths.get(gitRepoPath, "index");
        File index = indexPath.toFile();
        FileWriter indexWriter = new FileWriter(index, false);
        indexWriter.write("");
        indexWriter.close();

        createRootTree();
    }

    private String getLatestCommit() throws NoSuchAlgorithmException, IOException{
        return createUniqueFileName(Paths.get(gitRepoPath, "index").toString());
    }

    private void createRootTree() throws NoSuchAlgorithmException, IOException{
        createBlob(Paths.get(gitRepoPath, "index"));
    }

    //Add directory method
    // what this does is formats index file and handles cyclic directories and hidden files
    public void addDirectory(String directoryPath) throws IOException, NoSuchAlgorithmException {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + directoryPath);
        }
        boolean isEmpty = false;
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir);
        if (!dirStream.iterator().hasNext()){
            isEmpty = true;
        }

        dirStream.close();

        Set<Path> visitedPaths = new HashSet<>();
        String treeHash = createTree(dir, dir.getFileName().toString(), visitedPaths, true, true);
        updateIndex("tree", treeHash, dir.getFileName().toString(), isEmpty);
    }

    // This is the method for creating a new tree recursively
    // For the Bonus Section: I added Set<String> which is to keep track of visited directories.
    // Cyclic Directories - to ensure my code can handle symbolic links or shortcuts that may create cycles
    // Review this link later: https://stackoverflow.com/questions/12100299/whats-a-canonical-path
    private String createTree(Path dir, String relativePath, Set<Path> visitedPaths, boolean firstIndexLine, boolean firstTreeLine) throws IOException, NoSuchAlgorithmException {
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

                // IF FILE
                // if its a file then create a blob and add it to the tree
                if (Files.isRegularFile(path)) {
                    String blobHash = createBlob(path); 
                    updateIndex("blob", blobHash, fullPath, firstIndexLine);
                    firstIndexLine = false;
                    if (firstTreeLine){
                        treeContent.append(String.format("blob %s %s", blobHash, name));
                        firstTreeLine = false;
                    } else{
                        treeContent.append(String.format("\nblob %s %s", blobHash, name));
                    }
                } 

                // IF DIRECTORY
                // if its a directory use recursion to make another tree
                else if (Files.isDirectory(path)) {
                    String subTreeHash = createTree(path, fullPath, new HashSet<>(visitedPaths), firstIndexLine, true); 
                    firstIndexLine = false; //y
                    if (!subTreeHash.isEmpty()) {
                        updateIndex("tree", subTreeHash, fullPath, firstIndexLine);
                        if (firstTreeLine){
                            treeContent.append(String.format("tree %s %s", subTreeHash, name));
                            firstTreeLine = false;
                        }
                        else{
                            treeContent.append(String.format("\ntree %s %s", subTreeHash, name));
                        }
                    }
                }
            }
        } catch (AccessDeniedException e) {
            System.out.println("Access denied to directory: " + dir);
        }

        return saveObject(treeContent.toString());
    }

    private String createBlob(Path file) throws IOException, NoSuchAlgorithmException { 
        try {
            byte[] content = Files.readAllBytes(file);
            return saveObject(new String(content));
        } catch (AccessDeniedException e) {
            System.out.println("Access denied to file: " + file);
            return "";
        }
    }

    private String saveObject(String content) throws IOException, NoSuchAlgorithmException {
        String hash = calculateSHA1(content);
        Path objectPath = Paths.get(gitRepoPath, objectsDir, hash);
        Files.write(objectPath, content.getBytes(), StandardOpenOption.CREATE);
        return hash;
    }

    private void updateIndex(String type, String hash, String path, boolean isFirstLine) throws IOException {
        String entry;
        if (isFirstLine){
            entry = String.format("%s %s %s", type, hash, path);
        }
        else{
            entry = String.format("%n%s %s %s", type, hash, path);
        }
        Files.write(Paths.get(gitRepoPath, indexFile), entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }


    //Sorry Sky ~ I didn't wnat any redundancy / code duplication between calculateSHA1 and createUniqueFileName so I refactored the code to eliminate the duplication. 
    // I created a single method to handle SHA-1 calcualtion for both strings and files
    public String calculateSHA1(String content) throws NoSuchAlgorithmException, IOException {
        return calculateSHA1(new ByteArrayInputStream(content.getBytes()));
    }

    public String calculateSHA1(InputStream input) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            sha1Digest.update(buffer, 0, bytesRead);
        }
        byte[] hash = sha1Digest.digest();
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String createUniqueFileName(String path) throws IOException, NoSuchAlgorithmException {
        File file = new File(path);
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] processedContent = compressionEnabled ? compressBlob(content) : content;
        return calculateSHA1(new ByteArrayInputStream(processedContent));
    }

    public void createNewBlob(String filePath) throws FileNotFoundException, NoSuchAlgorithmException, IOException{
        
        String name = createUniqueFileName(filePath);
        File file = new File(gitRepoPath + File.separator + "objects", name); 
        boolean check = file.createNewFile();

        if (!check){
            System.out.println("blob already exists");
        }

        else{
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));

            if(compressionEnabled){
                byte[] inputBytes = inputStream.readAllBytes();
                byte[] endingBytes = compressBlob(inputBytes);
                outputStream.write(endingBytes);
            }
            else{
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead); 
                }
            }
            inputStream.close();
            outputStream.close();
        }
        
        BufferedWriter bw = new BufferedWriter (new FileWriter(gitRepoPath + File.separator + "index", true));
        File filePointer = new File(filePath);
        bw.write(name + " " + filePointer.getName() + "\n"); 
        bw.close();
    }
}   