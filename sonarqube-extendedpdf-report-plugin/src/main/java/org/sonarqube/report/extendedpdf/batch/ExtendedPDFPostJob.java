/*
 * SonarQube Extended-PDF Report (Sonar Plugin)
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
package org.sonarqube.report.extendedpdf.batch;

import org.apache.commons.codec.binary.Base64;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Project;
import org.sonarqube.report.extendedpdf.plugin.ExtendedReportDataMetric;

import java.io.*;

/**
 * Created by Momin.Khan
 */
public class ExtendedPDFPostJob implements PostJob, DependsUponMavenPlugin, CheckProject {
    public static final String SKIP_EXTENDED_PDF_KEY = "sonar.extendedpdf.skip";
    public static final boolean SKIP_EXTENDED_PDF_DEFAULT_VALUE = false;

    public static final String EXTENDED_REPORT_TYPE = "sonar.extendedpdf.report.type";
    public static final String EXTENDED_REPORT_TYPE_DEFAULT_VALUE = "overview";

    public static final String USERNAME = "sonar.login";
    public static final String USERNAME_DEFAULT_VALUE = "";

    public static final String PASSWORD = "sonar.password";
    public static final String PASSWORD_DEFAULT_VALUE = "";

    private ExtendedPDFMavenPluginHandler handler;

    public ExtendedPDFPostJob(ExtendedPDFMavenPluginHandler handler) {
        this.handler = handler;
    }

    public boolean shouldExecuteOnProject(Project project) {
        return !project.getConfiguration().getBoolean(SKIP_EXTENDED_PDF_KEY, SKIP_EXTENDED_PDF_DEFAULT_VALUE);
    }

    public void executeOn(Project project, SensorContext context) {
        Measure measure = new Measure(ExtendedReportDataMetric.EXTENDED_PDF_DATA);
        File[] targetFiles = project.getFileSystem().getBuildDir().listFiles();
        int i = 0;
        File pdf = null;
        while (i < targetFiles.length) {
            if (targetFiles[i].getName().equals(project.getArtifactId() + "-extended.pdf")) {
                pdf = targetFiles[i];
                break;
            }
            i++;
        }
        try {
            byte[] encoded = Base64.encodeBase64(loadFile(pdf));
            String data = new String(encoded);
            measure.setData(data);
            measure.setPersistenceMode(PersistenceMode.DATABASE);
            context.saveMeasure(measure);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MavenPluginHandler getMavenPluginHandler(Project project) {
        return handler;
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] barr = new byte[1024];
        while (true) {
            int r = in.read(barr);
            if (r <= 0) {
                break;
            }
            out.write(barr, 0, r);
        }
    }

    private byte[] loadFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copy(in, buffer);
            return buffer.toByteArray();
        } finally {
            in.close();
        }
    }
}
