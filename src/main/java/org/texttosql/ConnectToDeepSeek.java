package org.texttosql;

import java.io.*;

public class ConnectToDeepSeek {
    private String question;
    private final String pythonScriptPath = "G:\\py\\LLM\\ConnectModel.py";

    public ConnectToDeepSeek(String question) {
        this.question = question;
    }

    public String connection() throws Exception {
        try {
            return executePythonScript();
        } catch (Exception ex) {
            throw new Exception("Ошибка при соединении с API: " + ex.getMessage());
        }
    }

    private String executePythonScript() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath, question);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Python Error: " + result.toString());
        }
        return result.toString().trim();
    }
}