/*
 * SonarQube Extended-PDF Report (Maven plugin)
 * Copyright (C) 2014 hCentive - Technology Solutions to Simplify Healthcare
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarqube.report.extendedpdf;

import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import com.lowagie.text.pdf.*;
import org.sonar.report.pdf.entity.Project;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import org.sonar.report.pdf.util.Logger;

/**
 * Created by Momin.Khan
 * Page template - includes header and footer
 */
public class ExtendedHeader extends PdfPageEventHelper {
    private Project project;

    public ExtendedHeader(Project project) {
        this.project = project;
    }

    public void onEndPage(PdfWriter writer, Document document) {
        String pageTemplate = "/templates/page.pdf";
        try {
            PdfContentByte cb = writer.getDirectContentUnder();
            PdfReader reader = new PdfReader(this.getClass().getResourceAsStream(pageTemplate));
            PdfImportedPage page = writer.getImportedPage(reader, 1);
            cb.addTemplate(page, 0, 0);

            Font font = FontFactory.getFont(FontFactory.COURIER, 12, Font.NORMAL, Color.GRAY);
            Rectangle pageSize = document.getPageSize();
            PdfPTable head = new PdfPTable(1);
            head.getDefaultCell().setVerticalAlignment(PdfCell.ALIGN_MIDDLE);
            head.getDefaultCell().setHorizontalAlignment(PdfCell.ALIGN_CENTER);
            head.getDefaultCell().setBorder(0);
            Phrase projectName = new Phrase(project.getName(), font);
            head.addCell(projectName);
            head.setTotalWidth(pageSize.getWidth() - document.leftMargin() - document.rightMargin());
            head.writeSelectedRows(0, -1, document.leftMargin(), pageSize.getHeight() - 15, writer.getDirectContent());
            head.setSpacingAfter(10);

            PdfPTable foot = new PdfPTable(1);
            foot.getDefaultCell().setVerticalAlignment(PdfCell.ALIGN_MIDDLE);
            foot.getDefaultCell().setHorizontalAlignment(PdfCell.ALIGN_LEFT);
            foot.getDefaultCell().setBorder(0);
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm");
            Phrase projectAnalysisDate = new Phrase(df.format(project.getMeasures().getDate()), font);
            foot.addCell(projectAnalysisDate);
            foot.setTotalWidth(pageSize.getWidth() - document.leftMargin() - document.rightMargin());
            foot.writeSelectedRows(0, -1, document.leftMargin(), 20, writer.getDirectContent());
            foot.setSpacingBefore(10);
        } catch (IOException e) {
            Logger.error("Cannot find the required template: " + pageTemplate);
            e.printStackTrace();
        }
    }
}
