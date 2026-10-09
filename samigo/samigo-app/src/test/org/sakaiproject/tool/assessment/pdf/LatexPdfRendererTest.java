/**
 * Copyright (c) 2026 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.tool.assessment.pdf;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPRow;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import org.junit.Test;

public class LatexPdfRendererTest {

    private static final String EOQ = "\\(Q^* = \\sqrt{\\frac{2D \\times C_O}{C_h}}"
            + " = \\sqrt{\\frac{2 \\times 38 \\times 30}{0.25}}\\)";

    @Test
    public void rendersBothFeedbackEquationsInDownloadedPdf() throws Exception {
        List<Element> elements = parse("<p>What is the EOQ for widgets? ____ widgets</p>"
                + "<p><i>Round to the nearest 0.1 widgets</i></p>"
                + "<font size='2'>Answer Point Value: 1 points<br>Answer Key: 95|96<br>"
                + "Correct Feedback:<br>" + EOQ + "<br><br>Incorrect Feedback:<br>" + EOQ + "</font>");
        LatexPdfRenderer.render(elements);

        assertEquals(2, images(elements).size());
        String text = pdfText(elements);
        assertTrue(text.contains("Correct Feedback:"));
        assertTrue(text.contains("Incorrect Feedback:"));
        assertTrue(text.contains("Answer Key: 95|96"));
        assertFalse(text.contains("\\sqrt"));
        assertFalse(text.contains("\\("));
    }

    @Test
    public void supportsAllThreeMathDelimitersAndPreservesSurroundingText() throws Exception {
        List<Element> elements = parse("<p>before $$x+1$$ middle \\(x+2\\) next \\[x+3\\] after</p>");
        LatexPdfRenderer.render(elements);

        assertEquals(3, images(elements).size());
        String text = pdfText(elements);
        assertTrue(text.contains("before"));
        assertTrue(text.contains("middle"));
        assertTrue(text.contains("next"));
        assertTrue(text.contains("after"));
        assertFalse(text.contains("$$"));
    }

    @Test
    public void rendersEquationsInsideNestedTablesAndLists() throws Exception {
        List<Element> elements = parse("<table><tr><td><table><tr><td>" + EOQ
                + "</td></tr></table></td></tr></table><ul><li>$$x+1$$</li></ul>");
        LatexPdfRenderer.render(elements);

        assertEquals(2, images(elements).size());
        assertFalse(pdfText(elements).contains("\\sqrt"));
    }

    @Test
    public void leavesInvalidAndUnclosedExpressionsVisible() throws Exception {
        String invalid = "\\(\\notASupportedCommand{1}\\)";
        List<Element> elements = parse("<p>" + invalid + " then $$x+1$$ and \\(unclosed</p>");
        LatexPdfRenderer.render(elements);

        assertEquals(1, images(elements).size());
        String text = pdfText(elements);
        assertTrue(text.contains(invalid));
        assertTrue(text.contains("\\(unclosed"));
    }

    @Test
    public void leavesExternalImageCommandsAsText() throws Exception {
        String formula = "\\(\\includegraphics{/server/private.png}\\)";
        List<Element> elements = parse("<p>" + formula + "</p>");
        LatexPdfRenderer.render(elements);

        assertTrue(images(elements).isEmpty());
        assertTrue(pdfText(elements).contains(formula));
    }

    @Test
    public void rendersEquationsInSimpleTableCellPhrases() throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.addCell(new Phrase(EOQ));
        List<Element> elements = Collections.singletonList(table);
        LatexPdfRenderer.render(elements);

        assertEquals(1, images(elements).size());
        assertFalse(pdfText(elements).contains("\\sqrt"));
    }

    @Test
    public void leavesOrdinaryTextAndExistingImagesUnchanged() throws Exception {
        List<Element> elements = parse("<p><b>Cost: $5</b> and ordinary text</p>");
        Image existingImage = Image.getInstance(1, 1, 3, 8, new byte[] {0, 0, 0});
        ((Phrase) elements.get(0)).add(new Chunk(existingImage, 0, 0));
        Image originalImage = images(elements).get(0);
        Element original = elements.get(0);
        String before = pdfText(elements);
        LatexPdfRenderer.render(elements);

        assertSame(original, elements.get(0));
        assertEquals(before, pdfText(elements));
        assertEquals(1, images(elements).size());
        assertSame(originalImage, images(elements).get(0));
    }

    @Test
    public void scalesEquationsWithTheChosenFontSize() throws Exception {
        List<Element> small = parse("<font size='1'>" + EOQ + "</font>");
        List<Element> large = parse("<font size='5'>" + EOQ + "</font>");
        LatexPdfRenderer.render(small);
        LatexPdfRenderer.render(large);

        assertTrue(images(large).get(0).getScaledHeight() > images(small).get(0).getScaledHeight() * 2);
    }

    @Test
    public void preservesTextFormattingAroundInlineEquations() throws Exception {
        List<Element> elements = parse("<p><b>before \\(x+1\\) after</b></p>");
        LatexPdfRenderer.render(elements);

        for (Object value : elements.get(0).getChunks()) {
            Chunk chunk = (Chunk) value;
            if (chunk.getImage() == null) {
                assertTrue(chunk.getFont().isBold());
            }
        }
        assertEquals(1, images(elements).size());
    }

    private static List<Element> parse(String html) throws Exception {
        return com.lowagie.text.html.simpleparser.HTMLWorker.parseToList(
                new StringReader(html), null, Collections.<String, Object>emptyMap());
    }

    private static String pdfText(List<Element> elements) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, output);
        document.open();
        for (Element element : elements) {
            document.add(element);
        }
        document.close();
        PdfReader reader = new PdfReader(output.toByteArray());
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static List<Image> images(Iterable<?> elements) {
        List<Image> result = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof PdfPTable) {
                for (PdfPRow row : ((PdfPTable) element).getRows()) {
                    for (PdfPCell cell : row.getCells()) {
                        if (cell != null && cell.getCompositeElements() != null) {
                            result.addAll(images(cell.getCompositeElements()));
                        }
                        if (cell != null && cell.getPhrase() != null) {
                            result.addAll(images(Collections.singletonList(cell.getPhrase())));
                        }
                    }
                }
            } else if (element instanceof com.lowagie.text.List) {
                result.addAll(images(((com.lowagie.text.List) element).getItems()));
            } else if (element instanceof Phrase) {
                for (Object value : ((Phrase) element).getChunks()) {
                    Image image = ((Chunk) value).getImage();
                    if (image != null) {
                        result.add(image);
                    }
                }
            }
        }
        return result;
    }
}
