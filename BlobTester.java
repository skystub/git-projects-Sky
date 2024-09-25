import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class BlobTester {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        blobValidation("/Users/skystubbeman/Documents/HTCS_Projects/git");
    }
    public static void blobValidation(String gitPath) throws IOException, NoSuchAlgorithmException{
        File file1 = new File("../../../Desktop", "test.txt");
        file1.createNewFile();

        FileWriter fw1 = new FileWriter(file1);
        fw1.write("hello world and everyone in it!");
        //hash should be "88d9814d5c99271752f74fae7f363230a68e06b7"
        fw1.close(); 

        Blob.createNewBlob(file1.getPath(), gitPath);

        File testFile = new File (gitPath + "/objects", "88d9814d5c99271752f74fae7f363230a68e06b7");

        if (testFile.exists()){
            System.out.println("blob exists!");
        }
        else{
            System.out.println("blob doesn't exist!");
        }

        FileReader reader = new FileReader(testFile);
        BufferedReader br = new BufferedReader(reader);
        StringBuilder contents = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null){
            contents.append(line);
        }
        br.close();

        if (contents.toString().equals("hello world and everyone in it!")){
            System.out.println("content is the same");
        }
        else{
            System.out.println("content is different");
        }
        File indexFile = new File(gitPath + "/index");
        BufferedReader bufr = new BufferedReader(new FileReader(indexFile));
        String indexLine;
        boolean lineFound = false;
        while ((indexLine = bufr.readLine()) != null){
            if (indexLine.equals ("88d9814d5c99271752f74fae7f363230a68e06b7 test.txt")){
                lineFound = true;
                System.out.println("index file has correct entry");
                break;
            }
        }
        if (!lineFound){
            System.out.println("index file doesn't have the correct entry");
        }
        bufr.close();

        resetTestFiles(file1, testFile, indexFile);

    }

    public static void resetTestFiles(File contentFile, File hashedFile, File indexFile) throws IOException{
        if (contentFile.delete()){
            System.out.println(contentFile.getName() + " was deleted");
        }
        else{
            System.out.println("failed to delete " + contentFile.getName());
        }

        if (hashedFile.delete()){
            System.out.println(hashedFile.getName() + " was deleted");
        }
        else{
            System.out.println("failed to delete " + hashedFile.getName());
        }


        File tempFile = new File("tempFile.txt");

        BufferedReader reader = new BufferedReader(new FileReader(indexFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String lineToRemove = "88d9814d5c99271752f74fae7f363230a68e06b7";
        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            String trimmedLine = currentLine.trim();
            if(trimmedLine.equals(lineToRemove)){
                writer.write(currentLine + "\n");
            }
        }
        writer.close(); 
        reader.close(); 
        boolean successful = tempFile.renameTo(indexFile);
        if (successful){
            System.out.println("deleted the update in index");
        }
        else{
            System.out.println("failed to delete hash from index");
        }
    }


}
