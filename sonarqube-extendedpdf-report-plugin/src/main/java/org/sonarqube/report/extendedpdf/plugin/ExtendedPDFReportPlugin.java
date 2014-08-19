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
package org.sonarqube.report.extendedpdf.plugin;

import org.sonar.api.*;
import org.sonarqube.report.extendedpdf.batch.ExtendedPDFMavenPluginHandler;
import org.sonarqube.report.extendedpdf.batch.ExtendedPDFPostJob;
import org.sonarqube.report.extendedpdf.web.ExtendedReportWebService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Momin.Khan
 */
@Properties({
        @Property(
                key= ExtendedPDFPostJob.SKIP_EXTENDED_PDF_KEY,
                name="Skip",
                description = "Skip generation of PDF report.",
                defaultValue = "" + ExtendedPDFPostJob.SKIP_EXTENDED_PDF_DEFAULT_VALUE,
                global = true,
                project = true,
                module = false,
                type = PropertyType.BOOLEAN
        ),
        @Property(
                key=ExtendedPDFPostJob.EXTENDED_REPORT_TYPE,
                name="Type",
                description = "Report type.",
                defaultValue = ExtendedPDFPostJob.EXTENDED_REPORT_TYPE_DEFAULT_VALUE,
                global = true,
                project = true,
                module = false,
                type = PropertyType.SINGLE_SELECT_LIST,
                options = { "overview" }
        ),
        @Property(
                key=ExtendedPDFPostJob.USERNAME,
                name="Username",
                description = "Username for WS API access.",
                defaultValue = ExtendedPDFPostJob.USERNAME_DEFAULT_VALUE,
                global = true,
                project = true,
                module = false
        ),
        @Property(
                key=ExtendedPDFPostJob.PASSWORD,
                name="Password",
                description = "Password for WS API access.",
                defaultValue = ExtendedPDFPostJob.PASSWORD_DEFAULT_VALUE,
                global = true,
                project = true,
                module = false,
                type = PropertyType.PASSWORD
        )
})

public class ExtendedPDFReportPlugin implements Plugin {
    public static final String PLUGIN_KEY = "extendedpdf-report";

    public String getKey() {
        return PLUGIN_KEY;
    }

    public String getName() {
        return "Extended PDF Report";
    }

    public String getDescription() {
        return "Generate a PDF report that contains the information from Report Dashboard of a project.";
    }

    public List<Class< ? extends Extension>> getExtensions() {
        List<Class< ? extends Extension>> extensions = new ArrayList<Class< ? extends Extension>>();
        extensions.add(ExtendedPDFMavenPluginHandler.class);
        extensions.add(ExtendedPDFPostJob.class);
        extensions.add(ExtendedReportDataMetric.class);
        extensions.add(ExtendedReportWebService.class);
        extensions.add(ExtendedPdfReportWidget.class);
        return extensions;
    }
}
