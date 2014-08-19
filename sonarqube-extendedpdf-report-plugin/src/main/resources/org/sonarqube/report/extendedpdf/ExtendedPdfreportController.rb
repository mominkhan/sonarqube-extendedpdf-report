require "base64"

class Api::ExtendedPdfreportController < ApplicationController
  def getReport
    project=Project.by_key(params[:resource])
    measure=project.last_snapshot.measure('extendedpdf-data')
    send_data(Base64.decode64(measure.data), :filename => "sonarqube-report.pdf", :type => "application/pdf")
  end
end