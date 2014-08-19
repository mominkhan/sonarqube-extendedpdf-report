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

import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.openqa.selenium.internal.Base64Encoder;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.sonar.report.pdf.PDFReporter;
import org.sonar.report.pdf.Style;
import org.sonar.report.pdf.Toc;
import org.sonar.report.pdf.entity.Project;
import org.sonar.report.pdf.entity.exception.ReportException;
import org.sonar.report.pdf.util.Credentials;
import org.sonar.report.pdf.util.Logger;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Momin.Khan
 */
public class OverviewPDFReporter extends PDFReporter {
    private String projectKey;
    private String sonarUrl;
    private Properties configProperties;
    private Properties langProperties;
    private File outputDirectory;
    private String reportDashboardID;
    private String screenshotsDir;
    private String dashboardUrl;
    private String phantomjsPath;

    public OverviewPDFReporter(String projectKey, String sonarUrl,
                               String reportDashboardID,Properties configProperties,
                               Properties langProperties, File outputDirectory, String phantomjsPath) {
        this.projectKey = projectKey;
        this.sonarUrl = sonarUrl;
        this.reportDashboardID = reportDashboardID;
        this.configProperties = configProperties;
        this.langProperties = langProperties;
        reportType = "overview";
        this.outputDirectory = outputDirectory;
        this.screenshotsDir = outputDirectory + File.separator + "screenshots";
        this.dashboardUrl = sonarUrl + "/dashboard/index/" + projectKey + "?did=" + this.reportDashboardID;
        this.phantomjsPath = phantomjsPath;
    }

    @Override
    protected URL getLogo(){
        return null;  //no-op
    }

    @Override
    protected String getProjectKey() {
        return this.projectKey;
    }

    @Override
    protected String getSonarUrl() {
        return this.sonarUrl;
    }

    @Override
    protected Properties getLangProperties() {
        return langProperties;
    }

    @Override
    protected Properties getReportProperties() {
        return configProperties;
    }

    protected File getOutputDirectory() {
        return outputDirectory;
    }

    protected String getScreenshotsDir() {
        return this.screenshotsDir;
    }

    protected String getDashboardUrl() {
        return this.dashboardUrl;
    }

    protected String getPhantomjsPath() { return this.phantomjsPath; }

    @Override
    public ByteArrayOutputStream getReport() throws DocumentException, IOException, org.dom4j.DocumentException,
            ReportException {
        // Capture and save screenshots of the required widgets
        captureScreenshots();

        // Creation of documents
        Document mainDocument = new Document(PageSize.LETTER, 50, 50, 75, 50);
        ExtendedToc tocDocument = new ExtendedToc();
        Document frontPageDocument = new Document(PageSize.LETTER, 50, 50, 75, 50);

        ByteArrayOutputStream mainDocumentBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream frontPageDocumentBaos = new ByteArrayOutputStream();

        PdfWriter mainDocumentWriter = PdfWriter.getInstance(mainDocument, mainDocumentBaos);
        PdfWriter frontPageDocumentWriter = PdfWriter.getInstance(frontPageDocument, frontPageDocumentBaos);

        mainDocumentWriter.setStrictImageSequence(true);
        frontPageDocumentWriter.setStrictImageSequence(true);

        // Events for TOC, header and page numbers
        ExtendedEvents events = new ExtendedEvents(tocDocument, new ExtendedHeader(this.getProject()));
        mainDocumentWriter.setPageEvent(events);

        mainDocument.open();
        tocDocument.getTocDocument().open();
        frontPageDocument.open();

        Logger.info("Generating Overview PDF report...");
        printFrontPage(frontPageDocument, frontPageDocumentWriter);
        printTocTitle(tocDocument);
        printPdfBody(mainDocument);

        mainDocument.close();
        tocDocument.getTocDocument().close();
        frontPageDocument.close();

        // Get Readers
        PdfReader mainDocumentReader = new PdfReader(mainDocumentBaos.toByteArray());
        PdfReader tocDocumentReader = new PdfReader(tocDocument.getTocOutputStream().toByteArray());
        PdfReader frontPageDocumentReader = new PdfReader(frontPageDocumentBaos.toByteArray());

        // New document
        Document documentWithToc = new Document(tocDocumentReader.getPageSizeWithRotation(1));
        ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
        PdfCopy copy = new PdfCopy(documentWithToc, finalBaos);

        documentWithToc.open();
        copy.addPage(copy.getImportedPage(frontPageDocumentReader, 1));
        for (int i = 1; i <= tocDocumentReader.getNumberOfPages(); i++) {
            copy.addPage(copy.getImportedPage(tocDocumentReader, i));
        }
        for (int i = 1; i <= mainDocumentReader.getNumberOfPages(); i++) {
            copy.addPage(copy.getImportedPage(mainDocumentReader, i));
        }
        documentWithToc.close();

        // Return the final document (with TOC)
        return finalBaos;
    }

