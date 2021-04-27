package client;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class SaveChat {

    /**
     * The method returns a file that creates or finds an existing one
     * @param login Clint's login
     * @return Client's file
     * @throws IOException In/Out Error
     */
    public File findFile(String login) throws IOException {
        File file = new File("history/history_" + login + ".txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    /**
     * The method saves the chat history in a file
     * @param file Client's file
     * @param history Client's message
     */
    public void addHistory(File file, String history) {
        byte[] outData = history.getBytes();
        try (FileOutputStream reader = new FileOutputStream(file, true)) {
            reader.write(outData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method displays the number of lines from the message history at start
     * @param file Client's file
     * @param countOfLines Number of lines
     * @return History of messages
     * @throws IOException In/Out Error
     */
    public String publicHistory(File file, int countOfLines) throws IOException {
        List<String> lines = new LinkedList<>();
        FileReader fr = new FileReader(file);
        BufferedReader reader = new BufferedReader(fr);
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        for (int i = 0; i < lines.size(); i++) {
            if(i >= lines.size() - countOfLines)
                sb.append(lines.get(i)+"\n");
        }
        fr.close();
        reader.close();
        return sb.toString();
    }
}
