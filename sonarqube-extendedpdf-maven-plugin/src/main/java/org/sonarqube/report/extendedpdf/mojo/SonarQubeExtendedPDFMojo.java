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
package org.sonarqube.report.extendedpdf.mojo;

import org.sonarqube.report.extendedpdf.OverviewPDFReporter;
import com.lowagie.text.DocumentException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonar.report.pdf.PDFReporter;
import org.sonar.report.pdf.entity.exception.ReportException;
import org.sonar.report.pdf.util.Credentials;
import org.sonar.report.pdf.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Momin.Khan
 * Generate a PDF report. WARNING, Sonar server must be started.
 *
 * @goal generate
 * @aggregator
 */
public class SonarQubeExtendedPDFMojo extends AbstractMojo {
    /**
     * Project build directory
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Maven project info.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Sonar Base URL.
     *
     * @parameter expression="${sonar.host.url}"
     * @optional
     */
    private String sonarHostUrl;

    /**
     * Branch to be used.
     *
     * @parameter expression="${branch}"
     * @optional
     */
    private String branch;

    /**
     * Branch to be used.
     *
     * @parameter expression="${sonar.branch}"
     * @optional
     */
    private String sonarBranch;

    /**
     * Type of report.
     *
     * @parameter expression="${sonar.extendedpdf.report.type}"
     * @optional
     */
    private String reportType;

    /**
     * Username to access WS API.
     *
     * @parameter expression="${sonar.login}"
     * @optional
     */
    private String username;

    /**
     * Password to access WS API.
     *
     * @parameter expression="${sonar.password}"
     * @optional
     */
    private String password;

    /**
     * Report Dashboard ID.
     *
     * @parameter expression="${sonar.extendedpdf.reportDashboardID}"
     * @optional
     */
    private String sonarReportDashboardID;

    /**
     * PhantomJS binary path.
     *
     * @parameter expression="${sonar.extendedpdf.phantomjs.path}"
     * @optional
     */
    private String phantomjsPath;

    public void execute() throws MojoExecutionException {
        Logger.setLog(getLog());

        Properties config = new Properties();
        Properties configLang = new Properties();

        try {
            config.load(this.getClass().getResourceAsStream("/extendedpdf-report.properties"));
            configLang.load(this.getClass().getResourceAsStream("/extendedpdf-report-texts-en.properties"));

            if (sonarHostUrl != null) {
                if (sonarHostUrl.endsWith("/")) {
                    sonarHostUrl = sonarHostUrl.substring(0, sonarHostUrl.length() - 1);
                }
                // note: put method overrides the value if the key already exists
                config.put("sonar.base.url", sonarHostUrl);
            } else {
                config.load(this.getClass().getResourceAsStream("/extendedpdf-report.properties"));
            }

            String sonarProjectId = project.getGroupId() + ":" + project.getArtifactId();
            if (branch != null) {
                sonarProjectId += ":" + branch;
                Logger.warn("Use of branch parameter is deprecated, use sonar.branch instead");
                Logger.info("Branch " + branch + " selected");
            } else if (sonarBranch != null) {
                sonarProjectId += ":" + sonarBranch;
                Logger.info("Branch " + sonarBranch + " selected");
            }

            PDFReporter reporter = null;
            if (reportType != null) {
                if (reportType.equals("overview")) {
                    Logger.info("Overview report type selected");
                    reporter = new OverviewPDFReporter(sonarProjectId,
                            config.getProperty("sonar.base.url"),
                            sonarReportDashboardID,
                            config, configLang, outputDirectory,
                            phantomjsPath);
                }
            } else {
                Logger.info("No report type provided. Default report selected (Overview)");
                reporter = new OverviewPDFReporter(sonarProjectId,
                        config.getProperty("sonar.base.url"),
                        sonarReportDashboardID,
                        config, configLang, outputDirectory,
                        phantomjsPath);
            }

            Credentials.setUsername(username);
            Credentials.setPassword(password);

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            ByteArrayOutputStream baos = reporter.getReport();

            File reportFile = new File(outputDirectory, project.getArtifactId() + "-extended.pdf");
            FileOutputStream fos = new FileOutputStream(reportFile);
            baos.writeTo(fos);
            fos.flush();
            fos.close();
            Logger.info("Extended PDF report generated (see " + project.getArtifactId() + "-extended.pdf in build output directory)");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            Logger.error("Problem generating Extended PDF file.");
            e.printStackTrace();
        } catch (org.dom4j.DocumentException e) {
            Logger.error("Problem parsing response data.");
            e.printStackTrace();
        } catch (ReportException e) {
            Logger.error("Internal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
