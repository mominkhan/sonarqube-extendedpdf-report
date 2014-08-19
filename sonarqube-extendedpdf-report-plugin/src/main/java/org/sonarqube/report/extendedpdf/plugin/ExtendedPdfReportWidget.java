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

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWidget;

/**
 * Created by Momin.Khan
 * {@inheritDoc}
 */
public final class ExtendedPdfReportWidget extends AbstractRubyTemplate implements RubyRailsWidget {
    protected String getTemplatePath() {
        return "/org/sonarqube/report/extendedpdf/dashboard_widget.erb";
    }

    public String getId() {
        return "extendedpdf-report-widget";
    }

    public String getTitle() {
        return "Extended PDF report widget";
    }
}
