$(function () {
     $.getJSON($SCRIPT_ROOT + '/api/reports',
        function (data, status_code) {
            reports = data.reports
            $.each(reports, function (i, report) {
                $("#reportsView").append(
                    "<tr class='singleReport' id=" + report.id + ">" +
                    "<td>" + report.id + "</td>"+
                    "<td>" + report.username + "</td>"+
                    "<td>" + report.location + "</td>"+
                    "<td>" + report.date + "</td>"+
                    "<td>" + report.type + "</td>"+
                    "<td>" + report.description + "</td></tr>"+
                    "<tr hidden class='reportDetail'><td colspan='6'><div class='row'>" +
                    "<div class='col-sm'></div>" +
                    "<div class='col-sm'></div>" +
                    "<div class='col-sm'></div>" +
                    "</div></td ></tr >"
                    );
            });
            $(".singleReport").click(function () {
                $(".reportDetail").hide()
                $(this).next().show();
            });
        }
    );

});