import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Deflater;


public class Blob {
    public static boolean compressionEnabled;
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


    // Created for file updating process
    private static void updateIndex(String gitRepoPath, String type, String hash, String name) throws IOException {
        Path indexPath = Paths.get(gitRepoPath, "index");
        String entry = type + " " + hash + " " + name + "\n";
        Files.write(indexPath, entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
