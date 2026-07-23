import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

    // Terminal Color Codes (matching Analyzer.java style)
    public static final String RESET = "\u001B[0m";
    public static final String BLUE_BOLD = "\u001B[1;34m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String PURPLE = "\u001B[35m";
    public static final String WHITE_BOLD = "\u001B[1;37m";
    public static final String RED = "\u001B[31m";

    // Standard AES-128 Key for Project A
    private static final String ENCRYPTION_KEY_PHRASE = "ProjectA_2048_Suika_Physics_Engine_SecretKey_2026";
    private static final byte[] MAGIC_HEADER = new byte[]{'P', 'R', 'O', 'J'};

    public static void main(String[] args) {
        File currentDir = new File(System.getProperty("user.dir"));
        // If run inside tools/Encryptor or root, resolve project root directory
        File rootDir = currentDir;
        if (new File(currentDir, "version.toml").exists()) {
            rootDir = currentDir;
        } else if (currentDir.getParentFile() != null && new File(currentDir.getParentFile(), "version.toml").exists()) {
            rootDir = currentDir.getParentFile();
        } else if (currentDir.getParentFile() != null && currentDir.getParentFile().getParentFile() != null 
                   && new File(currentDir.getParentFile().getParentFile(), "version.toml").exists()) {
            rootDir = currentDir.getParentFile().getParentFile();
        }

        printHeader(rootDir.getName());

        List<FileEntry> filesToEncrypt = collectTomlFiles(rootDir);
        File outputFile = new File(rootDir, "data.bin");

        long totalRawBytes = 0;
        long totalEncryptedBytes = 0;
        int successCount = 0;

        try {
            byte[] keyBytes = deriveKey(ENCRYPTION_KEY_PHRASE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Magic header
            dos.write(MAGIC_HEADER);
            // Number of files
            dos.writeInt(filesToEncrypt.size());

            for (FileEntry entry : filesToEncrypt) {
                byte[] rawContent = Files.readAllBytes(entry.file.toPath());
                byte[] encryptedContent = encryptAES(rawContent, keyBytes);

                byte[] pathBytes = entry.relativePath.getBytes(StandardCharsets.UTF_8);
                dos.writeShort(pathBytes.length);
                dos.write(pathBytes);
                dos.writeInt(encryptedContent.length);
                dos.write(encryptedContent);

                totalRawBytes += rawContent.length;
                totalEncryptedBytes += encryptedContent.length;
                successCount++;

                printTableRow(
                    entry.relativePath,
                    formatSize(rawContent.length),
                    formatSize(encryptedContent.length),
                    GREEN + "ENCRYPTED" + RESET
                );
            }

            dos.flush();
            byte[] fullBinaryPayload = baos.toByteArray();
            Files.write(outputFile.toPath(), fullBinaryPayload);

            printFooter(successCount, formatSize(totalRawBytes), formatSize(fullBinaryPayload.length), outputFile.getName());

        } catch (Exception e) {
            System.err.println(RED + "Encryption failed: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    private static List<FileEntry> collectTomlFiles(File rootDir) {
        List<FileEntry> list = new ArrayList<>();
        Path rootPath = rootDir.toPath();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equalsIgnoreCase(".git") || name.equalsIgnoreCase("tools") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("target")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".toml")) {
                        Path relPath = rootPath.relativize(file);
                        String normalizedPath = relPath.toString().replace('\\', '/');
                        list.add(new FileEntry(file.toFile(), normalizedPath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        list.sort(Comparator.comparing(a -> a.relativePath));
        return list;
    }

    private static byte[] deriveKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16]; // AES-128
        System.arraycopy(hash, 0, key, 0, 16);
        return key;
    }

    public static byte[] encryptAES(byte[] data, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    public static byte[] decryptAES(byte[] encryptedData, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }

    private static void printHeader(String projectName) {
        System.out.println();
        System.out.println(PURPLE + "🔐 PROJECT A DATA ENCRYPTOR " + RESET + "| Target: " + CYAN + projectName + RESET);
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.printf(WHITE_BOLD + "%-45s \t %-12s \t %-14s \t %-15s\n" + RESET,
                "CONFIG FILE", "RAW SIZE", "ENCRYPTED SIZE", "STATUS");
        System.out.println("---------------------------------------------------------------------------------------------------");
    }

    private static void printTableRow(String file, String rawSize, String encSize, String status) {
        System.out.printf(BLUE_BOLD + "%-45s" + RESET + " \t " + YELLOW + "%-12s" + RESET + " \t " + CYAN + "%-14s" + RESET + " \t %-15s\n",
                file, rawSize, encSize, status);
    }

    private static void printFooter(int totalFiles, String totalRawSize, String totalEncSize, String outputFileName) {
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.printf(WHITE_BOLD + "%-45s" + RESET + " \t " + YELLOW + "%-12s" + RESET + " \t " + GREEN + "%-14s" + RESET + " \t %-15s\n",
                "GRAND TOTAL (" + totalFiles + " files -> " + outputFileName + ")", totalRawSize, totalEncSize, GREEN + "SUCCESS" + RESET);
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.println();
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private static class FileEntry {
        File file;
        String relativePath;

        FileEntry(File file, String relativePath) {
            this.file = file;
            this.relativePath = relativePath;
        }
    }
}
