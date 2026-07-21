import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;

public class Analyzer {

    private static final Set<String> YOKSAYILAN_KLASORLER = new HashSet<>(Arrays.asList(
        ".git", ".godot", ".vs", ".idea", "node_modules", "__pycache__", "out", "target", "bin", "analyzer"
    ));

    private static final Set<String> KOD_UZANTILARI = new HashSet<>(Arrays.asList(
        ".gd", ".cs", ".java", ".py", ".cpp", ".h", ".js", ".json", ".shader", ".tscn", ".xml", ".html"
    ));

    // Terminal Renk Kodları
    public static final String SIFIRLA = "\u001B[0m";
    public static final String MAVI_KALIN = "\u001B[1;34m";
    public static final String YESIL = "\u001B[32m";
    public static final String SARI = "\u001B[33m";
    public static final String SIYAN = "\u001B[36m";
    public static final String MOR = "\u001B[35m";
    public static final String BEYAZ_KALIN = "\u001B[1;37m";

    public static void main(String[] args) {
        File calismaDizini = new File(System.getProperty("user.dir"));
        File anaProjeDizini = calismaDizini.getParentFile() != null ? calismaDizini.getParentFile() : calismaDizini;

        baslikYazdir(anaProjeDizini.getName());

        File[] altKlasorler = anaProjeDizini.listFiles(File::isDirectory);

        if (altKlasorler == null || altKlasorler.length == 0) {
            System.out.println(" ⚠️  Analiz edilecek klasör bulunamadı.");
            return;
        }

        Arrays.sort(altKlasorler, Comparator.comparing(File::getName));

        long toplamProjeBoyutu = 0;
        long toplamProjeSatiri = 0;
        int toplamProjeDosyasi = 0;

        for (File klasor : altKlasorler) {
            if (YOKSAYILAN_KLASORLER.contains(klasor.getName())) continue;

            KlasorIstatistigi istatistik = klasorAnalizEt(klasor);

            toplamProjeBoyutu += istatistik.toplamBoyut;
            toplamProjeSatiri += istatistik.toplamSatir;
            toplamProjeDosyasi += istatistik.dosyaSayisi;

            tabloSatiriYazdir(
                "/" + klasor.getName(),
                istatistik.dosyaSayisi + " dosya, " + istatistik.klasorSayisi + " klasör",
                boyutFormatla(istatistik.toplamBoyut),
                istatistik.toplamSatir > 0 ? istatistik.toplamSatir + " satır" : "-",
                zamanFormati(istatistik.sonGuncelleme)
            );
        }

        tabloAltlikYazdir(toplamProjeDosyasi, boyutFormatla(toplamProjeBoyutu), toplamProjeSatiri);
    }

    private static KlasorIstatistigi klasorAnalizEt(File klasor) {
        KlasorIstatistigi stat = new KlasorIstatistigi();

        try {
            Files.walkFileTree(klasor.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (YOKSAYILAN_KLASORLER.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!dir.toFile().equals(klasor)) {
                        stat.klasorSayisi++;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    stat.dosyaSayisi++;
                    long boyut = attrs.size();
                    stat.toplamBoyut += boyut;

                    long sonMod = attrs.lastModifiedTime().toMillis();
                    if (sonMod > stat.sonGuncelleme) {
                        stat.sonGuncelleme = sonMod;
                    }

                    String dosyaAdi = file.getFileName().toString().toLowerCase();
                    String uzanti = dosyaAdi.contains(".") ? dosyaAdi.substring(dosyaAdi.lastIndexOf(".")) : "";

                    if (KOD_UZANTILARI.contains(uzanti)) {
                        try {
                            stat.toplamSatir += Files.lines(file).filter(l -> !l.trim().isEmpty()).count();
                        } catch (IOException ignored) {}
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        return stat;
    }

    private static void baslikYazdir(String projeAdi) {
        System.out.println();
        System.out.println(MOR + "📊 OYUN PROJESİ ANALİZ RAPORU " + SIFIRLA + "| Hedef: " + SIYAN + projeAdi + SIFIRLA);
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf(BEYAZ_KALIN + "%-18s \t %-22s \t %-12s \t %-12s \t %-15s\n" + SIFIRLA,
                "KLASÖR", "İÇERİK", "BOYUT", "KOD SATIRI", "SON GÜNCELLEME");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private static void tabloSatiriYazdir(String klasor, String icerik, String boyut, String kodSatiri, String sonGuncelleme) {
        System.out.printf(MAVI_KALIN + "%-18s" + SIFIRLA + " \t %-22s \t " + SARI + "%-12s" + SIFIRLA + " \t " + YESIL + "%-12s" + SIFIRLA + " \t %-15s\n",
                klasor, icerik, boyut, kodSatiri, sonGuncelleme);
    }

    private static void tabloAltlikYazdir(int toplamDosya, String toplamBoyut, long toplamSatir) {
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf(BEYAZ_KALIN + "%-18s" + SIFIRLA + " \t %-22s \t " + SARI + "%-12s" + SIFIRLA + " \t " + YESIL + "%-12s" + SIFIRLA + " \t %-15s\n",
                "GENEL TOPLAM", toplamDosya + " Dosya", toplamBoyut, toplamSatir + " satır", "-");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println();
    }

    private static String boyutFormatla(long bayt) {
        if (bayt <= 0) return "0 B";
        final String[] birimler = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bayt) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bayt / Math.pow(1024, digitGroups)) + " " + birimler[digitGroups];
    }

    private static String zamanFormati(long zamanMs) {
        if (zamanMs == 0) return "Bilinmiyor";
        long farkMs = System.currentTimeMillis() - zamanMs;
        long dakika = farkMs / (1000 * 60);
        long saat = dakika / 60;
        long gun = saat / 24;

        if (dakika < 1) return "Az önce";
        if (dakika < 60) return dakika + " dk önce";
        if (saat < 24) return saat + " saat önce";
        return gun + " gün önce";
    }

    private static class KlasorIstatistigi {
        int dosyaSayisi = 0;
        int klasorSayisi = 0;
        long toplamBoyut = 0;
        long toplamSatir = 0;
        long sonGuncelleme = 0;
    }
}
