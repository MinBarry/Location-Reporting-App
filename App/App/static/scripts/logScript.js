// AIzaSyBbVKx_ahhNY0PBFheQ_dH6XNssuk78zqk
var map
var marker

// Google map initilazation
function initMap() {
    var uluru = { lat: -25.363, lng: 131.044 };
    map = new google.maps.Map(document.getElementById('map'), {
        zoom: 4,
        center: uluru
    });
    marker = new google.maps.Marker({
        position: uluru,
        map: map
    });
    $("#map").hide()
}

$(function () {
     // Request report list
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
                    "<tr hidden class='reportDetail'><div class='row'>" +
                    "<td colspan='2'><div class='reportImage'></div></td>" +
                    "<td colspan='2'><div class='reportMap' ></div></td>" +
                    "<td colspan='2'><div class='reportInfo'></div></td>" +
                    "</div></tr >"
                );

            });
            // Show report details 
            $(".singleReport").click(function () {
                $(".reportDetail").hide()
                $(this).next().show();
                $(this).next().find(".reportMap").append(map.getDiv())
                $("#map").show()
            });
        }
    );

});