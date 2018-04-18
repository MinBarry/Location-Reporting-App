// TODO: change Google maps API Key: AIzaSyBbVKx_ahhNY0PBFheQ_dH6XNssuk78zqk

var map
var marker
var reportsList
var totalPages

///////////////////////////////////////////
// Displays reports summary list on page
///////////////////////////////////////////
function requestReportsList(page) {
    $(".pagination").empty()
    $("#reportsView").empty()
    $.getJSON($SCRIPT_ROOT + '/api/reports?perpage=20&page='+ page,
        function (data, status_code) {
            reportsList = data.reports
            totalPages = data.pages
            displayReports(reportsList)
            setPagination(totalPages, page)
            // Show report details when a report is clicked 
            $(".singleReport").click(function () {
                $(".reportDetail").hide()
                latlng = getLatLng($(this).attr("id"), reportsList)
                map.setCenter(latlng)
                marker.setPosition(latlng)
                console.log(map.center)
                $(this).next().show();
                $(this).next().find(".reportMap").append(map.getDiv())
                $("#map").show()
            });
        }
    );
}


///////////////////////////////////////////
// Displays reports summary list on page
///////////////////////////////////////////
function displayReports(reports) {
    $.each(reports, function (i, report) {
        user = requestUserInfo(report.user_id)
        $("#reportsView").append(
            "<tr class='singleReport' id=" + report.id + ">" +
            "<td>" + report.id + "</td>" +
            "<td>" + report.username + "</td>" +
            "<td>" + report.location + "</td>" +
            "<td>" + report.date + "</td>" +
            "<td>" + report.type + "</td>" +
            "<td>" + report.description + "</td></tr>" +
            "<tr hidden class='reportDetail'><div class='row'>" +
            "<td colspan='2'><div class='reportImage'><img src=" +
            report.image_path + " class='img - thumbnail' alt=" +
            report.image_path + "></div></td>" +
            "<td colspan='2'><div class='reportMap' ></div></td>" +
            "<td colspan='2'><div class='reportInfo'>" +
            "<h4>Report Address:</h4> <p>" + report.address + "</p>" +
            "<h4>User Full Name:</h4> <P>" + user.firstname + " " + user.lastname + "</p>" +
            "<h4>User Email:</h4><p>"+user.email+"</p></div ></td > " +
            "</div></tr >"
        );

    });
}

///////////////////////////////////////////
// Create pageination 
///////////////////////////////////////////
function setPagination(numPages, currentPage) {
    $(".pagination").empty()
    for (var i = 1; i <= numPages; i++) {
        if (currentPage == i) {
            $(".pagination").append("<li class='active'><a>" + i + "</a></li>")
        } else {
            $(".pagination").append("<li><a>" + i + "</a></li>")
        }              
    }
    $(".pagination").children().click(function () {
        console.log("clicked page num")
        requestReportsList($(this).text())
    })
}
///////////////////////////////////////////
// Requests user info from the server
///////////////////////////////////////////
function requestUserInfo(user_id) {
    var user = {firstname:"Jane",lastname:"Doe", email:"jane@host.com", phone:"664536373"}
    //TODO: get request to get user info
    return user
}

///////////////////////////////////////////
// Returns lat lng of given report id
///////////////////////////////////////////
function getLatLng(report_id, reports) {
    for (i = 0; i < reports.length; i++) {
        if (reports[i].id == report_id) {
            return { lat: reports[i].lat, lng: reports[i].lng }
        }
    }
    return { lat: 0, lng: 0 }
}

///////////////////////////////////////////
// Google map initilazation
///////////////////////////////////////////
function initMap() {
    var uluru = { lat: 0, lng: 0};
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
    requestReportsList(1)
});