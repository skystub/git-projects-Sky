import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Blob {
    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        System.out.println(createUniqueFileName("/Users/skystubbeman/Desktop/helloWorld.txt"));
    }

    public static String createUniqueFileName(String path) throws FileNotFoundException, IOException, NoSuchAlgorithmException{
        File file = new File(path);
        FileInputStream in = new FileInputStream(file);
        BufferedInputStream br = new BufferedInputStream(in);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

        byte[] byteArr = new byte[8192];
        int length = br.read(byteArr);

        while (length != -1){
            sha1Digest.update(byteArr, 0, length);
            length = br.read(byteArr);
        }
        
        byte[] hash = sha1Digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++){
            sb.append(String.format("%02x", hash[i]));
        }
        in.close();
        br.close();

        return sb.toString();

    }
}
