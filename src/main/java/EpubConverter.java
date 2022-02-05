import com.github.houbb.opencc4j.util.ZhConverterUtil;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * EPUB 繁体转简体工具
 *
 * @author Rosemoe
 */
public class EpubConverter {

    static int pageIndex;
    static int command;

    /**
     * 输出提示后，输入EPUB书籍路径并回车，自动生成繁体中文转换为简体中文的EPUB文件。
     */
    public static void main(String[] args) throws Exception {
        var input = new Scanner(System.in);
        System.out.println("输入转换类型（0: 繁转简，1:简转繁）");
        command = input.nextInt();
        System.out.println("请输入EPUB路径:");
        var path = input.nextLine();
        System.out.println("读取文件...");
        var book = new EpubReader().readEpub(new FileInputStream(path));
        System.out.println("正在转换...");
        book.getContents().forEach((page) -> {
            pageIndex++;
            System.out.println("正在转换第 " + pageIndex + " 节 （总计" + book.getContents().size() + " 节)");
            if ("application/xhtml+xml".equals(page.getMediaType().getName())) {
                try {
                    var original = new String(page.getData(), page.getInputEncoding());
                    page.setData(convertText(original).getBytes(page.getInputEncoding()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                page.setTitle(convertText(page.getTitle()));
            }
        });
        book.setOpfResource(new Resource(new String(book.getOpfResource().getData(), book.getOpfResource().getInputEncoding()).replace("'", "\"").getBytes(StandardCharsets.UTF_8), book.getOpfResource().getMediaType()));
        book.setNcxResource(new Resource(new String(book.getNcxResource().getData(), book.getNcxResource().getInputEncoding()).replace("'", "\"").getBytes(StandardCharsets.UTF_8), book.getNcxResource().getMediaType()));
        System.out.println("正在写出文件...");
        var tmpPath = path.replace(".epub", "-tmp.epub");
        var newPath = path.replace(".epub", "-converted.epub");
        new EpubWriter().write(book, new FileOutputStream(tmpPath));
        var zip = new ZipFile(tmpPath);
        var out = new ZipOutputStream(new FileOutputStream(newPath));
        final var buffer = new byte[8192];
        zip.entries().asIterator().forEachRemaining((zipEntry -> {
            System.out.println(zipEntry.getName());
            try {
                out.putNextEntry(zipEntry);
                if (zipEntry.getName().equals("OEBPS/content.opf") || zipEntry.getName().equals("OEBPS/toc.ncx")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(zipEntry)));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        if (first) {
                            sb.append(line.replace("'", "\""));
                            first = false;
                        } else {
                            sb.append('\n').append(line);
                        }
                    }
                    br.close();
                    out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    var in = zip.getInputStream(zipEntry);
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                out.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        zip.close();
        out.close();
        if(!new File(tmpPath).delete()) {
            System.err.println("删除临时文件失败");
        }
        System.out.println("新的EPUB文件已保存至: " + newPath);
    }

    private static String convertText(String text) {
        return command == 0 ? ZhConverterUtil.toSimple(text) : ZhConverterUtil.toTraditional(text);
    }

}
