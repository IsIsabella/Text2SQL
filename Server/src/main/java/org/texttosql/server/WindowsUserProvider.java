package org.texttosql.server;

import net.jcip.annotations.ThreadSafe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для получения информации о текущем пользователе Windows.
 * Использует PowerShell + SID
 */
@ThreadSafe
public final class WindowsUserProvider {

    private WindowsUserProvider() {
    }

    /**
     * Данные текущего пользователя Windows
     *
     * @param sid SID
     * @param login логин
     * @param fullName ФИО
     * @param description описание
     * @param enabled активен ли
     */
    public record CurrentUser(
            String sid,
            String login,
            String fullName,
            String description,
            boolean enabled) {
    }

    /**
     * Возвращает информацию о текущем пользователе Windows
     *
     * @return объект с данными или null, если получить не удалось
     * @throws Exception если возникла ошибка при получении текущего пользователя
     */
    public static CurrentUser getCurrentUser() {
        try {
            String sid = WindowsUserProvider.executePowerShellSID(
                    "[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value").trim();

            if (sid.isEmpty()){
                return null;
            }

            String psCommand = """
                    $OutputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding
                    Get-LocalUser | Where-Object { $_.SID.Value -eq '%s' } |
                    Select-Object Name,FullName,Description,Enabled |
                    ConvertTo-Csv -NoTypeInformation
                    """.formatted(sid);

            List<String> lines = WindowsUserProvider.executePowerShellUser(psCommand);
            if (lines.size() < 2){
                return null;
            }

            List<String> parts = WindowsUserProvider.parseCsvLine(lines.get(1));
            if (parts.size() < 4){
                return null;
            }

            String login = parts.get(0);
            String fullName = parts.get(1).isEmpty() ? null : parts.get(1);
            String description = parts.get(2).isEmpty() ? null : parts.get(2);
            boolean enabled = "True".equalsIgnoreCase(parts.get(3));

            return new CurrentUser(sid, login, fullName, description, enabled);

        } catch (Exception e) {
            System.err.println("WindowsUserProvider: ошибка получения текущего пользователя: " + e.getMessage());
            return null;
        }
    }

    /**
     * Выполняет команду для получения sid
     *
     * @param command команда для выполнения
     * @return sid
     * @throws Exception если возникла ошибка при получении sid текущего пользователя
     */
    private static String executePowerShellSID(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
                "$OutputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding; " + command
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            p.waitFor();
            return sb.toString();
        }
    }

    /**
     * Выполняет команду для получения данных пользователя
     *
     * @param command команда для выполнения
     * @return данные пользователя
     * @throws Exception если возникла ошибка при получении данных
     */
    private static List<String> executePowerShellUser(String command) throws Exception {
        String fullCmd = "$OutputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding; " + command;
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", fullCmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        List<String> result = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) result.add(line);
        }
        p.waitFor();
        return result;
    }

    /**
     * Преобразование данных
     *
     * @param line данные
     * @return преобразованные в нужный формат данные
     * @throws Exception если возникла ошибка при получении данных
     */
    private static List<String> parseCsvLine(String line) {
        if (line == null) {
            throw new RuntimeException("Пустая строка");
        }
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb = new StringBuilder();
            } else sb.append(c);
        }
        result.add(sb.toString());
        return result;
    }
}