    public void captureScreenshots() {
        Base64Encoder encoder = new Base64Encoder();
        String encodedCredentials = encoder.encode((Credentials.getUsername() + ":" + Credentials.getPassword()).getBytes());
        DesiredCapabilities caps = new DesiredCapabilities();
        if(phantomjsPath != null){
           caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);
        }
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Authorization",
                "Basic " + encodedCredentials);
        WebDriver driver = new PhantomJSDriver(caps);

        String[] chapters = StringUtils.split(configProperties.getProperty("chapters"), ",");
        List<String> cssSelectors = new ArrayList<String>();
        for (String chapter : chapters) {
            String[] sections = StringUtils.split(configProperties.getProperty(chapter + ".sections"), ",");
            for (String section : sections) {
                cssSelectors.add("." + section);
            }
        }

        try {
            driver.get(dashboardUrl);
            for (String selector : cssSelectors) {
                captureScreenshot(driver, selector);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private void captureScreenshot(WebDriver driver, String selector) throws IOException {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        driver.get(dashboardUrl);
        System.out.println("Screenshot captured...");
        List<WebElement> elements = driver.findElements(By.cssSelector(selector));
        if(!elements.isEmpty()) {
            WebElement element = elements.get(0);
            BufferedImage fullImg = ImageIO.read(screenshot);
            Point point = element.getLocation();
            int eleWidth = element.getSize().getWidth();
            int eleHeight = element.getSize().getHeight();
            BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(), eleWidth,
                    eleHeight);
            ImageIO.write(eleScreenshot, "png", screenshot);
            System.out.println("Cropping to get widget: " + selector.substring(1));
            FileUtils.copyFile(screenshot, new File(screenshotsDir + File.separator + selector.substring(1) + ".png"));
        }
    }

    protected void printFrontPage(Document frontPageDocument, PdfWriter frontPageWriter)
            throws org.dom4j.DocumentException, ReportException {
        String frontPageTemplate = "/templates/frontpage.pdf";
        try {
            PdfContentByte cb = frontPageWriter.getDirectContent();
            PdfReader reader = new PdfReader(this.getClass().getResourceAsStream(frontPageTemplate));
            PdfImportedPage page = frontPageWriter.getImportedPage(reader, 1);
            frontPageDocument.newPage();
            cb.addTemplate(page, 0, 0);

            Project project = getProject();

            Rectangle pageSize = frontPageDocument.getPageSize();
            PdfPTable projectInfo = new PdfPTable(1);
            projectInfo.getDefaultCell().setVerticalAlignment(PdfCell.ALIGN_MIDDLE);
            projectInfo.getDefaultCell().setHorizontalAlignment(PdfCell.ALIGN_LEFT);
            projectInfo.getDefaultCell().setBorder(Rectangle.BOTTOM);
            projectInfo.getDefaultCell().setPaddingBottom(10);
            projectInfo.getDefaultCell().setBorderColor(Color.GRAY);
            Font font = FontFactory.getFont(FontFactory.COURIER, 18, Font.NORMAL, Color.LIGHT_GRAY);

            Phrase projectName = new Phrase("Project: " + project.getName(), font);
            projectInfo.addCell(projectName);

            Phrase projectVersion = new Phrase("Version: " + project.getMeasures().getVersion(), font);
            projectInfo.addCell(projectVersion);

            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm");
            Phrase projectAnalysisDate = new Phrase("Analysis Date: " + df.format(project.getMeasures().getDate()), font);
            projectInfo.addCell(projectAnalysisDate);

            projectInfo.setTotalWidth(pageSize.getWidth() - frontPageDocument.leftMargin()*2 - frontPageDocument.rightMargin()*2);
            projectInfo.writeSelectedRows(0, -1, frontPageDocument.leftMargin(), pageSize.getHeight() - 575, frontPageWriter.getDirectContent());
            projectInfo.setSpacingAfter(10);
        } catch (IOException e) {
            Logger.error("Cannot find the required template: " + frontPageTemplate);
            e.printStackTrace();
        }
    }

    @Override
    protected void printTocTitle(Toc tocDocument) throws com.lowagie.text.DocumentException {
        Paragraph tocTitle = new Paragraph(super.getTextProperty("extendedpdf.main.table.of.contents"), Style.TOC_TITLE_FONT);
        tocTitle.setAlignment(Element.ALIGN_CENTER);
        tocDocument.getTocDocument().add(tocTitle);
        tocDocument.getTocDocument().add(Chunk.NEWLINE);
    }

    @Override
    protected void printPdfBody(Document document) throws DocumentException, IOException,
            org.dom4j.DocumentException, ReportException {
        for (String chapterName : getConfigProperty("chapters").split(",")) {
            printChapter(document, chapterName);
        }
    }

    private void printChapter(Document document, String chapterName)
            throws DocumentException, IOException {
        Paragraph title = new Paragraph(getTextProperty("extendedpdf." + chapterName), Style.CHAPTER_FONT);
        ChapterAutoNumber chapter = new ChapterAutoNumber(title);
        chapter.setTriggerNewPage(true);
        chapter.add(new Paragraph(getTextProperty("extendedpdf.misc.text"), Style.NORMAL_FONT));
        chapter.add(Chunk.NEWLINE);
        for (String sectionName : getConfigProperty(chapterName + ".sections").split(",")) {
            printChapterSection(chapter, sectionName);
        }
        document.add(chapter);
    }

    private void printChapterSection(ChapterAutoNumber chapter, String sectionName)
            throws DocumentException, IOException {
        String sectionDisplayName = getTextProperty("extendedpdf." + sectionName);
        String imagePath = screenshotsDir + File.separator + sectionName + ".png";
        File file = new File(imagePath);
        if(file.exists() && !file.isDirectory()) {
            Section section = chapter.addSection(new Paragraph(sectionDisplayName, Style.TITLE_FONT));
            Image image = Image.getInstance(imagePath);
            section.add(image);
            section.add(Chunk.NEWLINE);
        }
    }
}
