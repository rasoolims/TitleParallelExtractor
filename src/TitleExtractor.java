import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

public class TitleExtractor {
    public static void main(String[] args) throws Exception {
        BufferedReader titleReader = new BufferedReader(new FileReader(args[0]));
        HashMap<String, String> titleDict = new HashMap<>();
        HashMap<String, String> revTitleDict = new HashMap<>();

        System.out.println("Reading titles");
        String titleLine = null;
        while ((titleLine = titleReader.readLine()) != null) {
            String[] titles = titleLine.trim().split("\t");
            if (titles.length != 2) continue;
            titleDict.put(titles[0], titles[1]);
            revTitleDict.put(titles[1], titles[0]);
        }
        System.out.println(titleDict.size() + "->" + revTitleDict.size());

        System.out.println("Reading " + args[1]);
        BufferedReader smallWikiReader = new BufferedReader(new FileReader(args[1]));
        HashMap<String, String> wikiConent = new HashMap<>();
        String wikiLine = null;
        int lineNum = 0;
        while ((wikiLine = smallWikiReader.readLine()) != null) {
            String[] sentences = wikiLine.trim().split("</s>");
            String title = sentences[0].substring(sentences[0].indexOf(">") + 1).trim();
            if (titleDict.containsKey(title)) {
                wikiConent.put(title, wikiLine.trim());
            }
            lineNum++;
            if (lineNum % 1000 == 0)
                System.out.print(lineNum + " -> " + wikiConent.size() + "\r");
        }
        System.out.println("\n" + wikiConent.size());

        System.out.println("Reading " + args[2]);
        BufferedReader bigWikiReader = new BufferedReader(new FileReader(args[2]));
        BufferedWriter parWriter = new BufferedWriter(new FileWriter(args[3]));
        lineNum = 0;
        int parallel = 0;
        while ((wikiLine = bigWikiReader.readLine()) != null) {
            String[] sentences = wikiLine.trim().split("</s>");
            String title = sentences[0].substring(sentences[0].indexOf(">") + 1).trim();
            if (revTitleDict.containsKey(title) && wikiConent.containsKey(revTitleDict.get(title))) {
                parWriter.write(wikiConent.get(revTitleDict.get(title)));
                parWriter.write(wikiLine.trim());
                parWriter.write("\t");
                parWriter.write("\n");
                parallel++;
            }
            lineNum++;
            if (lineNum % 1000 == 0)
                System.out.print(lineNum + " -> " + parallel + "\r");
        }
        System.out.println("\nFinished: " + parallel);

    }
}
