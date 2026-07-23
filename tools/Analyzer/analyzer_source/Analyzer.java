import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;

public class Analyzer {

    private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>(Arrays.asList(
        ".git", ".godot", ".vs", ".idea", "node_modules", "__pycache__", "out", "target", "bin", "analyzer"
    ));

    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".gd", ".cs", ".java", ".py", ".cpp", ".h", ".js", ".json", ".shader",
        ".tscn", ".xml", ".html", ".toml", ".yaml", ".yml", ".ini", ".cfg"
    ));

    // Terminal Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BLUE_BOLD = "\u001B[1;34m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String PURPLE = "\u001B[35m";
    public static final String WHITE_BOLD = "\u001B[1;37m";

    public static void main(String[] args) {
        File currentDir = new File(System.getProperty("user.dir"));
        File rootDir = currentDir.getParentFile() != null ? currentDir.getParentFile() : currentDir;

        printHeader(rootDir.getName());

        // 1. Ekrandaki ağaç yapısını yazdırıyoruz
        analyzeDirectoryRecursive(rootDir, "");

        // 2. GRAND TOTAL için tüm projeyi (mükerrer olmadan) TEK SEFERDE hesaplıyoruz
        FolderStat globalStat = analyzeFolderContent(rootDir);

        printFooter(globalStat.fileCount, formatSize(globalStat.totalSize), globalStat.totalLines);
    }

    private static void analyzeDirectoryRecursive(File currentDir, String indent) {
        File[] children = currentDir.listFiles();
        if (children == null) return;

        List<File> subDirs = new ArrayList<>();
        for (File child : children) {
            if (child.isDirectory() && !IGNORED_DIRECTORIES.contains(child.getName())) {
                subDirs.add(child);
            }
        }

        subDirs.sort(Comparator.comparing(File::getName));

        for (int i = 0; i < subDirs.size(); i++) {
            File dir = subDirs.get(i);
            boolean isLast = (i == subDirs.size() - 1);
            String prefix = isLast ? "└── " : "├── ";

            FolderStat dirStat = analyzeFolderContent(dir);

            String folderDisplayName = indent + prefix + dir.getName();

            printTableRow(
                folderDisplayName,
                dirStat.fileCount + " files, " + dirStat.folderCount + " dirs",
                formatSize(dirStat.totalSize),
                dirStat.totalLines > 0 ? dirStat.totalLines + " lines" : "-",
                formatTime(dirStat.lastModified)
            );

            String nextIndent = indent + (isLast ? "    " : "│   ");
            analyzeDirectoryRecursive(dir, nextIndent);
        }
    }

    private static FolderStat analyzeFolderContent(File folder) {
        FolderStat stat = new FolderStat();

        try {
            Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (IGNORED_DIRECTORIES.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!dir.toFile().equals(folder)) {
                        stat.folderCount++;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    stat.fileCount++;
                    long size = attrs.size();
                    stat.totalSize += size;

                    long lastMod = attrs.lastModifiedTime().toMillis();
                    if (lastMod > stat.lastModified) {
                        stat.lastModified = lastMod;
                    }

                    String fileName = file.getFileName().toString().toLowerCase();
                    String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";

                    if (CODE_EXTENSIONS.contains(ext)) {
                        try {
                            stat.totalLines += Files.lines(file).filter(l -> !l.trim().isEmpty()).count();
                        } catch (IOException ignored) {}
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        return stat;
    }

    private static void printHeader(String projectName) {
        System.out.println();
        System.out.println(PURPLE + "📊 GAME PROJECT ANALYSIS REPORT " + RESET + "| Target: " + CYAN + projectName + RESET);
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.printf(WHITE_BOLD + "%-32s \t %-20s \t %-12s \t %-12s \t %-15s\n" + RESET,
                "DIRECTORY", "CONTENT", "SIZE", "CODE LINES", "LAST MODIFIED");
        System.out.println("---------------------------------------------------------------------------------------------------");
    }

    private static void printTableRow(String folder, String content, String size, String codeLines, String lastModified) {
        System.out.printf(BLUE_BOLD + "%-32s" + RESET + " \t %-20s \t " + YELLOW + "%-12s" + RESET + " \t " + GREEN + "%-12s" + RESET + " \t %-15s\n",
                folder, content, size, codeLines, lastModified);
    }

    private static void printFooter(int totalFiles, String totalSize, long totalLines) {
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.printf(WHITE_BOLD + "%-32s" + RESET + " \t %-20s \t " + YELLOW + "%-12s" + RESET + " \t " + GREEN + "%-12s" + RESET + " \t %-15s\n",
                "GRAND TOTAL", totalFiles + " Files", totalSize, totalLines + " lines", "-");
        System.out.println("---------------------------------------------------------------------------------------------------");
        System.out.println();
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private static String formatTime(long timeMs) {
        if (timeMs == 0) return "Unknown";
        long diffMs = System.currentTimeMillis() - timeMs;
        long minutes = diffMs / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        if (hours < 24) return hours + " hours ago";
        return days + " days ago";
    }

    private static class FolderStat {
        int fileCount = 0;
        int folderCount = 0;
        long totalSize = 0;
        long totalLines = 0;
        long lastModified = 0;
    }
}
