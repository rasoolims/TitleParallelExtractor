import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

public class TitleExtractor {
    public static String[] extractSentences(String input) {
        String[] sentences = input.trim().split("</s>");
        sentences[0].substring(sentences[0].indexOf(">") + 1).trim();
        return sentences;
    }

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
            String[] sentences = extractSentences(wikiLine);
            String title = sentences[0];
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
        boolean extract = args.length > 3 && args[3].equals("comp");
        lineNum = 0;
        int parallel = 0;
        int comparabale = 0;
        while ((wikiLine = bigWikiReader.readLine()) != null) {
            String[] sentences = extractSentences(wikiLine);
            String title = sentences[0];
            if (revTitleDict.containsKey(title) && wikiConent.containsKey(revTitleDict.get(title))) {
                if (!extract) {
                    parWriter.write(wikiConent.get(revTitleDict.get(title)));
                    parWriter.write(wikiLine.trim());
                    parWriter.write("\t");
                    parWriter.write("\n");
                } else {
                    String[] srcSentences = extractSentences(wikiConent.get(revTitleDict.get(title)));
                    for (int r = 0; r < srcSentences.length; r++) {
                        String refSen = srcSentences[r];
                        float refRegion = ((float) r) / srcSentences.length;
                        int refLen = refSen.split(" ").length;

                        int doc_start_range = (int) Math.floor(Math.max(0, refRegion - 0.1) * sentences.length);
                        int doc_end_range = 1; // Title by title alignment
                        if (r > 0)
                            doc_end_range = (int) Math.ceil(Math.min(1, refRegion + 0.1) * sentences.length);

                        for (int s = doc_start_range; s < doc_end_range; s++) {
                            String sen = sentences[s];

                            int senLen = sen.split(" ").length;
                            double proportion = ((double) refLen) / senLen;
                            if (Math.abs(refLen - senLen) <= 3 || (0.9 <= proportion && proportion <= 1.1)) {
                                parWriter.write(refSen + " ||| " + sen + "\n");
                                comparabale++;
                            }
                        }
                    }
                }
                parallel++;
            }
            lineNum++;
            if (lineNum % 1000 == 0)
                System.out.print(lineNum + " -> " + comparabale + " / " + parallel + "\r");
        }
        System.out.println("\nFinished: " + parallel);
    }
}
