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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lowagie.text.Chunk;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPRow;
import com.lowagie.text.pdf.PdfPTable;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import lombok.extern.slf4j.Slf4j;

/** Renders LaTeX in the elements produced by the existing assessment HTML parser. */
@Slf4j
final class LatexPdfRenderer {

    private static final Pattern MATH = Pattern.compile(
            "\\\\\\((.*?)\\\\\\)|\\\\\\[(.*?)\\\\\\]|\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
    private static final float IMAGE_SCALE = 4f;

    private LatexPdfRenderer() {
    }

    static void render(Iterable<?> elements) {
        for (Object element : elements) {
            if (element instanceof Phrase) {
                renderPhrase((Phrase) element);
            } else if (element instanceof PdfPTable) {
                for (PdfPRow row : ((PdfPTable) element).getRows()) {
                    if (row == null) {
                        continue;
                    }
                    for (PdfPCell cell : row.getCells()) {
                        if (cell != null) {
                            if (cell.getPhrase() != null) {
                                renderPhrase(cell.getPhrase());
                            }
                            if (cell.getCompositeElements() != null) {
                                render(cell.getCompositeElements());
                            }
                        }
                    }
                }
            } else if (element instanceof com.lowagie.text.List) {
                render(((com.lowagie.text.List) element).getItems());
            }
        }
    }

    private static void renderPhrase(Phrase phrase) {
        for (int index = 0; index < phrase.size(); index++) {
            Object element = phrase.get(index);
            if (!(element instanceof Chunk)) {
                if (element instanceof Phrase) {
                    renderPhrase((Phrase) element);
                }
                continue;
            }
            Chunk source = (Chunk) element;
            Matcher matcher = MATH.matcher(source.getContent());
            if (source.getImage() != null || !matcher.find()) {
                continue;
            }
            phrase.remove(index);
            int cursor = 0;
            do {
                if (matcher.start() > cursor) {
                    phrase.add(index++, textChunk(source.getContent().substring(cursor, matcher.start()), source));
                }
                String latex = matcher.group(1) != null ? matcher.group(1)
                        : matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
                Chunk replacement;
                try {
                    replacement = imageChunk(latex, source);
                } catch (Exception e) {
                    // Keep the source visible if an expression cannot be rendered.
                    log.warn("Could not render an assessment PDF equation ({})", e.getClass().getSimpleName());
                    replacement = textChunk(matcher.group(), source);
                }
                phrase.add(index++, replacement);
                cursor = matcher.end();
            } while (matcher.find());
            if (cursor < source.getContent().length()) {
                phrase.add(index++, textChunk(source.getContent().substring(cursor), source));
            }
            index--;
        }
    }

    private static Chunk textChunk(String text, Chunk source) {
        Chunk chunk = new Chunk(text, source.getFont());
        chunk.setAttributes(source.getAttributes());
        return chunk;
    }

    private static Chunk imageChunk(String latex, Chunk source) throws Exception {
        // Formula rendering must not load images from server files or URLs.
        if (latex.contains("\\includegraphics")) {
            throw new IllegalArgumentException("External images are not supported in PDF equations");
        }
        TeXIcon icon = new TeXFormula(latex).createTeXIcon(
                TeXConstants.STYLE_DISPLAY, source.getFont().getCalculatedSize() * IMAGE_SCALE);
        icon.setForeground(source.getFont().getColor() == null ? Color.BLACK : source.getFont().getColor());
        BufferedImage bitmap = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bitmap.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        Image image = Image.getInstance(bitmap, null);
        image.scaleAbsolute(icon.getIconWidth() / IMAGE_SCALE, icon.getIconHeight() / IMAGE_SCALE);
        return new Chunk(image, 0, -icon.getIconDepth() / IMAGE_SCALE, true);
    }
}
