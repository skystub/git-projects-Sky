import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.Deflater;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Blob implements GitInterface{
    public static boolean compressionEnabled = false;
    private String gitRepoPath;
    private String rootTreeName;
    private static final String objectsDir = "objects";
    private static final String indexFile = "index";
    private static final boolean includeHiddenFiles = true;
    
    public Blob (String gitRepoPath, String rootTreeName){
        this.gitRepoPath = gitRepoPath;
        this.rootTreeName = rootTreeName;
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

    public void stage(String filePath){
        try {
            int sepIndex = filePath.indexOf(File.separator);
            if (sepIndex != -1){
                String rootPath = filePath.substring(0, sepIndex);

                Path indexPath = Paths.get(gitRepoPath, "index");
                File index = indexPath.toFile();
                FileWriter indexWriter = new FileWriter(index, false);
                indexWriter.write("");
                indexWriter.close();

                addDirectory(rootPath); 
            } else{
                System.out.println("incorrect filePath");
            }
        } catch (IOException e) {
            System.err.println("I/O error while staging the file: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algorithm error while staging the file: " + e.getMessage());
        }
    }

    public String commit(String author, String message){

        Path filePath = Paths.get("git", "index");
        String rootTreeHash;
        try (RandomAccessFile file = new RandomAccessFile(filePath.toString(), "r")) {
            long fileLength = file.length() - 1;
            StringBuilder lastLine = new StringBuilder();

            for (long pointer = fileLength; pointer >= 0; pointer--) {
                file.seek(pointer);
                char ch = (char) file.read();

                if (ch == '\n' && lastLine.length() > 0) {
                    break;
                }
                lastLine.append(ch);
            }

            lastLine.reverse().toString();
            rootTreeHash = lastLine.toString().substring(5,45);
        
            StringBuilder content = new StringBuilder();
            java.util.Date date = new java.util.Date(); 

            Path headPath = Paths.get(gitRepoPath, "HEAD");
            File head = headPath.toFile();
            BufferedReader br = new BufferedReader(new FileReader(head));
            
            String parent = br.readLine();

            if (parent == null){
                parent = "";
            }
         
            br.close();

            content.append("tree: " + rootTreeHash + "\nparent: " + parent + "\nauthor: " + author + "\ndate: " + date + "\nmessage: " + message);

            String commitHash = calculateSHA1(content.toString());
            File commitFile = new File(Paths.get(gitRepoPath, "objects").toString(), commitHash);
            commitFile.createNewFile();

            FileWriter commitFileWriter = new FileWriter(commitFile);
            commitFileWriter.write(content.toString());
            commitFileWriter.close();
            
            FileWriter headWriter = new FileWriter(head, false);
            headWriter.write(commitHash);
            headWriter.close();

            return commitHash;
        
        } catch (IOException e) {
            return "error reading file: " + e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            return "error, cant to compute hash bc of an algorithm issue: " + e.getMessage(); 
        }
    }

    public void checkout(String commitHash){
        try {
            Path commitFilePath = Paths.get("git", "objects", commitHash);
            File commitFile = commitFilePath.toFile();
    
            if (!commitFile.exists()) {
                System.out.println("commit " + commitHash + " doesn't exist :(");
                return;
            }
    
            BufferedReader br = new BufferedReader(new FileReader(commitFile));
            String rootTreeHash = null;
            String line;
        
            while ((line = br.readLine()) != null) {
                if (line.startsWith("tree: ")) {
                    rootTreeHash = line.substring(6);
                    break;
                }
            }
            br.close();
            
            File directory = new File(Paths.get(rootTreeName).toString());

            if (!directory.exists()) {
                directory.mkdir();
            }

            Set<String> expectedFiles = new HashSet<>();

            restoreTree(rootTreeHash, rootTreeName, expectedFiles); 
            cleanUpWorkingDirectory(rootTreeName, expectedFiles);
    
            System.out.println("finished checkout");
    
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void cleanUpWorkingDirectory(String directoryPath, Set<String> expectedFiles){
        File directory = new File(directoryPath);
        File[] currentFiles = directory.listFiles();

        if (currentFiles != null) {
            for (File file : currentFiles) {
                if (file.isDirectory()) {
                    cleanUpWorkingDirectory(file.getPath(), expectedFiles);
                    if (!expectedFiles.contains(file.getPath())) {
                        if (file.delete()) {
                            System.out.println("deleted unexpected directory: " + file.getPath());
                        } else {
                            System.out.println("couldn't delete directory: " + file.getPath());
                        }
                    }
                } else {
                    if (!expectedFiles.contains(file.getPath())) {
                        if (file.delete()) {
                            System.out.println("deleted unexpected file: " + file.getPath());
                        } else {
                            System.out.println("couldn't delete: " + file.getPath());
                        }
                    }
                }
            }
        }
    }

    public void restoreTree(String treeHash, String currentDirectory, Set<String> expectedFiles) throws IOException, NoSuchAlgorithmException{
        Path treeFilePath = Paths.get("git", "objects", treeHash);
        File treeFile = treeFilePath.toFile();

        if (!treeFile.exists()) {
            System.out.println("tree " + treeHash + " doesn't exist in objects :(");
            return;
        }

        BufferedReader treeReader = new BufferedReader(new FileReader(treeFile));
        String line;

        while ((line = treeReader.readLine()) != null) {
            String[] parts = line.split(" ");
            String type = parts[0];
            String objectHash = parts[1];
            String fileName = parts[2];

            Path fullPath = Paths.get(currentDirectory, fileName);
            expectedFiles.add(fullPath.toString());

            if (type.equals("blob")) {
                restoreBlob(objectHash, fullPath.toString(), fileName);
            } else if (type.equals("tree")) {
                File dir = new File(fullPath.toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                restoreTree(objectHash, fullPath.toString(), expectedFiles);
            }
        }
        treeReader.close();
    }

    private void restoreBlob(String blobHash, String filePath, String fileName) throws IOException, NoSuchAlgorithmException {
        Path blobFilePath = Paths.get("git", "objects", blobHash);
        File blobFile = blobFilePath.toFile();

        if (!blobFile.exists()) {
            System.out.println("blob " + blobHash + " doesnt exist");
            return;
        }

        BufferedReader blobReader = new BufferedReader(new FileReader(blobFile));
        StringBuilder content = new StringBuilder();
        String line;
        List<String> lines = new ArrayList<>();

        while ((line = blobReader.readLine()) != null) {
            lines.add(line);
        }
        blobReader.close();

        for (int i = 0; i < lines.size(); i++) {
            content.append(lines.get(i));
            if (i < lines.size() - 1) {
                content.append("\n");
            }
        }

        File file = new File(filePath);
        if (!file.exists() || !createUniqueFileName(filePath.toString()).equals(blobHash)) {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(content.toString());
            fileWriter.close();
            System.out.println("restored file: " + filePath);
        }
    }

    private String getLatestCommit() throws NoSuchAlgorithmException, IOException{
        return createUniqueFileName(Paths.get(gitRepoPath, "index").toString());
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