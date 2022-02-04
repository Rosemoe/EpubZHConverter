import com.github.houbb.opencc4j.util.ZhConverterUtil;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * EPUB 繁体转简体工具
 * @author Rosemoe
 */
public class EpubConverter {

    static int pageIndex;

    /**
     * 输出提示后，输入EPUB书籍路径并回车，自动生成繁体中文转换为简体中文的EPUB文件。
     */
    public static void main(String[] args) throws Exception {
        var input = new Scanner(System.in);
        System.out.println("请输入EPUB路径:");
        var path = input.nextLine();
        System.out.println("读取文件...");
        var book = new EpubReader().readEpub(new FileInputStream(path));
        System.out.println("正在转换...");
        book.getContents().forEach((page) -> {
            pageIndex ++;
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
        System.out.println("正在写出文件...");
        var newPath = path.replace(".epub", "-converted.epub");
        new EpubWriter().write(book, new FileOutputStream(newPath));
        System.out.println("新的EPUB文件已保存至: " + newPath);
    }

    private static String convertText(String text) {
        return ZhConverterUtil.toSimple(text);
    }

}
